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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnumSetsTest {

    @Test
    @DisplayName("copies iterate in the enum's natural order, whatever the source order")
    void copiesIterateInNaturalOrder() {
        Set<Modifier> source = Set.of(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC);

        assertThat(EnumSets.ordered(source)).containsExactly(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
    }

    @Test
    @DisplayName("an empty source yields an empty immutable set")
    void emptySourceYieldsEmptySet() {
        Set<Modifier> empty = Set.of();

        assertThat(EnumSets.ordered(empty)).isEmpty();
    }

    @Test
    @DisplayName("copies are immutable")
    void copiesAreImmutable() {
        Set<Modifier> copy = EnumSets.ordered(Set.of(Modifier.PUBLIC));

        assertThatThrownBy(() -> copy.add(Modifier.FINAL)).isInstanceOf(UnsupportedOperationException.class);
    }
}
