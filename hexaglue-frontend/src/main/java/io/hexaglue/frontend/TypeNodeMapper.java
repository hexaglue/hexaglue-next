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

import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.code.TypeNode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtSealable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * Reads a parsed type declaration into a {@link TypeNode}.
 *
 * <p>Declaration-level facts only: what the type is, what it extends and implements, what it
 * permits, where it is written. Reference collections arrive from the parser unordered, so they
 * are sorted here — the code model is read by rules whose conclusions must not depend on parse
 * order.</p>
 */
final class TypeNodeMapper {

    /** Supertypes the language implies and the source never writes. */
    private static final Set<String> IMPLICIT_SUPERTYPES =
            Set.of("java.lang.Object", "java.lang.Record", "java.lang.Enum");

    private static final Comparator<TypeRef> BY_DISPLAY = Comparator.comparing(TypeRef::toDisplayString);

    private final SourceLocations locations;
    private final Members members;

    TypeNodeMapper(SourceLocations locations) {
        this.locations = locations;
        this.members = new Members(locations);
    }

    /**
     * Reads one parsed type.
     *
     * @param type the parsed type declaration
     * @return the corresponding code model node
     */
    TypeNode map(CtType<?> type) {
        TypeNode.Builder builder = TypeNode.builder(idOf(type), natureOf(type))
                .modifiers(Modifiers.of(type.getModifiers()))
                .interfaces(sorted(type.getSuperInterfaces()))
                .permittedSubtypes(permittedSubtypesOf(type))
                .annotations(Annotations.of(type.getAnnotations()))
                .fields(members.fieldsOf(type))
                .methods(members.methodsOf(type))
                .constructors(members.constructorsOf(type));
        CtType<?> enclosing = type.getDeclaringType();
        if (enclosing != null) {
            builder.enclosingType(idOf(enclosing));
        }
        declaredSuperClassOf(type).ifPresent(builder::superClass);
        Javadocs.of(type).ifPresent(builder::documentation);
        locations.of(type).ifPresent(builder::sourceLocation);
        return builder.build();
    }

    /**
     * Returns the identity of a parsed type. Nested types are named with the binary {@code $}
     * convention, which the parser already uses.
     *
     * @param type the parsed type
     * @return the type identity
     */
    static TypeId idOf(CtType<?> type) {
        return TypeId.of(type.getQualifiedName());
    }

    private static TypeNature natureOf(CtType<?> type) {
        if (type instanceof CtAnnotationType<?>) {
            return TypeNature.ANNOTATION;
        }
        if (type instanceof CtInterface<?>) {
            return TypeNature.INTERFACE;
        }
        if (type instanceof CtEnum<?>) {
            return TypeNature.ENUM;
        }
        if (type instanceof CtRecord) {
            return TypeNature.RECORD;
        }
        return TypeNature.CLASS;
    }

    private static Optional<TypeRef> declaredSuperClassOf(CtType<?> type) {
        if (!(type instanceof CtClass<?> declaration)) {
            return Optional.empty();
        }
        CtTypeReference<?> superClass = declaration.getSuperclass();
        if (superClass == null || IMPLICIT_SUPERTYPES.contains(superClass.getQualifiedName())) {
            return Optional.empty();
        }
        return Optional.of(TypeRefs.of(superClass));
    }

    private static List<TypeRef> permittedSubtypesOf(CtType<?> type) {
        return type instanceof CtSealable sealable ? sorted(sealable.getPermittedTypes()) : List.of();
    }

    private static List<TypeRef> sorted(Set<CtTypeReference<?>> references) {
        return references.stream().map(TypeRefs::of).sorted(BY_DISPLAY).toList();
    }
}
