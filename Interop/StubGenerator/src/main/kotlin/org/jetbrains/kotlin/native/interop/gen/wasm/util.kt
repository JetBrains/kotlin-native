package org.jetbrains.idl2k

fun <K, V> Map<K, List<V>>.reduceValues(reduce: (V, V) -> V = { a, b -> b }): Map<K, V> = mapValues { it.value.reduce(reduce) }

// These are copied from idl2k verbatim

fun merge(a: AttributeKind, b: AttributeKind): AttributeKind {
    if (a == b) {
        return a
    }

    if (a == AttributeKind.VAR || b == AttributeKind.VAR) {
        return AttributeKind.VAR
    }

    return a
}

fun merge(a: GenerateAttribute, b: GenerateAttribute): GenerateAttribute {
    require(a.name == b.name)

    val type = when {
        a.type.dropNullable() == b.type.dropNullable() -> a.type.withNullability(a.type.nullable || b.type.nullable)
        else -> DynamicType
    }

    return GenerateAttribute(
            a.name,
            type,
            a.initializer ?: b.initializer,
            a.getterSetterNoImpl || b.getterSetterNoImpl,
            merge(a.kind, b.kind),
            a.override,
            a.vararg,
            a.static,
            a.required || b.required
    )
}

