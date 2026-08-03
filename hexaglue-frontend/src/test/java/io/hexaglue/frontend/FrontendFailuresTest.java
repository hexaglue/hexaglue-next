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

package io.hexaglue.frontend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An analysis that cannot read what it was pointed at must say so. Returning an empty model
 * instead turns a broken setup into a clean report: every type unclassified, no violation, nothing
 * to fix — the most misleading answer the tool could give.
 */
class FrontendFailuresTest {

    @TempDir
    Path sources;

    private Diagnostic diagnosticOf(FrontendRequest request) {
        return assertThatExceptionOfType(FrontendException.class)
                .isThrownBy(() -> SpoonFrontend.analyze(request))
                .actual()
                .diagnostic();
    }

    @Test
    @DisplayName("refuses a source root that does not exist")
    void refusesMissingSourceRoot() {
        Diagnostic diagnostic = diagnosticOf(FrontendRequest.of(sources.resolve("absent")));

        assertThat(diagnostic.code()).isEqualTo(IssueCode.of("HG-FRONTEND-001"));
        assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.ERROR);
        assertThat(diagnostic.message()).contains("absent");
    }

    @Test
    @DisplayName("refuses a source root that is not a directory")
    void refusesSourceRootThatIsNotADirectory() {
        Path file = SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");

        assertThat(diagnosticOf(FrontendRequest.of(file)).code()).isEqualTo(IssueCode.of("HG-FRONTEND-001"));
    }

    @Test
    @DisplayName("refuses a classpath entry that does not exist")
    void refusesMissingClasspathEntry() {
        SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");

        Diagnostic diagnostic = diagnosticOf(FrontendRequest.builder()
                .sourceRoot(sources)
                .classpathEntry(sources.resolve("absent.jar"))
                .build());

        assertThat(diagnostic.code()).isEqualTo(IssueCode.of("HG-FRONTEND-002"));
        assertThat(diagnostic.message()).contains("absent.jar");
    }

    @Test
    @DisplayName("reports a parser failure as a coded diagnostic rather than a raw stack trace")
    void reportsParserFailure() {
        SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");

        Diagnostic diagnostic = diagnosticOf(
                FrontendRequest.builder().sourceRoot(sources).javaVersion(99).build());

        assertThat(diagnostic.code()).isEqualTo(IssueCode.of("HG-FRONTEND-003"));
        assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.ERROR);
    }

    @Test
    @DisplayName("recovers from a source it cannot fully parse, keeping what it could read")
    void recoversFromPartiallyUnparsableSource() {
        SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order { this is not Java");

        // Tolerant parsing is what makes an incomplete code base analyzable at all; the parser
        // recovers here rather than failing, and the type it could read is still reported.
        assertThat(SpoonFrontend.analyze(FrontendRequest.of(sources)).code().types()).isNotEmpty();
    }
}
