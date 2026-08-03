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

package io.hexaglue.engine.rule;

import io.hexaglue.engine.Derivation;
import io.hexaglue.engine.KnowledgeAssertion;
import io.hexaglue.engine.Predicate;
import io.hexaglue.engine.Rule;
import io.hexaglue.knowledge.KnowledgeFact;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Parameter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads a type that holds a tool reaching outside the hexagon as the driven adapter it is.
 *
 * <p>An {@code EntityManager}, a {@code JdbcTemplate}, an HTTP client: a type that keeps one of
 * these is talking to the world on the application's behalf, whatever it calls itself and wherever
 * it sits. That is the definition of a driven adapter, and it is read off the declaration rather
 * than off a package or a suffix.</p>
 *
 * <p>The tool is found wherever the declaration puts it — a field, a constructor parameter, a type
 * argument of either, or a supertype. Wrapping a client in a {@code List} or an {@code Optional}
 * does not make it less of a way out.</p>
 *
 * <p>Only classes and records are read. An interface is a contract and an adapter is by definition
 * an implementation, which is also what keeps a Spring Data repository — an interface extending
 * vendor code the packs know as infrastructure — from being read as a port and an adapter at
 * once.</p>
 *
 * @since 7.0.0
 */
public final class InfrastructureDependency implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("W1-DR");

    private static final Set<TypeNature> IMPLEMENTATIONS = Set.of(TypeNature.CLASS, TypeNature.RECORD);

    InfrastructureDependency() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "reads a type that holds a tool reaching outside the hexagon as the driven adapter it is";
    }

    @Override
    public Set<Predicate> reads() {
        return Set.of(Predicate.KNOWLEDGE);
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE);
    }

    @Override
    public void apply(Derivation derivation) {
        Map<TypeId, KnowledgeAssertion> tools = toolsOf(derivation);
        for (TypeNode type : derivation.perimeter().types()) {
            if (!IMPLEMENTATIONS.contains(type.nature())) {
                continue;
            }
            dependenciesOf(type)
                    .flatMap(InfrastructureDependency::named)
                    .map(tools::get)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .ifPresent(tool -> Adapters.speak(
                            derivation,
                            type.id(),
                            ArchKind.DRIVEN_ADAPTER,
                            tool,
                            "it holds " + tool.subject().simpleName() + ", which reaches outside the application",
                            ID));
        }
    }

    /**
     * Returns the types a pack recognized as a way out, by identity. The tool itself is never the
     * adapter: holding one is what places a type at the edge.
     */
    private static Map<TypeId, KnowledgeAssertion> toolsOf(Derivation derivation) {
        Map<TypeId, KnowledgeAssertion> tools = new LinkedHashMap<>();
        for (KnowledgeAssertion assertion : derivation.all(KnowledgeAssertion.class)) {
            if (assertion.finding().fact() == KnowledgeFact.INFRA_DEPENDENCY) {
                tools.putIfAbsent(assertion.subject(), assertion);
            }
        }
        return tools;
    }

    /**
     * Returns every type the declaration reaches for: what it extends, what it keeps, and what it
     * is handed, type arguments included.
     */
    private static Stream<TypeRef> dependenciesOf(TypeNode type) {
        return Stream.of(
                        type.superClass().stream(),
                        type.interfaces().stream(),
                        type.fields().stream().map(Field::type),
                        type.constructors().stream()
                                .map(Constructor::parameters)
                                .flatMap(List::stream)
                                .map(Parameter::type))
                .flatMap(references -> references);
    }

    /**
     * Returns the types a reference names: the reference itself and, recursively, its arguments.
     */
    private static Stream<TypeId> named(TypeRef reference) {
        Stream<TypeId> arguments = reference.typeArguments().stream().flatMap(InfrastructureDependency::named);
        return reference instanceof TypeRef.Named
                ? Stream.concat(Stream.of(TypeId.of(reference.qualifiedName())), arguments)
                : arguments;
    }
}
