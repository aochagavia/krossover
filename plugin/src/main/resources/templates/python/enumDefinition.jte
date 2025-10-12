@import gg.jte.support.ForSupport
@import nl.ochagavia.krossover.KotlinEnum
@import nl.ochagavia.krossover.codegen.IndentedContent
@import nl.ochagavia.krossover.codegen.PythonHelper
@param KotlinEnum enumClass

class ${enumClass.getName().unqualifiedName()}(Enum):
@if(enumClass.getDocString() != null)
${PythonHelper.formatDocString("    ", enumClass.getDocString())}
@endif

    def _to_kotlin_enum(self):
        match self:
        @for (var entry : enumClass.getEntries())
${@`            case ${entry.getName().unqualifiedNameWithNesting('.')}:
                return _access_static_field(b"${enumClass.getName().fullyQualifiedJniName()}", b"${entry.getName().unqualifiedName()}", b"L${enumClass.getName().fullyQualifiedJniName()};")`}
        @endfor

    @staticmethod
    def _from_kotlin_enum(jni_ref):
        index = _call_method(b"${enumClass.getName().fullyQualifiedJniName()}", jni_ref, b"ordinal", b"()I", "IntMethod", [])
        return ${enumClass.getName().unqualifiedName()}(index)

@for(var entry : ForSupport.of(enumClass.getEntries()))
${new IndentedContent(@`${entry.get().getName().unqualifiedName()} = ${entry.getIndex()}`)}
@endfor
