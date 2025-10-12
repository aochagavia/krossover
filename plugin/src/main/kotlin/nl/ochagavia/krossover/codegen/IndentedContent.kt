package nl.ochagavia.krossover.codegen

import gg.jte.Content
import gg.jte.TemplateOutput
import gg.jte.output.StringOutput

class IndentedContent(
    val content: Content,
    val times: Int,
) : Content {
    constructor(content: Content) : this(content, 1) {}

    override fun writeTo(to: TemplateOutput?) {
        val tmpOutput = StringOutput()
        content.writeTo(tmpOutput)

        val unindented = tmpOutput.toString()
        if (!unindented.isBlank()) {
            to?.writeContent(indent(unindented))
        }
    }

    fun indent(s: String): String {
        // Indent after each new line
        val indent = "    ".repeat(times)
        val indented = s.replace(Regex("""\r\n|\n|\r"""), "\n$indent").trimEnd()

        // Don't forget to indent the first line
        return "$indent$indented"
    }
}
