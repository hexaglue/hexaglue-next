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

package io.hexaglue.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.classification.EvidenceTier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The pack shapes are a published contract: the loader is one way of building them, a user
 * extension mechanism or a test is another. They defend their own invariants rather than trusting
 * whoever assembles them.
 */
class KnowledgeContractTest {

    private static final Selector ENTITY = new Selector.Annotated("jakarta.persistence.Entity");

    private static KnowledgeEntry persistenceModel() {
        return KnowledgeEntry.of(ENTITY, KnowledgeFact.PERSISTENCE_MODEL);
    }

    @Nested
    @DisplayName("a selector")
    class ASelector {

        @Test
        @DisplayName("exposes the symbol it matches on")
        void exposesItsSymbol() {
            assertThat(new Selector.Supertype("org.springframework.data.repository.Repository").symbol())
                    .isEqualTo("org.springframework.data.repository.Repository");
            assertThat(new Selector.PackagePrefix("feign").symbol()).isEqualTo("feign");
            assertThat(new Selector.MemberAnnotated("org.springframework.kafka.annotation.KafkaListener").symbol())
                    .isEqualTo("org.springframework.kafka.annotation.KafkaListener");
        }

        @Test
        @DisplayName("refuses a simple name, whatever its shape")
        void refusesASimpleName() {
            assertThatThrownBy(() -> new Selector.Annotated("Entity"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("qualified");
            assertThatThrownBy(() -> new Selector.Supertype("Repository")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Selector.Type("EntityManager")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Selector.MemberAnnotated("KafkaListener"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("accepts a single-segment package prefix, which names a package and not a type")
        void acceptsASingleSegmentPackagePrefix() {
            assertThat(new Selector.PackagePrefix("feign").symbol()).isEqualTo("feign");
        }

        @Test
        @DisplayName("refuses anything that is not a Java name")
        void refusesAnythingThatIsNotAJavaName() {
            assertThatThrownBy(() -> new Selector.Annotated("org.springframework.*"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Selector.PackagePrefix("org.springframework."))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Selector.Annotated(" ")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("accepts a nested annotation type, written the way the code model names it")
        void acceptsANestedType() {
            assertThat(new Selector.Annotated("com.acme.Intents$Aggregate").symbol())
                    .isEqualTo("com.acme.Intents$Aggregate");
        }
    }

    @Nested
    @DisplayName("a fact")
    class AFact {

        @Test
        @DisplayName("places declared intent above framework knowledge")
        void placesDeclaredIntentAboveFrameworkKnowledge() {
            assertThat(KnowledgeFact.DECLARED_KIND.tier()).isEqualTo(EvidenceTier.DECLARED_INTENT);
            assertThat(KnowledgeFact.SPRING_DATA_REPOSITORY.tier()).isEqualTo(EvidenceTier.FRAMEWORK_KNOWLEDGE);
            assertThat(KnowledgeFact.PERSISTENCE_MODEL.tier()).isEqualTo(EvidenceTier.FRAMEWORK_KNOWLEDGE);
        }

        @ParameterizedTest
        @EnumSource(KnowledgeFact.class)
        @DisplayName("is stated by a pack, so it never sits above declared intent")
        void neverSitsAboveDeclaredIntent(KnowledgeFact fact) {
            assertThat(fact.tier()).isIn(EvidenceTier.DECLARED_INTENT, EvidenceTier.FRAMEWORK_KNOWLEDGE);
            assertThat(fact.captureNames()).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("names the type arguments it captures")
        void namesTheTypeArgumentsItCaptures() {
            assertThat(KnowledgeFact.SPRING_DATA_REPOSITORY.captureNames()).containsExactly("subject", "id");
            assertThat(KnowledgeFact.PERSISTENCE_MODEL.captureNames()).isEmpty();
        }
    }

    @Nested
    @DisplayName("an entry")
    class AnEntry {

        @Test
        @DisplayName("requires a kind exactly when the fact carries one")
        void requiresAKindExactlyWhenTheFactCarriesOne() {
            assertThatThrownBy(() -> new KnowledgeEntry(ENTITY, KnowledgeFact.DECLARED_KIND, Optional.empty()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DECLARED_KIND");
            assertThatThrownBy(() ->
                            new KnowledgeEntry(ENTITY, KnowledgeFact.PERSISTENCE_MODEL, Optional.of(ArchKind.ENTITY)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("refuses the absence of a verdict as a declared kind")
        void refusesUnclassifiedAsADeclaredKind() {
            assertThatThrownBy(() -> KnowledgeEntry.declaring(ENTITY, ArchKind.UNCLASSIFIED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("UNCLASSIFIED");
        }

        @Test
        @DisplayName("binds a capturing fact to a supertype, the only selector carrying type arguments")
        void bindsACapturingFactToASupertype() {
            assertThatThrownBy(() -> KnowledgeEntry.of(ENTITY, KnowledgeFact.SPRING_DATA_REPOSITORY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("supertype");
            assertThat(KnowledgeEntry.of(
                                    new Selector.Supertype("org.springframework.data.repository.Repository"),
                                    KnowledgeFact.SPRING_DATA_REPOSITORY)
                            .fact())
                    .isEqualTo(KnowledgeFact.SPRING_DATA_REPOSITORY);
        }

        @Test
        @DisplayName("builds an intent entry with its kind")
        void buildsAnIntentEntry() {
            KnowledgeEntry entry = KnowledgeEntry.declaring(
                    new Selector.Annotated("org.jmolecules.ddd.annotation.AggregateRoot"), ArchKind.AGGREGATE_ROOT);

            assertThat(entry.fact()).isEqualTo(KnowledgeFact.DECLARED_KIND);
            assertThat(entry.declaredKind()).contains(ArchKind.AGGREGATE_ROOT);
        }
    }

    @Nested
    @DisplayName("a pack")
    class APack {

        @Test
        @DisplayName("keeps the order its entries were declared in")
        void keepsDeclarationOrder() {
            KnowledgeEntry first = persistenceModel();
            KnowledgeEntry second = KnowledgeEntry.of(
                    new Selector.Annotated("jakarta.persistence.Embeddable"), KnowledgeFact.PERSISTENCE_MODEL);

            KnowledgePack pack = new KnowledgePack("jakarta", "Persistence mapping.", List.of(first, second));

            assertThat(pack.entries()).containsExactly(first, second);
        }

        @Test
        @DisplayName("refuses an identity that cannot name a resource")
        void refusesAnIdentityThatCannotNameAResource() {
            assertThatThrownBy(() -> new KnowledgePack("Spring Data", "d", List.of(persistenceModel())))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new KnowledgePack("", "d", List.of(persistenceModel())))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("refuses to know nothing")
        void refusesToKnowNothing() {
            assertThatThrownBy(() -> new KnowledgePack("jakarta", "d", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("refuses a blank description: a pack states what it is for")
        void refusesABlankDescription() {
            assertThatThrownBy(() -> new KnowledgePack("jakarta", " ", List.of(persistenceModel())))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("refuses to say the same thing twice")
        void refusesToSayTheSameThingTwice() {
            assertThatThrownBy(() -> new KnowledgePack("jakarta", "d", List.of(persistenceModel(), persistenceModel())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("jakarta.persistence.Entity");
        }

        @Test
        @DisplayName("lets one symbol carry several facts")
        void letsOneSymbolCarrySeveralFacts() {
            KnowledgePack pack = new KnowledgePack(
                    "jakarta",
                    "A mapped type is also a technical one.",
                    List.of(persistenceModel(), KnowledgeEntry.of(ENTITY, KnowledgeFact.TECHNICAL)));

            assertThat(pack.entries()).hasSize(2);
        }
    }
}
