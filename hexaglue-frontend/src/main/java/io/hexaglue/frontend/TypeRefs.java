/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.frontend;

import io.hexaglue.model.TypeRef;
import java.util.List;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtWildcardReference;

/**
 * Reads a parser type reference into the model's single recursive {@link TypeRef}.
 *
 * <p>Every syntactic shape keeps its identity: a wildcard is never flattened into a type
 * variable, an array keeps its dimensions, and type arguments are read recursively so that
 * {@code JpaRepository<Order, OrderId>} arrives whole.</p>
 */
final class TypeRefs {

    private TypeRefs() {}

    /**
     * Reads a type reference, recursively.
     *
     * @param reference the parser reference, possibly null for an absent declaration
     * @return the model reference; {@code java.lang.Object} for a null reference
     */
    static TypeRef of(CtTypeReference<?> reference) {
        if (reference == null) {
            return TypeRef.of(Object.class.getName());
        }
        if (reference instanceof CtArrayTypeReference<?> array) {
            return arrayOf(array);
        }
        if (reference instanceof CtWildcardReference wildcard) {
            return wildcardOf(wildcard);
        }
        if (reference instanceof CtTypeParameterReference) {
            return TypeRef.typeVariable(nameOf(reference));
        }
        String qualifiedName = nameOf(reference);
        List<TypeRef> arguments =
                reference.getActualTypeArguments().stream().map(TypeRefs::of).toList();
        return arguments.isEmpty()
                ? TypeRef.of(qualifiedName)
                : TypeRef.parameterized(qualifiedName, arguments.toArray(TypeRef[]::new));
    }

    private static TypeRef arrayOf(CtArrayTypeReference<?> array) {
        int dimensions = 0;
        CtTypeReference<?> component = array;
        while (component instanceof CtArrayTypeReference<?> nested) {
            dimensions++;
            component = nested.getComponentType();
        }
        return TypeRef.array(of(component), dimensions);
    }

    private static TypeRef wildcardOf(CtWildcardReference wildcard) {
        CtTypeReference<?> bound = wildcard.getBoundingType();
        if (bound == null || bound.isImplicit()) {
            return TypeRef.wildcard();
        }
        return wildcard.isUpper() ? TypeRef.wildcardExtends(of(bound)) : TypeRef.wildcardSuper(of(bound));
    }

    /**
     * Returns the qualified name of a reference, falling back to its simple name when the parser
     * could not resolve it. Tolerant parsing is what lets an incomplete classpath still yield
     * usable facts, so an unresolvable reference degrades to what the source text says rather
     * than failing the whole analysis.
     */
    private static String nameOf(CtTypeReference<?> reference) {
        String qualifiedName = reference.getQualifiedName();
        if (qualifiedName != null && !qualifiedName.isBlank()) {
            return qualifiedName;
        }
        String simpleName = reference.getSimpleName();
        return simpleName == null || simpleName.isBlank() ? Object.class.getName() : simpleName;
    }
}
