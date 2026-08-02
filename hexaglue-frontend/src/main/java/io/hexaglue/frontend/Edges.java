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
import io.hexaglue.model.code.Edge;
import io.hexaglue.model.code.EdgeKind;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Derives the typed relations of the code graph from the declarations already read.
 *
 * <p>No relation is filtered by package. {@code extends JpaRepository<Order, OrderId>} is the
 * single most decisive line enterprise code writes — it establishes a persistence port, an
 * aggregate and an identifier at once — and it says nothing at all if edges toward the classpath
 * are dropped for pointing outside the analyzed packages.</p>
 *
 * <p>Every relation carries its provenance: the member it appears on, the parameter position, the
 * type-argument position. Nested arguments carry the position of the top-level argument they sit
 * under, which is what a rule reading {@code Repository<A, ID>} needs.</p>
 *
 * <p>A target that is not analyzed becomes an external stub. Its Java form is read from the
 * relation that names it: what a type implements is an interface, what annotates it is an
 * annotation type; anything else is recorded as a class for want of a better fact. Types with no
 * identity of their own — primitives, type variables, wildcards — are never targets, and an array
 * points at its component type.</p>
 */
final class Edges {

    /** Names the constructor an edge comes from; constructors have no name of their own. */
    private static final String CONSTRUCTOR = "<init>";

    private final Set<TypeId> analyzed;
    private final List<Edge> collected = new ArrayList<>();
    private final SortedMap<TypeId, TypeNature> stubs = new TreeMap<>();

    private Edges(Set<TypeId> analyzed) {
        this.analyzed = analyzed;
    }

    /**
     * Derives the relations of a set of analyzed types.
     *
     * @param nodes the analyzed type nodes, in identity order
     * @return the derived relations and the stubs they point at
     */
    static Edges from(List<TypeNode> nodes) {
        Edges derived = new Edges(nodes.stream().map(TypeNode::id).collect(Collectors.toUnmodifiableSet()));
        nodes.forEach(derived::collect);
        return derived;
    }

    /**
     * Returns the derived relations, in emission order.
     *
     * @return the immutable edge list
     */
    List<Edge> all() {
        return List.copyOf(collected);
    }

