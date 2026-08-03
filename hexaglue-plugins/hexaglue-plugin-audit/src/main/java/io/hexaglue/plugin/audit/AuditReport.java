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

package io.hexaglue.plugin.audit;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.ModuleDescriptor;
import io.hexaglue.model.arch.ModuleTopology;
import io.hexaglue.model.arch.Stability;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.Severity;
import io.hexaglue.render.Graph;
import io.hexaglue.render.Markdown;
import io.hexaglue.render.Table;
import io.hexaglue.spi.Measurements;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The report: what the architecture is, what holds, what does not, and what it would take.
 *
 * <p>Every section is read from what the run already concluded. Nothing here classifies a type,
 * judges an architecture or measures a package — those answers exist, and a report that produced
 * its own would be a second set that disagreed with the gate the first time they parted ways.</p>
 *
 * <p>What earns the rest its credit is the section on the reading itself: how much was declared
 * and how much deduced. A report that showed only conclusions would let an inferred architecture
 * be read as a stated one.</p>
 *
 * @since 7.0.0
 */
final class AuditReport {

    /** The file name of the report. */
    static final String NAME = "architecture-audit.md";

    /** Roughly what one violation of each severity costs to put right, in hours. */
    private static final Map<Severity, Integer> HOURS =
            Map.of(Severity.BLOCKER, 8, Severity.CRITICAL, 4, Severity.MAJOR, 2, Severity.MINOR, 1, Severity.INFO, 0);

    private final ArchModel model;
    private final List<Finding> findings;
    private final Measurements measurements;
    private final AuditOptions options;
    private final Score score;

