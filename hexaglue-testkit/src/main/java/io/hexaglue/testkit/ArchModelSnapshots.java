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

package io.hexaglue.testkit;

import io.hexaglue.model.arch.AdapterType;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ApplicationType;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DomainType;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.Identifier;
import io.hexaglue.model.arch.PortType;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.declaration.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Renders an {@link ArchModel} to its canonical JSON snapshot for golden-file comparison.
 *
 * <p>The rendering is deterministic by construction: sections always appear in the same order
 * (domain, application, ports, adapters, unclassified), types follow the model's identity order,
 * fields and methods are sorted by name. Every sealed branch of the model has its section, so no
 * classification is silently absent from a snapshot. The writer is hand-rolled on purpose — the
 * testkit stays free of JSON dependencies.</p>
 *
 * @since 7.0.0
 */
public final class ArchModelSnapshots {

    private ArchModelSnapshots() {}

    /**
     * Serializes the given model to its canonical snapshot.
     *
     * @param model the classified model to render
     * @return the deterministic JSON snapshot, newline-terminated
     */
    public static String serialize(ArchModel model) {
        return "{\n"
                + section("domain", model.all(DomainType.class).map(ArchModelSnapshots::domainEntry), ",")
                + section(
                        "application", model.all(ApplicationType.class).map(ArchModelSnapshots::applicationEntry), ",")
                + section("ports", model.all(PortType.class).map(ArchModelSnapshots::portEntry), ",")
                + section("adapters", model.all(AdapterType.class).map(ArchModelSnapshots::adapterEntry), ",")
                + section(
                        "unclassified",
                        model.all(UnclassifiedType.class).map(ArchModelSnapshots::unclassifiedEntry),
                        "")
                + "}\n";
    }

    private static String section(String name, Stream<String> entryStream, String separator) {
        List<String> entries = entryStream.toList();
        String body = entries.isEmpty() ? "[]" : "[\n" + String.join(",\n", entries) + "\n  ]";
        return "  " + quote(name) + ": " + body + separator + "\n";
    }

    private static String domainEntry(DomainType type) {
        List<String> lines = new ArrayList<>();
        lines.add("    {");
        lines.addAll(verdictHeader(type, true));
        if (type instanceof AggregateRoot aggregate) {
            lines.addAll(identity(aggregate));
        }
        if (type instanceof Identifier identifier) {
            lines.add("      \"wrappedType\": "
                    + identifier
                            .wrappedType()
                            .map(ref -> quote(ref.qualifiedName()))
                            .orElse("null") + ",");
        }
        lines.addAll(propertyLines(type.structure().fields()));
        lines.add("    }");
        return String.join("\n", lines);
    }

    /**
     * Renders the identity of an aggregate, or {@code null} when the engine could not name the
     * field carrying it — an aggregate read from a repository declaration or from a declared
     * intent has a kind before it has a named identity.
     */
    private static List<String> identity(AggregateRoot aggregate) {
        Optional<Field> field = aggregate.identityField();
        if (field.isEmpty()) {
            return List.of("      \"identity\": null,");
        }
        return List.of(
                "      \"identity\": {",
                "        \"field\": " + quote(field.orElseThrow().name()) + ",",
                "        \"type\": " + quote(field.orElseThrow().type().qualifiedName()),
                "      },");
    }

    private static String applicationEntry(ApplicationType type) {
        List<String> lines = new ArrayList<>();
        lines.add("    {");
        lines.addAll(verdictHeader(type, false));
        lines.add("    }");
        return String.join("\n", lines);
    }

    private static String portEntry(PortType port) {
        List<String> lines = new ArrayList<>();
        lines.add("    {");
        lines.add("      \"qualifiedName\": " + quote(port.qualifiedName()) + ",");
        lines.add("      \"direction\": " + quote(port.direction().name()) + ",");
        if (port instanceof DrivenPort driven) {
            lines.add("      \"portType\": " + quote(driven.portType().name()) + ",");
        }
        lines.add("      \"confidence\": "
                + quote(port.classification().confidence().name()) + ",");
        lines.add("      \"basis\": " + quote(port.classification().basis().name()) + ",");
        List<String> methods = port.structure().methods().stream()
                .map(method -> quote(method.name()))
                .sorted()
                .toList();
        lines.add("      \"methods\": [" + String.join(", ", methods) + "]");
        lines.add("    }");
        return String.join("\n", lines);
    }

    private static String adapterEntry(AdapterType adapter) {
        List<String> lines = new ArrayList<>();
        lines.add("    {");
        lines.add("      \"qualifiedName\": " + quote(adapter.qualifiedName()) + ",");
        lines.add("      \"direction\": " + quote(adapter.direction().name()) + ",");
        List<String> ports = adapter.ports().stream()
                .map(port -> quote(port.qualifiedName()))
                .sorted()
                .toList();
        lines.add("      \"ports\": [" + String.join(", ", ports) + "],");
        lines.add("      \"confidence\": "
                + quote(adapter.classification().confidence().name()) + ",");
        lines.add("      \"basis\": " + quote(adapter.classification().basis().name()));
        lines.add("    }");
        return String.join("\n", lines);
    }

    private static String unclassifiedEntry(UnclassifiedType type) {
        List<String> lines = new ArrayList<>();
        lines.add("    {");
        lines.add("      \"qualifiedName\": " + quote(type.qualifiedName()) + ",");
        lines.add("      \"category\": " + quote(type.category().name()));
        lines.add("    }");
        return String.join("\n", lines);
    }

    private static List<String> verdictHeader(ArchType type, boolean continued) {
        List<String> lines = new ArrayList<>();
        lines.add("      \"qualifiedName\": " + quote(type.qualifiedName()) + ",");
        lines.add("      \"kind\": " + quote(type.kind().name()) + ",");
        lines.add("      \"confidence\": "
                + quote(type.classification().confidence().name()) + ",");
        lines.add("      \"basis\": " + quote(type.classification().basis().name()) + ",");
        lines.add("      \"nature\": " + quote(type.structure().nature().name()) + (continued ? "," : ""));
        return lines;
    }

    private static List<String> propertyLines(List<Field> fields) {
        List<Field> sorted =
                fields.stream().sorted(Comparator.comparing(Field::name)).toList();
        List<String> lines = new ArrayList<>();
        if (sorted.isEmpty()) {
            lines.add("      \"properties\": []");
            return lines;
        }
        lines.add("      \"properties\": [");
        for (int i = 0; i < sorted.size(); i++) {
            Field field = sorted.get(i);
            String cardinality = field.elementType().isPresent() ? "COLLECTION" : "SINGLE";
            lines.add("        {");
            lines.add("          \"name\": " + quote(field.name()) + ",");
            lines.add("          \"type\": " + quote(field.type().qualifiedName()) + ",");
            lines.add("          \"cardinality\": " + quote(cardinality));
            lines.add(i < sorted.size() - 1 ? "        }," : "        }");
        }
        lines.add("      ]");
        return lines;
    }

    private static String quote(String value) {
        String escaped = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return '"' + escaped + '"';
    }
}
