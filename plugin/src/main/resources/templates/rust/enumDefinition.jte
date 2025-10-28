@import nl.ochagavia.krossover.KotlinClass
@import nl.ochagavia.krossover.codegen.*

@param PublicApi publicApi
@param KotlinClass clazz
!{var enumName = RustHelper.classDefName(clazz.getName());}
${RustHelper.formatDocString("", clazz.getDocString())}
#[derive(Clone)]
pub enum ${enumName} {
@for(var variant : clazz.getSealedSubclasses())
${@`    ${variant.unqualifiedName()}(${RustHelper.classDefName(variant)}),`}
@endfor
}

@for(var variant : clazz.getSealedSubclasses())
@template.rust.classDefinition(publicApi = publicApi, clazz = publicApi.getClasses().get(variant))
@endfor

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