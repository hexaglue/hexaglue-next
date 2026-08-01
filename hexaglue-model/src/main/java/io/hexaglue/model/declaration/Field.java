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

package io.hexaglue.model.declaration;

import io.hexaglue.model.EnumSets;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A field declaration, with the semantic enrichments the engine derives from it.
 *
 * <p>The frontend fills the syntactic components and leaves {@code wrappedType},
 * {@code elementType} and {@code roles} empty; the engine fills them when the architectural model
 * is assembled: {@code wrappedType} for identifier wrappers ({@code OrderId} wrapping
 * {@code UUID}), {@code elementType} for collections ({@code List<Item>} carrying {@code Item}),
 * roles for the semantic reading of the field.</p>
 *
 * @param name the field name
 * @param type the declared field type
 * @param modifiers the field modifiers, iterated in natural order
 * @param annotations the annotations on this field, in declaration order
 * @param documentation the field's documentation, when present
 * @param wrappedType the wrapped type for identifier wrappers, when derived
 * @param elementType the element type for collections, when derived
 * @param roles the semantic roles of this field, iterated in natural order
 * @param sourceLocation the source location, when known
 * @since 7.0.0
 */
public record Field(
        String name,
        TypeRef type,
        Set<Modifier> modifiers,
        List<Annotation> annotations,
        Optional<String> documentation,
        Optional<TypeRef> wrappedType,
        Optional<TypeRef> elementType,
        Set<FieldRole> roles,
        Optional<SourceLocation> sourceLocation) {

    /**
     * Validates the name and defensively copies every collection.
     */
    public Field {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(modifiers, "modifiers must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        Objects.requireNonNull(documentation, "documentation must not be null");
        Objects.requireNonNull(wrappedType, "wrappedType must not be null");
        Objects.requireNonNull(elementType, "elementType must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        Objects.requireNonNull(sourceLocation, "sourceLocation must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        modifiers = EnumSets.ordered(modifiers);
        annotations = List.copyOf(annotations);
        roles = EnumSets.ordered(roles);
    }

    /**
     * Creates a field with the given name and type and no other information.
     *
     * @param name the field name
     * @param type the field type
     * @return a new Field
     */
    public static Field of(String name, TypeRef type) {
        return builder(name, type).build();
    }

    /**
     * Creates a builder for a field.
     *
     * @param name the field name
     * @param type the field type
     * @return a new builder
     */
    public static Builder builder(String name, TypeRef type) {
        return new Builder(name, type);
    }

    /**
     * Returns whether this field has the given role.
     *
     * @param role the role to check
     * @return true when the role is present
     */
    public boolean hasRole(FieldRole role) {
        return roles.contains(role);
    }

    /**
     * Returns whether this field carries the identity of its owner.
     *
     * @return true when the IDENTITY role is present
     */
    public boolean isIdentity() {
        return hasRole(FieldRole.IDENTITY);
    }

    /**
     * Returns whether this field is a collection.
     *
     * @return true when the COLLECTION role is present
     */
    public boolean isCollection() {
        return hasRole(FieldRole.COLLECTION);
    }

    /**
     * Returns whether this field carries the given annotation.
     *
     * @param qualifiedName the fully qualified annotation type name
     * @return true when present
     */
    public boolean hasAnnotation(String qualifiedName) {
        return annotations.stream().anyMatch(annotation -> annotation.is(qualifiedName));
    }

    /**
     * Builder for {@link Field} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final String name;
        private final TypeRef type;
        private Set<Modifier> modifiers = Set.of();
        private List<Annotation> annotations = List.of();
        private Optional<String> documentation = Optional.empty();
        private Optional<TypeRef> wrappedType = Optional.empty();
        private Optional<TypeRef> elementType = Optional.empty();
        private Set<FieldRole> roles = Set.of();
        private Optional<SourceLocation> sourceLocation = Optional.empty();

        private Builder(String name, TypeRef type) {
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.type = Objects.requireNonNull(type, "type must not be null");
        }

        /**
         * Sets the modifiers.
         *
         * @param modifiers the modifiers
         * @return this builder
         */
        public Builder modifiers(Set<Modifier> modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        /**
         * Sets the annotations.
         *
         * @param annotations the annotations
         * @return this builder
         */
        public Builder annotations(List<Annotation> annotations) {
            this.annotations = annotations;
            return this;
        }

        /**
         * Sets the documentation.
         *
         * @param documentation the documentation text
         * @return this builder
         */
        public Builder documentation(String documentation) {
            this.documentation = Optional.of(documentation);
            return this;
        }

        /**
         * Sets the wrapped type for identifier wrappers.
         *
         * @param wrappedType the wrapped type
         * @return this builder
         */
        public Builder wrappedType(TypeRef wrappedType) {
            this.wrappedType = Optional.of(wrappedType);
            return this;
        }

        /**
         * Sets the element type for collections.
         *
         * @param elementType the element type
         * @return this builder
         */
        public Builder elementType(TypeRef elementType) {
            this.elementType = Optional.of(elementType);
            return this;
        }

        /**
         * Sets the semantic roles.
         *
         * @param roles the roles
         * @return this builder
         */
        public Builder roles(Set<FieldRole> roles) {
            this.roles = roles;
            return this;
        }

        /**
         * Sets the source location.
         *
         * @param sourceLocation the source location
         * @return this builder
         */
        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }

        /**
         * Builds the field.
         *
         * @return a new Field
         */
        public Field build() {
            return new Field(
                    name, type, modifiers, annotations, documentation, wrappedType, elementType, roles, sourceLocation);
        }
    }
}
