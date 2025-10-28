package nl.ochagavia.krossover

import kotlinx.serialization.Serializable

@Serializable
class KotlinLibrary(
    val classes: HashMap<ClassName, KotlinClass>,
    val enums: HashMap<ClassName, KotlinEnum>,
    val nestedClasses: HashMap<ClassName, ArrayList<ClassName>>,
    val sealedSubclasses: HashSet<ClassName>,
    val externalTypes: List<KotlinType>,
)

@Serializable
class KotlinEnum(
    val name: ClassName,
    val entries: MutableList<KotlinEnumEntry>,
    val docString: String?,
)

@Serializable
class KotlinEnumEntry(
    val name: ClassName,
    val docString: String?,
)

@Serializable
class KotlinClass(
    val kind: KotlinClassKind,
    val name: ClassName,
    val sealedSubclasses: Array<ClassName>,
    val superclass: KotlinType?,
    val constructors: Array<KotlinConstructor>,
    val functions: Array<KotlinFunction>,
    val properties: Array<KotlinProperty>,
    val docString: String?,
) {
    fun isSealed(): Boolean = sealedSubclasses.isNotEmpty()
}

@Serializable
enum class KotlinClassKind {
    OBJECT,
    OTHER,
}

@Serializable
class KotlinProperty(
    val name: String,
    val getter: KotlinFunction,
)

@Serializable
class KotlinConstructor(
    val params: List<KotlinFunctionParam>,
    val docString: String?,
)

@Serializable
class KotlinFunction(
    val name: String,
    val kind: FunctionKind,
    val params: List<KotlinFunctionParam>,
    val returnType: KotlinType?,
    val docString: String?,
) {
    val isStatic = kind == FunctionKind.StaticTopLevel || kind == FunctionKind.StaticCompanion
}

@Serializable
class KotlinFunctionParam(
    val name: String,
    val type: KotlinType,
)

@Serializable
sealed class FunctionKind {
    @Serializable
    object StaticTopLevel : FunctionKind()

    @Serializable
    object StaticCompanion : FunctionKind()

    @Serializable
    object NonStatic : FunctionKind()
}
