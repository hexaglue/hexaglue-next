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

import io.hexaglue.engine.Dependencies;
import io.hexaglue.engine.Perimeter;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ApplicationService;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DomainEvent;
import io.hexaglue.model.arch.DrivenAdapter;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.arch.DrivingAdapter;
import io.hexaglue.model.arch.DrivingPort;
import io.hexaglue.model.arch.Entity;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.Edge;
import io.hexaglue.model.code.EdgeKind;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.model.config.ClassificationConfig;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.FieldRole;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import io.hexaglue.model.finding.Finding;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A shop stated one fact at a time, so that each check is read against the smallest architecture
 * that can trigger it — and against the neighbouring one that must not.
 */
// One verb per architectural fact is what lets a test state the smallest shop that triggers a
// check and the neighbouring one that must not. Splitting the vocabulary would scatter that.
@SuppressWarnings("PMD.TooManyMethods")
final class ShopJudgements {

    private final List<ArchType> types = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    private ShopJudgements() {}

    static ShopJudgements shop() {
        return new ShopJudgements();
    }

    private static Classification verdict(ArchKind kind) {
        Classification.Builder builder =
                Classification.builder(kind, Confidence.HIGH, Basis.INFERRED, ProofNode.fact(kind + " by fixture"));
        if (kind == ArchKind.DRIVEN_PORT || kind == ArchKind.DRIVEN_ADAPTER) {
            builder.direction(PortDirection.DRIVEN);
        }
        return builder.build();
    }

    private static TypeStructure structure(TypeNature nature, List<Field> fields, List<Method> methods) {
        return TypeStructure.builder(nature).fields(fields).methods(methods).build();
    }

    private static List<TypeRef> refs(String... qualifiedNames) {
        return List.of(qualifiedNames).stream().map(TypeRef::of).toList();
    }

    ShopJudgements aggregate(String id, String... entityParts) {
        types.add(new AggregateRoot(
                TypeId.of(id),
                structure(TypeNature.CLASS, List.of(), List.of()),
                verdict(ArchKind.AGGREGATE_ROOT),
                Optional.empty(),
                Optional.empty(),
                refs(entityParts),
                List.of(),
                List.of(),
                Optional.empty(),
                List.of()));
        return this;
    }

    ShopJudgements aggregateHolding(String id, String... valueParts) {
        types.add(new AggregateRoot(
                TypeId.of(id),
                structure(TypeNature.CLASS, List.of(), List.of()),
                verdict(ArchKind.AGGREGATE_ROOT),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                refs(valueParts),
                List.of(),
                Optional.empty(),
                List.of()));
        return this;
    }

    ShopJudgements aggregateStoredBy(String id, String portId) {
        types.add(new AggregateRoot(
                TypeId.of(id),
                structure(TypeNature.CLASS, List.of(), List.of()),
                verdict(ArchKind.AGGREGATE_ROOT),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                Optional.of(TypeRef.of(portId)),
                List.of()));
        return this;
    }

