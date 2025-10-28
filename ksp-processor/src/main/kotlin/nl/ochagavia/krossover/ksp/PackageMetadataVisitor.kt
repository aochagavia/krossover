package nl.ochagavia.krossover.ksp

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Visibility
import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.FunctionKind
import nl.ochagavia.krossover.JvmLibrary
import nl.ochagavia.krossover.KotlinClass
import nl.ochagavia.krossover.KotlinClassKind
import nl.ochagavia.krossover.KotlinConstructor
import nl.ochagavia.krossover.KotlinEnum
import nl.ochagavia.krossover.KotlinEnumEntry
import nl.ochagavia.krossover.KotlinFunction
import nl.ochagavia.krossover.KotlinFunctionParam
import nl.ochagavia.krossover.KotlinProperty
import nl.ochagavia.krossover.KotlinType
import java.util.*

class PackageMetadataVisitor {
    val pendingClassDecls: Queue<KSClassDeclaration> = LinkedList()
    val classes: HashMap<ClassName, KotlinClass> = HashMap()
    val enums: HashMap<ClassName, KotlinEnum> = HashMap()
    val nestedClasses: HashMap<ClassName, ArrayList<ClassName>> = HashMap()
    val sealedSubclasses: HashSet<ClassName> = HashSet()
    val seenExternalTypes: HashSet<KotlinType> = HashSet()
    val ignoredFunctions: HashSet<String> =
        HashSet(
            listOf(
                "component1",
                "component2",
                "component3",
                "component4",
                "component5",
                "component6",
                "component7",
                "component8",
                "component9",
                "copy",
                "equals",
                "hashCode",
            ),
        )

    val stackAllocatedTypes: HashSet<ClassName> =
        HashSet(
            listOf(
                ClassName.notNested("kotlin.Int"),
                ClassName.notNested("kotlin.Boolean"),
            ),
        )

    val allowedPackages: List<String>

    constructor(topLevelClassDecl: List<KSClassDeclaration>, allowedPackages: List<String>) {
        pendingClassDecls.addAll(topLevelClassDecl)
        this.allowedPackages = allowedPackages
    }

    fun getPackageMetadata(): JvmLibrary =
        JvmLibrary(
            classes,
            enums,
            nestedClasses,
            sealedSubclasses,
            seenExternalTypes.toList(),
        )

