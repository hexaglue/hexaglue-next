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

package io.hexaglue.model.code;

import io.hexaglue.model.EnumSets;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A type of the code graph: a source type of the analyzed project — nested types included — or a
 * lightweight external stub for a referenced classpath type.
 *
 * <p>An external stub carries no members: it exists so that edges toward the classpath
 * ({@code extends JpaRepository<...>}, {@code @Entity}) have a target, and so that the supertype
 * closure can include classpath types. Construction enforces this: an external node with members
 * is rejected.</p>
 *
 * @param id the stable type identity
 * @param nature the Java form of the declaration
 * @param modifiers the type modifiers, iterated in natural order
 * @param external true for a classpath stub, false for an analyzed source type
 * @param enclosingType the enclosing type for nested types, when any
 * @param superClass the extended class, when any
 * @param interfaces the implemented (or extended, for interfaces) interfaces, in a stable order
 * @param permittedSubtypes the permitted subtypes of a sealed type, in a stable order
 * @param annotations the annotations on this type, in declaration order
 * @param fields the declared fields, in declaration order
 * @param methods the declared methods, in declaration order
 * @param constructors the declared constructors, in declaration order
 * @param documentation the type's documentation, when present
 * @param sourceLocation the source location, when known
 * @param moduleName the reactor module declaring this type, when known
 * @since 7.0.0
 */
