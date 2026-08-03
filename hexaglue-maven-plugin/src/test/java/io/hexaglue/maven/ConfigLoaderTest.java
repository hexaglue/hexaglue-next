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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.ModuleRole;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Configuration is read strictly: a key nobody reads, a value nobody can honour, a document that
 * is not one — each fails the build with a coded diagnostic. A configuration quietly ignored is a
 * gate the user believes is armed and is not.
 */
class ConfigLoaderTest {

    @TempDir
    Path projectDir;

    private static HexaGlueConfig load(String yaml) {
        return ConfigLoader.load("hexaglue.yaml", yaml);
    }

    private static String messageOf(String yaml) {
        return assertThatExceptionOfType(ConfigException.class)
                .isThrownBy(() -> load(yaml))
                .actual()
                .diagnostic()
                .message();
    }

    @Nested
    @DisplayName("binds what the document states")
    class BindsWhatTheDocumentStates {

        @Test
        @DisplayName("the perimeter of the analysis")
        void thePerimeterOfTheAnalysis() {
            HexaGlueConfig config = load("""
                    analysis:
                      basePackage: com.acme
                      includePackages: [com.acme.domain, com.acme.application]
                      excludePackages: [com.acme.legacy]
                    """);

            assertThat(config.analysis().basePackage()).contains("com.acme");
            assertThat(config.analysis().includePackages()).containsExactly("com.acme.domain", "com.acme.application");
            assertThat(config.analysis().excludePackages()).containsExactly("com.acme.legacy");
        }

