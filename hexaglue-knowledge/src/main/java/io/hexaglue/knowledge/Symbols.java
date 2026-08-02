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

package io.hexaglue.knowledge;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Single implementation of "is this symbol precise enough to be knowledge". A selector rejected
 * here is a selector that could have matched a type its pack never meant.
 *
 * <p>Both the selectors and the pack reader consult it: the first to defend its own invariant, the
 * second to answer with a coded diagnostic instead of an exception. One rule, two reactions.</p>
 */
final class Symbols {

    /** A Java name segment; {@code $} is admitted so that nested types can be named. */
    private static final String SEGMENT = "[A-Za-z_$][A-Za-z0-9_$]*";

    /** A qualified name: at least a package and a type, never a bare simple name. */
    private static final Pattern QUALIFIED_NAME = Pattern.compile(SEGMENT + "(\\." + SEGMENT + ")+");

    /** A package prefix: one or more segments, no trailing dot. */
    private static final Pattern PACKAGE_PREFIX = Pattern.compile(SEGMENT + "(\\." + SEGMENT + ")*");

    private Symbols() {}

    /**
     * Returns why the symbol cannot name a type, or empty when it can.
     *
     * @param symbol the candidate qualified name
     * @return the reason it is refused, empty when acceptable
     */
    static Optional<String> qualifiedNameProblem(String symbol) {
        if (QUALIFIED_NAME.matcher(symbol).matches()) {
            return Optional.empty();
        }
        return Optional.of("must be a qualified name, package included, but was: " + quote(symbol));
    }

    /**
     * Returns why the symbol cannot name a package prefix, or empty when it can.
     *
     * @param prefix the candidate package prefix
     * @return the reason it is refused, empty when acceptable
     */
    static Optional<String> packagePrefixProblem(String prefix) {
        if (PACKAGE_PREFIX.matcher(prefix).matches()) {
            return Optional.empty();
        }
        return Optional.of("must be a package prefix, written without a trailing dot, but was: " + quote(prefix));
    }

    private static String quote(String symbol) {
        return "'" + symbol + "'";
    }
}
