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

package io.hexaglue.model.arch;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Classification;

/**
 * Single implementation of the kind-coherence invariant: an architectural record can only carry a
 * verdict whose kind matches the record itself.
 */
final class KindCoherence {

    private KindCoherence() {}

    /**
     * Rejects a verdict whose kind does not match the carrying record.
     *
     * @param expected the kind the record represents
     * @param classification the verdict to check
     * @param id the type id, for the error message
     */
    static void require(ArchKind expected, Classification classification, TypeId id) {
        if (classification.kind() != expected) {
            throw new IllegalArgumentException(
                    id + " carries a " + classification.kind() + " verdict in a " + expected + " record");
        }
    }
}
