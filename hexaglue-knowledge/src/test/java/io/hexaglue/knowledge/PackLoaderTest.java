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
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;

import io.hexaglue.model.ArchKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A pack is knowledge stated as data, so the only thing standing between a typo and a silently
 * weaker engine is the strictness of this reader. Everything it cannot make sense of is refused,
 * with a code, rather than skipped.
 */
class PackLoaderTest {

    private static KnowledgePack load(String yaml) {
        return PackLoader.load("test-pack.yaml", yaml);
    }

    private static void assertRefused(String yaml, String expectedCode) {
        assertThatThrownBy(() -> load(yaml))
                .isInstanceOf(KnowledgeException.class)
                .asInstanceOf(throwable(KnowledgeException.class))
                .satisfies(failure -> {
                    assertThat(failure.diagnostic().code().value()).isEqualTo(expectedCode);
                    assertThat(failure.getMessage()).contains("test-pack.yaml");
                });
    }

    @Nested
    @DisplayName("reads a well-formed pack")
    class ReadsAWellFormedPack {

        @Test
        @DisplayName("keeps the identity, the description and the declaration order of the entries")
        void keepsIdentityDescriptionAndOrder() {
            KnowledgePack pack = load("""
                    pack: spring
                    description: What Spring says about a type.
                    entries:
                      - annotation: org.springframework.web.bind.annotation.RestController
                        emits: DRIVING_ENTRYPOINT
                      - annotation: org.springframework.stereotype.Service
                        emits: APPLICATION_STEREOTYPE
                    """);

            assertThat(pack.id()).isEqualTo("spring");
            assertThat(pack.description()).isEqualTo("What Spring says about a type.");
            assertThat(pack.entries())
                    .extracting(entry -> entry.selector().symbol())
                    .containsExactly(
                            "org.springframework.web.bind.annotation.RestController",
                            "org.springframework.stereotype.Service");
        }

        @Test
        @DisplayName("reads the four selector shapes")
        void readsTheFourSelectorShapes() {
            KnowledgePack pack = load("""
                    pack: spring
                    description: Every way of naming a framework symbol.
                    entries:
                      - annotation: jakarta.persistence.Entity
                        emits: PERSISTENCE_MODEL
                      - supertype: org.springframework.data.repository.Repository
                        emits: SPRING_DATA_REPOSITORY
                      - type: jakarta.persistence.EntityManager
                        emits: INFRA_DEPENDENCY
                      - package-prefix: feign
                        emits: INFRA_DEPENDENCY
                    """);

            assertThat(pack.entries())
                    .extracting(KnowledgeEntry::selector)
                    .containsExactly(
                            new Selector.Annotated("jakarta.persistence.Entity"),
                            new Selector.Supertype("org.springframework.data.repository.Repository"),
                            new Selector.Type("jakarta.persistence.EntityManager"),
                            new Selector.PackagePrefix("feign"));
        }

