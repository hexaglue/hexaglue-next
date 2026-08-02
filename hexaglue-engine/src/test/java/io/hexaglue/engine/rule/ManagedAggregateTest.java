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
import io.hexaglue.engine.Verdicts;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ManagedAggregateTest {

    private static final TypeId PORT = TypeId.of("com.acme.Ledger");
    private static final TypeId SUBJECT = TypeId.of("com.acme.Fleet");
    private static final TypeId CALLER = TypeId.of("com.acme.Checkout");
    private static final TypeRef PORT_REF = TypeRef.of(PORT.qualifiedName());
    private static final TypeRef SUBJECT_REF = TypeRef.of(SUBJECT.qualifiedName());
    private static final TypeRef TEXT = TypeRef.of("java.lang.String");
    private static final TypeRef VOID = TypeRef.of("void");

    /** A way out the core calls, whose signatures revolve around the subject it keeps. */
    private static TypeNode storage() {
        return TypeNode.builder(PORT, TypeNature.INTERFACE)
                .methods(List.of(
                        Method.builder("locate", TypeRef.parameterized("java.util.Optional", SUBJECT_REF))
                                .parameters(List.of(Parameter.of("reference", TEXT)))
                                .build(),
                        Method.builder("keep", VOID)
                                .parameters(List.of(Parameter.of("subject", SUBJECT_REF)))
                                .build()))
                .build();
    }

    /** A way out that hands the subject over and never takes it back: nothing converges. */
    private static TypeNode errand() {
        return TypeNode.builder(PORT, TypeNature.INTERFACE)
                .methods(List.of(Method.builder("quote", SUBJECT_REF)
                        .parameters(List.of(Parameter.of("reference", TEXT)))
                        .build()))
                .build();
    }

    private static TypeNode caller() {
        return TypeNode.builder(CALLER, TypeNature.CLASS)
                .fields(List.of(Field.of("ledger", PORT_REF)))
                .build();
    }

    /** State that can change, so the shape of the declaration says nothing on its own. */
    private static TypeNode subject() {
        return TypeNode.builder(SUBJECT, TypeNature.CLASS)
                .fields(List.of(Field.of("reference", TEXT)))
                .build();
    }

    private static Verdicts verdicts(TypeNode... types) {
        CodeModel.Builder code = CodeModel.builder();
        for (TypeNode type : types) {
            code.addType(type);
        }
        return Classifier.classify(
                EngineContext.of(code.build(), KnowledgePacks.embedded(), HexaGlueConfig.defaults()));
    }

    @Nested
    @DisplayName("reads the subject a way out keeps as the aggregate it is")
    class ReadsTheSubject {

        @Test
        @DisplayName("when the signatures of a way out converge on it")
        void whenTheSignaturesConvergeOnIt() {
            Verdicts settled = verdicts(storage(), caller(), subject());

            assertThat(settled.kindOf(PORT)).contains(ArchKind.DRIVEN_PORT);
            assertThat(settled.kindOf(SUBJECT)).contains(ArchKind.AGGREGATE_ROOT);
            assertThat(settled.verdict(SUBJECT).orElseThrow().evidences())
                    .extracting(Evidence::tier)
                    .containsExactly(EvidenceTier.GRAPH_RELATION);
        }

        @Test
        @DisplayName("naming the way out that keeps it, so the reason is readable")
        void namingTheWayOutThatKeepsIt() {
            Verdicts settled = verdicts(storage(), caller(), subject());

            assertThat(settled.verdict(SUBJECT).orElseThrow().evidences())
                    .extracting(Evidence::relatedTypes)
                    .containsExactly(List.of(PORT));
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a type a way out hands over without ever keeping it")
        void aboutATypeNoWayOutKeeps() {
            // A gateway names the subject too, and means something else entirely by it: asking a
            // service about a thing is not storing the thing.
            Verdicts settled = verdicts(errand(), caller(), subject());

            assertThat(settled.kindOf(PORT)).contains(ArchKind.DRIVEN_PORT);
            assertThat(settled.kindOf(SUBJECT)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about the subject of an interface no rule established as a way out")
        void aboutTheSubjectOfAnInterfaceThatIsNotAPort() {
            Verdicts settled = verdicts(storage(), subject());

            assertThat(settled.kindOf(PORT)).contains(ArchKind.UNCLASSIFIED);
            assertThat(settled.kindOf(SUBJECT)).contains(ArchKind.UNCLASSIFIED);
        }
    }
}
