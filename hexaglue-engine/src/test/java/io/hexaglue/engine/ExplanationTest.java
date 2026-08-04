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
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.arch.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RemediationHint;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.AnalysisScope;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExplanationTest {

    private static final TypeId ORDER = TypeId.of("com.acme.Order");
    private static final TypeId REPOSITORY = TypeId.of("com.acme.OrderRepository");

    private static final SourceLocation DECLARATION = new SourceLocation("com/acme/Order.java", 10, 43);

    private static TypeStructure structure() {
        CodeModel code = CodeModel.builder()
                .addType(TypeNode.builder(ORDER, TypeNature.CLASS)
                        .sourceLocation(DECLARATION)
                        .build())
                .build();
        TypeNode order = code.type(ORDER).orElseThrow();
        return Structures.of(code).of(order, order.fields());
    }

    private static Evidence evidence(EvidenceTier tier, String fact, String justification) {
        return Evidence.of(tier, tier.maxConfidence(), fact, justification);
    }

    private static Evidence evidenceAt(SourceLocation location) {
        return new Evidence(
                EvidenceTier.GRAPH_RELATION,
                Confidence.HIGH,
                "MANAGED_BY(com.acme.OrderRepository)",
                "com.acme.Order is kept and handed back by OrderRepository",
                Optional.of(location),
                List.of(REPOSITORY));
    }

    private static Classification withEvidence(Evidence evidence) {
        return Classification.builder(
                        ArchKind.AGGREGATE_ROOT, Confidence.HIGH, Basis.INFERRED, ProofNode.fact("managed"))
                .evidences(List.of(evidence))
                .build();
    }

    private static ArchType aggregate(Classification verdict) {
        return new AggregateRoot(
                ORDER,
                structure(),
                verdict,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                List.of());
    }

    private static ArchType unclassified(Classification verdict, UnclassifiedCategory category, String reason) {
        return new UnclassifiedType(ORDER, structure(), verdict, category, Optional.ofNullable(reason));
    }

    private static Perimeter perimeterOf(TypeId id) {
        return Perimeter.of(
                CodeModel.builder()
                        .addType(TypeNode.builder(id, TypeNature.CLASS).build())
                        .build(),
                AnalysisScope.everything());
    }

    private static FactBase factsAbout(TypeId id) {
        FactBase facts = new FactBase();
        facts.add(new KindEvidence(
                id,
                ArchKind.AGGREGATE_ROOT,
                evidence(EvidenceTier.FRAMEWORK_KNOWLEDGE, "REPO(x)", "a repository stores it"),
                0,
                ProofNode.fact("REPO(x)")));
        return facts;
    }

    private static Classification decided() {
        return Classification.builder(
                        ArchKind.AGGREGATE_ROOT,
                        Confidence.HIGH,
                        Basis.INFERRED,
                        ProofNode.derived(
                                RuleId.of("R1"),
                                "AGGREGATE_ROOT(com.acme.Order)",
                                ProofNode.fact("SPRING_DATA_REPOSITORY(com.acme.OrderRepository)")))
                .evidences(List.of(evidence(
                        EvidenceTier.FRAMEWORK_KNOWLEDGE,
                        "SPRING_DATA_REPOSITORY(com.acme.OrderRepository)",
                        "com.acme.Order is an AGGREGATE_ROOT because a repository stores and retrieves it")))
                .build();
    }

    @Nested
    @DisplayName("states the verdict")
    class StatesTheVerdict {

        @Test
        @DisplayName("naming the type, its kind, the confidence and whether it was declared")
        void namingTheTypeItsKindTheConfidenceAndWhetherItWasDeclared() {
            List<String> lines = Explanation.of(aggregate(decided()));

            assertThat(lines.get(0)).isEqualTo("com.acme.Order: AGGREGATE_ROOT (HIGH, inferred)");
        }

        @Test
        @DisplayName("giving one reason per evidence, prefixed by the tier that carried it")
        void givingOneReasonPerEvidencePrefixedByTheTierThatCarriedIt() {
            List<String> lines = Explanation.of(aggregate(decided()));

            assertThat(lines)
                    .contains(
                            "  [framework knowledge] com.acme.Order is an AGGREGATE_ROOT because a repository stores and retrieves it");
        }

        @Test
        @DisplayName("spelling out the pecking order when more than one kind of signal was weighed")
        void spellingOutThePeckingOrderWhenMoreThanOneKindOfSignalWasWeighed() {
            Classification weighed = Classification.builder(
                            ArchKind.AGGREGATE_ROOT, Confidence.HIGH, Basis.INFERRED, ProofNode.fact("managed"))
                    .evidences(List.of(
                            evidence(EvidenceTier.FRAMEWORK_KNOWLEDGE, "REPO(x)", "a repository stores it"),
                            evidence(EvidenceTier.LOCAL_STRUCTURE, "MUTABLE(x)", "its state changes")))
                    .build();

            assertThat(Explanation.of(aggregate(weighed)))
                    .contains("  signals, strongest first: declared intent > framework knowledge > graph relation"
                            + " > local structure > topology > naming");
        }

        @Test
        @DisplayName("keeping quiet about an order no comparison ever applied")
        void keepingQuietAboutAnOrderNoComparisonEverApplied() {
            assertThat(Explanation.of(aggregate(decided())))
                    .noneMatch(line -> line.contains("signals, strongest first"));
        }

        @Test
        @DisplayName("pointing at the source of a signal when it sits somewhere the header does not")
        void pointingAtTheSourceOfASignalWhenItSitsSomewhereTheHeaderDoesNot() {
            Evidence located = evidenceAt(new SourceLocation("com/acme/Order.java", 22, 22));

            assertThat(Explanation.of(aggregate(withEvidence(located)))).contains("    at com/acme/Order.java:22");
        }

        @Test
        @DisplayName("keeping quiet about a location that only repeats the declaration being explained")
        void keepingQuietAboutALocationThatOnlyRepeatsTheDeclarationBeingExplained() {
            Evidence onTheDeclaration = evidenceAt(DECLARATION);

            assertThat(Explanation.of(aggregate(withEvidence(onTheDeclaration))))
                    .noneMatch(line -> line.contains(" at "));
        }

        @Test
        @DisplayName("leaving the explained type out of the types its own reason involves")
        void leavingTheExplainedTypeOutOfTheTypesItsOwnReasonInvolves() {
            Evidence aboutItself = new Evidence(
                    EvidenceTier.FRAMEWORK_KNOWLEDGE,
                    Confidence.HIGH,
                    "DRIVING_ENTRYPOINT(com.acme.Order)",
                    "com.acme.Order is called by the framework from outside",
                    Optional.empty(),
                    List.of(ORDER));

            assertThat(Explanation.of(aggregate(withEvidence(aboutItself))))
                    .noneMatch(line -> line.contains("involving"));
        }

        @Test
        @DisplayName("naming the types a reason leans on, so the reader can ask about them in turn")
        void namingTheTypesAReasonLeansOnSoTheReaderCanAskAboutThemInTurn() {
            assertThat(Explanation.of(aggregate(withEvidence(evidenceAt(DECLARATION)))))
                    .contains("    involving com.acme.OrderRepository");
        }

        @Test
        @DisplayName("offering the remediation that would make the verdict explicit")
        void offeringTheRemediationThatWouldMakeTheVerdictExplicit() {
            RemediationHint hint = RemediationHint.addAnnotation(
                    TypeId.of("org.jmolecules.ddd.annotation.AggregateRoot"), ArchKind.AGGREGATE_ROOT);

            List<String> lines = Explanation.of(aggregate(Classification.builder(
                            ArchKind.AGGREGATE_ROOT, Confidence.HIGH, Basis.INFERRED, ProofNode.fact("managed"))
                    .remediations(List.of(hint))
                    .build()));

            assertThat(lines)
                    .contains(
                            "  to make it explicit: Add @org.jmolecules.ddd.annotation.AggregateRoot" + " on the type");
        }
    }

    @Nested
    @DisplayName("says why a type reached no kind")
    class SaysWhyATypeReachedNoKind {

        @Test
        @DisplayName("naming the category and the reason the fallback recorded")
        void namingTheCategoryAndTheReasonTheFallbackRecorded() {
            Classification silent = Classification.builder(
                            ArchKind.UNCLASSIFIED,
                            Confidence.LOW,
                            Basis.INFERRED,
                            ProofNode.fact("no signal about com.acme.Order"))
                    .build();

            List<String> lines =
                    Explanation.of(unclassified(silent, UnclassifiedCategory.UNKNOWN, "nothing in scope uses it"));

            assertThat(lines)
                    .containsExactly(
                            "com.acme.Order: UNCLASSIFIED (LOW, inferred)", "  UNKNOWN: nothing in scope uses it");
        }

        @Test
        @DisplayName("listing the candidates that could not be separated")
        void listingTheCandidatesThatCouldNotBeSeparated() {
            Evidence shape = evidence(EvidenceTier.LOCAL_STRUCTURE, "IMMUTABLE(com.acme.Order)", "it never changes");
            Classification ambiguous = Classification.builder(
                            ArchKind.UNCLASSIFIED, Confidence.LOW, Basis.INFERRED, ProofNode.fact("tied"))
                    .candidates(List.of(
                            new Candidate(ArchKind.ENTITY, 1000, List.of(shape)),
                            new Candidate(ArchKind.VALUE_OBJECT, 1000, List.of(shape))))
                    .build();

            List<String> lines = Explanation.of(unclassified(ambiguous, UnclassifiedCategory.AMBIGUOUS, null));

            assertThat(lines)
                    .contains(
                            "  AMBIGUOUS",
                            "  candidate ENTITY (1 signal of local structure)",
                            "    [local structure] it never changes",
                            "  candidate VALUE_OBJECT (1 signal of local structure)");
        }
    }

    @Nested
    @DisplayName("shows the derivation when asked for it")
    class ShowsTheDerivationWhenAskedForIt {

        @Test
        @DisplayName("indenting each premise under the conclusion it served")
        void indentingEachPremiseUnderTheConclusionItServed() {
            List<String> lines = Explanation.withDerivation(aggregate(decided()));

            assertThat(lines)
                    .containsSubsequence(
                            "  derivation:",
                            "    [R1] AGGREGATE_ROOT(com.acme.Order)",
                            "      SPRING_DATA_REPOSITORY(com.acme.OrderRepository)");
        }

        @Test
        @DisplayName("keeping everything the plain verdict already said")
        void keepingEverythingThePlainVerdictAlreadySaid() {
            assertThat(Explanation.withDerivation(aggregate(decided())))
                    .containsAll(Explanation.of(aggregate(decided())));
        }

        @Test
        @DisplayName("saying what each rule it names actually does, once per rule")
        void sayingWhatEachRuleItNamesActuallyDoesOncePerRule() {
            assertThat(Explanation.withDerivation(aggregate(decided())))
                    .containsSubsequence(
                            "  rules cited:",
                            "    R1: reads a Spring Data repository declaration for everything it says");
        }

        @Test
        @DisplayName("naming the decision step too, which is where every verdict ends")
        void namingTheDecisionStepTooWhichIsWhereEveryVerdictEnds() {
            Classification decided = Aggregator.decide(factsAbout(ORDER), perimeterOf(ORDER))
                    .verdict(ORDER)
                    .orElseThrow();

            assertThat(Explanation.withDerivation(aggregate(decided)))
                    .contains("    DECISION: weighs every signal held about a type and commits to one kind");
        }

        @Test
        @DisplayName("citing nothing when the verdict rests on an observation no rule derived")
        void citingNothingWhenTheVerdictRestsOnAnObservationNoRuleDerived() {
            Classification observed = Classification.builder(
                            ArchKind.AGGREGATE_ROOT, Confidence.HIGH, Basis.INFERRED, ProofNode.fact("just observed"))
                    .build();

            assertThat(Explanation.withDerivation(aggregate(observed))).noneMatch(line -> line.contains("rules cited"));
        }
    }

    @Nested
    @DisplayName("says what the gates refused")
    class SaysWhatTheGatesRefused {

        private static Validation refusing(Gate... gates) {
            Classification silent = Classification.builder(
                            ArchKind.UNCLASSIFIED, Confidence.LOW, Basis.INFERRED, ProofNode.fact("no signal"))
                    .remediations(List.of(RemediationHint.configureExplicit(ORDER, ArchKind.AGGREGATE_ROOT)))
                    .build();
            ArchType subject = unclassified(silent, UnclassifiedCategory.UNKNOWN, null);
            return new Validation(Arrays.stream(gates)
                    .map(gate -> new Validation.Refusal(subject, gate, "because " + gate))
                    .toList());
        }

        @Test
        @DisplayName("saying so plainly when nothing was refused")
        void sayingSoPlainlyWhenNothingWasRefused() {
            assertThat(Explanation.of(new Validation(List.of()))).containsExactly("validation passed");
        }

        @Test
        @DisplayName("counting the types refused, then naming each of them once")
        void countingTheTypesRefusedThenNamingEachOfThemOnce() {
            List<String> lines = Explanation.of(refusing(Gate.UNCLASSIFIED, Gate.CONFIDENCE));

            // Grouped under the type: the gates are independent conditions, but a reader fixing a
            // code base works type by type, and the way to unblock the build is stated once.
            assertThat(lines)
                    .containsExactly(
                            "validation refused 1 type",
                            "  com.acme.Order",
                            "    [UNCLASSIFIED] because UNCLASSIFIED",
                            "    [CONFIDENCE] because CONFIDENCE",
                            "    to make it explicit: Declare com.acme.Order as AGGREGATE_ROOT in the explicit"
                                    + " classification configuration");
        }
    }

    @Nested
    @DisplayName("renders the same bytes twice")
    class RendersTheSameBytesTwice {

        @Test
        @DisplayName("so a host can diff two runs without reading the model")
        void soAHostCanDiffTwoRunsWithoutReadingTheModel() {
            assertThat(Explanation.withDerivation(aggregate(decided())))
                    .isEqualTo(Explanation.withDerivation(aggregate(decided())));
        }
    }
}
