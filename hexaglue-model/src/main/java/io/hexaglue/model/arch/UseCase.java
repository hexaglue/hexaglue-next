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

import io.hexaglue.model.declaration.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * One use case exposed by a driving port: the method, its intent and whether it mutates state.
 *
 * @param method the port method carrying the use case
 * @param description the use case intent, when documented
 * @param type whether the use case commands, queries, or both
 * @since 7.0.0
 */
public record UseCase(Method method, Optional<String> description, UseCaseType type) {

    /**
     * Validates the components.
     */
    public UseCase {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * The mutation profile of a use case.
     *
     * @since 7.0.0
     */
    public enum UseCaseType {

        /** Mutates state. */
        COMMAND,

        /** Reads without mutating. */
        QUERY,

        /** Both mutates and answers (discouraged but observed). */
        COMMAND_QUERY;

        /**
         * Returns whether this use case mutates state.
         *
         * @return true for COMMAND and COMMAND_QUERY
         */
        public boolean isCommand() {
            return this == COMMAND || this == COMMAND_QUERY;
        }

        /**
         * Returns whether this use case reads state.
         *
         * @return true for QUERY and COMMAND_QUERY
         */
        public boolean isQuery() {
            return this == QUERY || this == COMMAND_QUERY;
        }
    }
}
