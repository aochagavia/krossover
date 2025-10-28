@import nl.ochagavia.krossover.KotlinClass
@import nl.ochagavia.krossover.codegen.*
@import nl.ochagavia.krossover.KotlinType
@import nl.ochagavia.krossover.codegen.IndentedContent
@import nl.ochagavia.krossover.codegen.JniHelper

@param PublicApi publicApi
@param KotlinClass clazz

!{var className = RustHelper.classDefName(clazz.getName());}
${RustHelper.formatDocString("", clazz.getDocString())}
#[derive(Clone)]
pub struct ${className} {
    kotlin_ptr: KotlinPtr,
}

impl FromKotlinObject for ${className} {
    fn from_kotlin_object(obj: self::sys::jobject) -> Self {
        let (env, _detach_guard) = util::attach_thread_to_java_vm();
        let kotlin_ptr = util::to_gc_global_ref(env, obj);
        Self { kotlin_ptr }
    }
}

impl ToKotlinObject for ${className} {
    fn to_kotlin_object(&self) -> KotlinPtr {
        self.kotlin_ptr.clone()
    }
}

impl AsKotlinObject for ${className} {
    fn as_kotlin_object(&self) -> self::sys::jobject {
        self.kotlin_ptr.inner.inner
    }
}

impl ${className} {
    @for(var constructor : clazz.getConstructors())
    pub fn new(
${new IndentedContent(@`@template.rust.funcParamDecl(funcParams = constructor.getParams())`, 2)}
    ) -> Self {
        let (env, _detach_guard) = util::attach_thread_to_java_vm();
        let class = util::get_class(env, c"${clazz.getName().fullyQualifiedJniName()}");
        let constructor = util::get_method(env, class.as_kotlin_object(), c"<init>", c"${JniHelper.toJniFunctionSignature(constructor.getParams(), KotlinType.unit())}");

${new IndentedContent(@`@template.rust.funcCallParamObjects(functionParams = constructor.getParams())`, 2)}

        let jobject = unsafe { (**env).NewObject.unwrap()(env, class.as_kotlin_object(), constructor,
${new IndentedContent(@`@template.rust.funcCallParams(functionParams = constructor.getParams())`, 3)}
        )};
        util::panic_upon_exception(env);
        Self {
            kotlin_ptr: util::to_gc_global_ref(env, jobject)
        }
    }
    @endfor
}

@template.rust.structOrEnumImpls(publicApi = publicApi, clazz = clazz)