    AuditReport(ArchModel model, List<Finding> findings, Measurements measurements, AuditOptions options) {
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.findings = List.copyOf(findings);
        this.measurements = Objects.requireNonNull(measurements, "measurements must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.score = Score.of(model, this.findings, measurements);
    }

    /**
     * Returns the score the whole report rests on.
     */
    Score score() {
        return score;
    }

    /**
     * Returns the lines a host logs, for a reader who will not open the report.
     */
    List<String> summary() {
        List<String> lines = new ArrayList<>();
        lines.add("Architecture audit: " + score.overall() + "/100, grade " + score.grade());
        lines.add("  read " + score.readable() + " · sound " + score.sound() + " · untangled " + score.untangled()
                + " · well-directed " + score.wellDirected());
        counts().forEach((severity, count) -> lines.add("  " + severity + ": " + count));
        if (findings.isEmpty()) {
            lines.add("  nothing to report");
        }
        return lines;
    }

    /**
     * Writes the report.
     */
    String render() {
        Markdown document = Markdown.document().heading(1, "Architecture audit");
        writeVerdict(document);
        writeBreakdown(document);
        writeViolations(document);
        writeReliability(document);
        writeMetrics(document);
        writeInventory(document);
        writeStability(document);
        writeRemediation(document);
        return document.render();
    }

    private void writeVerdict(Markdown document) {
        document.heading(2, "Verdict")
                .paragraph(Markdown.bold(score.overall() + "/100") + " — grade " + Markdown.bold(score.grade()) + ", "
                        + findings.size() + " violation" + (findings.size() == 1 ? "" : "s") + " over "
                        + model.types().size() + " analysed types.");
        Table table = Table.withHeaders("Severity", "Violations");
        counts().forEach((severity, count) -> table.row(severity.name(), String.valueOf(count)));
        if (!table.isEmpty()) {
            document.table(table);
        }
    }

    private void writeBreakdown(Markdown document) {
        document.heading(2, "What the score is made of")
                .paragraph("A single figure is the worst thing to look at alone: a codebase scores badly because "
                        + "nothing could be recognised, or because everything was and half of it breaks its own "
                        + "rules — and those call for opposite things.");
        document.table(Table.withHeaders("Dimension", "Score", "What it counts")
                .row("Read", score.readable() + "/100", "types the analysis could name")
                .row("Sound", score.sound() + "/100", "named types no serious violation is about")
                .row("Untangled", score.untangled() + "/100", "packages outside every dependency knot")
                .row("Well-directed", score.wellDirected() + "/100", "packages whose abstraction matches their use"));
    }

    private void writeViolations(Markdown document) {
        document.heading(2, "Violations");
        if (findings.isEmpty()) {
            document.paragraph("Nothing was found against this architecture.");
            return;
        }
        Table table = Table.withHeaders("Code", "Severity", "Subject", "What was found");
        findings.forEach(finding -> table.row(
                Markdown.inlineCode(finding.code().value()),
                finding.severity().name(),
                Markdown.inlineCode(finding.subject().simpleName()),
                finding.message()));
        document.table(table);
    }

    private void writeReliability(Markdown document) {
        long declared = model.types().stream()
                .filter(type -> type.classification().basis() == Basis.DECLARED)
                .count();
        long ambiguous = model.types().stream()
                .filter(type -> type.classification().isAmbiguous())
                .count();
        long unclassified = model.types().stream()
                .filter(type -> type.kind() == ArchKind.UNCLASSIFIED)
                .count();
        document.heading(2, "How far to trust this")
                .paragraph("Everything above rests on the reading below. A kind the sources state is a fact; a kind "
                        + "the engine deduced is a reading, and a good one is still a reading.");
        document.table(Table.withHeaders("Reading", "Types")
                .row("Stated by the sources", String.valueOf(declared))
                .row("Deduced by the engine", String.valueOf(model.types().size() - declared))
                .row("Left between candidates", String.valueOf(ambiguous))
                .row("Not classified at all", String.valueOf(unclassified)));
    }

    private void writeMetrics(Markdown document) {
        Map<ArchKind, Integer> byKind = new LinkedHashMap<>();
        model.types().forEach(type -> byKind.merge(type.kind(), 1, Integer::sum));
        document.heading(2, "Quality metrics");
        Table table = Table.withHeaders("Metric", "Value")
                .row("Types analysed", String.valueOf(model.types().size()))
                .row("Packages", String.valueOf(measurements.packages().size()))
                .row("Dependency knots", String.valueOf(measurements.cycles().size()))
                .row("Aggregate roots", String.valueOf(byKind.getOrDefault(ArchKind.AGGREGATE_ROOT, 0)))
                .row(
                        "Ports",
                        String.valueOf(byKind.getOrDefault(ArchKind.DRIVING_PORT, 0)
                                + byKind.getOrDefault(ArchKind.DRIVEN_PORT, 0)))
                .row(
                        "Adapters",
                        String.valueOf(byKind.getOrDefault(ArchKind.DRIVING_ADAPTER, 0)
                                + byKind.getOrDefault(ArchKind.DRIVEN_ADAPTER, 0)));
        document.table(table);
        if (options.diagrams() && !measurements.cycles().isEmpty()) {
            document.paragraph("The packages that depend on each other in a circle:")
                    .code("mermaid", knots());
        }
    }

    private String knots() {
        Graph graph = Graph.flowing(Graph.Direction.LEFT_TO_RIGHT);
        for (int index = 0; index < measurements.cycles().size(); index++) {
            List<String> knot = measurements.cycles().get(index);
            graph.group("knot" + index, "Knot " + (index + 1));
            knot.forEach(name -> graph.node(name, name, Graph.Shape.BOX));
            graph.endGroup();
            for (int step = 0; step < knot.size(); step++) {
                graph.arrow(knot.get(step), knot.get((step + 1) % knot.size()));
            }
        }
        return graph.render();
    }

    private void writeInventory(Markdown document) {
        document.heading(2, "Inventory").paragraph("Every analysed type, with what the verdict about it rests on.");
        writeLayout(document);
        ModuleTopology topology = model.moduleTopology();
        Table table = topology.isEmpty()
                ? Table.withHeaders("Type", "Kind", "Confidence", "Basis", "Package")
                : Table.withHeaders("Type", "Kind", "Confidence", "Basis", "Package", "Module");
        for (ArchType type : model.types()) {
            List<String> cells = new ArrayList<>(List.of(
                    Markdown.inlineCode(type.id().simpleName()),
                    type.kind().name(),
                    type.classification().confidence().name(),
                    type.classification().basis().name(),
                    Markdown.inlineCode(type.id().packageName())));
            if (!topology.isEmpty()) {
                cells.add(topology.moduleOf(type.id())
                        .map(module -> Markdown.inlineCode(module.name()))
                        .orElse("—"));
            }
            table.row(cells.toArray(String[]::new));
        }
        document.table(table);
    }

    /**
     * Says how the build is laid out, when there is more than one module to lay out.
     *
     * <p>On a reactor the split itself is a claim about the architecture — this module holds the
     * domain, that one the plumbing — and a report that only listed types would leave the reader to
     * check that claim by opening POMs. What is stated as a role and what the references actually
     * do are both here, side by side, because they can disagree.</p>
     */
    private void writeLayout(Markdown document) {
        ModuleTopology topology = model.moduleTopology();
        if (topology.isEmpty()) {
            return;
        }
        document.paragraph("The reactor holds " + topology.size() + " module(s) whose role the project declares.");
        Table table = Table.withHeaders("Module", "Declared role", "Depends on", "Types", "Holds a domain");
        for (ModuleDescriptor module : topology.modules()) {
            Set<String> dependencies = topology.dependenciesOf(module.name());
            table.row(
                    Markdown.inlineCode(module.name()),
                    module.role().name(),
                    dependencies.isEmpty() ? "nothing" : String.join(", ", dependencies),
                    String.valueOf(topology.typesInModule(module.name()).size()),
                    topology.isDomainCandidate(module.name()) ? "yes" : "no");
        }
        document.table(table);
    }

    private void writeStability(Markdown document) {
        document.heading(2, "Package stability");
        if (measurements.packages().isEmpty()) {
            document.paragraph("No package was measured.");
            return;
        }
        document.paragraph("A package many others depend on is hard to change, and should be abstract enough that "
                + "what depends on it depends on a shape. The distance says how far from that line it sits.");
        Table table = Table.withHeaders("Package", "Used by", "Uses", "Instability", "Abstractness", "Distance");
        measurements
                .packages()
                .forEach(stability -> table.row(
                        Markdown.inlineCode(stability.packageName()),
                        String.valueOf(stability.afferent()),
                        String.valueOf(stability.efferent()),
                        ratio(stability.instability()),
                        ratio(stability.abstractness()),
                        ratio(stability.distance())));
        document.table(table);
    }

    private void writeRemediation(Markdown document) {
        document.heading(2, "What it would take");
        if (findings.isEmpty()) {
            document.paragraph("Nothing.");
            return;
        }
        document.paragraph("A rough order of magnitude, not an estimate: one violation of each kind, costed at what "
                + "that kind of change usually costs. Read it to sort the work, never to plan it.");
        Table table = Table.withHeaders("Severity", "Violations", "Hours");
        int total = 0;
        for (Map.Entry<Severity, Integer> entry : counts().entrySet()) {
            int hours = entry.getValue() * HOURS.getOrDefault(entry.getKey(), 0);
            total += hours;
            table.row(entry.getKey().name(), String.valueOf(entry.getValue()), String.valueOf(hours));
        }
        table.row(Markdown.bold("Total"), String.valueOf(findings.size()), Markdown.bold(String.valueOf(total)));
        document.table(table);
    }

    /**
     * The violations by severity, most serious first, skipping the severities nothing reached.
     */
    Map<Severity, Integer> counts() {
        Map<Severity, Integer> counts = new LinkedHashMap<>();
        for (Severity severity : Severity.values()) {
            int count = (int) findings.stream()
                    .filter(finding -> finding.severity() == severity)
                    .count();
            if (count > 0) {
                counts.put(severity, count);
            }
        }
        return counts;
    }

    List<Stability> packages() {
        return measurements.packages();
    }

    List<Finding> findings() {
        return findings;
    }

    ArchModel model() {
        return model;
    }

    private static String ratio(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
