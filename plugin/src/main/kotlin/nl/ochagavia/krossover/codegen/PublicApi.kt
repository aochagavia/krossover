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
    val returnTypeMappings: ReturnTypeMappings,
)

class ReturnTypeMappings(
    val rust: Map<ClassName, ReturnTypeMapping>,
)
