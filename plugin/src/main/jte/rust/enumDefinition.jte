@import nl.ochagavia.krossover.KotlinClass
@import nl.ochagavia.krossover.codegen.*

@param PublicApi publicApi
@param KotlinClass clazz
!{var enumName = clazz.getName().unqualifiedName();}
${RustHelper.formatDocString("", clazz.getDocString())}
#[derive(Clone)]
pub enum ${enumName} {
@for(var variant : clazz.getSealedSubclasses())
${@`    ${variant.unqualifiedName()}(${RustHelper.qualifiedClassName(variant)}),`}
@endfor
}

@if (clazz.getSealedSubclasses().length > 0)
pub mod ${IdentHelper.snakeCase(clazz.getName().unqualifiedName())} {
    use super::*;

@for(var variant : clazz.getSealedSubclasses())
${new IndentedContent(@`@template.rust.classDefinition(publicApi = publicApi, clazz = publicApi.getClasses().get(variant))`)}
@endfor
}
@endif

impl FromKotlinObject for ${enumName} {
    fn from_kotlin_object(obj: jobject) -> Self {
        match util::class_name(obj).as_str() {
@for(var variant : publicApi.getClassHierarchy().allSealedChildren(clazz.getName()))
${@`            "${variant.fullyQualifiedName()}" => ${RustHelper.enumConstructor(clazz.getName(), variant)},`}
@endfor
            _ => unreachable!(),
        }
    }
}

impl ToKotlinObject for ${enumName} {
    fn to_kotlin_object(&self) -> KotlinPtr {
        match self {
@for(var variant : clazz.getSealedSubclasses())
${@`            Self::${variant.unqualifiedName()}(inner) => inner.to_kotlin_object(),`}
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

@template.rust.structOrEnumImpls(publicApi = publicApi, clazz = clazz)