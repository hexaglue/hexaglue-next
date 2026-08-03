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

package io.hexaglue.engine.finding;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DomainType;
import io.hexaglue.model.arch.Entity;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.RemediationHint;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * What holds a domain together, checked against what the model says it is.
 *
 * <p>Every check here reads the verdicts and the references, never the source. What it condemns,
 * identification was right to tolerate: a codebase whose aggregate has no repository is still a
 * codebase whose aggregate the engine could recognise, and saying both things is the point.</p>
 *
 * @since 7.0.0
 */
public final class DomainFindings {

    /** A part of an aggregate is reachable from outside it. */
    static final IssueCode BOUNDARY = IssueCode.of("HG-DDD-001");

    /** Two aggregates claim the same part. */
    static final IssueCode OWNERSHIP = IssueCode.of("HG-DDD-002");

    /** Aggregates depend on each other in a circle. */
    static final IssueCode AGGREGATE_CYCLE = IssueCode.of("HG-DDD-003");

    /** An aggregate root has no way in and out of storage. */
    static final IssueCode NO_REPOSITORY = IssueCode.of("HG-DDD-004");

    /** A domain type names something outside the domain. */
    static final IssueCode PURITY = IssueCode.of("HG-DDD-005");

    /** An entity has nothing to tell its instances apart. */
    static final IssueCode NO_IDENTITY = IssueCode.of("HG-DDD-006");

    /** A value can be changed after it is made. */
    static final IssueCode MUTABLE_VALUE = IssueCode.of("HG-DDD-007");

    /** A part is stored like an entity but reads like a value. */
    static final IssueCode UNDECIDABLE_PART = IssueCode.of("HG-DDD-008");

    private DomainFindings() {}

    /**
     * Runs every domain check.
     *
     * @param judgement what the checks may read
     * @return the findings, in the order the checks are stated
     */
    static List<Finding> of(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        findings.addAll(partsReachableFromOutside(judgement));
        findings.addAll(partsClaimedTwice(judgement));
        findings.addAll(aggregatesInACircle(judgement));
        findings.addAll(aggregatesWithoutStorage(judgement));
        findings.addAll(domainTypesNamingTheOutside(judgement));
        findings.addAll(entitiesWithoutIdentity(judgement));
        findings.addAll(valuesThatCanChange(judgement));
        findings.addAll(partsNothingDistinguishes(judgement));
        return findings;
    }

    /**
     * An aggregate is a consistency boundary: everything inside it changes through its root. A part
     * that another type reaches directly is a part whose invariants the root cannot hold.
     */
    private static List<Finding> partsReachableFromOutside(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        for (AggregateRoot aggregate :
                judgement.model().domainIndex().aggregateRoots().toList()) {
            Set<TypeId> inside = insideOf(aggregate);
            for (TypeRef part : aggregate.entities()) {
                TypeId partId = TypeId.of(part.qualifiedName());
                List<TypeId> outsiders = judgement.dependencies().usersOf(partId).stream()
                        .filter(user -> !inside.contains(user))
                        .filter(user -> judgement.model().type(user).isPresent())
                        .toList();
                if (!outsiders.isEmpty()) {
                    findings.add(finding(
                                    BOUNDARY,
                                    Severity.MAJOR,
                                    partId,
                                    names(outsiders) + " reach " + partId.simpleName()
                                            + " directly, although it is part of the aggregate "
                                            + aggregate.id().simpleName()
                                            + ". Everything inside an aggregate changes through its root, so a "
                                            + "reference that goes around the root goes around its invariants.",
                                    evidence(
                                            "reached-from-outside",
                                            "outside the aggregate " + aggregate.id(),
                                            outsiders))
                            .relatedTypes(outsiders)
                            .build());
                }
            }
        }
        return findings;
    }

    /**
     * Two roots claiming one part is not a boundary drawn twice, it is a boundary not drawn: the
     * part changes under two sets of invariants that nobody reconciles.
     */
    private static List<Finding> partsClaimedTwice(Judgement judgement) {
        List<AggregateRoot> aggregates =
                judgement.model().domainIndex().aggregateRoots().toList();
        List<Finding> findings = new ArrayList<>();
        for (Entity entity : judgement.model().domainIndex().entities().toList()) {
            List<TypeId> claimants = aggregates.stream()
                    .filter(aggregate -> aggregate.entities().stream()
                            .anyMatch(part -> TypeId.of(part.qualifiedName()).equals(entity.id())))
                    .map(AggregateRoot::id)
                    .toList();
            if (claimants.size() > 1) {
                findings.add(finding(
                                OWNERSHIP,
                                Severity.CRITICAL,
                                entity.id(),
                                entity.id().simpleName() + " is part of " + names(claimants)
                                        + ". A part belongs to exactly one aggregate — two owners means neither of "
                                        + "them can say what it may become.",
                                evidence("claimed-by", "claimed by more than one aggregate", claimants))
                        .relatedTypes(claimants)
                        .build());
            }
        }
        return findings;
    }

