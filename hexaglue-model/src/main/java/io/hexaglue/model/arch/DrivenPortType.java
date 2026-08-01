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

import java.util.Objects;

/**
 * The functional family of a driven port.
 *
 * @since 7.0.0
 */
public enum DrivenPortType {

    /** Persistence abstraction for aggregates. */
    REPOSITORY("Persistence abstraction for aggregates"),

    /** External service abstraction. */
    GATEWAY("External service abstraction"),

    /** Publishes domain events. */
    EVENT_PUBLISHER("Publishes domain events"),

    /** Sends notifications. */
    NOTIFICATION("Sends notifications"),

    /** Generic driven port. */
    OTHER("Generic driven port");

    private final String description;

    DrivenPortType(String description) {
        this.description = Objects.requireNonNull(description);
    }

    /**
     * Returns the human-readable description of this family.
     *
     * @return the description
     */
    public String description() {
        return description;
    }
}
