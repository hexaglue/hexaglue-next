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

package io.hexaglue.plugin.jpa;

import com.palantir.javapoet.ClassName;

/**
 * The types of the framework the generated code is written against, named once.
 *
 * <p>This backend does not depend on Spring: it writes code that will be compiled against it.
 * Naming them here is what keeps a package rename from being a search across every generator.</p>
 */
final class Spring {

    /** What the generated interface inherits its store operations from. */
    static final ClassName JPA_REPOSITORY = ClassName.get("org.springframework.data.jpa.repository", "JpaRepository");

    /** What makes a generated adapter something the container plugs in by itself. */
    static final ClassName COMPONENT = ClassName.get("org.springframework.stereotype", "Component");

    private Spring() {}
}