    /**
     * Aggregates that reach each other in a circle cannot be loaded, changed or stored one at a
     * time, which is the one thing being an aggregate is supposed to guarantee.
     */
    private static List<Finding> aggregatesInACircle(Judgement judgement) {
        List<TypeId> roots = judgement
                .model()
                .domainIndex()
                .aggregateRoots()
                .map(AggregateRoot::id)
                .toList();
        return judgement.dependencies().knotsAmong(roots).stream()
                .map(knot -> finding(
                                AGGREGATE_CYCLE,
                                Severity.CRITICAL,
                                knot.get(0),
                                names(knot)
                                        + " depend on each other in a circle. Aggregates are the units a system "
                                        + "loads and stores one at a time; a circle means none of them can be.",
                                evidence("aggregate-knot", "in a circle of aggregates", knot))
                        .relatedTypes(knot)
                        .build())
                .toList();
    }

    /**
     * An aggregate nothing can store or fetch is a shape without a life: whatever holds it holds it
     * by accident, and its boundary means nothing to the system around it.
     */
    private static List<Finding> aggregatesWithoutStorage(Judgement judgement) {
        return judgement
                .model()
                .domainIndex()
                .aggregateRoots()
                .filter(aggregate -> aggregate.drivenPort().isEmpty())
                .filter(aggregate -> judgement
                        .model()
                        .portIndex()
                        .repositoryFor(aggregate.id())
                        .isEmpty())
                .map(aggregate -> finding(
                                NO_REPOSITORY,
                                Severity.MAJOR,
                                aggregate.id(),
                                aggregate.id().simpleName()
                                        + " is an aggregate root that nothing stores or fetches. Declare the port "
                                        + "that does, so the boundary the model reads is the boundary the system "
                                        + "keeps.",
                                evidence("no-repository", "no driven port manages it", List.of()))
                        .build())
                .toList();
    }

    /**
     * The domain is what the rest of the system is written against, so it may not be written
     * against the rest of the system. A domain type naming an adapter or a service inverts the one
     * dependency the architecture exists to protect.
     */
    private static List<Finding> domainTypesNamingTheOutside(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        for (ArchType type : judgement.model().types()) {
            if (!(type instanceof DomainType)) {
                continue;
            }
            List<TypeId> outside = judgement.dependencies().usedBy(type.id()).stream()
                    .filter(used -> judgement
                            .model()
                            .type(used)
                            .map(DomainFindings::isOutsideTheDomain)
                            .orElse(false))
                    .toList();
            if (!outside.isEmpty()) {
                findings.add(finding(
                                PURITY,
                                Severity.CRITICAL,
                                type.id(),
                                type.id().simpleName() + " is a domain type and it names " + names(outside)
                                        + ". The domain is what everything else is written against; naming the "
                                        + "outside from the inside turns that around.",
                                evidence("names-the-outside", "outside the domain", outside))
                        .relatedTypes(outside)
                        .build());
            }
        }
        return findings;
    }

    private static boolean isOutsideTheDomain(ArchType type) {
        return type.kind() == ArchKind.DRIVING_ADAPTER
                || type.kind() == ArchKind.DRIVEN_ADAPTER
                || type.kind() == ArchKind.APPLICATION_SERVICE
                || type.kind() == ArchKind.COMMAND_HANDLER
                || type.kind() == ArchKind.QUERY_HANDLER;
    }

    /**
     * An entity is the thing that stays itself while its contents change. With nothing to tell one
     * from another, there is no "itself" to stay.
     */
    private static List<Finding> entitiesWithoutIdentity(Judgement judgement) {
        return judgement
                .model()
                .domainIndex()
                .entities()
                .filter(entity -> entity.identityField().isEmpty())
                .map(entity -> finding(
                                NO_IDENTITY,
                                Severity.MAJOR,
                                entity.id(),
                                entity.id().simpleName()
                                        + " is an entity with no identity. An entity is what stays itself while "
                                        + "its contents change; give it the field that says which one it is.",
                                evidence("no-identity-field", "no field carries identity", List.of()))
                        .build())
                .toList();
    }

