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

/**
 * The persistence backend: what a domain looks like once it is stored.
 *
 * <p>Everything here is written from the classified model and from nothing else. What a type is,
 * what carries its identity, what it is made of and which port keeps it are all questions the
 * engine has already answered; this backend turns those answers into entities, embeddables,
 * repositories and adapters, and asks none of them again.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.plugin.jpa;
