@import gg.jte.support.ForSupport
@import nl.ochagavia.krossover.KotlinEnum
@import nl.ochagavia.krossover.codegen.*

@param KotlinEnum clazz
!{var enumName = clazz.getName().unqualifiedName();}
${RustHelper.formatDocString("", clazz.getDocString())}
#[derive(Copy, Clone)]
pub enum ${enumName} {
@for(var variant : clazz.getEntries())
${@`    ${RustHelper.enumVariantName(variant.getName())},`}
@endfor
}

impl FromKotlinObject for ${enumName} {
    fn from_kotlin_object(obj: jobject) -> Self {
        match util::enum_ordinal(c"${clazz.getName().fullyQualifiedJniName()}", obj) {
@for(var variant : ForSupport.of(clazz.getEntries()))
${@`            ${variant.getIndex()} => ${enumName}::${RustHelper.enumVariantName(variant.get().getName())}`},
@endfor
            _ => unreachable!(),
        }
    }
}

impl ToKotlinObject for ${enumName} {
    fn to_kotlin_object(&self) -> KotlinPtr {
        match self {
@for(var variant : clazz.getEntries())
${@`            ${enumName}::${RustHelper.enumVariantName(variant.getName())} => util::access_static_field(c"${clazz.getName().fullyQualifiedJniName()}", c"${variant.getName().unqualifiedName()}", c"L${clazz.getName().fullyQualifiedJniName()};"),`}
@endfor
        }
    }
}

impl ${enumName} {
    pub fn name(self) -> String {
        let obj = self.to_kotlin_object();
        util::enum_name(&obj)
    }
}