    /**
     * A value is defined by what it holds. Change what it holds and it is a different value, so a
     * method that changes it in place is a method that makes it a different value without telling
     * anyone holding it.
     */
    private static List<Finding> valuesThatCanChange(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        for (ValueObject value : judgement.model().domainIndex().valueObjects().toList()) {
            List<String> mutators = value.structure().methods().stream()
                    .filter(DomainFindings::changesInPlace)
                    .map(Method::name)
                    .sorted()
                    .toList();
            if (!mutators.isEmpty()) {
                findings.add(finding(
                                MUTABLE_VALUE,
                                Severity.MAJOR,
                                value.id(),
                                value.id().simpleName() + " is a value and " + String.join(", ", mutators)
                                        + " change it in place. A value is what it holds: changing it makes it a "
                                        + "different value, which everything holding the old one should have been "
                                        + "told about.",
                                evidence("changes-in-place", String.join(", ", mutators), List.of()))
                        .build());
            }
        }
        return findings;
    }

    private static boolean changesInPlace(Method method) {
        return method.name().startsWith("set")
                && method.parameters().size() == 1
                && "void".equals(method.returnType().qualifiedName());
    }

    /**
     * The reading this states is correct by the rules and wrong about the domain, which is exactly
     * why it is said here rather than fixed there. A part whose identity is a bare platform type
     * carries nothing the model can read as identity, so it reads as a value — and a value is
     * embedded, copied and compared by its contents, which is not what its storage does with it.
     *
     * <p>Identification stays silent because the only thing in the sources that says otherwise is
     * a persistence annotation, and letting persistence decide a kind is a door that does not shut
     * again. Conformity says it instead, and the fix is one line of declaration.</p>
     */
    private static List<Finding> partsNothingDistinguishes(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        for (AggregateRoot aggregate :
                judgement.model().domainIndex().aggregateRoots().toList()) {
            for (TypeRef part : aggregate.valueObjects()) {
                TypeId partId = TypeId.of(part.qualifiedName());
                Optional<ValueObject> value = judgement.model().type(partId).stream()
                        .filter(ValueObject.class::isInstance)
                        .map(ValueObject.class::cast)
                        .findFirst();
                if (value.isEmpty() || !readsLikeAnIdentity(value.orElseThrow())) {
                    continue;
                }
                findings.add(finding(
                                UNDECIDABLE_PART,
                                Severity.MINOR,
                                partId,
                                partId.simpleName() + " is part of "
                                        + aggregate.id().simpleName()
                                        + " and carries a field that looks like identity, but nothing in the model "
                                        + "distinguishes it from a value. Declare what it is: read as a value it "
                                        + "will be embedded and compared by its contents, which is not what a "
                                        + "stored entity does.",
                                evidence(
                                        "identity-of-a-platform-type",
                                        "its identity is a type outside the perimeter",
                                        List.of(aggregate.id())))
                        .relatedTypes(List.of(aggregate.id()))
                        .remediations(List.of(RemediationHint.configureExplicit(partId, ArchKind.ENTITY)))
                        .build());
            }
        }
        return findings;
    }

    /**
     * A single field named as an identity whose type nothing in the perimeter classifies — the
     * shape of a part identified by a bare {@code Long} or {@code Integer}.
     */
    private static boolean readsLikeAnIdentity(ValueObject value) {
        return value.structure().fields().stream()
                .anyMatch(field -> "id".equals(field.name()) || field.name().endsWith("Id"));
    }

    private static Set<TypeId> insideOf(AggregateRoot aggregate) {
        Set<TypeId> inside = new TreeSet<>();
        inside.add(aggregate.id());
        aggregate.entities().forEach(part -> inside.add(TypeId.of(part.qualifiedName())));
        aggregate.valueObjects().forEach(part -> inside.add(TypeId.of(part.qualifiedName())));
        return inside;
    }

    private static String names(List<TypeId> types) {
        return types.stream().map(TypeId::simpleName).collect(Collectors.joining(", "));
    }

    private static Evidence evidence(String fact, String justification, List<TypeId> related) {
        return new Evidence(
                EvidenceTier.GRAPH_RELATION, Confidence.HIGH, fact, justification, Optional.empty(), related);
    }

    private static Finding.Builder finding(
            IssueCode code, Severity severity, TypeId subject, String message, Evidence evidence) {
        return Finding.builder(code, severity, message, subject).evidences(List.of(evidence));
    }
}
