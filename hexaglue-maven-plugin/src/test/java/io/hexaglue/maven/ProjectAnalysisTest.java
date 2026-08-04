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

import io.hexaglue.engine.Gate;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.Backends;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.arch.PortFamily;
import io.hexaglue.model.config.HexaGlueConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The chain end to end, from a directory of sources to a verdict on the build: what a host does
 * with HexaGlue, minus the logging.
 */
class ProjectAnalysisTest {

    @TempDir
    Path projectDir;

    private MavenProject projectWith(String relativePath, String source) {
        Path file = projectDir.resolve("src/main/java").resolve(relativePath);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, source);
        } catch (IOException unwritable) {
            throw new UncheckedIOException("Failed to write " + file, unwritable);
        }
        MavenProject project = new MavenProject();
        Build build = new Build();
        build.setSourceDirectory(projectDir.resolve("src/main/java").toString());
        build.setDirectory(projectDir.resolve("target").toString());
        build.setOutputDirectory(projectDir.resolve("target/classes").toString());
        project.getModel().setBuild(build);
        return project;
    }

    /**
     * A port nothing implements is a hole in these sources, and not a fault when a backend of this
     * build states it writes what fills it. The host is where the two facts meet.
     */
    @Test
    @DisplayName("says nothing about a hole this build states it fills, and says it left it out")
    void saysNothingAboutAHoleThisBuildFills() {
        MavenProject project = projectWith("com/acme/Order.java", """
                package com.acme;
                @org.jmolecules.ddd.annotation.AggregateRoot
                public class Order {
                    private final java.util.UUID id;
                    public Order(java.util.UUID id) { this.id = id; }
                    public java.util.UUID id() { return id; }
                }
                """);
        write("com/acme/Orders.java", """
                package com.acme;
                public interface Orders {
                    void save(Order order);
                    java.util.Optional<Order> findById(java.util.UUID id);
                }
                """);
        write("com/acme/PlaceOrder.java", """
                package com.acme;
                public class PlaceOrder {
                    private final Orders orders;
                    public PlaceOrder(Orders orders) { this.orders = orders; }
                    public void place(Order order) { orders.save(order); }
                }
                """);
        HexaGlueConfig config = ValidateMojo.configuration(HexaGlueConfig.defaults(), "com.acme", false);

        ProjectAnalysis.Result reported = ProjectAnalysis.run(project, config, Backends.none());
        ProjectAnalysis.Result generating = ProjectAnalysis.run(
                project,
                config,
                new Backends(Map.of("io.hexaglue.jpa", Set.of(PortFamily.driven(DrivenPortType.REPOSITORY)))));

        assertThat(reported.findings())
                .anySatisfy(finding -> assertThat(finding.code().value()).isEqualTo("HG-HEX-002"));
        assertThat(generating.findings())
                .noneSatisfy(finding -> assertThat(finding.code().value()).isEqualTo("HG-HEX-002"));
        assertThat(generating.diagnostics())
                .anySatisfy(diagnostic -> assertThat(diagnostic.message())
                        .contains("com.acme.Orders")
                        .contains("io.hexaglue.jpa"));
    }

    private void write(String relativePath, String source) {
        Path file = projectDir.resolve("src/main/java").resolve(relativePath);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, source);
        } catch (IOException unwritable) {
            throw new UncheckedIOException("Failed to write " + file, unwritable);
        }
    }

    @Test
    @DisplayName("classifies what the project declares, and passes when no gate is armed")
    void classifiesWhatTheProjectDeclares() {
        MavenProject project = projectWith("com/acme/Order.java", """
                package com.acme;
                @org.jmolecules.ddd.annotation.AggregateRoot
                public class Order {}
                """);

        ProjectAnalysis.Result result = ProjectAnalysis.run(
                project, ValidateMojo.configuration(HexaGlueConfig.defaults(), "com.acme", false), Backends.none());

        assertThat(result.model()
                        .type(TypeId.of("com.acme.Order"))
                        .orElseThrow()
                        .kind())
                .isEqualTo(ArchKind.AGGREGATE_ROOT);
        assertThat(result.validation().passed()).isTrue();
    }

    @Test
    @DisplayName("stops the build on a type it could not decide, once the gate is armed")
    void stopsTheBuildOnAnUndecidedType() {
        MavenProject project = projectWith("com/acme/Thing.java", "package com.acme; public class Thing {}");

        ProjectAnalysis.Result result = ProjectAnalysis.run(
                project, ValidateMojo.configuration(HexaGlueConfig.defaults(), "com.acme", true), Backends.none());

        assertThat(result.validation().passed()).isFalse();
        assertThat(result.validation().refusals()).singleElement().satisfies(refusal -> {
            assertThat(refusal.gate()).isEqualTo(Gate.UNCLASSIFIED);
            assertThat(refusal.subject().id()).isEqualTo(TypeId.of("com.acme.Thing"));
        });
    }

    @Test
    @DisplayName("accounts for a type it read and did not classify, which no reading can report")
    void accountsForATypeReadButNotClassified() {
        MavenProject project = projectWith("com/acme/Order.java", "package com.acme; public class Order {}");
        Path outside = projectDir.resolve("src/main/java/com/other/Tool.java");
        try {
            Files.createDirectories(outside.getParent());
            Files.writeString(outside, "package com.other; public class Tool {}");
        } catch (IOException unwritable) {
            throw new UncheckedIOException("Failed to write " + outside, unwritable);
        }

        ProjectAnalysis.Result result = ProjectAnalysis.run(
                project, ValidateMojo.configuration(HexaGlueConfig.defaults(), "com.acme", false), Backends.none());

        // Read on purpose — what it says about its neighbours counts — and then left without a
        // verdict. The frontend cannot report it: from where it stands, the type was read.
        assertThat(result.model().type(TypeId.of("com.other.Tool"))).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .satisfies(diagnostic -> assertThat(diagnostic.subject()).contains(TypeId.of("com.other.Tool")));
    }

    @Test
    @DisplayName("accounts for what the reading left out, rather than losing it silently")
    void accountsForWhatTheReadingLeftOut() {
        MavenProject project = projectWith("com/acme/OrderAdapter.java", """
                package com.acme;
                @jakarta.annotation.Generated("some-generator")
                public class OrderAdapter {}
                """);

        ProjectAnalysis.Result result = ProjectAnalysis.run(
                project, ValidateMojo.configuration(HexaGlueConfig.defaults(), "com.acme", false), Backends.none());

        assertThat(result.model().type(TypeId.of("com.acme.OrderAdapter"))).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .satisfies(diagnostic -> assertThat(diagnostic.subject()).contains(TypeId.of("com.acme.OrderAdapter")));
    }
}
