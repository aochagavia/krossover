@import nl.ochagavia.krossover.codegen.PublicApi

@param PublicApi publicApi
#![allow(non_camel_case_types)]

#[macro_use]
mod util;
mod sys {
    pub use kson_sys::*;
}

use std::ffi::c_int;
use self::sys::jobject;
use self::util::{AsKotlinObject, FromKotlinObject, KotlinPtr, ToKotlinObject};

@for(var clazz : publicApi.getClasses().values())
<%--Sealed subclasses are transpiled by their parents, not at the top level--%>
@if(!publicApi.getSealedSubclasses().contains(clazz.getName()))
@template.rust.classDefinition(publicApi = publicApi, clazz = clazz)
@endif
@endfor

@for(var clazz : publicApi.getEnums().values())
@template.rust.cLikeEnumDefinition(clazz = clazz)
@endfor