public record TypeNode(
        TypeId id,
        TypeNature nature,
        Set<Modifier> modifiers,
        boolean external,
        Optional<TypeId> enclosingType,
        Optional<TypeRef> superClass,
        List<TypeRef> interfaces,
        List<TypeRef> permittedSubtypes,
        List<Annotation> annotations,
        List<Field> fields,
        List<Method> methods,
        List<Constructor> constructors,
        Optional<String> documentation,
        Optional<SourceLocation> sourceLocation,
        Optional<String> moduleName) {

    /**
     * Validates the invariants — an external stub carries no members — and defensively copies
     * every collection.
     */
    public TypeNode {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(nature, "nature must not be null");
        Objects.requireNonNull(modifiers, "modifiers must not be null");
        Objects.requireNonNull(enclosingType, "enclosingType must not be null");
        Objects.requireNonNull(superClass, "superClass must not be null");
        Objects.requireNonNull(interfaces, "interfaces must not be null");
        Objects.requireNonNull(permittedSubtypes, "permittedSubtypes must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        Objects.requireNonNull(fields, "fields must not be null");
        Objects.requireNonNull(methods, "methods must not be null");
        Objects.requireNonNull(constructors, "constructors must not be null");
        Objects.requireNonNull(documentation, "documentation must not be null");
        Objects.requireNonNull(sourceLocation, "sourceLocation must not be null");
        Objects.requireNonNull(moduleName, "moduleName must not be null");
        if (external && !(fields.isEmpty() && methods.isEmpty() && constructors.isEmpty())) {
            throw new IllegalArgumentException("external stub " + id + " must not carry members");
        }
        modifiers = EnumSets.ordered(modifiers);
        interfaces = List.copyOf(interfaces);
        permittedSubtypes = List.copyOf(permittedSubtypes);
        annotations = List.copyOf(annotations);
        fields = List.copyOf(fields);
        methods = List.copyOf(methods);
        constructors = List.copyOf(constructors);
    }

    /**
     * Creates a builder for an analyzed source type.
     *
     * @param id the type identity
     * @param nature the Java form
     * @return a new builder
     */
    public static Builder builder(TypeId id, TypeNature nature) {
        return new Builder(id, nature, false);
    }

    /**
     * Creates a lightweight stub for a referenced classpath type.
     *
     * @param id the type identity
     * @param nature the Java form, as far as it is known
     * @return a new external TypeNode without members
     */
    public static TypeNode externalStub(TypeId id, TypeNature nature) {
        return new Builder(id, nature, true).build();
    }

    /**
     * Returns whether this type is nested in another type.
     *
     * @return true when an enclosing type is present
     */
    public boolean isNested() {
        return enclosingType.isPresent();
    }

    /**
     * Returns whether this type carries the given annotation.
     *
     * @param qualifiedName the fully qualified annotation type name
     * @return true when present
     */
    public boolean hasAnnotation(String qualifiedName) {
        return annotations.stream().anyMatch(annotation -> annotation.is(qualifiedName));
    }

    /**
     * Returns this node as read from a named module of a reactor.
     *
     * <p>Which module a type came from is something only the reading knows: the same package can
     * be spread over several modules, and nothing in a type says which one it was compiled in. So
     * the reading stamps it, once, on the way out.</p>
     *
     * @param moduleName the module the type was read from
     * @return a copy carrying the module name
     */
    public TypeNode inModule(String moduleName) {
        Objects.requireNonNull(moduleName, "moduleName must not be null");
        return new TypeNode(
                id,
                nature,
                modifiers,
                external,
                enclosingType,
                superClass,
                interfaces,
                permittedSubtypes,
                annotations,
                fields,
                methods,
                constructors,
                documentation,
                sourceLocation,
                Optional.of(moduleName));
    }

    /**
     * Builder for {@link TypeNode} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final TypeId id;
        private final TypeNature nature;
        private final boolean external;
        private Set<Modifier> modifiers = Set.of();
        private Optional<TypeId> enclosingType = Optional.empty();
        private Optional<TypeRef> superClass = Optional.empty();
        private List<TypeRef> interfaces = List.of();
        private List<TypeRef> permittedSubtypes = List.of();
        private List<Annotation> annotations = List.of();
        private List<Field> fields = List.of();
        private List<Method> methods = List.of();
        private List<Constructor> constructors = List.of();
        private Optional<String> documentation = Optional.empty();
        private Optional<SourceLocation> sourceLocation = Optional.empty();
        private Optional<String> moduleName = Optional.empty();

        private Builder(TypeId id, TypeNature nature, boolean external) {
            this.id = Objects.requireNonNull(id, "id must not be null");
            this.nature = Objects.requireNonNull(nature, "nature must not be null");
            this.external = external;
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
         * Sets the enclosing type, for nested types.
         *
         * @param enclosingType the enclosing type id
         * @return this builder
         */
        public Builder enclosingType(TypeId enclosingType) {
            this.enclosingType = Optional.of(enclosingType);
            return this;
        }

        /**
         * Sets the extended class.
         *
         * @param superClass the superclass reference
         * @return this builder
         */
        public Builder superClass(TypeRef superClass) {
            this.superClass = Optional.of(superClass);
            return this;
        }

        /**
         * Sets the implemented or extended interfaces.
         *
         * @param interfaces the interface references, in a stable order
         * @return this builder
         */
        public Builder interfaces(List<TypeRef> interfaces) {
            this.interfaces = interfaces;
            return this;
        }

        /**
         * Sets the permitted subtypes of a sealed type.
         *
         * @param permittedSubtypes the permitted subtype references, in a stable order
         * @return this builder
         */
        public Builder permittedSubtypes(List<TypeRef> permittedSubtypes) {
            this.permittedSubtypes = permittedSubtypes;
            return this;
        }

        /**
         * Sets the annotations.
         *
         * @param annotations the annotations, in declaration order
         * @return this builder
         */
        public Builder annotations(List<Annotation> annotations) {
            this.annotations = annotations;
            return this;
        }

        /**
         * Sets the declared fields.
         *
         * @param fields the fields, in declaration order
         * @return this builder
         */
        public Builder fields(List<Field> fields) {
            this.fields = fields;
            return this;
        }

        /**
         * Sets the declared methods.
         *
         * @param methods the methods, in declaration order
         * @return this builder
         */
        public Builder methods(List<Method> methods) {
            this.methods = methods;
            return this;
        }

        /**
         * Sets the declared constructors.
         *
         * @param constructors the constructors, in declaration order
         * @return this builder
         */
        public Builder constructors(List<Constructor> constructors) {
            this.constructors = constructors;
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
         * Sets the declaring reactor module.
         *
         * @param moduleName the module name
         * @return this builder
         */
        public Builder moduleName(String moduleName) {
            this.moduleName = Optional.of(moduleName);
            return this;
        }

        /**
         * Builds the type node.
         *
         * @return a new TypeNode
         */
        public TypeNode build() {
            return new TypeNode(
                    id,
                    nature,
                    modifiers,
                    external,
                    enclosingType,
                    superClass,
                    interfaces,
                    permittedSubtypes,
                    annotations,
                    fields,
                    methods,
                    constructors,
                    documentation,
                    sourceLocation,
                    moduleName);
        }
    }
}
