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

import io.hexaglue.model.TypeId;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the frontend leaves out must be nameable. Nothing downstream can say it: the engine only
 * ever sees what was read, so a type dropped here is a type nobody can account for — and a model
 * that is merely smaller reads as a smaller code base rather than as a narrower reading.
 */
class FrontendDiagnosticsTest {

    private static final IssueCode OUT_OF_SCOPE = IssueCode.of("HG-FRONTEND-004");
    private static final IssueCode GENERATED = IssueCode.of("HG-FRONTEND-005");

    @TempDir
    Path sources;

    private FrontendResult read(AnalysisScope scope) {
        return SpoonFrontend.analyze(
                FrontendRequest.builder().sourceRoot(sources).scope(scope).build());
    }

    private static Optional<Diagnostic> about(FrontendResult result, String qualifiedName) {
        return result.diagnostics().stream()
                .filter(diagnostic -> diagnostic.subject().equals(Optional.of(TypeId.of(qualifiedName))))
                .findFirst();
    }

    @Test
    @DisplayName("names a type its package perimeter left out")
    void namesATypeLeftOutOfTheScope() {
        SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
        SourceFixtures.write(sources, "com/other/Tool.java", "package com.other; public class Tool {}");

        FrontendResult result = read(new AnalysisScope(Optional.empty(), List.of("com.acme"), List.of()));

        assertThat(result.code().types()).hasSize(1);
        Diagnostic left = about(result, "com.other.Tool").orElseThrow();
        assertThat(left.code()).isEqualTo(OUT_OF_SCOPE);
        assertThat(left.severity()).isEqualTo(DiagnosticSeverity.INFO);
        assertThat(left.message()).contains("com.other");
    }

    @Test
    @DisplayName("names a type it left out as generated code, and the marker that says so")
    void namesGeneratedCodeAndItsMarker() {
        SourceFixtures.write(sources, "com/acme/OrderAdapter.java", """
                package com.acme;
                @jakarta.annotation.Generated("hexaglue")
                public class OrderAdapter {}
                """);

        FrontendResult result = read(AnalysisScope.everything());

        assertThat(result.code().types()).isEmpty();
        Diagnostic left = about(result, "com.acme.OrderAdapter").orElseThrow();
        assertThat(left.code()).isEqualTo(GENERATED);
        assertThat(left.severity()).isEqualTo(DiagnosticSeverity.INFO);
        assertThat(left.message()).contains("jakarta.annotation.Generated");
    }

    @Test
    @DisplayName("says nothing when it read everything it was given")
    void saysNothingWhenEverythingWasRead() {
        SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");

        assertThat(read(AnalysisScope.everything()).diagnostics()).isEmpty();
    }

    @Test
    @DisplayName("says when the parser recovered from a source it could not fully read")
    void saysWhenTheParserRecovered() {
        SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order { this is not Java");

        FrontendResult result = read(AnalysisScope.everything());

        // Tolerant parsing is what makes an incomplete code base analyzable at all, so the type is
        // still there — but a declaration read half-way is a reason for a verdict to be wrong, and
        // nothing downstream can tell that from a declaration the author wrote that way.
        assertThat(result.code().types()).isNotEmpty();
        Diagnostic recovered = result.diagnostics().get(0);
        assertThat(recovered.code()).isEqualTo(IssueCode.of("HG-FRONTEND-006"));
        assertThat(recovered.severity()).isEqualTo(DiagnosticSeverity.WARNING);
        assertThat(recovered.subject()).isEmpty();
    }

    @Test
    @DisplayName("stays silent about the parser when a reference it cannot resolve is the only gap")
    void staysSilentOnUnresolvedReferences() {
        SourceFixtures.write(sources, "com/acme/Order.java", """
                package com.acme;
                public class Order extends com.absent.Base {
                    private com.absent.Money total;
                }
                """);

        // An unresolved reference is the normal condition of an analysis run without a full
        // classpath, not a degraded reading: saying so on every project would make the channel
        // worthless on the day it has something to report.
        assertThat(read(AnalysisScope.everything()).diagnostics()).isEmpty();
    }

    @Test
    @DisplayName("reports what it left out in a stable order")
    void reportsExclusionsInAStableOrder() {
        SourceFixtures.write(sources, "com/other/Zebra.java", "package com.other; public class Zebra {}");
        SourceFixtures.write(sources, "com/other/Alpha.java", "package com.other; public class Alpha {}");

        FrontendResult result = read(new AnalysisScope(Optional.empty(), List.of("com.acme"), List.of()));

        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.subject().orElseThrow().qualifiedName())
                .containsExactly("com.other.Alpha", "com.other.Zebra");
    }
}
