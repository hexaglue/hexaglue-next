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

package io.hexaglue.testkit.corpus;

/**
 * The three populations the acceptance corpus is answerable to.
 *
 * <p>They are not three sizes of the same test. Each one asks the engine a question the others
 * cannot ask: whether it still answers what it used to, whether it can read an application whose
 * domain is welded to its storage, and whether it can read one that gives it no word to lean on.
 * An engine that passes only the first has proved that it has not regressed, and nothing more.</p>
 *
 * @since 7.0.0
 */
public enum CorpusProfile {

    /**
     * Sources written in HexaGlue's own vocabulary, harvested from the legacy classification suite.
     * Its names are conventional on purpose: it is where the engine proves it does not need them.
     */
    PROFILE_1("profile1"),

    /**
     * An enterprise application in the shape the field actually has: entities mapped with JPA,
     * repositories declared to Spring Data, stereotyped services, controllers. The domain is
     * coupled to its storage, and the engine has to read the roles as if the mapping were not
     * there — and report the coupling separately.
     */
    PROFILE_2("profile2"),

    /**
     * An application with no naming convention to lean on: no suffix that names a role, no package
     * that names a layer, identifiers held as bare platform types or as wrappers nothing but a
     * lookup can tell from a value. What is left to read is position, which is the whole claim.
     */
    PROFILE_3("profile3");

    private final String directory;

    CorpusProfile(String directory) {
        this.directory = directory;
    }

    /**
     * Returns the profile's directory name, which also prefixes its keys in the committed floor.
     *
     * @return the directory name, e.g. {@code profile1}
     */
    public String directory() {
        return directory;
    }
}
