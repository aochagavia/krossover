package nl.ochagavia.krossover.codegen

import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.KotlinClass
import nl.ochagavia.krossover.KotlinFunction
import nl.ochagavia.krossover.KotlinLibrary
import nl.ochagavia.krossover.KotlinProperty
import nl.ochagavia.krossover.KotlinType

class ClassHierarchy {
    private val classes: HashMap<ClassName, KotlinClass>
    private val childMap: HashMap<ClassName, ArrayList<ClassName>>

    constructor(publicApi: KotlinLibrary) {
        val map = hashMapOf<ClassName, ArrayList<ClassName>>()
        publicApi.classes.forEach { clazz ->
            val superclass = clazz.value.superclass ?: return@forEach
            val superclassDefinedByUs = publicApi.classes.containsKey(superclass.name)

            if (superclassDefinedByUs) {
                val childrenOfSuperclass = map.getOrPut(superclass.name) { arrayListOf() }
                childrenOfSuperclass.add(clazz.value.name)
            }
        }

        classes = publicApi.classes
        childMap = map
    }

    fun allChildren(className: ClassName): List<ClassName> {
        val directChildren = childMap.get(className) ?: return emptyList()
        val transitiveChildren = directChildren.flatMap { allChildren(it) }
        return directChildren.plus(transitiveChildren)
    }

    fun allSealedChildren(className: ClassName): List<ClassName> {
        val clazz = classes.get(className) ?: return emptyList()
        val directSealedChildren = clazz.sealedSubclasses.toList()
        val transitiveChildren = directSealedChildren.flatMap { allSealedChildren(it) }
        return directSealedChildren.plus(transitiveChildren)
    }

    fun hasChildren(className: ClassName): Boolean {
        val directChildren = childMap.get(className) ?: return false
        return directChildren.isNotEmpty()
    }

    fun nonOverriddenInheritedProperties(clazz: KotlinClass): List<Inherited<KotlinProperty>> {
        val alreadyDefined = clazz.properties.map { FunctionSignature(it) }.toCollection(HashSet())
        val inherited = mutableListOf<Inherited<KotlinProperty>>()
        walkSuperclasses(clazz) { superclass ->
            superclass.properties.forEach {
                val functionSignature = FunctionSignature(it)
                if (alreadyDefined.contains(functionSignature)) {
                    return@forEach
                }

                alreadyDefined.add(functionSignature)
                inherited.add(Inherited(it, superclass))
            }
        }

        return inherited
    }

    fun nonOverriddenInheritedFunctions(clazz: KotlinClass): List<Inherited<KotlinFunction>> {
        val alreadyDefined = clazz.functions.map { FunctionSignature(it) }.toCollection(HashSet())
        val inherited = mutableListOf<Inherited<KotlinFunction>>()
        walkSuperclasses(clazz) { superclass ->
            superclass.functions.forEach {
                val functionSignature = FunctionSignature(it)
                if (alreadyDefined.contains(functionSignature)) {
                    return@forEach
                }

                alreadyDefined.add(functionSignature)
                inherited.add(Inherited(it, superclass))
            }
        }

        return inherited
    }

    fun walkSuperclasses(
        clazz: KotlinClass,
        callback: (KotlinClass) -> Unit,
    ) {
        var superclassType = clazz.superclass
        while (superclassType != null) {
            val superclass = classes[superclassType.name]!!
            callback(superclass)

            // Move up the hierarchy
            superclassType = superclass.superclass
        }
    }
}

class Inherited<T>(
    val value: T,
    val sourceClass: KotlinClass,
)

data class FunctionSignature(
    val name: String,
    val parameters: List<KotlinType>,
    val returnType: KotlinType?,
) {
    constructor(function: KotlinFunction) : this(function.name, function.params.map { it.type }, function.returnType)
    constructor(property: KotlinProperty) : this(property.getter)
}
