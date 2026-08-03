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

package io.hexaglue.plugin.livingdoc;

import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.DomainEvent;
import io.hexaglue.model.arch.DomainService;
import io.hexaglue.model.arch.DomainType;
import io.hexaglue.model.arch.Entity;
import io.hexaglue.model.arch.Identifier;
import io.hexaglue.model.arch.Invariant;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.render.ClassDiagram;
import io.hexaglue.render.Markdown;
import io.hexaglue.render.Table;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The domain, as the model holds it: what the aggregates are, what they are made of, and what
 * identifies them.
 *
 * <p>Nothing here is derived. An aggregate names its identity field, the entities and values it
 * owns and the events it announces because the engine wrote those links down; this page only puts
 * them in an order a person can read.</p>
 */
final class DomainDocument {

    /** The file name of this document, which the other documents link to. */
    static final String NAME = "domain.md";

    private final ArchModel model;
    private final LivingDocOptions options;

    DomainDocument(ArchModel model, LivingDocOptions options) {
        this.model = model;
        this.options = options;
    }

    /**
     * Writes the document.
     */
    String render() {
        Markdown document = Markdown.document()
                .heading(1, "Domain")
                .paragraph("What the domain holds, read from the classified model.");
        if (options.diagrams()) {
            writeDiagram(document);
        }
        writeAggregates(document);
        writeEntities(document);
        writeValueObjects(document);
        writeIdentifiers(document);
        writeEvents(document);
        writeServices(document);
        return document.render();
    }

    private void writeDiagram(Markdown document) {
        List<AggregateRoot> aggregates = model.domainIndex().aggregateRoots().toList();
        if (aggregates.isEmpty()) {
            return;
        }
        ClassDiagram diagram = ClassDiagram.empty();
        aggregates.forEach(aggregate -> {
            diagram.type(aggregate.id().toString(), "AggregateRoot", membersOf(aggregate));
            aggregate.entities().forEach(entity -> relate(diagram, aggregate.id(), entity, "owns"));
            aggregate.valueObjects().forEach(value -> relate(diagram, aggregate.id(), value, "holds"));
            aggregate
                    .domainEvents()
                    .forEach(event -> diagram.relate(
                            aggregate.id().toString(),
                            ClassDiagram.Relation.ASSOCIATION,
                            event.qualifiedName(),
                            "announces"));
            aggregate
                    .effectiveIdentityType()
                    .ifPresent(identity ->
                            diagram.note(aggregate.id().toString(), "identified by " + Names.displayOf(identity)));
        });
        document.heading(2, "At a glance").code("mermaid", diagram.render());
    }

    private static void relate(ClassDiagram diagram, TypeId owner, TypeRef part, String label) {
        diagram.relate(owner.toString(), ClassDiagram.Relation.COMPOSITION, part.qualifiedName(), label);
    }

    private List<String> membersOf(AggregateRoot aggregate) {
        return aggregate.structure().fields().stream()
                .limit(options.propertiesPerDiagram())
                .map(field ->
                        "+" + Names.displayOf(field.type()).replace('<', '~').replace('>', '~') + " " + field.name())
                .toList();
    }

    private void writeAggregates(Markdown document) {
        List<AggregateRoot> aggregates = model.domainIndex().aggregateRoots().toList();
        document.heading(2, "Aggregates");
        if (aggregates.isEmpty()) {
            document.paragraph("The model holds no aggregate root.");
            return;
        }
        aggregates.forEach(aggregate -> {
            heading(document, aggregate.id());
            aggregate
                    .effectiveIdentityType()
                    .ifPresent(identity -> document.bullet(Markdown.bold("Identified by") + ": "
                            + Markdown.inlineCode(Names.displayOf(identity))
                            + aggregate
                                    .identityField()
                                    .map(field -> ", on " + Markdown.inlineCode(field.name()))
                                    .orElse("")));
            aggregate
                    .drivenPort()
                    .ifPresent(port -> document.bullet(Markdown.bold("Persisted through") + ": "
                            + Names.linkTo(TypeId.of(port.qualifiedName()), PortsDocument.NAME)));
            listOf(document, "Entities", aggregate.entities());
            listOf(document, "Values", aggregate.valueObjects());
            listOf(document, "Events", aggregate.domainEvents());
            if (!aggregate.invariants().isEmpty()) {
                document.bullet(Markdown.bold("Invariants") + ": "
                        + aggregate.invariants().stream()
                                .map(Invariant::description)
                                .collect(Collectors.joining("; ")));
            }
            document.blank();
            writeProperties(document, aggregate.structure().fields());
            provenance(document, aggregate);
        });
    }

