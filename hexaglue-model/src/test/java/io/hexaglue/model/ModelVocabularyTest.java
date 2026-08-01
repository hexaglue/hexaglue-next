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

package io.hexaglue.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the base vocabulary enums against accidental removal or renaming: snapshots and golden
 * files serialize these constant names, so any change here is a format change.
 */
class ModelVocabularyTest {

    @Test
    @DisplayName("type natures cover every Java type form")
    void typeNaturesCoverJavaTypeForms() {
        assertThat(TypeNature.values())
                .extracting(Enum::name)
                .containsExactly("CLASS", "INTERFACE", "RECORD", "ENUM", "ANNOTATION");
    }

    @Test
    @DisplayName("architectural kinds cover the hexagon and the categorized fallback")
    void archKindsCoverHexagonAndFallback() {
        assertThat(ArchKind.values())
                .extracting(Enum::name)
                .containsExactly(
                        "AGGREGATE_ROOT",
                        "ENTITY",
                        "VALUE_OBJECT",
                        "IDENTIFIER",
                        "DOMAIN_EVENT",
                        "DOMAIN_SERVICE",
                        "DRIVING_PORT",
                        "DRIVEN_PORT",
                        "APPLICATION_SERVICE",
                        "COMMAND_HANDLER",
                        "QUERY_HANDLER",
                        "UNCLASSIFIED");
        assertThat(ArchKind.AGGREGATE_ROOT.isDomain()).isTrue();
        assertThat(ArchKind.DRIVEN_PORT.isPort()).isTrue();
        assertThat(ArchKind.APPLICATION_SERVICE.isApplication()).isTrue();
        assertThat(ArchKind.UNCLASSIFIED.isDomain()).isFalse();
        assertThat(ArchKind.UNCLASSIFIED.isPort()).isFalse();
        assertThat(ArchKind.UNCLASSIFIED.isApplication()).isFalse();
    }

    @Test
    @DisplayName("port directions cover both sides of the hexagon")
    void portDirectionsCoverBothSides() {
        assertThat(PortDirection.values()).extracting(Enum::name).containsExactly("DRIVING", "DRIVEN");
    }

    @Test
    @DisplayName("modifiers cover the Java modifier set")
    void modifiersCoverJavaModifierSet() {
        assertThat(Modifier.values())
                .extracting(Enum::name)
                .containsExactly(
                        "PUBLIC",
                        "PROTECTED",
                        "PRIVATE",
                        "STATIC",
                        "FINAL",
                        "ABSTRACT",
                        "NATIVE",
                        "SYNCHRONIZED",
                        "TRANSIENT",
                        "VOLATILE",
                        "STRICTFP",
                        "SEALED",
                        "NON_SEALED",
                        "DEFAULT");
    }
}
