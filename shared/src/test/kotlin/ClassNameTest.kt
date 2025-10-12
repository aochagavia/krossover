import nl.ochagavia.krossover.ClassName
import org.junit.Test
import kotlin.test.assertEquals

class ClassNameTest {
    @Test
    fun testNotNested() {
        val pairs =
            listOf(
                Pair("com.example.Foo", "com.example.Foo"),
                Pair("Foo", "Foo"),
            )

        pairs.forEach {
            assertEquals(it.second, ClassName.notNested(it.first).fullyQualifiedName())
        }
    }

    @Test
    fun testFromKsName() {
        val pairs =
            listOf(
                Triple("com.example", "com.example.Foo.Bar", $$"com.example.Foo$Bar"),
                Triple("com.example", "com.example.Foo.Bar.Baz", $$"com.example.Foo$Bar$Baz"),
                Triple("", "Foo.Bar.Baz", $$"Foo$Bar$Baz"),
            )

        pairs.forEach {
            assertEquals(it.third, ClassName.potentiallyNested(it.first, it.second).fullyQualifiedName())
        }
    }
}
