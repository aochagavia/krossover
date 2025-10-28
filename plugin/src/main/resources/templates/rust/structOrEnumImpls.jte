@import nl.ochagavia.krossover.KotlinClass
@import nl.ochagavia.krossover.KotlinType
@import nl.ochagavia.krossover.codegen.*
@import nl.ochagavia.krossover.codegen.IndentedContent

@param PublicApi publicApi
@param KotlinClass clazz

!{var className = RustHelper.classDefName(clazz.getName());}
impl ${className} {
    <%--Own functions--%>
    @for(var property : clazz.getProperties())
    ${new IndentedContent(@`@template.rust.funcDefinition(publicApi = publicApi, className = clazz.getName(), function = property.getGetter(), functionNameOverride = IdentHelper.trimGetter(property.getGetter().getName()))`)}
    @endfor

    @for(var function : clazz.getFunctions())
    ${new IndentedContent(@`@template.rust.funcDefinition(publicApi = publicApi, className = clazz.getName(), function = function)`)}
    @endfor

    <%--Inherited functions--%>
    @for(var property : publicApi.getClassHierarchy().nonOverriddenInheritedProperties(clazz))
    ${new IndentedContent(@`@template.rust.funcDefinition(publicApi = publicApi, className = property.getSourceClass().getName(), function = property.getValue().getGetter(), functionNameOverride = IdentHelper.trimGetter(property.getValue().getGetter().getName()))`)}
    @endfor

    @for(var function : publicApi.getClassHierarchy().nonOverriddenInheritedFunctions(clazz))
    ${new IndentedContent(@`@template.rust.funcDefinition(publicApi = publicApi, className = function.getSourceClass().getName(), function = function.getValue())`)}
    @endfor
}

impl std::fmt::Debug for ${className} {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let obj = self.to_kotlin_object();
        write!(f, "{}", util::call_to_string(c"${clazz.getName().fullyQualifiedJniName()}", &obj))
    }
}

impl Eq for ${className} {}
impl PartialEq for ${className} {
    fn eq(&self, other: &${className}) -> bool {
        util::equals(self.to_kotlin_object(), other.to_kotlin_object())
    }
}
impl std::hash::Hash for ${className} {
    fn hash<H>(&self, state: &mut H)
    where
        H: std::hash::Hasher,
    {
        util::apply_hash_code(self.to_kotlin_object(), state)
    }
}