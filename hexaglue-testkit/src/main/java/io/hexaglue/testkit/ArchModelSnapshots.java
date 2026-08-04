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

import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AdapterType;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ApplicationType;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DomainEvent;
import io.hexaglue.model.arch.DomainService;
import io.hexaglue.model.arch.DomainType;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivingPort;
import io.hexaglue.model.arch.Entity;
import io.hexaglue.model.arch.Identifier;
import io.hexaglue.model.arch.PortType;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.arch.UseCase;
import io.hexaglue.model.declaration.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
 * <p>What each record was filled with is rendered too, not its kind alone. A snapshot showing
 * verdicts and nothing else would go on matching while the links between types changed underneath
 * it, and those links are most of what a generator reads.</p>
 *
 * @since 7.0.0
 */
public final class ArchModelSnapshots {

    private static final String INDENT = "      ";

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
        List<String> members = verdictHeader(type);
        if (type instanceof AggregateRoot aggregate) {
            members.add(identity(aggregate.identityField(), aggregate.effectiveIdentityType()));
            members.add(list("entities", aggregate.entities()));
            members.add(list("valueObjects", aggregate.valueObjects()));
            members.add(list("domainEvents", aggregate.domainEvents()));
            members.add(member("drivenPort", named(aggregate.drivenPort())));
        }
        if (type instanceof Entity entity) {
            members.add(identity(entity.identityField(), Optional.empty()));
            members.add(member("owningAggregate", named(entity.owningAggregate())));
        }
        if (type instanceof Identifier identifier) {
            members.add(member("wrappedType", named(identifier.wrappedType())));
        }
        if (type instanceof DomainEvent event) {
            members.add(identity(event.aggregateIdField(), Optional.empty()));
            members.add(member("sourceAggregate", named(event.sourceAggregate())));
        }
        if (type instanceof DomainService service) {
            members.add(list("injectedPorts", service.injectedPorts()));
        }
        members.add(properties(type.structure().fields()));
        return entry(members);
    }

    /**
     * Renders the identity a type carries, or {@code null} when the engine could not name the field
     * carrying it — a type read from a repository declaration or from a declared intent has a kind
     * before it has a named identity. The effective type is the value it is really stored under.
     */
    private static String identity(Optional<Field> field, Optional<TypeRef> effective) {
        if (field.isEmpty()) {
            return member("identity", "null");
        }
        List<String> members = new ArrayList<>(List.of(
                nested("field", quote(field.orElseThrow().name())),
                nested("type", quote(field.orElseThrow().type().qualifiedName()))));
        effective.ifPresent(type -> members.add(nested("effectiveType", quote(type.qualifiedName()))));
        return member("identity", "{\n" + String.join(",\n", members) + "\n" + INDENT + "}");
    }

    private static String applicationEntry(ApplicationType type) {
        return entry(verdictHeader(type));
    }

    private static String portEntry(PortType port) {
        List<String> members = new ArrayList<>();
        members.add(member("qualifiedName", quote(port.qualifiedName())));
        members.add(member("direction", quote(port.direction().name())));
        if (port instanceof DrivenPort driven) {
            members.add(member("portType", quote(driven.portType().name())));
            members.add(member("managedAggregate", named(driven.managedAggregate())));
        }
        if (port instanceof DrivingPort driving) {
            members.add(inline("useCases", driving.useCases().stream().map(ArchModelSnapshots::useCase)));
            members.add(list("inputTypes", driving.inputTypes()));
            members.add(list("outputTypes", driving.outputTypes()));
        }
        members.add(
                member("confidence", quote(port.classification().confidence().name())));
        members.add(member("basis", quote(port.classification().basis().name())));
        members.add(inline(
                "methods",
                port.structure().methods().stream()
                        .map(method -> quote(method.name()))
                        .sorted()));
        return entry(members);
    }

    private static String useCase(UseCase useCase) {
        return quote(useCase.method().name() + ": " + useCase.type().name());
    }

    private static String adapterEntry(AdapterType adapter) {
        return entry(new ArrayList<>(List.of(
                member("qualifiedName", quote(adapter.qualifiedName())),
                member("direction", quote(adapter.direction().name())),
                inline(
                        "ports",
                        adapter.ports().stream()
                                .map(port -> quote(port.qualifiedName()))
                                .sorted()),
                member("confidence", quote(adapter.classification().confidence().name())),
                member("basis", quote(adapter.classification().basis().name())))));
    }

    private static String unclassifiedEntry(UnclassifiedType type) {
        return entry(new ArrayList<>(List.of(
                member("qualifiedName", quote(type.qualifiedName())),
                member("category", quote(type.category().name())),
                member("reason", type.reason().map(ArchModelSnapshots::quote).orElse("null")))));
    }

    private static List<String> verdictHeader(ArchType type) {
        return new ArrayList<>(List.of(
                member("qualifiedName", quote(type.qualifiedName())),
                member("kind", quote(type.kind().name())),
                member("confidence", quote(type.classification().confidence().name())),
                member("basis", quote(type.classification().basis().name())),
                member("nature", quote(type.structure().nature().name()))));
    }

    private static String properties(List<Field> fields) {
        List<Field> sorted =
                fields.stream().sorted(Comparator.comparing(Field::name)).toList();
        if (sorted.isEmpty()) {
            return member("properties", "[]");
        }
        List<String> entries = sorted.stream().map(ArchModelSnapshots::property).toList();
        return member("properties", "[\n" + String.join(",\n", entries) + "\n" + INDENT + "]");
    }

    /**
     * What a field is, and what the analysis reached about it. Whatever the analysis did not reach
     * is left out rather than spelled as empty, so a snapshot shows what is known — and so that
     * recording one of these is a diff a person reads rather than a wall of absences.
     */
    private static String property(Field field) {
        List<String> members = new ArrayList<>(List.of(
                "          " + quote("name") + ": " + quote(field.name()),
                "          " + quote("type") + ": " + quote(field.type().qualifiedName())));
        field.elementType()
                .ifPresent(element ->
                        members.add("          " + quote("elementType") + ": " + quote(element.qualifiedName())));
        field.wrappedType()
                .ifPresent(wrapped ->
                        members.add("          " + quote("wrappedType") + ": " + quote(wrapped.qualifiedName())));
        if (!field.roles().isEmpty()) {
            String roles =
                    field.roles().stream().map(role -> quote(role.name())).collect(Collectors.joining(", "));
            members.add("          " + quote("roles") + ": [" + roles + "]");
        }
        return "        {\n" + String.join(",\n", members) + "\n        }";
    }

    private static String entry(List<String> members) {
        return "    {\n" + String.join(",\n", members) + "\n    }";
    }

    private static String member(String name, String value) {
        return INDENT + quote(name) + ": " + value;
    }

    private static String nested(String name, String value) {
        return INDENT + "  " + quote(name) + ": " + value;
    }

    /** A short array kept on one line, which is what makes a diff of a hundred of them readable. */
    private static String inline(String name, Stream<String> values) {
        return member(name, "[" + String.join(", ", values.toList()) + "]");
    }

    private static String list(String name, List<TypeRef> references) {
        return inline(name, references.stream().map(reference -> quote(reference.qualifiedName())));
    }

    private static String named(Optional<TypeRef> reference) {
        return reference.map(type -> quote(type.qualifiedName())).orElse("null");
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
