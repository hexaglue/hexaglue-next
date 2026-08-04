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

import io.hexaglue.model.EnumSets;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;

/**
 * Reads the members a type declares.
 *
 * <p>Declared members only: inherited members belong to the supertype closure. What a class gets
 * for having written no constructor is left out — it takes nothing and expresses no decision of the
 * author. A record is the other case: its header states its whole state, and the components, their
 * accessors and the canonical constructor are that statement written where the author put it.</p>
 *
 * <p>Enum constants are left out of the fields: they are the values of the type, not state it
 * holds, and the shape of an enum is already carried by its nature.</p>
 *
 * <p>Members arrive as facts. Semantic roles, the type a wrapper wraps and the element type of a
 * collection are conclusions the engine draws; the element type in particular is already readable
 * from the recursive type reference and is not restated here.</p>
 */
final class Members {

    // Ordered on qualified parameter types, not on the display signature: two overloads whose
    // parameters share a simple name would otherwise tie, and the parser hands executables over
    // in an unspecified order.
    private static final Comparator<Method> BY_METHOD_SIGNATURE =
            Comparator.comparing(Method::name).thenComparing(method -> qualifiedParameters(method.parameters()));
    private static final Comparator<Constructor> BY_CONSTRUCTOR_SIGNATURE =
            Comparator.comparing(constructor -> qualifiedParameters(constructor.parameters()));
    private static final Comparator<TypeRef> BY_DISPLAY = Comparator.comparing(TypeRef::toDisplayString);

    private final SourceLocations locations;

    Members(SourceLocations locations) {
        this.locations = locations;
    }

    /**
     * Reads the fields a type declares, in declaration order.
     *
     * @param type the parsed type
     * @return the declared fields
     */
    List<Field> fieldsOf(CtType<?> type) {
        return type.getFields().stream()
                .filter(field -> !(field instanceof CtEnumValue))
                .map(this::fieldOf)
                .toList();
    }

    /**
     * Reads the methods a type declares, ordered by signature.
     *
     * @param type the parsed type
     * @return the declared methods
     */
    List<Method> methodsOf(CtType<?> type, MethodBodies bodies) {
        return type.getMethods().stream()
                .filter(method -> declares(type, method.isImplicit()))
                .map(method -> methodOf(method, bodies))
                .sorted(BY_METHOD_SIGNATURE)
                .toList();
    }

    /**
     * Reads the constructors a type declares, ordered by signature.
     *
     * @param type the parsed type
     * @return the declared constructors, empty for a type that declares none
     */
    List<Constructor> constructorsOf(CtType<?> type) {
        if (!(type instanceof CtClass<?> declaration)) {
            return List.of();
        }
        return declaration.getConstructors().stream()
                .filter(constructor -> declares(type, constructor.isImplicit()))
                .map(this::constructorOf)
                .sorted(BY_CONSTRUCTOR_SIGNATURE)
                .toList();
    }

    /**
     * Whether a member the source did not write out is nonetheless part of what the type declares.
     *
     * <p>A record states its whole state in its header, and the language writes down the reader
     * and the builder of that state for it: those <em>are</em> the declaration, put where the
     * author put it. A class that wrote no constructor declares nothing by it — what it gets takes
     * nothing and says nothing — so that one stays out.</p>
     *
     * <p>The distinction matters beyond tidiness. Anything generated against a domain has to call
     * what the domain offers, and a record reported with no way in and no way out is a record
     * nothing can be written for.</p>
     */
    private static boolean declares(CtType<?> type, boolean implicit) {
        return !implicit || type instanceof CtRecord;
    }

    private Field fieldOf(CtField<?> field) {
        Field.Builder builder = Field.builder(field.getSimpleName(), TypeRefs.of(field.getType()))
                .modifiers(Modifiers.of(field.getModifiers()))
                .annotations(Annotations.of(field.getAnnotations()));
        Javadocs.of(field).ifPresent(builder::documentation);
        locations.of(field).ifPresent(builder::sourceLocation);
        return builder.build();
    }

    private Method methodOf(CtMethod<?> method, MethodBodies bodies) {
        Method.Builder builder = Method.builder(method.getSimpleName(), TypeRefs.of(method.getType()))
                .parameters(parametersOf(method))
                .modifiers(modifiersOf(method))
                .annotations(Annotations.of(method.getAnnotations()))
                .thrownExceptions(thrownBy(method));
        bodies.complexityOf(method).ifPresent(builder::cyclomaticComplexity);
        Javadocs.of(method).ifPresent(builder::documentation);
        locations.of(method).ifPresent(builder::sourceLocation);
        return builder.build();
    }

    private Constructor constructorOf(CtConstructor<?> constructor) {
        return new Constructor(
                parametersOf(constructor),
                Modifiers.of(constructor.getModifiers()),
                Annotations.of(constructor.getAnnotations()),
                Javadocs.of(constructor),
                thrownBy(constructor),
                locations.of(constructor));
    }

    private static String qualifiedParameters(List<Parameter> parameters) {
        return parameters.stream()
                .map(parameter -> parameter.type().toDisplayString())
                .collect(Collectors.joining(","));
    }

    private static List<Parameter> parametersOf(CtExecutable<?> executable) {
        return executable.getParameters().stream().map(Members::parameterOf).toList();
    }

    private static Parameter parameterOf(CtParameter<?> parameter) {
        return new Parameter(
                parameter.getSimpleName(),
                TypeRefs.of(parameter.getType()),
                Annotations.of(parameter.getAnnotations()));
    }

    /**
     * Reads the modifiers of a method. {@code default} is a property of an interface method for the
     * parser rather than a modifier of its own, so it is restored here.
     */
    private static Set<Modifier> modifiersOf(CtMethod<?> method) {
        Set<Modifier> declared = Modifiers.of(method.getModifiers());
        if (!method.isDefaultMethod()) {
            return declared;
        }
        Set<Modifier> withDefault = EnumSet.noneOf(Modifier.class);
        withDefault.addAll(declared);
        withDefault.add(Modifier.DEFAULT);
        return EnumSets.ordered(withDefault);
    }

    private static List<TypeRef> thrownBy(CtExecutable<?> executable) {
        return executable.getThrownTypes().stream()
                .map(TypeRefs::of)
                .sorted(BY_DISPLAY)
                .toList();
    }
}
