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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Determinism assertion: same inputs must yield byte-identical outputs across repeated runs.
 *
 * <p>Determinism is a product invariant of HexaGlue: analysis results must not depend on
 * iteration order, hash seeds or classpath ordering.
 *
 * @since 7.0.0
 */
public final class Determinism {

    private Determinism() {}

    /**
     * Runs {@code snapshot} {@code runs} times and asserts that every run yields a result
     * byte-identical to the first.
     *
     * @param runs the number of executions, at least 2
     * @param snapshot produces the snapshot under test, re-executed from scratch on each call
     * @throws IllegalArgumentException if {@code runs} is less than 2
     */
    public static void assertStable(int runs, Supplier<String> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (runs < 2) {
            throw new IllegalArgumentException("runs must be >= 2, got " + runs);
        }
        String first = snapshot.get();
        for (int run = 2; run <= runs; run++) {
            assertThat(snapshot.get())
                    .as("Snapshot differs between run 1 and run %d: output is not deterministic", run)
                    .isEqualTo(first);
        }
    }
}
