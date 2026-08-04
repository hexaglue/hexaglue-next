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

package io.hexaglue.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ApplicationService;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.arch.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OutcomeTest {

    private static TypeStructure structure(TypeId id) {
        CodeModel code = CodeModel.builder()
                .addType(TypeNode.builder(id, TypeNature.CLASS).build())
                .build();
        TypeNode declared = code.type(id).orElseThrow();
        return Structures.of(code).of(declared, declared.fields());
    }

    private static Classification verdict(ArchKind kind, Basis basis) {
        return Classification.builder(kind, Confidence.HIGH, basis, ProofNode.fact("because " + kind))
                .build();
    }

    private static ArchType aggregate(String name) {
        TypeId id = TypeId.of(name);
        return new AggregateRoot(
                id,
                structure(id),
                verdict(ArchKind.AGGREGATE_ROOT, Basis.DECLARED),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                List.of());
    }

    private static ArchType value(String name) {
        TypeId id = TypeId.of(name);
        return new ValueObject(id, structure(id), verdict(ArchKind.VALUE_OBJECT, Basis.INFERRED));
    }

    private static ArchType service(String name) {
        TypeId id = TypeId.of(name);
        return new ApplicationService(id, structure(id), verdict(ArchKind.APPLICATION_SERVICE, Basis.INFERRED));
    }

    private static ArchType silent(String name, UnclassifiedCategory category) {
        TypeId id = TypeId.of(name);
        return new UnclassifiedType(
                id,
                structure(id),
                Classification.builder(
                                ArchKind.UNCLASSIFIED, Confidence.LOW, Basis.INFERRED, ProofNode.fact("no signal"))
                        .build(),
                category,
                Optional.empty());
    }

    private static ArchType tied(String name) {
        TypeId id = TypeId.of(name);
        Evidence shape = Evidence.of(
                EvidenceTier.LOCAL_STRUCTURE, Confidence.HIGH, "IMMUTABLE(" + name + ")", "it never changes");
        return new UnclassifiedType(
                id,
                structure(id),
                Classification.builder(ArchKind.UNCLASSIFIED, Confidence.LOW, Basis.INFERRED, ProofNode.fact("tied"))
                        .candidates(List.of(
                                new Candidate(ArchKind.ENTITY, 1000, List.of(shape)),
                                new Candidate(ArchKind.VALUE_OBJECT, 1000, List.of(shape))))
                        .build(),
                UnclassifiedCategory.AMBIGUOUS,
                Optional.empty());
    }

    private static ArchModel model(ArchType... types) {
        ArchModel.Builder builder = ArchModel.builder();
        for (ArchType type : types) {
            builder.addType(type);
        }
        return builder.build();
    }

    @Nested
    @DisplayName("counts what the engine decided")
    class CountsWhatTheEngineDecided {

        @Test
        @DisplayName("one tally per kind anything reached, in the order of the vocabulary")
        void oneTallyPerKindAnythingReachedInTheOrderOfTheVocabulary() {
            Outcome outcome =
                    Outcome.of(model(aggregate("com.acme.Order"), value("com.acme.Money"), value("com.acme.Address")));

            assertThat(outcome.kinds())
                    .containsExactly(
                            new Outcome.Tally<>(ArchKind.AGGREGATE_ROOT, 1),
                            new Outcome.Tally<>(ArchKind.VALUE_OBJECT, 2));
        }

        @Test
        @DisplayName("a total that is the sum of the tallies")
        void aTotalThatIsTheSumOfTheTallies() {
            Outcome outcome = Outcome.of(model(aggregate("com.acme.Order"), value("com.acme.Money")));

            assertThat(outcome.types()).isEqualTo(2);
        }

        @Test
        @DisplayName("separating what was declared from what was inferred")
        void separatingWhatWasDeclaredFromWhatWasInferred() {
            Outcome outcome = Outcome.of(model(aggregate("com.acme.Order"), service("com.acme.Ordering")));

            assertThat(outcome.declared()).isEqualTo(1);
            assertThat(outcome.inferred()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("counts what it could not decide")
    class CountsWhatItCouldNotDecide {

        @Test
        @DisplayName("breaking the fallback down by the category that explains it")
        void breakingTheFallbackDownByTheCategoryThatExplainsIt() {
            Outcome outcome = Outcome.of(model(
                    silent("com.acme.Helper", UnclassifiedCategory.UTILITY),
                    silent("com.acme.Config", UnclassifiedCategory.TECHNICAL),
                    silent("com.acme.Stray", UnclassifiedCategory.UTILITY)));

            assertThat(outcome.unclassified())
                    .containsExactly(
                            new Outcome.Tally<>(UnclassifiedCategory.UTILITY, 2),
                            new Outcome.Tally<>(UnclassifiedCategory.TECHNICAL, 1));
        }

        @Test
        @DisplayName("counting apart the decisions that kept competing candidates")
        void countingApartTheDecisionsThatKeptCompetingCandidates() {
            Outcome outcome = Outcome.of(
                    model(tied("com.acme.Reference"), silent("com.acme.Helper", UnclassifiedCategory.UTILITY)));

            assertThat(outcome.ambiguous()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("reads back as the lines a host logs")
    class ReadsBackAsTheLinesAHostLogs {

        @Test
        @DisplayName("the total, then a tally per kind, then how sure the run was overall")
        void theTotalThenATallyPerKindThenHowSureTheRunWasOverall() {
            List<String> lines = Explanation.of(Outcome.of(
                    model(aggregate("com.acme.Order"), value("com.acme.Money"), tied("com.acme.Reference"))));

            assertThat(lines)
                    .containsExactly(
                            "3 types analysed",
                            "  1 AGGREGATE_ROOT",
                            "  1 VALUE_OBJECT",
                            "  1 UNCLASSIFIED",
                            "    1 AMBIGUOUS",
                            "1 declared, 2 inferred, 1 left ambiguous");
        }

        @Test
        @DisplayName("saying plainly that nothing was read rather than printing an empty tally")
        void sayingPlainlyThatNothingWasReadRatherThanPrintingAnEmptyTally() {
            assertThat(Explanation.of(Outcome.of(model()))).containsExactly("no type was analysed");
        }
    }
}
