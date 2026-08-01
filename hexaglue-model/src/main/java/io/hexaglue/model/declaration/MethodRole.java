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

package io.hexaglue.model.declaration;

/**
 * Semantic role of a method, assigned by the classification engine — the frontend leaves roles
 * empty.
 *
 * @since 7.0.0
 */
public enum MethodRole {

    /** Reads a property without side effects. */
    GETTER,

    /** Writes a property. */
    SETTER,

    /** Creates instances (static factory or builder step). */
    FACTORY,

    /** Carries domain behaviour that is not a plain accessor. */
    BUSINESS,

    /** Validates state or arguments. */
    VALIDATION,

    /** Participates in a lifecycle protocol (callbacks, hooks). */
    LIFECYCLE,

    /** Standard object protocol: {@code equals}, {@code hashCode}, {@code toString}. */
    OBJECT_METHOD,

    /** Mutates state in response to an instruction. */
    COMMAND,

    /** Answers a question without mutating state. */
    QUERY;

    /**
     * Returns whether this role implies state mutation.
     *
     * @return true for SETTER, COMMAND and BUSINESS
     */
    public boolean isMutation() {
        return this == SETTER || this == COMMAND || this == BUSINESS;
    }

    /**
     * Returns whether this role reads state without mutating it.
     *
     * @return true for GETTER and QUERY
     */
    public boolean isAccessor() {
        return this == GETTER || this == QUERY;
    }

    /**
     * Returns whether this role is infrastructure rather than domain behaviour.
     *
     * @return true for OBJECT_METHOD, LIFECYCLE and FACTORY
     */
    public boolean isInfrastructure() {
        return this == OBJECT_METHOD || this == LIFECYCLE || this == FACTORY;
    }

    /**
     * Returns whether this role is a domain operation.
     *
     * @return true for BUSINESS, COMMAND, QUERY and VALIDATION
     */
    public boolean isDomainOperation() {
        return this == BUSINESS || this == COMMAND || this == QUERY || this == VALIDATION;
    }
}
