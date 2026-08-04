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

package io.hexaglue.maven;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.TypeId;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What a build log carries of what a run left out. A perimeter doing its job excludes most of a
 * codebase, so those are counted; anything said about the run itself is said.
 */
class DiagnosticsTest {

    /** A log that keeps what was said at each level, so a test can read the build log. */
    private static final class Recording extends SystemStreamLog {

        private final List<String> info = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        @Override
        public void info(CharSequence content) {
            info.add(content.toString());
        }

        @Override
        public void warn(CharSequence content) {
            warnings.add(content.toString());
        }
    }

    private static Diagnostic aboutOneType(String type) {
        return Diagnostic.builder(
                        IssueCode.of("HG-ENGINE-003"),
                        DiagnosticSeverity.INFO,
                        type + " was read but not classified: out of the configured scope")
                .subject(TypeId.of(type))
                .build();
    }

    private static Diagnostic aboutTheRun(String message) {
        return Diagnostic.builder(IssueCode.of("HG-ENGINE-005"), DiagnosticSeverity.INFO, message)
                .build();
    }

    @Test
    @DisplayName("counts what was left out of the analysis, one type at a time")
    void countsWhatWasLeftOut() {
        Recording log = new Recording();

        Diagnostics.report(List.of(aboutOneType("com.acme.Order"), aboutOneType("com.acme.Money")), log);

        assertThat(log.info).singleElement().asString().contains("2 type(s) were not analysed");
    }

    /**
     * A check falling silent is about the run and not about one type. Counting it among the
     * exclusions would hide the one thing a reader has to be told: that fewer findings than the
     * sources warrant is deliberate.
     */
    @Test
    @DisplayName("but says plainly what it has to say about the run itself")
    void saysWhatIsAboutTheRun() {
        Recording log = new Recording();

        Diagnostics.report(
                List.of(
                        aboutOneType("com.acme.Order"),
                        aboutTheRun("1 port(s) were not reported: this build writes"
                                + " what fills them — com.acme.Orders (io.hexaglue.jpa)")),
                log);

        assertThat(log.info)
                .anySatisfy(said -> assertThat(said).contains("HG-ENGINE-005").contains("com.acme.Orders"))
                .anySatisfy(said -> assertThat(said).contains("1 type(s) were not analysed"));
        assertThat(log.warnings).isEmpty();
    }

    @Test
    @DisplayName("and warns about anything that went wrong, one line each")
    void warnsAboutWhatWentWrong() {
        Recording log = new Recording();
        Log logging = log;

        Diagnostics.report(
                List.of(Diagnostic.builder(
                                IssueCode.of("HG-ENGINE-004"),
                                DiagnosticSeverity.WARNING,
                                "persistence was read but its role is not declared")
                        .build()),
                logging);

        assertThat(log.warnings).singleElement().asString().contains("HG-ENGINE-004");
        assertThat(log.info).isEmpty();
    }
}
