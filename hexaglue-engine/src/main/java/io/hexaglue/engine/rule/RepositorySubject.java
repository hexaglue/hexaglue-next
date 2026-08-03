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
import io.hexaglue.engine.KindEvidence;
import io.hexaglue.engine.KnowledgeAssertion;
import io.hexaglue.engine.Predicate;
import io.hexaglue.engine.Relation;
import io.hexaglue.engine.RelationKind;
import io.hexaglue.engine.Rule;
import io.hexaglue.knowledge.KnowledgeFact;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads a Spring Data repository declaration for everything it says.
 *
 * <p>{@code interface OrderRepository extends JpaRepository<Order, OrderId>} states three things
 * at once, and the strongest signal enterprise code offers: the interface is how the hexagon
 * reaches storage, {@code Order} is the aggregate being stored, and {@code OrderId} is what
 * identifies it. None of the three needs a name to be read — which is the point, because the
 * previous engine could see none of them and fell back on suffixes.</p>
 *
 * <p>Only types of the perimeter receive a verdict, but the ties are stated whatever side of the
 * boundary they land on: an aggregate identified by a {@code java.util.UUID} still has an
 * identity, and the generator needs to know which type it is.</p>
 *
 * @since 7.0.0
 */
public final class RepositorySubject implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R1");

    RepositorySubject() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "reads a Spring Data repository declaration for everything it says";
    }

    @Override
    public Set<Predicate> reads() {
        return Set.of(Predicate.KNOWLEDGE);
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE, Predicate.RELATION);
    }

    @Override
    public void apply(Derivation derivation) {
        for (KnowledgeAssertion assertion : derivation.all(KnowledgeAssertion.class)) {
            if (assertion.finding().fact() == KnowledgeFact.SPRING_DATA_REPOSITORY) {
                read(derivation, assertion);
            }
        }
    }

    private void read(Derivation derivation, KnowledgeAssertion repository) {
        TypeId port = repository.subject();
        Optional<TypeId> aggregate = named(repository.finding().capture("subject"));
        Optional<TypeId> identity = named(repository.finding().capture("id"));

        speak(derivation, port, ArchKind.DRIVEN_PORT, repository, "it is a Spring Data repository");
        aggregate.ifPresent(subject -> {
            speak(derivation, subject, ArchKind.AGGREGATE_ROOT, repository, "a repository stores and retrieves it");
            derivation.derive(Relation.derived(port, RelationKind.MANAGES, subject, ID, repository.proof()));
            identity.ifPresent(id -> derivation.derive(
                    Relation.derived(subject, RelationKind.IDENTIFIED_BY, id, ID, repository.proof())));
        });
        identity.ifPresent(id ->
                speak(derivation, id, ArchKind.IDENTIFIER, repository, "a repository looks an aggregate up by it"));
    }

    /**
     * States a kind for a type of the perimeter. A captured type argument outside it — a JDK
     * identifier, a type from another module — is read but not judged.
     */
    private void speak(
            Derivation derivation, TypeId subject, ArchKind kind, KnowledgeAssertion repository, String why) {
        if (!derivation.perimeter().contains(subject)) {
            return;
        }
        KnowledgeFact fact = repository.finding().fact();
        Evidence evidence = new Evidence(
                fact.tier(),
                fact.tier().maxConfidence(),
                fact + "(" + repository.subject().qualifiedName() + ")",
                subject.qualifiedName() + " is a " + kind + " because " + why + " ("
                        + repository.finding().symbol() + ")",
                derivation.code().type(subject).flatMap(TypeNode::sourceLocation),
                List.of(repository.subject()));
        derivation.derive(KindEvidence.derived(subject, kind, evidence, 0, ID, repository.proof()));
    }

    /**
     * Returns the type a captured argument names, when it names one: a type variable or a
     * wildcard names nothing the engine can classify.
     */
    private static Optional<TypeId> named(Optional<TypeRef> capture) {
        return capture.filter(TypeRef.Named.class::isInstance).map(ref -> TypeId.of(ref.qualifiedName()));
    }
}
