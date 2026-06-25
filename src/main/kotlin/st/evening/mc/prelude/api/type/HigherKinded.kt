package st.evening.mc.prelude.api.type

// see "Lightweight Higher-Kinded Polymorphism" by Jeremy Yallop & Leo White (2014)
// https://doi.org/10.1007/978-3-319-07151-0_8

interface TT

interface Ap<F : TT, A>
