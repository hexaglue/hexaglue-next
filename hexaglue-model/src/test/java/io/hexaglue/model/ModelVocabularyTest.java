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
