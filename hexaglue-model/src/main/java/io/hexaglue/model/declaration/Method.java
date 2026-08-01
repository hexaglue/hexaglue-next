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
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A method declaration.
 *
 * <p>The frontend fills the syntactic components; the engine fills {@code roles}. The cyclomatic
 * complexity is present only when method-body analysis was requested from the frontend.</p>
 *
 * @param name the method name
 * @param returnType the declared return type
 * @param parameters the parameters, in declaration order
 * @param modifiers the method modifiers, iterated in natural order
 * @param annotations the annotations on this method, in declaration order
 * @param documentation the method's documentation, when present
 * @param thrownExceptions the declared thrown exception types, in declaration order
 * @param roles the semantic roles of this method, iterated in natural order
 * @param cyclomaticComplexity the body complexity, when body analysis ran
 * @param sourceLocation the source location, when known
 * @since 7.0.0
 */
public record Method(
        String name,
        TypeRef returnType,
        List<Parameter> parameters,
        Set<Modifier> modifiers,
        List<Annotation> annotations,
        Optional<String> documentation,
        List<TypeRef> thrownExceptions,
        Set<MethodRole> roles,
        OptionalInt cyclomaticComplexity,
        Optional<SourceLocation> sourceLocation) {

    /**
     * Validates the name and defensively copies every collection.
     */
    public Method {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(returnType, "returnType must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(modifiers, "modifiers must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        Objects.requireNonNull(documentation, "documentation must not be null");
        Objects.requireNonNull(thrownExceptions, "thrownExceptions must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        Objects.requireNonNull(cyclomaticComplexity, "cyclomaticComplexity must not be null");
        Objects.requireNonNull(sourceLocation, "sourceLocation must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        parameters = List.copyOf(parameters);
        modifiers = EnumSets.ordered(modifiers);
        annotations = List.copyOf(annotations);
        thrownExceptions = List.copyOf(thrownExceptions);
        roles = EnumSets.ordered(roles);
    }

    /**
     * Creates a method with the given name and return type and no other information.
     *
     * @param name the method name
     * @param returnType the return type
     * @return a new Method
     */
    public static Method of(String name, TypeRef returnType) {
        return builder(name, returnType).build();
    }

    /**
     * Creates a builder for a method.
     *
     * @param name the method name
     * @param returnType the return type
     * @return a new builder
     */
    public static Builder builder(String name, TypeRef returnType) {
        return new Builder(name, returnType);
    }

    /**
     * Returns whether this method has the given role.
     *
     * @param role the role to check
     * @return true when the role is present
     */
    public boolean hasRole(MethodRole role) {
        return roles.contains(role);
    }

    /**
     * Returns whether this method carries the given annotation.
     *
     * @param qualifiedName the fully qualified annotation type name
     * @return true when present
     */
    public boolean hasAnnotation(String qualifiedName) {
        return annotations.stream().anyMatch(annotation -> annotation.is(qualifiedName));
    }

    /**
     * Returns a compact display signature: the name and the parameter type simple names.
     *
     * @return the signature (e.g. {@code findById(OrderId)})
     */
    public String signature() {
        String parameterTypes = parameters.stream()
                .map(parameter -> parameter.type().simpleName())
                .collect(Collectors.joining(", "));
        return name + "(" + parameterTypes + ")";
    }

    /**
     * Builder for {@link Method} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final String name;
        private final TypeRef returnType;
        private List<Parameter> parameters = List.of();
        private Set<Modifier> modifiers = Set.of();
        private List<Annotation> annotations = List.of();
        private Optional<String> documentation = Optional.empty();
        private List<TypeRef> thrownExceptions = List.of();
        private Set<MethodRole> roles = Set.of();
        private OptionalInt cyclomaticComplexity = OptionalInt.empty();
        private Optional<SourceLocation> sourceLocation = Optional.empty();

        private Builder(String name, TypeRef returnType) {
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.returnType = Objects.requireNonNull(returnType, "returnType must not be null");
        }

        /**
         * Sets the parameters.
         *
         * @param parameters the parameters, in declaration order
         * @return this builder
         */
        public Builder parameters(List<Parameter> parameters) {
            this.parameters = parameters;
            return this;
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
         * Sets the declared thrown exception types.
         *
         * @param thrownExceptions the exception types, in declaration order
         * @return this builder
         */
        public Builder thrownExceptions(List<TypeRef> thrownExceptions) {
            this.thrownExceptions = thrownExceptions;
            return this;
        }

        /**
         * Sets the semantic roles.
         *
         * @param roles the roles
         * @return this builder
         */
        public Builder roles(Set<MethodRole> roles) {
            this.roles = roles;
            return this;
        }

        /**
         * Sets the cyclomatic complexity measured on the body.
         *
         * @param cyclomaticComplexity the complexity value
         * @return this builder
         */
        public Builder cyclomaticComplexity(int cyclomaticComplexity) {
            this.cyclomaticComplexity = OptionalInt.of(cyclomaticComplexity);
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
         * Builds the method.
         *
         * @return a new Method
         */
        public Method build() {
            return new Method(
                    name,
                    returnType,
                    parameters,
                    modifiers,
                    annotations,
                    documentation,
                    thrownExceptions,
                    roles,
                    cyclomaticComplexity,
                    sourceLocation);
        }
    }
}
