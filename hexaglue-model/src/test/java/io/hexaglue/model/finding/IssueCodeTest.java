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

package io.hexaglue.model.finding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IssueCodeTest {

    @Nested
    @DisplayName("Canonical form")
    class CanonicalForm {

        @Test
        @DisplayName("a well-formed code exposes its parts")
        void wellFormedCodeExposesItsParts() {
            IssueCode code = IssueCode.of("HG-DDD-012");

            assertThat(code.value()).isEqualTo("HG-DDD-012");
            assertThat(code.category()).isEqualTo("DDD");
            assertThat(code.number()).isEqualTo(12);
        }

        @Test
        @DisplayName("codes order by their value")
        void codesOrderByValue() {
            assertThat(IssueCode.of("HG-DDD-001")).isLessThan(IssueCode.of("HG-GEN-001"));
            assertThat(IssueCode.of("HG-DDD-001")).isLessThan(IssueCode.of("HG-DDD-002"));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "HG-DDD-12",
                    "HG-DDD-0123",
                    "hg-ddd-012",
                    "DDD-012",
                    "HG--012",
                    "HG-ddd-012",
                    "HG-DDD-abc",
                    "HG-DDD012",
                    "XX-DDD-012",
                    " HG-DDD-012"
                })
        @DisplayName("a malformed code is rejected")
        void malformedCodeIsRejected(String value) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> IssueCode.of(value))
                    .withMessageContaining("HG-CATEGORY-NNN");
        }
    }
}
