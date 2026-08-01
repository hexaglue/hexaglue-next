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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Construction helper for the model's enum-set components: copies preserve the enum's natural
 * order, so iterating a modifier or role set is deterministic by construction — one of the
 * building blocks of the byte-for-byte reproducibility invariant.
 *
 * @since 7.0.0
 */
public final class EnumSets {

    private EnumSets() {}

    /**
     * Returns an immutable copy of the given set that iterates in the enum's natural order.
     *
     * @param source the set to copy
     * @param <E> the enum type
     * @return an immutable, natural-order copy
     */
    public static <E extends Enum<E>> Set<E> ordered(Set<E> source) {
        Objects.requireNonNull(source, "source must not be null");
        return source.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(source));
    }
}
