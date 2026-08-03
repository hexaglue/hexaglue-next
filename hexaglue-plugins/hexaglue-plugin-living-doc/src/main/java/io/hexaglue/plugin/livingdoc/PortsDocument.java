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
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivingPort;
import io.hexaglue.plugin.livingdoc.render.Graph;
import io.hexaglue.plugin.livingdoc.render.Markdown;
import io.hexaglue.plugin.livingdoc.render.Table;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The boundary: what drives the application, and what the application drives.
 *
 * <p>A port is where the hexagon meets the world, so the page says which way each one faces before
 * it says anything else. A driven port also says what family it belongs to and which aggregate it
 * manages, because that is what tells a repository from a gateway.</p>
 */
final class PortsDocument {

    /** The file name of this document, which the other documents link to. */
    static final String NAME = "ports.md";

    private final ArchModel model;
    private final LivingDocOptions options;

    PortsDocument(ArchModel model, LivingDocOptions options) {
        this.model = model;
        this.options = options;
    }

    /**
     * Writes the document.
     */
    String render() {
        Markdown document = Markdown.document()
                .heading(1, "Ports")
                .paragraph("Where the application meets the world, read from the classified model.");
        if (options.diagrams()) {
            writeDiagram(document);
        }
        writeDrivingPorts(document);
        writeDrivenPorts(document);
        return document.render();
    }

    private void writeDiagram(Markdown document) {
        List<DrivingPort> driving = model.portIndex().drivingPorts().toList();
        List<DrivenPort> driven = model.portIndex().drivenPorts().toList();
        if (driving.isEmpty() && driven.isEmpty()) {
            return;
        }
        Graph graph = Graph.flowing(Graph.Direction.LEFT_TO_RIGHT);
        if (!driving.isEmpty()) {
            graph.group("driving", "Driving");
            driving.forEach(port -> graph.node(port.id().toString(), Names.displayOf(port.id()), Graph.Shape.CIRCLE));
            graph.endGroup();
        }
        graph.group("core", "Core");
        model.domainIndex()
                .aggregateRoots()
                .forEach(aggregate ->
                        graph.node(aggregate.id().toString(), Names.displayOf(aggregate.id()), Graph.Shape.BOX));
        graph.endGroup();
        if (!driven.isEmpty()) {
            graph.group("driven", "Driven");
            driven.forEach(port -> graph.node(port.id().toString(), Names.displayOf(port.id()), Graph.Shape.ROUNDED));
            graph.endGroup();
            driven.forEach(port -> port.managedAggregate()
                    .ifPresent(aggregate -> graph.arrow(port.id().toString(), aggregate.qualifiedName(), "manages")));
        }
        document.heading(2, "At a glance").code("mermaid", graph.render());
    }

    private void writeDrivingPorts(Markdown document) {
        List<DrivingPort> ports = model.portIndex().drivingPorts().toList();
        document.heading(2, "Driving ports");
        if (ports.isEmpty()) {
            document.paragraph("Nothing drives this application through a port of its own.");
            return;
        }
        ports.forEach(port -> {
            heading(document, port.id());
            if (!port.useCases().isEmpty()) {
                document.bullet(Markdown.bold("Use cases") + ": "
                        + port.useCases().stream()
                                .map(useCase ->
                                        Markdown.inlineCode(useCase.method().name()))
                                .collect(Collectors.joining(", ")));
            }
            types(
                    document,
                    "Takes",
                    port.inputTypes().stream()
                            .map(type -> TypeId.of(type.qualifiedName()))
                            .toList());
            types(
                    document,
                    "Gives back",
                    port.outputTypes().stream()
                            .map(type -> TypeId.of(type.qualifiedName()))
                            .toList());
            document.blank();
            operations(document, port);
            provenance(document, port);
        });
    }

    private void writeDrivenPorts(Markdown document) {
        List<DrivenPort> ports = model.portIndex().drivenPorts().toList();
        document.heading(2, "Driven ports");
        if (ports.isEmpty()) {
            document.paragraph("This application drives nothing through a port of its own.");
            return;
        }
        ports.forEach(port -> {
            heading(document, port.id());
            document.bullet(Markdown.bold("Family") + ": " + port.portType() + " — "
                    + port.portType().description());
            port.managedAggregate()
                    .ifPresent(aggregate -> document.bullet(Markdown.bold("Manages") + ": "
                            + Names.linkTo(TypeId.of(aggregate.qualifiedName()), DomainDocument.NAME)));
            document.blank();
            operations(document, port);
            provenance(document, port);
        });
    }

    private static void types(Markdown document, String title, List<TypeId> types) {
        if (types.isEmpty()) {
            return;
        }
        document.bullet(Markdown.bold(title) + ": "
                + types.stream()
                        .map(type -> Markdown.inlineCode(Names.displayOf(type)))
                        .collect(Collectors.joining(", ")));
    }

    private static void operations(Markdown document, ArchType port) {
        if (port.structure().methods().isEmpty()) {
            return;
        }
        Table operations = Table.withHeaders("Operation");
        port.structure().methods().forEach(method -> operations.row(Markdown.inlineCode(Names.signatureOf(method))));
        document.table(operations);
    }

    private static void heading(Markdown document, TypeId id) {
        document.anchor(Names.anchorOf(id))
                .heading(3, Names.displayOf(id))
                .paragraph(Markdown.inlineCode(id.toString()));
    }

    private void provenance(Markdown document, ArchType port) {
        if (options.provenance()) {
            Provenance.writeTo(document, port);
        }
    }
}
