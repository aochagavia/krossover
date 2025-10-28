package nl.ochagavia.krossover.codegen

import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.KotlinClass
import nl.ochagavia.krossover.KotlinEnum
import nl.ochagavia.krossover.gradle.ReturnTypeMapping

class PublicApi(
    val classes: HashMap<ClassName, KotlinClass>,
    val sealedSubclasses: Set<ClassName>,
    val enums: HashMap<ClassName, KotlinEnum>,
    val nestedClasses: HashMap<ClassName, ArrayList<ClassName>>,
    val classHierarchy: ClassHierarchy,
    val libName: String,
    val rustConfig: RustConfig,
)

class RustConfig(
    val jniSysModule: String,
    val returnTypeMappings: Map<ClassName, ReturnTypeMapping>,
)
