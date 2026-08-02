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

/**
 * The architectural kinds a type can be classified as, in a DDD / hexagonal reading.
 *
 * <p>Every type of the analysis scope receives a verdict: one of these kinds, with
 * {@code UNCLASSIFIED} as the categorized fallback — never a silent disappearance. The kinds cover
 * the hexagon and the adapters that surround it, so that an audit of an application under
 * migration describes every type it reads instead of leaving the outer ring to be guessed.</p>
 *
 * @since 7.0.0
 */
public enum ArchKind {

    /** An aggregate root: the consistency entry point of an aggregate. */
    AGGREGATE_ROOT,

    /** A domain entity: identity and lifecycle. */
    ENTITY,

    /** A value object: immutable, identified by its attributes. */
    VALUE_OBJECT,

    /** An identifier: a value object carrying an entity's identity. */
    IDENTIFIER,

    /** A domain event: something that happened in the domain. */
    DOMAIN_EVENT,

    /** A domain service: stateless domain behaviour that fits no entity. */
    DOMAIN_SERVICE,

    /** A driving port: an interface the outside world calls into the hexagon. */
    DRIVING_PORT,

    /** A driven port: an interface the hexagon calls toward the outside world. */
    DRIVEN_PORT,

    /** An application service: orchestrates use cases over domain and ports. */
    APPLICATION_SERVICE,

    /** A command handler: mutates state in response to a command. */
    COMMAND_HANDLER,

    /** A query handler: answers a question without mutating state. */
    QUERY_HANDLER,

    /** A driving adapter: the entry point through which the outside world reaches a driving port. */
    DRIVING_ADAPTER,

    /** A driven adapter: the implementation through which a driven port reaches the outside world. */
    DRIVEN_ADAPTER,

    /** The categorized fallback: no kind reached sufficient confidence. */
    UNCLASSIFIED;

    /**
     * Returns whether this kind belongs to the domain layer.
     *
     * @return true for aggregate roots, entities, value objects, identifiers, domain events and
     *     domain services
     */
    public boolean isDomain() {
        return switch (this) {
            case AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, IDENTIFIER, DOMAIN_EVENT, DOMAIN_SERVICE -> true;
            default -> false;
        };
    }

    /**
     * Returns whether this kind is a port.
     *
     * @return true for driving and driven ports
     */
    public boolean isPort() {
        return this == DRIVING_PORT || this == DRIVEN_PORT;
    }

    /**
     * Returns whether this kind belongs to the application layer.
     *
     * @return true for application services, command handlers and query handlers
     */
    public boolean isApplication() {
        return switch (this) {
            case APPLICATION_SERVICE, COMMAND_HANDLER, QUERY_HANDLER -> true;
            default -> false;
        };
    }

    /**
     * Returns whether this kind is an adapter read from the analyzed sources. Such an adapter is
     * classified so that audit and migration cover the whole perimeter; an adapter produced by a
     * generation plugin is an output of the pipeline and never an input of classification.
     *
     * @return true for driving and driven adapters
     */
    public boolean isAdapter() {
        return this == DRIVING_ADAPTER || this == DRIVEN_ADAPTER;
    }
}
