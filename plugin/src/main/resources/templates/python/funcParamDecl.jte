@import nl.ochagavia.krossover.KotlinFunctionParam
@import nl.ochagavia.krossover.codegen.CasingHelper
@import java.util.List

@param List<KotlinFunctionParam> funcParams
<%-------%>
@for(var param : funcParams)
${@`    ${CasingHelper.snakeCase(param.getName())},`}
@endfor