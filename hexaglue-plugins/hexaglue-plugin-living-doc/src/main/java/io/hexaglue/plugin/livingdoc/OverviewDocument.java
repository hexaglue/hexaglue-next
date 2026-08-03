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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.plugin.livingdoc.render.Markdown;
import io.hexaglue.plugin.livingdoc.render.Table;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The way in: how much of what there is, where it lives, and where to read about it.
 *
 * <p>The last section is the one that earns the page its trust — what the engine could not
 * classify, said out loud with the reason. Documentation that only shows what worked reads as a
 * complete architecture when it is a partial one.</p>
 */
final class OverviewDocument {

    /** The file name of this document. */
    static final String NAME = "README.md";

    private final ArchModel model;

    OverviewDocument(ArchModel model) {
        this.model = model;
    }

    /**
     * Writes the document.
     */
    String render() {
        Markdown document = Markdown.document()
                .heading(1, "Architecture")
                .paragraph("What this codebase is, read from its own source. Every page here is derived from the "
                        + "classified model, so nothing in it can drift from the code it describes.")
                .paragraph(Markdown.link("Domain", DomainDocument.NAME) + " · "
                        + Markdown.link("Ports", PortsDocument.NAME));
        writeCounts(document);
        writePackages(document);
        writeIndex(document);
        writeUnclassified(document);
        return document.render();
    }

    private void writeCounts(Markdown document) {
        Map<ArchKind, Integer> counts = new LinkedHashMap<>();
        model.types().forEach(type -> counts.merge(type.kind(), 1, Integer::sum));
        document.heading(2, "What the model holds");
        if (counts.isEmpty()) {
            document.paragraph("The analysis classified no type.");
            return;
        }
        Table table = Table.withHeaders("Kind", "Types");
        List.of(ArchKind.values()).stream()
                .filter(counts::containsKey)
                .forEach(kind -> table.row(kind.name(), String.valueOf(counts.get(kind))));
        document.table(table);
    }

    private void writePackages(Markdown document) {
        Map<String, Integer> byPackage = new TreeMap<>();
        model.types().forEach(type -> byPackage.merge(type.id().packageName(), 1, Integer::sum));
        document.heading(2, "Where it lives");
        if (byPackage.isEmpty()) {
            document.paragraph("No package holds a classified type.");
            return;
        }
        Table table = Table.withHeaders("Package", "Types");
        byPackage.forEach((name, count) -> table.row(Markdown.inlineCode(name), String.valueOf(count)));
        document.table(table);
    }

    private void writeIndex(Markdown document) {
        document.heading(2, "Index");
        List<ArchType> documented = model.types().stream()
                .filter(type -> documentOf(type).isPresent())
                .toList();
        if (documented.isEmpty()) {
            document.paragraph("No type has a page of its own.");
            return;
        }
        Table table = Table.withHeaders("Type", "Kind", "Package");
        documented.forEach(type -> table.row(
                Names.linkTo(type.id(), documentOf(type).orElseThrow()),
                type.kind().name(),
                Markdown.inlineCode(type.id().packageName())));
        document.table(table);
    }

    /**
     * Returns the document a type is written up in, empty when no page covers its kind.
     */
    private static Optional<String> documentOf(ArchType type) {
        return switch (type.kind()) {
            case AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, DOMAIN_EVENT, DOMAIN_SERVICE -> Optional.of(DomainDocument.NAME);
            case DRIVING_PORT, DRIVEN_PORT -> Optional.of(PortsDocument.NAME);
            default -> Optional.empty();
        };
    }

    private void writeUnclassified(Markdown document) {
        List<UnclassifiedType> unclassified = model.all(UnclassifiedType.class).toList();
        document.heading(2, "What could not be read");
        if (unclassified.isEmpty()) {
            document.paragraph("Every type of the perimeter carries a verdict.");
            return;
        }
        document.paragraph("These types were analyzed and left unclassified. They are part of the codebase all the "
                + "same, and a page that omitted them would read as a smaller architecture than the real one.");
        Table table = Table.withHeaders("Type", "Category", "Why");
        unclassified.forEach(type -> table.row(
                Markdown.inlineCode(type.id().toString()),
                type.category().name(),
                type.reason().orElse("—")));
        document.table(table);
    }
}