    ShopJudgements entity(String id, String owner) {
        Field identity = Field.builder("id", TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        types.add(new Entity(
                TypeId.of(id),
                structure(TypeNature.CLASS, List.of(identity), List.of()),
                verdict(ArchKind.ENTITY),
                Optional.of(identity),
                Optional.of(TypeRef.of(owner))));
        return this;
    }

    ShopJudgements entityWithoutIdentity(String id, String owner) {
        types.add(new Entity(
                TypeId.of(id),
                structure(TypeNature.CLASS, List.of(), List.of()),
                verdict(ArchKind.ENTITY),
                Optional.empty(),
                Optional.of(TypeRef.of(owner))));
        return this;
    }

    ShopJudgements value(String id) {
        types.add(new ValueObject(
                TypeId.of(id),
                structure(
                        TypeNature.RECORD,
                        List.of(Field.of("amount", TypeRef.of("java.math.BigDecimal"))),
                        List.of(Method.of("amount", TypeRef.of("java.math.BigDecimal")))),
                verdict(ArchKind.VALUE_OBJECT)));
        return this;
    }

    ShopJudgements valueWithIdentityField(String id) {
        types.add(new ValueObject(
                TypeId.of(id),
                structure(TypeNature.CLASS, List.of(Field.of("id", TypeRef.of("java.lang.Integer"))), List.of()),
                verdict(ArchKind.VALUE_OBJECT)));
        return this;
    }

    ShopJudgements mutableValue(String id, String mutator) {
        Method setter = Method.builder(mutator, TypeRef.of("void"))
                .parameters(List.of(Parameter.of("value", TypeRef.of("java.math.BigDecimal"))))
                .build();
        types.add(new ValueObject(
                TypeId.of(id),
                structure(
                        TypeNature.CLASS,
                        List.of(Field.of("amount", TypeRef.of("java.math.BigDecimal"))),
                        List.of(setter)),
                verdict(ArchKind.VALUE_OBJECT)));
        return this;
    }

    ShopJudgements repository(String id, String aggregateId) {
        types.add(new DrivenPort(
                TypeId.of(id),
                structure(TypeNature.INTERFACE, List.of(), List.of()),
                verdict(ArchKind.DRIVEN_PORT),
                DrivenPortType.REPOSITORY,
                Optional.of(TypeRef.of(aggregateId))));
        return this;
    }

    ShopJudgements drivenAdapter(String id) {
        types.add(new DrivenAdapter(
                TypeId.of(id),
                structure(TypeNature.CLASS, List.of(), List.of()),
                verdict(ArchKind.DRIVEN_ADAPTER),
                List.of()));
        return this;
    }

    ShopJudgements drivenAdapterFor(String id, String portId) {
        types.add(new DrivenAdapter(
                TypeId.of(id),
                TypeStructure.builder(TypeNature.CLASS)
                        .interfaces(List.of(TypeRef.of(portId)))
                        .build(),
                verdict(ArchKind.DRIVEN_ADAPTER),
                List.of(TypeRef.of(portId))));
        return this;
    }

    ShopJudgements drivingAdapterFor(String id, String portId) {
        types.add(new DrivingAdapter(
                TypeId.of(id),
                structure(TypeNature.CLASS, List.of(), List.of()),
                verdict(ArchKind.DRIVING_ADAPTER),
                List.of(TypeRef.of(portId))));
        return this;
    }

    ShopJudgements drivingPort(String id) {
        types.add(new DrivingPort(
                TypeId.of(id),
                structure(TypeNature.INTERFACE, List.of(), List.of()),
                verdict(ArchKind.DRIVING_PORT),
                List.of(),
                List.of(),
                List.of()));
        return this;
    }

    ShopJudgements drivingPortAsClass(String id) {
        types.add(new DrivingPort(
                TypeId.of(id),
                structure(TypeNature.CLASS, List.of(), List.of()),
                verdict(ArchKind.DRIVING_PORT),
                List.of(),
                List.of(),
                List.of()));
        return this;
    }

    ShopJudgements applicationService(String id, String... implementedPorts) {
        types.add(new ApplicationService(
                TypeId.of(id),
                TypeStructure.builder(TypeNature.CLASS)
                        .interfaces(refs(implementedPorts))
                        .build(),
                verdict(ArchKind.APPLICATION_SERVICE)));
        return this;
    }

    ShopJudgements domainEvent(String id) {
        types.add(new DomainEvent(
                TypeId.of(id),
                structure(TypeNature.RECORD, List.of(), List.of()),
                verdict(ArchKind.DOMAIN_EVENT),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
        return this;
    }

    ShopJudgements uses(String from, String to) {
        edges.add(Edge.of(TypeId.of(from), EdgeKind.FIELD_TYPE, TypeId.of(to)));
        return this;
    }

    List<Finding> judgeWith(ClassificationConfig vocabulary) {
        return judge(vocabulary);
    }

    List<Finding> judge() {
        return judge(ClassificationConfig.defaults());
    }

    private List<Finding> judge(ClassificationConfig vocabulary) {
        ArchModel.Builder model = ArchModel.builder();
        types.forEach(model::addType);
        CodeModel.Builder code = CodeModel.builder();
        types.forEach(type -> code.addType(TypeNode.builder(type.id(), TypeNature.CLASS)
                .modifiers(Set.<Modifier>of())
                .build()));
        edges.forEach(code::addEdge);
        CodeModel built = code.build();
        return Findings.of(
                model.build(), Dependencies.of(built, Perimeter.of(built, AnalysisScope.everything())), vocabulary);
    }
}
