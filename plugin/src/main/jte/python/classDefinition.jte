@import nl.ochagavia.krossover.KotlinClass
@import nl.ochagavia.krossover.codegen.*
@import nl.ochagavia.krossover.KotlinType
@import nl.ochagavia.krossover.codegen.IndentedContent
@import nl.ochagavia.krossover.codegen.JniHelper
@import nl.ochagavia.krossover.codegen.IdentHelper
@import nl.ochagavia.krossover.codegen.PythonHelper
@import java.util.ArrayList

@param PublicApi publicApi
@param KotlinClass clazz

<%--Define the outer class--%>
class ${PythonHelper.classDefName(clazz.getName())}${PythonHelper.classDefInherits(clazz)}:
${PythonHelper.formatDocString("    ", clazz.getDocString())}
    _jni_ref: Any

    @for(var subclass : publicApi.getClassHierarchy().allChildren(clazz.getName()))
    ${subclass.unqualifiedName()}: TypeAlias
    @endfor

    @for(var constructor : clazz.getConstructors())
    def __init__(
        self,
${new IndentedContent(@`@template.python.funcParamDecl(funcParams = constructor.getParams())`)}
    ):
${new IndentedContent(@`@template.python.funcParamNullCheck(funcParams = constructor.getParams())`, 2)}
        self._jni_ref = _construct(
            b"${clazz.getName().fullyQualifiedJniName()}",
            b"${JniHelper.toJniFunctionSignature(constructor.getParams(), KotlinType.unit())}",
${new IndentedContent(@`@template.python.funcCallParams(publicApi = publicApi, functionParams = constructor.getParams())`, 3)}
        )
    @endfor

    def __eq__(self, other):
        return _call_method(b"java/lang/Object", self._jni_ref, b"equals", b"(Ljava/lang/Object;)Z", "BooleanMethod", [other._jni_ref])

    def __hash__(self):
        return _call_method(b"java/lang/Object", self._jni_ref, b"hashCode", b"()I", "IntMethod", [])

    @for(var property : clazz.getProperties())
    ${new IndentedContent(@`@template.python.funcDefinition(publicApi = publicApi, className = clazz.getName(), function = property.getGetter(), functionNameOverride = IdentHelper.trimGetter(property.getGetter().getName()))`)}
    @endfor

    @for(var function : clazz.getFunctions())
    ${new IndentedContent(@`@template.python.funcDefinition(publicApi = publicApi, className = clazz.getName(), function = function)`)}
    @endfor

    !{var subclasses = publicApi.getClassHierarchy().allChildren(clazz.getName());}
    @if(!subclasses.isEmpty())
    @staticmethod
    def _downcast(jni_ref) -> Any:
        match _jni_class_name(jni_ref):
        @for(var subclass : subclasses)
${@`
            case "${subclass.fullyQualifiedName()}":
                return _from_kotlin_object(${PythonHelper.classDefName(subclass)}, jni_ref)`}
        @endfor
    @endif
<%--Create user friendly name for this class, if it is nested--%>
@if(clazz.getName().isNestedClass())
${clazz.getName().unqualifiedNameWithNesting(".")} = ${PythonHelper.nestedClassDefName(clazz.getName())}
@endif
<%--Define nested classes, if any--%>
!{var nestedClasses = publicApi.getNestedClasses().getOrDefault(clazz.getName(), new ArrayList<>());}
@for(var nestedClass : nestedClasses)
@template.python.classDefinition(publicApi = publicApi, clazz = publicApi.getClasses().get(nestedClass))
@endfor