@import nl.ochagavia.krossover.KotlinClass
@import nl.ochagavia.krossover.codegen.*

@param PublicApi publicApi
@param KotlinClass clazz
@if(clazz.isSealed())
<%--Sealed classes get translated as Rust enums--%>
@template.rust.enumDefinition(publicApi = publicApi, clazz = clazz)
@else
@template.rust.structDefinition(publicApi = publicApi, clazz = clazz)
@endif