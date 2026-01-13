@import nl.ochagavia.krossover.KotlinFunctionParam
@import nl.ochagavia.krossover.codegen.IdentHelper
@import java.util.List

@param List<KotlinFunctionParam> funcParams
@for(var param : funcParams)
@if(!param.getType().isNullable())
if ${IdentHelper.snakeCase(param.getName())} is None:
    raise ValueError("`${IdentHelper.snakeCase(param.getName())}` cannot be None")
@endif
@endfor