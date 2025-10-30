@import nl.ochagavia.krossover.KotlinFunctionParam
@import nl.ochagavia.krossover.codegen.IdentHelper
@import nl.ochagavia.krossover.codegen.PublicApi
@import nl.ochagavia.krossover.codegen.RustHelper
@import java.util.List

@param List<KotlinFunctionParam> funcParams
<%-------%>
@for(var param : funcParams)
${IdentHelper.snakeCase(param.getName())}: ${RustHelper.paramType(param.getType())},
@endfor