        @Test
        @DisplayName("carries the declared kind of an intent entry")
        void carriesTheDeclaredKind() {
            KnowledgePack pack = load("""
                    pack: jmolecules
                    description: Intent, declared by the author.
                    entries:
                      - annotation: org.jmolecules.ddd.annotation.AggregateRoot
                        emits: DECLARED_KIND
                        kind: AGGREGATE_ROOT
                    """);

            KnowledgeEntry entry = pack.entries().get(0);
            assertThat(entry.fact()).isEqualTo(KnowledgeFact.DECLARED_KIND);
            assertThat(entry.declaredKind()).contains(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("accepts a comment and blank lines")
        void acceptsCommentsAndBlankLines() {
            KnowledgePack pack = load("""
                    # Knowledge about Jakarta Persistence.
                    pack: jakarta
                    description: Persistence mapping is a fact about storage, never a DDD verdict.

                    entries:
                      - annotation: jakarta.persistence.Entity
                        emits: PERSISTENCE_MODEL
                    """);

            assertThat(pack.entries()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("refuses a document it cannot make sense of")
    class RefusesAnUnreadableDocument {

        @Test
        @DisplayName("malformed YAML")
        void malformedYaml() {
            assertRefused("""
                    pack: spring
                     description: misaligned
                      entries: [
                    """, "HG-KNOWLEDGE-001");
        }

        @Test
        @DisplayName("a document that is not a mapping")
        void documentThatIsNotAMapping() {
            assertRefused("- spring\n- jakarta\n", "HG-KNOWLEDGE-001");
        }

        @Test
        @DisplayName("an empty document")
        void emptyDocument() {
            assertRefused("# nothing but a comment\n", "HG-KNOWLEDGE-001");
        }
    }

    @Nested
    @DisplayName("refuses a structure it cannot bind")
    class RefusesAnUnbindableStructure {

        @Test
        @DisplayName("an unknown top-level key")
        void unknownTopLevelKey() {
            assertRefused("""
                    pack: spring
                    description: A pack with a key nobody reads.
                    version: 3
                    entries:
                      - annotation: jakarta.persistence.Entity
                        emits: PERSISTENCE_MODEL
                    """, "HG-KNOWLEDGE-002");
        }

        @Test
        @DisplayName("a missing top-level key")
        void missingTopLevelKey() {
            assertRefused("""
                    pack: spring
                    entries:
                      - annotation: jakarta.persistence.Entity
                        emits: PERSISTENCE_MODEL
                    """, "HG-KNOWLEDGE-002");
        }

        @Test
        @DisplayName("an unknown entry key")
        void unknownEntryKey() {
            assertRefused("""
                    pack: spring
                    description: An entry with a key nobody reads.
                    entries:
                      - annotation: jakarta.persistence.Entity
                        emits: PERSISTENCE_MODEL
                        since: 3.1
                    """, "HG-KNOWLEDGE-002");
        }

        @Test
        @DisplayName("an entry without a selector")
        void entryWithoutSelector() {
            assertRefused("""
                    pack: spring
                    description: An entry that selects nothing.
                    entries:
                      - emits: PERSISTENCE_MODEL
                    """, "HG-KNOWLEDGE-002");
        }

        @Test
        @DisplayName("an entry with two selectors")
        void entryWithTwoSelectors() {
            assertRefused("""
                    pack: spring
                    description: An entry that selects twice.
                    entries:
                      - annotation: jakarta.persistence.Entity
                        type: jakarta.persistence.EntityManager
                        emits: PERSISTENCE_MODEL
                    """, "HG-KNOWLEDGE-002");
        }

        @Test
        @DisplayName("an entry without a fact")
        void entryWithoutFact() {
            assertRefused("""
                    pack: spring
                    description: An entry that says nothing.
                    entries:
                      - annotation: jakarta.persistence.Entity
                    """, "HG-KNOWLEDGE-002");
        }

        @Test
        @DisplayName("an empty entry list")
        void emptyEntryList() {
            assertRefused("""
                    pack: spring
                    description: A pack that knows nothing.
                    entries: []
                    """, "HG-KNOWLEDGE-002");
        }

        @Test
        @DisplayName("the same symbol emitting the same fact twice")
        void duplicateEntry() {
            assertRefused("""
                    pack: spring
                    description: A pack that says the same thing twice.
                    entries:
                      - annotation: jakarta.persistence.Entity
                        emits: PERSISTENCE_MODEL
                      - annotation: jakarta.persistence.Entity
                        emits: PERSISTENCE_MODEL
                    """, "HG-KNOWLEDGE-002");
        }

        @Test
        @DisplayName("a pack identity that is not a plain slug")
        void packIdentityThatIsNotASlug() {
            assertRefused("""
                    pack: Spring Data
                    description: An identity that cannot name a resource.
                    entries:
                      - annotation: jakarta.persistence.Entity
                        emits: PERSISTENCE_MODEL
                    """, "HG-KNOWLEDGE-002");
        }

        @Test
        @DisplayName("a scalar where a list of entries is expected")
        void scalarInsteadOfEntryList() {
            assertRefused("""
                    pack: spring
                    description: Entries given as a scalar.
                    entries: jakarta.persistence.Entity
                    """, "HG-KNOWLEDGE-002");
        }
    }

    @Nested
    @DisplayName("refuses a symbol that could match the wrong thing")
    class RefusesAnAmbiguousSymbol {

        @Test
        @DisplayName("an annotation named by its simple name")
        void annotationNamedBySimpleName() {
            assertRefused("""
                    pack: spring
                    description: The mistake that made @jakarta.persistence.Entity a DDD entity.
                    entries:
                      - annotation: Entity
                        emits: PERSISTENCE_MODEL
                    """, "HG-KNOWLEDGE-003");
        }

        @Test
        @DisplayName("a supertype named by its simple name")
        void supertypeNamedBySimpleName() {
            assertRefused("""
                    pack: spring
                    description: A supertype nobody can identify.
                    entries:
                      - supertype: Repository
                        emits: SPRING_DATA_REPOSITORY
                    """, "HG-KNOWLEDGE-003");
        }

        @Test
        @DisplayName("a symbol that is not a Java name")
        void symbolThatIsNotAJavaName() {
            assertRefused("""
                    pack: spring
                    description: A glob is not a qualified name.
                    entries:
                      - annotation: org.springframework.*
                        emits: APPLICATION_STEREOTYPE
                    """, "HG-KNOWLEDGE-003");
        }

        @Test
        @DisplayName("a blank symbol")
        void blankSymbol() {
            assertRefused("""
                    pack: spring
                    description: A symbol that names nothing.
                    entries:
                      - annotation: '  '
                        emits: APPLICATION_STEREOTYPE
                    """, "HG-KNOWLEDGE-003");
        }
    }

    @Nested
    @DisplayName("refuses a fact it cannot honour")
    class RefusesAnUnhonourableFact {

        @Test
        @DisplayName("an unknown fact name")
        void unknownFactName() {
            assertRefused("""
                    pack: spring
                    description: A fact nobody consumes.
                    entries:
                      - annotation: org.springframework.stereotype.Service
                        emits: SOUNDS_IMPORTANT
                    """, "HG-KNOWLEDGE-004");
        }

        @Test
        @DisplayName("a declared kind without its kind")
        void declaredKindWithoutItsKind() {
            assertRefused("""
                    pack: jmolecules
                    description: Intent without the intent.
                    entries:
                      - annotation: org.jmolecules.ddd.annotation.AggregateRoot
                        emits: DECLARED_KIND
                    """, "HG-KNOWLEDGE-004");
        }

        @Test
        @DisplayName("an unknown kind")
        void unknownKind() {
            assertRefused("""
                    pack: jmolecules
                    description: A kind the model does not have.
                    entries:
                      - annotation: org.jmolecules.ddd.annotation.AggregateRoot
                        emits: DECLARED_KIND
                        kind: SUPER_AGGREGATE
                    """, "HG-KNOWLEDGE-004");
        }

        @Test
        @DisplayName("the absence of a kind, declared as a kind")
        void unclassifiedDeclaredAsAKind() {
            assertRefused("""
                    pack: jmolecules
                    description: Declaring that a type has no verdict is not an intent.
                    entries:
                      - annotation: org.jmolecules.ddd.annotation.AggregateRoot
                        emits: DECLARED_KIND
                        kind: UNCLASSIFIED
                    """, "HG-KNOWLEDGE-004");
        }

        @Test
        @DisplayName("a kind on a fact that carries none")
        void kindOnAFactThatCarriesNone() {
            assertRefused("""
                    pack: jakarta
                    description: A persistence mapping is never a kind.
                    entries:
                      - annotation: jakarta.persistence.Entity
                        emits: PERSISTENCE_MODEL
                        kind: ENTITY
                    """, "HG-KNOWLEDGE-004");
        }

        @Test
        @DisplayName("a fact that captures type arguments, on a selector that has none")
        void captureFactOnASelectorWithoutTypeArguments() {
            assertRefused("""
                    pack: spring
                    description: An annotation carries no type arguments to capture.
                    entries:
                      - annotation: org.springframework.stereotype.Repository
                        emits: SPRING_DATA_REPOSITORY
                    """, "HG-KNOWLEDGE-004");
        }
    }
}