        @Test
        @DisplayName("the kinds the user declares out of the code")
        void theKindsTheUserDeclares() {
            HexaGlueConfig config = load("""
                    classification:
                      explicit:
                        com.acme.Order: AGGREGATE_ROOT
                        com.acme.Money: VALUE_OBJECT
                    """);

            assertThat(config.classification().explicit())
                    .containsEntry(TypeId.of("com.acme.Order"), ArchKind.AGGREGATE_ROOT)
                    .containsEntry(TypeId.of("com.acme.Money"), ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("the vocabulary a code base opts into")
        void theVocabularyACodeBaseOptsInto() {
            HexaGlueConfig config = load("""
                    classification:
                      namingSuffixes:
                        IDENTIFIER: [Id, Ref]
                    """);

            assertThat(config.classification().namingSuffixes())
                    .containsEntry(ArchKind.IDENTIFIER, List.of("Id", "Ref"));
        }

        @Test
        @DisplayName("the gates, and the severity a finding code carries")
        void theGatesAndTheSeverityOfAFindingCode() {
            HexaGlueConfig config = load("""
                    validation:
                      failOnUnclassified: true
                      minConfidence: HIGH
                      failOnAmbiguous: true
                      allowInferred: false
                      findings:
                        HG-DDD-012: BLOCKER
                    """);

            assertThat(config.validation().failOnUnclassified()).isTrue();
            assertThat(config.validation().minConfidence()).isEqualTo(Confidence.HIGH);
            assertThat(config.validation().failOnAmbiguous()).isTrue();
            assertThat(config.validation().allowInferred()).isFalse();
            assertThat(config.validation().findingThresholds())
                    .containsEntry(IssueCode.of("HG-DDD-012"), Severity.BLOCKER);
        }

        @Test
        @DisplayName("the certainty generated code claims")
        void theCertaintyGeneratedCodeClaims() {
            HexaGlueConfig config = load("""
                    generation:
                      minConfidence: EXPLICIT
                    """);

            assertThat(config.generation().minConfidence()).isEqualTo(Confidence.EXPLICIT);
        }

        @Test
        @DisplayName("the role each module of the reactor plays")
        void theRoleEachModulePlays() {
            HexaGlueConfig config = load("""
                    modules:
                      shop-domain: DOMAIN
                      shop-infra: INFRASTRUCTURE
                    """);

            assertThat(config.modules().roleOf("shop-domain")).contains(ModuleRole.DOMAIN);
            assertThat(config.modules().roleOf("shop-infra")).contains(ModuleRole.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("leaving unstated blocks at the documented defaults")
        void leavingUnstatedBlocksAtTheDefaults() {
            HexaGlueConfig config = load("analysis:\n  basePackage: com.acme\n");

            assertThat(config.validation()).isEqualTo(HexaGlueConfig.defaults().validation());
            assertThat(config.generation()).isEqualTo(HexaGlueConfig.defaults().generation());
        }
    }

    @Nested
    @DisplayName("refuses what it cannot honour")
    class RefusesWhatItCannotHonour {

        @Test
        @DisplayName("a key nobody reads, naming the ones that are read")
        void aKeyNobodyReads() {
            assertThat(messageOf("validaton:\n  failOnUnclassified: true\n"))
                    .contains("validaton")
                    .contains("analysis")
                    .contains("validation");
        }

        @Test
        @DisplayName("a key nobody reads inside a block")
        void aKeyNobodyReadsInsideABlock() {
            assertThat(messageOf("validation:\n  failOnUnclassifed: true\n")).contains("failOnUnclassifed");
        }

        @Test
        @DisplayName("a document that is not YAML")
        void aDocumentThatIsNotYaml() {
            assertThat(messageOf("analysis: [unclosed\n")).contains("hexaglue.yaml");
        }

        @Test
        @DisplayName("a block that is not a mapping")
        void aBlockThatIsNotAMapping() {
            assertThat(messageOf("validation: true\n")).contains("validation");
        }

        @Test
        @DisplayName("a module role outside the vocabulary, naming the roles that exist")
        void aModuleRoleOutsideTheVocabulary() {
            assertThat(messageOf("modules:\n  shop-domain: BUSINESS\n"))
                    .contains("BUSINESS")
                    .contains("DOMAIN")
                    .contains("INFRASTRUCTURE");
        }

        @Test
        @DisplayName("a value outside the vocabulary, naming the vocabulary")
        void aValueOutsideTheVocabulary() {
            assertThat(messageOf("validation:\n  minConfidence: VERY_HIGH\n"))
                    .contains("VERY_HIGH")
                    .contains("EXPLICIT");
        }

        @Test
        @DisplayName("a boolean gate stated as something else")
        void aBooleanGateStatedAsSomethingElse() {
            assertThat(messageOf("validation:\n  failOnUnclassified: maybe\n")).contains("failOnUnclassified");
        }

        @Test
        @DisplayName("a declaration the model itself refuses, in the model's own words")
        void aDeclarationTheModelRefuses() {
            assertThat(messageOf("classification:\n  explicit:\n    com.acme.Thing: UNCLASSIFIED\n"))
                    .contains("com.acme.Thing");
        }

        @Test
        @DisplayName("the same key stated twice")
        void theSameKeyStatedTwice() {
            assertThat(messageOf("analysis:\n  basePackage: com.acme\n  basePackage: com.other\n"))
                    .contains("hexaglue.yaml");
        }
    }

    @Nested
    @DisplayName("finds the document beside the project")
    class FindsTheDocument {

        @Test
        @DisplayName("answering the documented defaults when the project states nothing")
        void answeringDefaultsWhenNothingIsStated() {
            assertThat(ConfigLoader.read(projectDir)).isEqualTo(HexaGlueConfig.defaults());
        }

        @Test
        @DisplayName("reading hexaglue.yaml when it is there")
        void readingHexaglueYaml() {
            write("hexaglue.yaml", "analysis:\n  basePackage: com.acme\n");

            assertThat(ConfigLoader.read(projectDir).analysis().basePackage()).contains("com.acme");
        }

        @Test
        @DisplayName("reading hexaglue.yml too, since both spellings are in use")
        void readingHexaglueYml() {
            write("hexaglue.yml", "analysis:\n  basePackage: com.other\n");

            assertThat(ConfigLoader.read(projectDir).analysis().basePackage()).contains("com.other");
        }

        @Test
        @DisplayName("taking an empty document as a project that configured nothing")
        void takingAnEmptyDocumentAsNothingConfigured() {
            write("hexaglue.yaml", "");

            assertThat(ConfigLoader.read(projectDir)).isEqualTo(HexaGlueConfig.defaults());
        }

        private void write(String name, String content) {
            SourceFixtures.write(projectDir, name, content);
        }
    }

    @Nested
    @DisplayName("inherits the document of the reactor it belongs to")
    class InheritsFromTheReactor {

        private Path module;

        @BeforeEach
        void layOutAReactor() {
            module = projectDir.resolve("shop-domain");
        }

        @Test
        @DisplayName("a module without a document of its own reads the one beside the reactor")
        void aModuleWithoutADocumentReadsTheReactors() {
            SourceFixtures.write(projectDir, "hexaglue.yaml", "analysis:\n  basePackage: com.acme\n");

            HexaGlueConfig config = ConfigLoader.read(List.of(module, projectDir));

            assertThat(config.analysis().basePackage()).contains("com.acme");
        }

        @Test
        @DisplayName("a module stating its own document is read on its own, the nearest one winning whole")
        void theNearestDocumentWinsWhole() {
            SourceFixtures.write(
                    projectDir,
                    "hexaglue.yaml",
                    "analysis:\n  basePackage: com.acme\nmodules:\n  shop-domain: DOMAIN\n");
            SourceFixtures.write(module, "hexaglue.yaml", "analysis:\n  basePackage: com.acme.shop\n");

            HexaGlueConfig config = ConfigLoader.read(List.of(module, projectDir));

            assertThat(config.analysis().basePackage()).contains("com.acme.shop");
            assertThat(config.modules().roles()).isEmpty();
        }

        @Test
        @DisplayName("a reactor stating nothing anywhere answers the documented defaults")
        void nothingAnywhereAnswersTheDefaults() {
            assertThat(ConfigLoader.read(List.of(module, projectDir))).isEqualTo(HexaGlueConfig.defaults());
        }

        @Test
        @DisplayName("what a backend is asked is inherited the same way")
        void pluginOptionsAreInheritedTheSameWay() {
            SourceFixtures.write(projectDir, "hexaglue.yaml", "plugins:\n  audit:\n    sections: verdict\n");

            assertThat(ConfigLoader.readPluginOptions(List.of(module, projectDir)))
                    .containsEntry("audit", Map.of("sections", "verdict"));
        }
    }

    @Nested
    @DisplayName("says where a document comes from")
    class SaysWhereADocumentComesFrom {

        @Test
        @DisplayName("so a reader knows which file to open")
        void soAReaderKnowsWhichFileToOpen() {
            SourceFixtures.write(projectDir, "hexaglue.yml", "nope: true\n");

            assertThatExceptionOfType(ConfigException.class)
                    .isThrownBy(() -> ConfigLoader.read(projectDir))
                    .withMessageContaining("hexaglue.yml");
        }
    }
}
