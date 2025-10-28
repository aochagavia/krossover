import nl.ochagavia.krossover.codegen.IdentHelper
import kotlin.test.Test
import kotlin.test.assertEquals

class IdentHelperTests {
    @Test
    fun testSnakeCase() {
        assertEquals("get_token_type", IdentHelper.snakeCase("getTokenType"))
        assertEquals("to_json", IdentHelper.snakeCase("ToJson"))
        assertEquals("to_json", IdentHelper.snakeCase("toJson"))
        assertEquals("to_j_s_o_n", IdentHelper.snakeCase("toJSON"))
    }

    @Test
    fun testPascalCase() {
        assertEquals("Plain", IdentHelper.pascalCase("PLAIN"))
        assertEquals("CurlyBraceL", IdentHelper.pascalCase("CURLY_BRACE_L"))
    }
}
