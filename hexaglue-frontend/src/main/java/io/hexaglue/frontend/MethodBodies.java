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
import io.hexaglue.model.code.MethodBodyFacts;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

/**
 * Reads what the bodies of a type do — what they call, what they create, how many decisions they
 * take — in a single traversal per body.
 *
 * <p>Bodies are the expensive part of an analysis and only some rules need them, so nothing is
 * read unless the capability was asked for. When it is, every question is answered by the same
 * pass: walking an AST once per body and once per question is the same work multiplied by the
 * number of questions.</p>
 */
final class MethodBodies {

    /** Names the body of a constructor, which has no name of its own. */
    static final String CONSTRUCTOR = "<init>";

    private final Map<CtExecutable<?>, Integer> complexities = new IdentityHashMap<>();
    private final List<MethodBodyFacts> facts = new ArrayList<>();

    private MethodBodies() {}

    /**
     * Reads the bodies a type declares.
     *
     * @param type the parsed type
     * @param enabled whether the method-bodies capability was requested
     * @return the facts read, empty when the capability was not requested
     */
    static MethodBodies of(CtType<?> type, boolean enabled) {
        MethodBodies bodies = new MethodBodies();
        if (!enabled) {
            return bodies;
        }
        TypeId declaring = TypeNodeMapper.idOf(type);
        type.getMethods().stream()
                .filter(method -> !method.isImplicit())
                .sorted(java.util.Comparator.comparing(CtMethod::getSimpleName))
                .forEach(method -> bodies.read(declaring, method.getSimpleName(), method));
        if (type instanceof CtClass<?> declaration) {
            declaration.getConstructors().stream()
                    .filter(constructor -> !constructor.isImplicit())
                    .sorted(java.util.Comparator.comparing(CtConstructor::getSignature))
                    .forEach(constructor -> bodies.read(declaring, CONSTRUCTOR, constructor));
        }
        return bodies;
    }

    /**
     * Returns how many decisions a body takes, empty when it has no body or was not read.
     *
     * @param executable the parsed method or constructor
     * @return the cyclomatic complexity
     */
    OptionalInt complexityOf(CtExecutable<?> executable) {
        Integer complexity = complexities.get(executable);
        return complexity == null ? OptionalInt.empty() : OptionalInt.of(complexity);
    }

    /**
     * Returns the facts read from the bodies, in reading order.
     *
     * @return the immutable fact list
     */
    List<MethodBodyFacts> facts() {
        return List.copyOf(facts);
    }

    private void read(TypeId declaring, String name, CtExecutable<?> executable) {
        if (executable.getBody() == null) {
            return;
        }
        BodyScanner scanner = new BodyScanner();
        executable.getBody().accept(scanner);
        complexities.put(executable, scanner.decisions);
        facts.add(new MethodBodyFacts(declaring, name, scanner.invocations, scanner.instantiations));
    }

    /**
     * Collects every fact a body carries in one pass: the calls it makes, the types it creates and
     * the branches it takes.
     */
    private static final class BodyScanner extends CtScanner {

        private final List<MethodBodyFacts.Invocation> invocations = new ArrayList<>();
        private final List<MethodBodyFacts.Instantiation> instantiations = new ArrayList<>();

        /** One path through the body, plus one per decision met. */
        private int decisions = 1;

        @Override
        public <T> void visitCtInvocation(CtInvocation<T> invocation) {
            CtExecutableReference<T> executable = invocation.getExecutable();
            if (executable != null) {
                identityOf(executable.getDeclaringType())
                        .ifPresent(target ->
                                invocations.add(new MethodBodyFacts.Invocation(target, executable.getSimpleName())));
            }
            super.visitCtInvocation(invocation);
        }

        @Override
        public <T> void visitCtConstructorCall(CtConstructorCall<T> call) {
            identityOf(call.getType())
                    .ifPresent(target -> instantiations.add(new MethodBodyFacts.Instantiation(target)));
            super.visitCtConstructorCall(call);
        }

        @Override
        public void visitCtIf(CtIf branch) {
            decisions++;
            super.visitCtIf(branch);
        }

        @Override
        public <T> void visitCtConditional(CtConditional<T> conditional) {
            decisions++;
            super.visitCtConditional(conditional);
        }

        @Override
        public void visitCtFor(CtFor loop) {
            decisions++;
            super.visitCtFor(loop);
        }

        @Override
        public void visitCtForEach(CtForEach loop) {
            decisions++;
            super.visitCtForEach(loop);
        }

        @Override
        public void visitCtWhile(CtWhile loop) {
            decisions++;
            super.visitCtWhile(loop);
        }

        @Override
        public void visitCtDo(CtDo loop) {
            decisions++;
            super.visitCtDo(loop);
        }

        @Override
        public void visitCtCatch(CtCatch handler) {
            decisions++;
            super.visitCtCatch(handler);
        }

        @Override
        public <S> void visitCtCase(CtCase<S> branch) {
            decisions++;
            super.visitCtCase(branch);
        }

        @Override
        public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
            if (operator.getKind() == BinaryOperatorKind.AND || operator.getKind() == BinaryOperatorKind.OR) {
                decisions++;
            }
            super.visitCtBinaryOperator(operator);
        }

        private static Optional<TypeId> identityOf(CtTypeReference<?> reference) {
            if (reference == null) {
                return Optional.empty();
            }
            String qualifiedName = reference.getQualifiedName();
            return qualifiedName == null || qualifiedName.isBlank()
                    ? Optional.empty()
                    : Optional.of(TypeId.of(qualifiedName));
        }
    }
}
