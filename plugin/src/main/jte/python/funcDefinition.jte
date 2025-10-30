@import nl.ochagavia.krossover.ClassName
@import nl.ochagavia.krossover.KotlinFunction
@import nl.ochagavia.krossover.codegen.PublicApi
@import nl.ochagavia.krossover.codegen.IndentedContent
@import nl.ochagavia.krossover.codegen.JniHelper
@import nl.ochagavia.krossover.codegen.PythonHelper
@import nl.ochagavia.krossover.codegen.IdentHelper

@param PublicApi publicApi
@param ClassName className
@param KotlinFunction function
@param String functionNameOverride = null

<%--Function header--%>
@if(function.isStatic())
@staticmethod
@endif
@if(functionNameOverride == null)
def ${IdentHelper.snakeCase(function.getName())}(
@else
def ${IdentHelper.snakeCase(functionNameOverride)}(
@endif
@if(!function.isStatic())
${@`    self,`}
@endif
@if(!function.getParams().isEmpty())
@template.python.funcParamDecl(funcParams = function.getParams())
@endif
)${PythonHelper.returnTypeAnnotation(function.getReturnType())}:
${PythonHelper.formatDocString("    ", function.getDocString())}
<%--Function body--%>
${new IndentedContent(@`@template.python.funcParamNullCheck(funcParams = function.getParams())`)}
    @if(function.isStatic())
    jni_ref = _access_static_field(b"${className.fullyQualifiedJniName()}", b"INSTANCE", b"L${className.fullyQualifiedJniName()};")
    @else
    jni_ref = self._jni_ref
    @endif

    result = _call_method(
        b"${className.fullyQualifiedJniName()}",
        jni_ref,
        b"${function.getName()}",
        b"${JniHelper.toJniFunctionSignature(function.getParams(), function.getReturnType())}",
        "${JniHelper.returnTypeToJniCallName(function.getReturnType())}",
${new IndentedContent(@`@template.python.funcCallParams(publicApi = publicApi, functionParams = function.getParams())`, 2)}
    )

    ${PythonHelper.returnStatement(publicApi, function.getReturnType())}
