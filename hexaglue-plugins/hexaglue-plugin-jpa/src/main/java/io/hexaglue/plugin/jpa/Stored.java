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

package io.hexaglue.plugin.jpa;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.Identifier;
import io.hexaglue.model.declaration.Field;
import java.util.Objects;
import java.util.Optional;

/**
 * What a domain field becomes once it is stored.
 *
 * <p>Every answer comes from a verdict the engine already reached. What a field holds is read from
 * the model, not worked out again from its shape or its name: a value becomes an embeddable, a part
 * or an aggregate becomes the entity generated for it, and an identity becomes the single value it
 * is written around — because an aggregate identified by an {@code OrderId} is found by the
 * {@code UUID} inside it, and a column holding the wrapper is a column no query can match.</p>
 */
final class Stored {

    private final ArchModel model;
    private final JpaOptions options;

    Stored(ArchModel model, JpaOptions options) {
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    /**
     * Returns the Java type the given field is stored as.
     *
     * @param field the domain field
     * @return the type of the generated field
     */
    TypeName typeOf(Field field) {
        TypeRef element = field.elementType().orElse(field.type());
        TypeName stored = single(element);
        return field.isCollection() ? ParameterizedTypeName.get(ClassName.get("java.util", "List"), stored) : stored;
    }

    /**
     * Answers whether the given field is stored inside its owner rather than beside it. A value
     * lives in the same row as what holds it; anything with a life of its own does not.
     */
    boolean isEmbedded(Field field) {
        TypeRef held = field.elementType().orElse(field.type());
        return !field.isCollection()
                && !isOneOfAClosedSet(held)
                && kindOf(held).filter(ArchKind.VALUE_OBJECT::equals).isPresent();
    }

    /**
     * Answers whether the given field holds one of a closed set, which the provider keeps in the
     * column itself rather than spread over several.
     *
     * @param field the domain field
     * @return true when what it holds is an enum
     */
    boolean isOneOfAClosedSet(Field field) {
        return isOneOfAClosedSet(field.elementType().orElse(field.type()));
    }

    /**
     * Answers whether the given field holds something stored in a table of its own.
     *
     * @param field the domain field
     * @return true when what it holds is an aggregate or a part
     */
    boolean isRelation(Field field) {
        return kindOf(field.elementType().orElse(field.type()))
                .filter(kind -> kind == ArchKind.AGGREGATE_ROOT || kind == ArchKind.ENTITY)
                .isPresent();
    }

    private TypeName single(TypeRef element) {
        Optional<ArchKind> kind = kindOf(element);
        if (kind.isEmpty()) {
            return named(element);
        }
        return switch (kind.orElseThrow()) {
            case VALUE_OBJECT -> isOneOfAClosedSet(element) ? named(element) : embeddable(element);
            case IDENTIFIER -> unwrapped(element);
            case AGGREGATE_ROOT, ENTITY -> entity(element);
            default -> named(element);
        };
    }

    /**
     * An identity is stored as what it wraps, whether it is the identity of the type holding it or
     * a reference to another aggregate. One that wraps nothing the analysis could name is stored as
     * itself, and the compiler of the generated code will say so — which is better than a column
     * quietly holding the wrong thing.
     */
    private TypeName unwrapped(TypeRef element) {
        return model.type(TypeId.of(element.qualifiedName()))
                .filter(Identifier.class::isInstance)
                .map(Identifier.class::cast)
                .flatMap(Identifier::wrappedType)
                .map(Stored::named)
                .orElseGet(() -> named(element));
    }

    /**
     * Answers whether the value is one of a closed set the type itself lists.
     *
     * <p>Such a value holds no state to spread over columns: the provider stores it as itself, so
     * this backend writes it nothing — neither an embeddable with nothing in it, nor a mapper with
     * nothing to carry.</p>
     *
     * @param type the domain type
     * @return true when the sources declared it as an enum
     */
    boolean isOneOfAClosedSet(TypeRef type) {
        return model.type(TypeId.of(type.qualifiedName()))
                .filter(declared -> declared.structure().nature() == TypeNature.ENUM)
                .isPresent();
    }

    /** The entity generated for a domain type, in the same package as the type it stores. */
    TypeName entity(TypeRef type) {
        return ClassName.get(type.packageName(), options.entityFor(type.simpleName()));
    }

    /** The embeddable generated for a domain value, in the same package as the value it stores. */
    TypeName embeddable(TypeRef type) {
        return ClassName.get(type.packageName(), options.embeddableFor(type.simpleName()));
    }

    private Optional<ArchKind> kindOf(TypeRef type) {
        return model.type(TypeId.of(type.qualifiedName())).map(ArchType::kind);
    }

    /**
     * A reference as the sources wrote it. Primitives keep their own spelling; anything else is a
     * class the generated code refers to by name.
     */
    static TypeName named(TypeRef type) {
        return switch (type.qualifiedName()) {
            case "void" -> TypeName.VOID;
            case "boolean" -> TypeName.BOOLEAN;
            case "byte" -> TypeName.BYTE;
            case "short" -> TypeName.SHORT;
            case "int" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            case "char" -> TypeName.CHAR;
            case "float" -> TypeName.FLOAT;
            case "double" -> TypeName.DOUBLE;
            default -> withArguments(type);
        };
    }

    /**
     * A reference keeps what it was written with: a port answering {@code Optional<Order>} is
     * implemented by a method answering that and not a raw {@code Optional}, which would not
     * override it.
     */
    private static TypeName withArguments(TypeRef type) {
        ClassName raw = ClassName.bestGuess(type.qualifiedName());
        if (type.typeArguments().isEmpty()) {
            return raw;
        }
        return ParameterizedTypeName.get(
                raw, type.typeArguments().stream().map(Stored::named).toArray(TypeName[]::new));
    }
}
