import nl.ochagavia.krossover.ClassName
import nl.ochagavia.krossover.codegen.PythonHelper
import kotlin.test.Test
import kotlin.test.assertEquals

class PythonHelperTests {
    @Test
    fun testNestedClassDefName() {
        val class1 = ClassName.notNested("com.example.Outer\$Inner")
        assertEquals("_Outer_Inner", PythonHelper.nestedClassDefName(class1))

        val class2 = ClassName.notNested("com.example.Outer\$Inner1\$Inner2")
        assertEquals("_Outer_Inner1_Inner2", PythonHelper.nestedClassDefName(class2))
    }
}
