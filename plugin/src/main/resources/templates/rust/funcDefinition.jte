@import nl.ochagavia.krossover.ClassName
@import nl.ochagavia.krossover.KotlinFunction
@import nl.ochagavia.krossover.codegen.*
@import nl.ochagavia.krossover.codegen.IndentedContent
@import nl.ochagavia.krossover.codegen.JniHelper
@import nl.ochagavia.krossover.codegen.RustHelper
@import nl.ochagavia.krossover.codegen.IdentHelper

@param PublicApi publicApi
@param ClassName className
@param KotlinFunction function
@param String functionNameOverride = null

<%--Function header--%>
!{var functionName = functionNameOverride == null ? function.getName() : functionNameOverride;}
${RustHelper.formatDocString("", function.getDocString())}
pub fn ${RustHelper.sanitizeIdentifier(IdentHelper.snakeCase(functionName))}(
@if(!function.isStatic())
${@`    &self,`}
@endif
@if(!function.getParams().isEmpty())
${new IndentedContent(@`@template.rust.funcParamDecl(funcParams = function.getParams())`)}
@endif
)${RustHelper.returnTypeAnnotation(publicApi.getReturnTypeMappings().getRust(), function.getReturnType())} {
<%--Function body--%>
    @if(function.isStatic())
    let self_ptr = util::access_static_field(c"${className.fullyQualifiedJniName()}", c"INSTANCE", c"L${className.fullyQualifiedJniName()};");
    @else
    let self_ptr = self.to_kotlin_object();
    @endif

    let self_obj = self_ptr.as_kotlin_object();
${new IndentedContent(@`@template.rust.funcCallParamObjects(functionParams = function.getParams())`, 1)}

    let (_, _detach_guard) = util::attach_thread_to_java_vm();
    let result = call_jvm_function!(
        util,
        c"${className.fullyQualifiedJniName()}",
        c"${function.getName()}",
        c"${JniHelper.toJniFunctionSignature(function.getParams(), function.getReturnType())}",
        Call${JniHelper.returnTypeToJniCallName(function.getReturnType())},
        self_obj,
${new IndentedContent(@`@template.rust.funcCallParams(functionParams = function.getParams())`, 2)}
    );

    ${RustHelper.returnStatement(publicApi.getReturnTypeMappings().getRust(), function.getReturnType())}
}
