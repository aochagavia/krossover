import nl.ochagavia.krossover.codegen.CasingHelper
import kotlin.test.Test
import kotlin.test.assertEquals

class CasingHelperTests {
    @Test
    fun testSnakeCase() {
        assertEquals("get_token_type", CasingHelper.snakeCase("getTokenType"))
        assertEquals("to_json", CasingHelper.snakeCase("ToJson"))
        assertEquals("to_json", CasingHelper.snakeCase("toJson"))
        assertEquals("to_j_s_o_n", CasingHelper.snakeCase("toJSON"))
    }
}
