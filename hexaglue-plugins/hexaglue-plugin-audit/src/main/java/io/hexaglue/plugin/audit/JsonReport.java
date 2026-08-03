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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.hexaglue.model.arch.ArchType;

/**
 * The same report, as data.
 *
 * <p>Written for whatever reads it next — a site, a dashboard, a diff against yesterday's. The
 * output is ordered and indented so that two runs on the same sources produce the same bytes:
 * a report that cannot be diffed is a report nobody notices a change in.</p>
 *
 * <p>Escaping is Jackson's business. The previous engine wrote this by hand in 834 lines and got
 * it wrong on names carrying quotes.</p>
 *
 * @since 7.0.0
 */
final class JsonReport {

    /** The file name of the data form of the report. */
    static final String NAME = "architecture-audit.json";

    private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private JsonReport() {}

    /**
     * Writes the report as JSON.
     *
     * @param report the assembled report
     * @return the JSON document
     * @throws IllegalStateException if the tree cannot be written, which would be a defect here
     */
    static String render(AuditReport report) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        Score score = report.score();
        ObjectNode verdict = root.putObject("verdict");
        verdict.put("score", score.overall());
        verdict.put("grade", score.grade());
        verdict.put("violations", report.findings().size());
        verdict.put("types", report.model().types().size());

        ObjectNode breakdown = root.putObject("breakdown");
        breakdown.put("read", score.readable());
        breakdown.put("sound", score.sound());
        breakdown.put("untangled", score.untangled());
        breakdown.put("wellDirected", score.wellDirected());

        ObjectNode severities = root.putObject("severities");
        report.counts().forEach((severity, count) -> severities.put(severity.name(), count));

        ArrayNode violations = root.putArray("violations");
        report.findings().forEach(finding -> {
            ObjectNode node = violations.addObject();
            node.put("code", finding.code().value());
            node.put("severity", finding.severity().name());
            node.put("subject", finding.subject().toString());
            node.put("message", finding.message());
        });

        ArrayNode inventory = root.putArray("inventory");
        for (ArchType type : report.model().types()) {
            ObjectNode node = inventory.addObject();
            node.put("type", type.id().toString());
            node.put("kind", type.kind().name());
            node.put("confidence", type.classification().confidence().name());
            node.put("basis", type.classification().basis().name());
        }

        ArrayNode packages = root.putArray("packages");
        report.packages().forEach(stability -> {
            ObjectNode node = packages.addObject();
            node.put("package", stability.packageName());
            node.put("afferent", stability.afferent());
            node.put("efferent", stability.efferent());
            node.put("instability", stability.instability());
            node.put("abstractness", stability.abstractness());
            node.put("distance", stability.distance());
        });

        try {
            return WRITER.writeValueAsString(root) + "\n";
        } catch (JsonProcessingException impossible) {
            // The tree is built here from primitives and strings; nothing in it can fail to write.
            throw new IllegalStateException("the report could not be written as JSON", impossible);
        }
    }
}