    /**
     * Returns the external stubs the relations point at, in identity order.
     *
     * @return the immutable stub list
     */
    List<TypeNode> stubs() {
        return stubs.entrySet().stream()
                .map(entry -> TypeNode.externalStub(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void collect(TypeNode node) {
        TypeId source = node.id();
        node.enclosingType().ifPresent(enclosing -> add(enclosing, EdgeKind.DECLARES, source, none(), noIndex()));
        node.superClass().ifPresent(superClass -> reference(source, EdgeKind.EXTENDS, superClass, none(), noIndex()));
        node.interfaces()
                .forEach(implemented -> reference(source, EdgeKind.IMPLEMENTS, implemented, none(), noIndex()));
        node.permittedSubtypes()
                .forEach(permitted -> reference(source, EdgeKind.PERMITS, permitted, none(), noIndex()));
        annotations(source, node.annotations(), none(), noIndex());
        node.fields().forEach(field -> {
            Optional<String> member = Optional.of(field.name());
            reference(source, EdgeKind.FIELD_TYPE, field.type(), member, noIndex());
            annotations(source, field.annotations(), member, noIndex());
        });
        node.methods().forEach(method -> {
            Optional<String> member = Optional.of(method.name());
            reference(source, EdgeKind.RETURN_TYPE, method.returnType(), member, noIndex());
            method.thrownExceptions()
                    .forEach(thrown -> reference(source, EdgeKind.THROWS_TYPE, thrown, member, noIndex()));
            parameters(source, member, method.parameters());
            annotations(source, method.annotations(), member, noIndex());
        });
        node.constructors().forEach(constructor -> {
            Optional<String> member = Optional.of(CONSTRUCTOR);
            constructor
                    .thrownExceptions()
                    .forEach(thrown -> reference(source, EdgeKind.THROWS_TYPE, thrown, member, noIndex()));
            parameters(source, member, constructor.parameters());
            annotations(source, constructor.annotations(), member, noIndex());
        });
    }

    private void parameters(TypeId source, Optional<String> member, List<Parameter> parameters) {
        for (int position = 0; position < parameters.size(); position++) {
            Parameter parameter = parameters.get(position);
            OptionalInt index = OptionalInt.of(position);
            reference(source, EdgeKind.PARAMETER_TYPE, parameter.type(), member, index);
            annotations(source, parameter.annotations(), member, index);
        }
    }

    private void annotations(
            TypeId source, List<Annotation> annotations, Optional<String> member, OptionalInt parameterIndex) {
        annotations.forEach(annotation ->
                add(source, EdgeKind.ANNOTATED_BY, TypeId.of(annotation.qualifiedName()), member, parameterIndex));
    }

    /**
     * Records a relation toward a declared type reference, then the relations toward every type
     * naming a type argument of it.
     */
    private void reference(
            TypeId source, EdgeKind kind, TypeRef reference, Optional<String> member, OptionalInt parameterIndex) {
        TypeRef head = component(reference);
        identityOf(head).ifPresent(target -> add(source, kind, target, member, parameterIndex));
        List<TypeRef> arguments = head.typeArguments();
        for (int position = 0; position < arguments.size(); position++) {
            typeArguments(source, arguments.get(position), position, member, parameterIndex);
        }
    }

    private void typeArguments(
            TypeId source, TypeRef argument, int position, Optional<String> member, OptionalInt parameterIndex) {
        TypeRef head = component(argument);
        identityOf(head)
                .ifPresent(target -> collected.add(new Edge(
                        source,
                        registered(target, EdgeKind.TYPE_ARGUMENT),
                        EdgeKind.TYPE_ARGUMENT,
                        member,
                        parameterIndex,
                        OptionalInt.of(position))));
        bounds(head).forEach(bound -> typeArguments(source, bound, position, member, parameterIndex));
        head.typeArguments().forEach(nested -> typeArguments(source, nested, position, member, parameterIndex));
    }

    private static List<TypeRef> bounds(TypeRef reference) {
        if (!(reference instanceof TypeRef.Wildcard wildcard)) {
            return List.of();
        }
        return wildcard.upperBound().or(wildcard::lowerBound).map(List::of).orElseGet(List::of);
    }

    private void add(TypeId source, EdgeKind kind, TypeId target, Optional<String> member, OptionalInt parameterIndex) {
        collected.add(new Edge(source, registered(target, kind), kind, member, parameterIndex, OptionalInt.empty()));
    }

    /**
     * Registers a target that is not analyzed as an external stub, keeping the most informative
     * Java form seen for it.
     */
    private TypeId registered(TypeId target, EdgeKind kind) {
        if (!analyzed.contains(target)) {
            stubs.merge(target, natureFor(kind), (known, derived) -> known == TypeNature.CLASS ? derived : known);
        }
        return target;
    }

    private static TypeNature natureFor(EdgeKind kind) {
        return switch (kind) {
            case IMPLEMENTS -> TypeNature.INTERFACE;
            case ANNOTATED_BY -> TypeNature.ANNOTATION;
            default -> TypeNature.CLASS;
        };
    }

    /** Unwraps an array to the type it holds: an array has no identity of its own. */
    private static TypeRef component(TypeRef reference) {
        return reference instanceof TypeRef.Array array ? component(array.component()) : reference;
    }

    /** Returns the identity of a reference, empty for anything that names no type. */
    private static Optional<TypeId> identityOf(TypeRef reference) {
        return reference instanceof TypeRef.Named named
                ? Optional.of(TypeId.of(named.qualifiedName()))
                : Optional.empty();
    }

    private static Optional<String> none() {
        return Optional.empty();
    }

    private static OptionalInt noIndex() {
        return OptionalInt.empty();
    }
}
