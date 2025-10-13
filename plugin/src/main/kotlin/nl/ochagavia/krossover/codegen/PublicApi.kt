package nl.ochagavia.krossover.codegen

import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.JvmLibrary
import nl.ochagavia.krossover.KotlinClass
import nl.ochagavia.krossover.KotlinEnum

class PublicApi(
    val classes: HashMap<ClassName, KotlinClass>,
    val enums: HashMap<ClassName, KotlinEnum>,
    val nestedClasses: HashMap<ClassName, ArrayList<ClassName>>,
    val classHierarchy: ClassHierarchy,
)

class ClassHierarchy {
    private val childMap: HashMap<ClassName, ArrayList<ClassName>>

    constructor(publicApi: JvmLibrary) {
        val map = hashMapOf<ClassName, ArrayList<ClassName>>()
        publicApi.classes.forEach { clazz ->
            val superclass = clazz.value.superclass ?: return@forEach
            val superclassDefinedByUs = publicApi.classes.containsKey(superclass.name)

            if (superclassDefinedByUs) {
                val childrenOfSuperclass = map.getOrPut(superclass.name) { arrayListOf() }
                childrenOfSuperclass.add(clazz.value.name)
            }
        }

        childMap = map
    }

    fun directChildren(className: ClassName): List<ClassName> {
        return childMap.get(className) ?: return emptyList()
    }

    fun allChildren(className: ClassName): List<ClassName> {
        val directChildren = childMap.get(className) ?: return emptyList()
        val transitiveChildren = directChildren.flatMap { allChildren(it) }
        return directChildren.plus(transitiveChildren)
    }

    fun hasChildren(className: ClassName): Boolean {
        val directChildren = childMap.get(className) ?: return false
        return directChildren.isNotEmpty()
    }
}
