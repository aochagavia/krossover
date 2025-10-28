@import nl.ochagavia.krossover.KotlinFunctionParam
@import nl.ochagavia.krossover.codegen.IdentHelper
@import nl.ochagavia.krossover.codegen.RustHelper
@import java.util.List

@param List<KotlinFunctionParam> functionParams
<%-------%>
@for(var p : functionParams)
${IdentHelper.snakeCase(p.getName())},
@endfor
