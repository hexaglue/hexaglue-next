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

import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.AnnotationValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.reference.CtFieldReference;

/**
 * Reads annotation uses with their attribute values fully typed.
 *
 * <p>Nothing is turned into text on the way out: a string stays a string, an enum constant keeps
 * its type and its name, a class literal stays a type reference, and a nested annotation stays an
 * annotation. Downstream rules read {@code @Table(name = "orders")} as a value, never by parsing a
 * rendering of it.</p>
 *
 * <p>Attribute values are compile-time constants by language rule, so constant expressions are
 * folded to the value they denote. A value that survives folding without matching any known shape
 * is left out of the map rather than degraded, and reported.</p>
 */
final class Annotations {

    private static final Logger LOG = LoggerFactory.getLogger(Annotations.class);

    private Annotations() {}

    /**
     * Reads the annotations of a declaration, in declaration order.
     *
     * @param annotations the parsed annotation uses
     * @return the model annotations
     */
    static List<Annotation> of(List<CtAnnotation<?>> annotations) {
        return annotations.stream().map(Annotations::of).toList();
    }

    /**
     * Reads one annotation use.
     *
     * @param annotation the parsed annotation use
     * @return the model annotation
     */
    static Annotation of(CtAnnotation<?> annotation) {
        String qualifiedName = annotation.getAnnotationType().getQualifiedName();
        Map<String, AnnotationValue> values = new TreeMap<>();
        annotation
                .getValues()
                .forEach((attribute, expression) -> valueOf(expression)
                        .ifPresentOrElse(
                                value -> values.put(attribute, value),
                                () -> LOG.warn(
                                        "Unreadable value for {}.{}, attribute left out", qualifiedName, attribute)));
        return Annotation.of(qualifiedName, values);
    }

    private static Optional<AnnotationValue> valueOf(CtExpression<?> expression) {
        if (expression == null) {
            return Optional.empty();
        }
        Optional<AnnotationValue> direct = readShape(expression);
        return direct.isPresent() ? direct : folded(expression);
    }

    private static Optional<AnnotationValue> readShape(CtExpression<?> expression) {
        if (expression instanceof CtLiteral<?> literal) {
            return literalOf(literal);
        }
        if (expression instanceof CtFieldRead<?> fieldRead) {
            return Optional.of(fieldValueOf(fieldRead));
        }
        if (expression instanceof CtTypeAccess<?> typeAccess) {
            return Optional.of(AnnotationValue.ofClass(TypeRefs.of(typeAccess.getAccessedType())));
        }
        if (expression instanceof CtAnnotation<?> nested) {
            return Optional.of(AnnotationValue.ofAnnotation(of(nested)));
        }
        if (expression instanceof CtNewArray<?> array) {
            return Optional.of(AnnotationValue.ofArray(array.getElements().stream()
                    .map(Annotations::valueOf)
                    .flatMap(Optional::stream)
                    .toList()));
        }
        return Optional.empty();
    }

    /**
     * Folds a constant expression — {@code 10 * 5}, {@code "a" + "b"} — into its value, then reads
     * the result. Returns empty when even the folded form is unreadable, which happens when the
     * expression leans on a constant the classpath does not carry.
     */
    private static Optional<AnnotationValue> folded(CtExpression<?> expression) {
        CtElement evaluated = expression.partiallyEvaluate();
        return evaluated instanceof CtExpression<?> folded ? readShape(folded) : Optional.empty();
    }

    private static Optional<AnnotationValue> literalOf(CtLiteral<?> literal) {
        Object value = literal.getValue();
        if (value instanceof String text) {
            return Optional.of(AnnotationValue.ofString(text));
        }
        if (value instanceof Boolean || value instanceof Character || value instanceof Number) {
            return Optional.of(AnnotationValue.ofPrimitive(value));
        }
        return Optional.empty();
    }

    /**
     * Reads a static field reference. A class literal — modelled by the parser as a read of the
     * synthetic {@code class} field — is a type reference. A resolved constant of another type
     * yields the value it holds. Anything else is an enum constant: the only other field a legal
     * annotation value can name, and the common reading when the classpath does not carry the
     * annotation's own types.
     */
    private static AnnotationValue fieldValueOf(CtFieldRead<?> fieldRead) {
        CtFieldReference<?> field = fieldRead.getVariable();
        if ("class".equals(field.getSimpleName()) && fieldRead.getTarget() instanceof CtTypeAccess<?> typeAccess) {
            return AnnotationValue.ofClass(TypeRefs.of(typeAccess.getAccessedType()));
        }
        Optional<AnnotationValue> constant = constantValueOf(field);
        if (constant.isPresent()) {
            return constant.get();
        }
        String declaringType = field.getDeclaringType() == null
                ? field.getType().getQualifiedName()
                : field.getDeclaringType().getQualifiedName();
        return AnnotationValue.ofEnum(declaringType, field.getSimpleName());
    }

    private static Optional<AnnotationValue> constantValueOf(CtFieldReference<?> field) {
        CtField<?> declaration = field.getFieldDeclaration();
        if (declaration == null || !(declaration.getDefaultExpression() instanceof CtLiteral<?> literal)) {
            return Optional.empty();
        }
        return literalOf(literal);
    }
}
