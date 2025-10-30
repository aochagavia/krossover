@import nl.ochagavia.krossover.KotlinFunctionParam
@import nl.ochagavia.krossover.codegen.IdentHelper
@import nl.ochagavia.krossover.codegen.PythonHelper
@import java.util.List

@param List<KotlinFunctionParam> funcParams
<%-------%>
@for(var param : funcParams)
${@`    ${IdentHelper.snakeCase(param.getName())}: ${PythonHelper.typeAnnotation(param.getType())},`}
@endfor