@import nl.ochagavia.krossover.KotlinFunctionParam
@import nl.ochagavia.krossover.codegen.CasingHelper
@import nl.ochagavia.krossover.codegen.IndentedContent
@import nl.ochagavia.krossover.codegen.PublicApi
@import nl.ochagavia.krossover.codegen.PythonHelper
@import java.util.List

@param PublicApi publicApi
@param List<KotlinFunctionParam> functionParams
<%-------%>
@if(functionParams.isEmpty()) 
[]
@else
[
${new IndentedContent(@`
@for(var p : functionParams)
${PythonHelper.castParam(publicApi, CasingHelper.snakeCase(p))},
@endfor
`)}
]
@endif