    private static void listOf(Markdown document, String title, List<TypeRef> types) {
        if (types.isEmpty()) {
            return;
        }
        document.bullet(Markdown.bold(title) + ": "
                + types.stream()
                        .map(type -> Names.linkTo(TypeId.of(type.qualifiedName())))
                        .collect(Collectors.joining(", ")));
    }

    private void writeEntities(Markdown document) {
        List<Entity> entities = model.domainIndex().entities().toList();
        document.heading(2, "Entities");
        if (entities.isEmpty()) {
            document.paragraph("The model holds no entity outside its aggregate roots.");
            return;
        }
        entities.forEach(entity -> {
            heading(document, entity.id());
            entity.owningAggregate()
                    .ifPresent(owner -> document.bullet(
                            Markdown.bold("Owned by") + ": " + Names.linkTo(TypeId.of(owner.qualifiedName()))));
            entity.identityField()
                    .ifPresent(field ->
                            document.bullet(Markdown.bold("Identified by") + ": " + Markdown.inlineCode(field.name())));
            document.blank();
            writeProperties(document, entity.structure().fields());
            provenance(document, entity);
        });
    }

    private void writeValueObjects(Markdown document) {
        List<ValueObject> values = model.domainIndex().valueObjects().toList();
        document.heading(2, "Values");
        if (values.isEmpty()) {
            document.paragraph("The model holds no value object.");
            return;
        }
        values.forEach(value -> {
            heading(document, value.id());
            writeProperties(document, value.structure().fields());
            provenance(document, value);
        });
    }

    private void writeIdentifiers(Markdown document) {
        List<Identifier> identifiers = model.domainIndex().identifiers().toList();
        document.heading(2, "Identifiers");
        if (identifiers.isEmpty()) {
            document.paragraph("The model holds no identifier of its own.");
            return;
        }
        Table table = Table.withHeaders("Identifier", "Wraps", "Identifies");
        identifiers.forEach(identifier -> table.row(
                Names.displayOf(identifier.id()),
                identifier
                        .wrappedType()
                        .map(Names::displayOf)
                        .map(Markdown::inlineCode)
                        .orElse("—"),
                model.compositionIndex()
                        .aggregateOf(identifier.id())
                        .map(Names::linkTo)
                        .orElse("—")));
        document.table(table);
    }

    private void writeEvents(Markdown document) {
        List<DomainEvent> events = model.domainIndex().domainEvents().toList();
        document.heading(2, "Events");
        if (events.isEmpty()) {
            document.paragraph("The model holds no domain event.");
            return;
        }
        events.forEach(event -> {
            heading(document, event.id());
            event.sourceAggregate()
                    .ifPresent(source -> document.bullet(
                            Markdown.bold("Announced by") + ": " + Names.linkTo(TypeId.of(source.qualifiedName()))));
            document.blank();
            writeProperties(document, event.structure().fields());
            provenance(document, event);
        });
    }

    private void writeServices(Markdown document) {
        List<DomainService> services = model.domainIndex().domainServices().toList();
        document.heading(2, "Domain services");
        if (services.isEmpty()) {
            document.paragraph("The model holds no domain service.");
            return;
        }
        services.forEach(service -> {
            heading(document, service.id());
            Table operations = Table.withHeaders("Operation");
            service.structure()
                    .methods()
                    .forEach(method -> operations.row(Markdown.inlineCode(Names.signatureOf(method))));
            if (!operations.isEmpty()) {
                document.table(operations);
            }
            provenance(document, service);
        });
    }

    private static void writeProperties(Markdown document, List<Field> fields) {
        if (fields.isEmpty()) {
            return;
        }
        Table properties = Table.withHeaders("Property", "Type");
        fields.forEach(field ->
                properties.row(Markdown.inlineCode(field.name()), Markdown.inlineCode(Names.displayOf(field.type()))));
        document.table(properties);
    }

    private static void heading(Markdown document, TypeId id) {
        document.anchor(Names.anchorOf(id))
                .heading(3, Names.displayOf(id))
                .paragraph(Markdown.inlineCode(id.toString()));
    }

    private void provenance(Markdown document, DomainType type) {
        if (options.provenance()) {
            Provenance.writeTo(document, type);
        }
    }
}
