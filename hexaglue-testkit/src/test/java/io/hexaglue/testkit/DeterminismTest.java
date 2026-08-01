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

package io.hexaglue.testkit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeterminismTest {

    @Test
    @DisplayName("passes when every run yields the same snapshot")
    void passesOnStableSnapshot() {
        Determinism.assertStable(5, () -> "same-output");
    }

    @Test
    @DisplayName("fails when a run yields a different snapshot")
    void failsOnUnstableSnapshot() {
        AtomicInteger counter = new AtomicInteger();

        assertThatThrownBy(() -> Determinism.assertStable(3, () -> "output-" + counter.incrementAndGet()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("not deterministic");
    }

    @Test
    @DisplayName("rejects fewer than two runs")
    void rejectsTooFewRuns() {
        assertThatThrownBy(() -> Determinism.assertStable(1, () -> "output"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runs must be >= 2");
    }
}
