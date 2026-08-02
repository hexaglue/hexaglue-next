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
import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Field;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortPivotTest {

    private static final TypeId WAY_OUT = TypeId.of("com.acme.Ledger");
    private static final TypeId WAY_IN = TypeId.of("com.acme.Boarding");
    private static final TypeId PIVOT = TypeId.of("com.acme.Checkout");
    private static final TypeId PLAIN = TypeId.of("com.acme.Tally");
    private static final String SECONDARY_PORT = "org.jmolecules.architecture.hexagonal.SecondaryPort";
    private static final String PRIMARY_PORT = "org.jmolecules.architecture.hexagonal.PrimaryPort";
    private static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";

    /** A way out and a way in the author declared, so the pivot is read against settled ports. */
    private static TypeNode port(TypeId id, String annotation) {
        return TypeNode.builder(id, TypeNature.INTERFACE)
                .annotations(List.of(Annotation.of(annotation)))
                .build();
    }

    private static TypeNode plainNeighbour() {
        return TypeNode.builder(PLAIN, TypeNature.CLASS).build();
    }

    private static TypeNode holding(TypeId held, Modifier... modifiers) {
        return TypeNode.builder(PIVOT, TypeNature.CLASS)
                .modifiers(Set.of(modifiers))
                .fields(List.of(Field.of("collaborator", TypeRef.of(held.qualifiedName()))))
                .build();
    }

    private static TypeNode fulfilling(TypeId contract, TypeNature nature, Modifier... modifiers) {
        return TypeNode.builder(PIVOT, nature)
                .modifiers(Set.of(modifiers))
                .interfaces(List.of(TypeRef.of(contract.qualifiedName())))
                .build();
    }

    private static EngineContext context(TypeNode... types) {
        CodeModel.Builder code = CodeModel.builder();
        for (TypeNode type : types) {
            code.addType(type);
        }
        return EngineContext.of(code.build(), KnowledgePacks.embedded(), HexaGlueConfig.defaults());
    }

    private static Verdicts verdicts(TypeNode... types) {
        return Classifier.classify(context(types));
    }

    private static List<ArchKind> readingsOf(EngineContext context, TypeId subject) {
        Verdicts settled = Classifier.classify(context);
        return Saturation.saturate(RuleSet.standard(), context.withVerdicts(settled))
                .about(subject, KindEvidence.class)
                .stream()
                .map(KindEvidence::kind)
                .toList();
    }

    @Nested
    @DisplayName("reads the type standing between the ports")
    class ReadsTheTypeBetweenThePorts {

        @Test
        @DisplayName("as the application service it is, when it calls a way out")
        void whenItCallsAWayOut() {
            Classification verdict = verdicts(port(WAY_OUT, SECONDARY_PORT), holding(WAY_OUT))
                    .verdict(PIVOT)
                    .orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.APPLICATION_SERVICE);
            assertThat(verdict.evidences()).allMatch(evidence -> evidence.tier() == EvidenceTier.GRAPH_RELATION);
        }

        @Test
        @DisplayName("as the application service it is, when it answers a way in")
        void whenItAnswersAWayIn() {
            assertThat(verdicts(port(WAY_IN, PRIMARY_PORT), fulfilling(WAY_IN, TypeNature.CLASS))
                            .kindOf(PIVOT))
                    .contains(ArchKind.APPLICATION_SERVICE);
        }

        @Test
        @DisplayName("whether or not the class is abstract, because a partial answer is still an answer")
        void whetherOrNotTheClassIsAbstract() {
            // An abstract class holding ports plays the application role as much as the subclass
            // completing it does. Withholding the reading would say something about how the code
            // is factored, which is not what is being asked.
            assertThat(verdicts(port(WAY_IN, PRIMARY_PORT), fulfilling(WAY_IN, TypeNature.CLASS, Modifier.ABSTRACT))
                            .kindOf(PIVOT))
                    .contains(ArchKind.APPLICATION_SERVICE);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a class whose collaborator is no port at all, which orchestrates nothing")
        void aboutAClassWhoseCollaboratorIsNoPort() {
            // Holding a class is holding an implementation, and an implementation is not a
            // boundary. The way in sitting next to it belongs to nobody here: standing between
            // ports means standing between the ones this type actually touches.
            assertThat(verdicts(port(WAY_IN, PRIMARY_PORT), plainNeighbour(), holding(PLAIN))
                            .kindOf(PIVOT))
                    .contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about an interface refining a way in, which is a contract and not a pivot")
        void aboutAnInterfaceRefiningAWayIn() {
            assertThat(verdicts(port(WAY_IN, PRIMARY_PORT), fulfilling(WAY_IN, TypeNature.INTERFACE))
                            .kindOf(PIVOT))
                    .contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about an entry point reaching a way out directly, which stays on the ring")
        void aboutAnEntryPointReachingAWayOutDirectly() {
            // Skipping the application layer is a shortcut to report, not a promotion to earn: the
            // reading is withheld rather than emitted and outranked, so nothing in the model
            // claims this type orchestrates anything.
            TypeNode entryPoint = TypeNode.builder(PIVOT, TypeNature.CLASS)
                    .annotations(List.of(Annotation.of(REST_CONTROLLER)))
                    .fields(List.of(Field.of("collaborator", TypeRef.of(WAY_OUT.qualifiedName()))))
                    .build();
            EngineContext context = context(port(WAY_OUT, SECONDARY_PORT), entryPoint);

            assertThat(Classifier.classify(context).kindOf(PIVOT)).contains(ArchKind.DRIVING_ADAPTER);
            assertThat(readingsOf(context, PIVOT)).doesNotContain(ArchKind.APPLICATION_SERVICE);
        }
    }
}
