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

package io.hexaglue.engine.rule;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.engine.Classifier;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.engine.KindEvidence;
import io.hexaglue.engine.RuleSet;
import io.hexaglue.engine.Saturation;
import io.hexaglue.engine.Verdicts;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.ClassificationConfig;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortImplementationTest {

    private static final TypeId CONTRACT = TypeId.of("com.acme.Ledger");
    private static final TypeId IMPLEMENTER = TypeId.of("com.acme.LedgerBook");
    private static final String SECONDARY_PORT = "org.jmolecules.architecture.hexagonal.SecondaryPort";
    private static final String JMOLECULES_AGGREGATE = "org.jmolecules.ddd.annotation.AggregateRoot";

    /** The author's own declaration that this contract is a way out of the hexagon. */
    private static TypeNode declaredPort() {
        return TypeNode.builder(CONTRACT, TypeNature.INTERFACE)
                .annotations(List.of(Annotation.of(SECONDARY_PORT)))
                .build();
    }

    private static TypeNode plainContract() {
        return TypeNode.builder(CONTRACT, TypeNature.INTERFACE).build();
    }

    private static TypeNode implementer(TypeNature nature, String... annotations) {
        return TypeNode.builder(IMPLEMENTER, nature)
                .interfaces(List.of(TypeRef.of(CONTRACT.qualifiedName())))
                .annotations(List.of(annotations).stream().map(Annotation::of).toList())
                .build();
    }

    private static EngineContext context(TypeNode... types) {
        return context(HexaGlueConfig.defaults(), types);
    }

    private static EngineContext context(HexaGlueConfig config, TypeNode... types) {
        CodeModel.Builder code = CodeModel.builder();
        for (TypeNode type : types) {
            code.addType(type);
        }
        return EngineContext.of(code.build(), KnowledgePacks.embedded(), config);
    }

    /** The user's own word on what a type is, which nothing the engine reads may contradict. */
    private static HexaGlueConfig declaring(TypeId subject, ArchKind kind) {
        HexaGlueConfig defaults = HexaGlueConfig.defaults();
        return new HexaGlueConfig(
                defaults.analysis(),
                new ClassificationConfig(Map.of(subject, kind), Map.of()),
                defaults.validation(),
                defaults.generation());
    }

    private static Verdicts verdicts(TypeNode... types) {
        return Classifier.classify(context(types));
    }

    @Nested
    @DisplayName("reads what implements an established port")
    class ReadsWhatImplementsAnEstablishedPort {

        @Test
        @DisplayName("as the driven adapter it is, even with no framework symbol on it at all")
        void anImplementerOfAPortIsADrivenAdapter() {
            // The other side of a way out is an adapter by position. This is the reading the
            // hexagonal codebases need: their adapters carry nothing a pack would recognize, and
            // only the port they implement places them.
            Classification verdict = verdicts(declaredPort(), implementer(TypeNature.CLASS))
                    .verdict(IMPLEMENTER)
                    .orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVEN_ADAPTER);
            assertThat(verdict.direction()).contains(PortDirection.DRIVEN);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about what implements an interface no rule established as a port")
        void aboutWhatImplementsAPlainInterface() {
            assertThat(verdicts(plainContract(), implementer(TypeNature.CLASS)).kindOf(IMPLEMENTER))
                    .contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about an interface extending a port, which refines a contract rather than fulfils it")
        void aboutAnInterfaceExtendingAPort() {
            assertThat(verdicts(declaredPort(), implementer(TypeNature.INTERFACE))
                            .kindOf(IMPLEMENTER))
                    .contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about an implementer the core already claims, which is a false frontier and not an adapter")
        void aboutAnImplementerTheCoreClaims() {
            // A domain type fulfilling a way out is a frontier that is not one, and the audit has
            // a finding for it. What it must not become is an adapter: the reading is withheld
            // rather than emitted and outweighed, so nothing in the report claims two positions
            // for one type.
            EngineContext context = context(declaredPort(), implementer(TypeNature.CLASS, JMOLECULES_AGGREGATE));
            Verdicts settled = Classifier.classify(context);

            assertThat(settled.kindOf(IMPLEMENTER)).contains(ArchKind.AGGREGATE_ROOT);
            assertThat(Saturation.saturate(RuleSet.standard(), context.withVerdicts(settled))
                            .about(IMPLEMENTER, KindEvidence.class))
                    .noneMatch(evidence -> evidence.kind() == ArchKind.DRIVEN_ADAPTER);
        }

        @Test
        @DisplayName("about an implementer the application claims, which orchestrates rather than adapts")
        void aboutAnImplementerTheApplicationClaims() {
            // The same withholding for the layer inside the boundary rather than at its centre: a
            // service the user declared is where the use cases live, and fulfilling a way out from
            // there is a coupling to report, not a position to reassign.
            EngineContext context = context(
                    declaring(IMPLEMENTER, ArchKind.APPLICATION_SERVICE),
                    declaredPort(),
                    implementer(TypeNature.CLASS));
            Verdicts settled = Classifier.classify(context);

            assertThat(settled.kindOf(IMPLEMENTER)).contains(ArchKind.APPLICATION_SERVICE);
            assertThat(Saturation.saturate(RuleSet.standard(), context.withVerdicts(settled))
                            .about(IMPLEMENTER, KindEvidence.class))
                    .noneMatch(evidence -> evidence.kind() == ArchKind.DRIVEN_ADAPTER);
        }
    }
}
