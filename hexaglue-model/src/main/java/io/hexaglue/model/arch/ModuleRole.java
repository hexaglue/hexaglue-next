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

/**
 * The architectural role of a build module in a multi-module project.
 *
 * <p>The role determines how a module participates in the hexagon — where domain types are
 * expected to live, where generated adapters are routed. It is an architectural reading of the
 * build layout, decided by the engine or declared by configuration, never guessed by plugins.</p>
 *
 * @since 7.0.0
 */
public enum ModuleRole {

    /** Hosts the domain model: aggregates, entities, value objects, identifiers, ports. */
    DOMAIN,

    /** Hosts infrastructure adapters: persistence, messaging, external gateways. */
    INFRASTRUCTURE,

    /** Hosts application services and use-case orchestration. */
    APPLICATION,

    /** Hosts API contracts: DTOs, REST controllers, OpenAPI descriptions. */
    API,

    /** Hosts the assembly and bootstrap: main class, dependency wiring, configuration. */
    ASSEMBLY,

    /** Hosts shared utilities and cross-cutting concerns. */
    SHARED
}
