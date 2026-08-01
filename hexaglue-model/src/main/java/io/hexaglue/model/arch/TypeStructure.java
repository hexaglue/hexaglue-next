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

package io.hexaglue.model.arch;

import io.hexaglue.model.EnumSets;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.FieldRole;
import io.hexaglue.model.declaration.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The structural description an architectural type exposes to the plugins: the same declaration
 * records the code model uses, with the semantic roles filled by the engine.
 *
 * @param nature the Java form of the declaration
 * @param modifiers the type modifiers, iterated in natural order
 * @param documentation the type's documentation, when present
 * @param superClass the extended class, when any
 * @param interfaces the implemented or extended interfaces, in declaration order
 * @param permittedSubtypes the permitted subtypes of a sealed type, in declaration order
 * @param fields the fields, in declaration order, with their roles
 * @param methods the methods, in declaration order, with their roles
 * @param constructors the constructors, in declaration order
 * @param annotations the annotations, in declaration order, values fully typed
 * @param nestedTypes the nested types declared by this type, in declaration order
 * @param sourceLocation the source location, when known
 * @since 7.0.0
 */
public record TypeStructure(
        TypeNature nature,
        Set<Modifier> modifiers,
        Optional<String> documentation,
        Optional<TypeRef> superClass,
        List<TypeRef> interfaces,
        List<TypeRef> permittedSubtypes,
        List<Field> fields,
        List<Method> methods,
        List<Constructor> constructors,
        List<Annotation> annotations,
        List<TypeRef> nestedTypes,
        Optional<SourceLocation> sourceLocation) {

    /**
     * Defensively copies every collection.
     */
    public TypeStructure {
        Objects.requireNonNull(nature, "nature must not be null");
        Objects.requireNonNull(modifiers, "modifiers must not be null");
        Objects.requireNonNull(documentation, "documentation must not be null");
        Objects.requireNonNull(superClass, "superClass must not be null");
        Objects.requireNonNull(interfaces, "interfaces must not be null");
        Objects.requireNonNull(permittedSubtypes, "permittedSubtypes must not be null");
        Objects.requireNonNull(fields, "fields must not be null");
        Objects.requireNonNull(methods, "methods must not be null");
        Objects.requireNonNull(constructors, "constructors must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        Objects.requireNonNull(nestedTypes, "nestedTypes must not be null");
        Objects.requireNonNull(sourceLocation, "sourceLocation must not be null");
        modifiers = EnumSets.ordered(modifiers);
        interfaces = List.copyOf(interfaces);
        permittedSubtypes = List.copyOf(permittedSubtypes);
        fields = List.copyOf(fields);
        methods = List.copyOf(methods);
        constructors = List.copyOf(constructors);
        annotations = List.copyOf(annotations);
        nestedTypes = List.copyOf(nestedTypes);
    }

    /**
     * Creates a builder for a structure.
     *
     * @param nature the Java form of the declaration
     * @return a new builder
     */
    public static Builder builder(TypeNature nature) {
        return new Builder(nature);
    }

    /**
     * Returns whether this type is a plain class.
     *
     * @return true for CLASS
     */
    public boolean isClass() {
        return nature == TypeNature.CLASS;
    }

    /**
     * Returns whether this type is an interface.
     *
     * @return true for INTERFACE
     */
    public boolean isInterface() {
        return nature == TypeNature.INTERFACE;
    }

    /**
     * Returns whether this type is a record.
     *
     * @return true for RECORD
     */
    public boolean isRecord() {
        return nature == TypeNature.RECORD;
    }

    /**
     * Returns whether this type is sealed.
     *
     * @return true when the SEALED modifier is present
     */
    public boolean isSealed() {
        return modifiers.contains(Modifier.SEALED);
    }

    /**
     * Returns the field with the given name.
     *
     * @param name the field name
     * @return the field, or empty when absent
     */
    public Optional<Field> field(String name) {
        return fields.stream().filter(field -> field.name().equals(name)).findFirst();
    }

    /**
     * Returns the fields carrying the given role.
     *
     * @param role the role to filter by
     * @return the matching fields, in declaration order
     */
    public List<Field> fieldsWithRole(FieldRole role) {
        return fields.stream().filter(field -> field.hasRole(role)).toList();
    }

    /**
     * Builder for {@link TypeStructure} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final TypeNature nature;
        private Set<Modifier> modifiers = Set.of();
        private Optional<String> documentation = Optional.empty();
        private Optional<TypeRef> superClass = Optional.empty();
        private List<TypeRef> interfaces = List.of();
        private List<TypeRef> permittedSubtypes = List.of();
        private List<Field> fields = List.of();
        private List<Method> methods = List.of();
        private List<Constructor> constructors = List.of();
        private List<Annotation> annotations = List.of();
        private List<TypeRef> nestedTypes = List.of();
        private Optional<SourceLocation> sourceLocation = Optional.empty();

        private Builder(TypeNature nature) {
            this.nature = Objects.requireNonNull(nature, "nature must not be null");
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
         * Sets the interfaces.
         *
         * @param interfaces the interface references, in declaration order
         * @return this builder
         */
        public Builder interfaces(List<TypeRef> interfaces) {
            this.interfaces = interfaces;
            return this;
        }

        /**
         * Sets the permitted subtypes of a sealed type.
         *
         * @param permittedSubtypes the permitted subtype references
         * @return this builder
         */
        public Builder permittedSubtypes(List<TypeRef> permittedSubtypes) {
            this.permittedSubtypes = permittedSubtypes;
            return this;
        }

        /**
         * Sets the fields.
         *
         * @param fields the fields, in declaration order
         * @return this builder
         */
        public Builder fields(List<Field> fields) {
            this.fields = fields;
            return this;
        }

        /**
         * Sets the methods.
         *
         * @param methods the methods, in declaration order
         * @return this builder
         */
        public Builder methods(List<Method> methods) {
            this.methods = methods;
            return this;
        }

        /**
         * Sets the constructors.
         *
         * @param constructors the constructors, in declaration order
         * @return this builder
         */
        public Builder constructors(List<Constructor> constructors) {
            this.constructors = constructors;
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
         * Sets the nested types.
         *
         * @param nestedTypes the nested type references, in declaration order
         * @return this builder
         */
        public Builder nestedTypes(List<TypeRef> nestedTypes) {
            this.nestedTypes = nestedTypes;
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
         * Builds the structure.
         *
         * @return a new TypeStructure
         */
        public TypeStructure build() {
            return new TypeStructure(
                    nature,
                    modifiers,
                    documentation,
                    superClass,
                    interfaces,
                    permittedSubtypes,
                    fields,
                    methods,
                    constructors,
                    annotations,
                    nestedTypes,
                    sourceLocation);
        }
    }
}