    // Returns false when there are no more classes to visit
    fun visitNextClass(): Boolean {
        val classDecl = pendingClassDecls.poll() ?: return false

        // Local declarations cannot be public
        if (classDecl.isLocal()) {
            return true
        }

        val className = ClassName.potentiallyNested(classDecl.packageName.asString(), classDecl.qualifiedName!!.asString())
        if (this.classes.containsKey(className) || this.enums.containsKey(className)) {
            // Already handled, move on to the next one
            return true
        }

        // Find the direct superclass of the class (if any)
        val superclass =
            classDecl.superTypes
                .flatMap {
                    val resolvedSuperTypeRaw = it.resolve()
                    val resolvedSuperType = toKotlinType(resolvedSuperTypeRaw)
                    if (resolvedSuperType.name == ClassName.any) {
                        return@flatMap emptySequence<KotlinType>()
                    }

                    val declaration = resolvedSuperTypeRaw.declaration
                    if (declaration is KSClassDeclaration && declaration.classKind != ClassKind.INTERFACE) {
                        this.visitType(resolvedSuperTypeRaw)
                        return@flatMap sequenceOf(resolvedSuperType)
                    }

                    return@flatMap emptySequence<KotlinType>()
                }.firstOrNull()

        val constructors = arrayListOf<KotlinConstructor>()
        val functions = arrayListOf<KotlinFunction>()
        val properties = arrayListOf<KotlinProperty>()
        classDecl.declarations.forEach {
            if (it is KSClassDeclaration) {
                if (it.isCompanionObject) {
                    // Static functions
                    it.getDeclaredFunctions().forEach { fnDecl ->
                        if (fnDecl.simpleName.asString() != "<init>") {
                            visitFunction(functions, fnDecl, FunctionKind.StaticCompanion)
                        }
                    }
                } else {
                    // Nested class declarations
                    this.pendingClassDecls.add(it)
                }
            }

            // Public functions
            if (it is KSFunctionDeclaration && it.getVisibility() == Visibility.PUBLIC) {
                if (it.isConstructor()) {
                    // Objects also have constructors, but those aren't exposed in the FFI (objects are singletons, so
                    // creation is handled by kotlin itself)
                    if (classDecl.classKind != ClassKind.OBJECT) {
                        constructors.add(
                            KotlinConstructor(
                                it.parameters.map { parameter ->
                                    KotlinFunctionParam(
                                        parameter.name!!.asString(),
                                        toKotlinType(parameter.type.resolve()),
                                    )
                                },
                                it.docString,
                            ),
                        )
                        it.parameters.forEach { param ->
                            this.visitType(param.type.resolve())
                        }
                    }
                } else {
                    val kind =
                        if (classDecl.classKind == ClassKind.OBJECT) {
                            FunctionKind.StaticTopLevel
                        } else {
                            FunctionKind.NonStatic
                        }
                    this.visitFunction(functions, it, kind)
                }
            }

            // Properties
            if (it is KSPropertyDeclaration && it.getVisibility() == Visibility.PUBLIC && it.origin != Origin.SYNTHETIC) {
                val type = it.type.resolve()
                val getter =
                    KotlinFunction(
                        "get${it.simpleName.asString().replaceFirstChar { c -> c.uppercase() }}",
                        FunctionKind.NonStatic,
                        arrayListOf(),
                        toKotlinType(type),
                        it.docString,
                    )
                properties.add(KotlinProperty(it.simpleName.asString(), getter))
                this.visitType(type)
            }
        }

        when (classDecl.classKind) {
            ClassKind.ENUM_CLASS -> {
                // An enum should have none of these
                assert(constructors.isEmpty())
                assert(functions.isEmpty())

                // Entries will be populated later (see `ClassKind.ENUM_ENTRY` branch of this `when`)
                val enumMetadata =
                    KotlinEnum(
                        className,
                        entries = mutableListOf(),
                        classDecl.docString,
                    )
                this.enums[className] = enumMetadata
            }
            ClassKind.ENUM_ENTRY -> {
                // An enum entry should have none of these
                assert(constructors.isEmpty())
                assert(functions.isEmpty())

                val classParent = classDecl.parentDeclaration
                if (classParent !is KSClassDeclaration) {
                    throw RuntimeException("parent of enum entry should always be a class declaration")
                }

                val enumName = ClassName.potentiallyNested(classParent.packageName.asString(), classParent.qualifiedName!!.asString())
                val enumMetadata = this.enums[enumName]!!

                val enumEntryName = ClassName.potentiallyNested(classDecl.packageName.asString(), classDecl.qualifiedName!!.asString())
                enumMetadata.entries.add(KotlinEnumEntry(enumEntryName, classDecl.docString))
            }
            else -> {
                val sealedSubclassesForThisClass = arrayListOf<ClassName>()
                classDecl.getSealedSubclasses().forEach {
                    val subclassName = ClassName.potentiallyNested(it.packageName.asString(), it.qualifiedName!!.asString())
                    sealedSubclassesForThisClass.add(subclassName)
                    sealedSubclasses.add(subclassName)
                }

                val classMetadata =
                    KotlinClass(
                        if (classDecl.classKind == ClassKind.OBJECT) {
                            KotlinClassKind.OBJECT
                        } else {
                            KotlinClassKind.OTHER
                        },
                        className,
                        sealedSubclassesForThisClass.toTypedArray(),
                        superclass,
                        constructors.toTypedArray(),
                        functions.toTypedArray(),
                        properties.toTypedArray(),
                        classDecl.docString,
                    )
                val classParent = classDecl.parentDeclaration
                if (classParent is KSClassDeclaration) {
                    val list =
                        this.nestedClasses.getOrPut(
                            ClassName.potentiallyNested(classParent.packageName.asString(), classParent.qualifiedName!!.asString()),
                        ) {
                            arrayListOf()
                        }
                    list.add(ClassName.potentiallyNested(classDecl.packageName.asString(), classDecl.qualifiedName!!.asString()))
                }
                this.classes[className] = classMetadata
            }
        }

        return true
    }

    fun visitFunction(
        functions: ArrayList<KotlinFunction>,
        function: KSFunctionDeclaration,
        kind: FunctionKind,
    ) {
        if (function.getVisibility() == Visibility.PUBLIC &&
            function.typeParameters.isEmpty() &&
            function.origin != Origin.SYNTHETIC &&
            !this.ignoredFunctions.contains(function.simpleName.asString())
        ) {
            functions.add(
                KotlinFunction(
                    function.simpleName.asString(),
                    kind,
                    function.parameters.map {
                        KotlinFunctionParam(
                            it.name!!.asString(),
                            toKotlinType(it.type.resolve()),
                        )
                    },
                    function.returnType?.resolve()?.let { toKotlinType(it) },
                    function.docString,
                ),
            )

            function.parameters.forEach {
                this.visitType(it.type.resolve())
            }

            function.returnType?.resolve()?.let { this.visitType(it) }
        }
    }

    private fun visitType(type: KSType) {
        val isOurs =
            this.allowedPackages.any {
                type.declaration.packageName
                    .asString()
                    .startsWith(it)
            }
        if (isOurs) {
            val typeDecl = type.declaration
            if (typeDecl is KSClassDeclaration) {
                this.pendingClassDecls.add(typeDecl)
            }
        } else {
            this.seenExternalTypes.add(toKotlinType(type))
        }

        // Also visit concrete types that instantiate generics
        type.arguments.forEach {
            visitType(it.type!!.resolve())
        }
    }

    fun toKotlinType(type: KSType?): KotlinType {
        if (type == null) {
            return KotlinType(ClassName.any, isNullable = true)
        }

        val typeDecl = type.declaration
        val name = ClassName.potentiallyNested(typeDecl.packageName.asString(), typeDecl.qualifiedName!!.asString())

        // Generics
        val params = mutableListOf<KotlinType>()
        type.arguments.forEach {
            params.add(toKotlinType(it.type?.resolve()))
        }

        val isStackAllocated = stackAllocatedTypes.contains(name)
        return KotlinType(name, type.nullability == Nullability.NULLABLE, params.toList(), isStackAllocated)
    }
}
