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

package io.hexaglue.engine.rule;

import io.hexaglue.engine.Rule;
import java.util.List;

/**
 * Every rule the engine runs.
 *
 * <p>Listing them in one place is what makes the rule set reviewable: what the engine knows how
 * to conclude is this list, and nothing hides in a cascade of conditions somewhere else.</p>
 *
 * @since 7.0.0
 */
public final class Catalogue {

    private Catalogue() {}

    /**
     * Returns every rule, in no particular order — the rule set orders them by identifier.
     *
     * @return the rules
     */
    public static List<Rule> all() {
        return List.of(
                new AssertKnowledge(),
                new ConfiguredKind(),
                new DeclaredKind(),
                new ConventionalName(),
                new LocalShape(),
                new RepositorySubject(),
                new FrameworkEntryPoint(),
                new InfrastructureDependency(),
                new PortImplementation(),
                new ConsumedContract(),
                new ExposedContract(),
                new PortSignatures());
    }
}
