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

package io.hexaglue.frontend;

import io.hexaglue.model.EnumSets;
import io.hexaglue.model.Modifier;
import java.util.EnumSet;
import java.util.Set;
import spoon.reflect.declaration.ModifierKind;

/**
 * Reads parser modifiers into model modifiers. The resulting set iterates in the model's natural
 * modifier order, so a rendering of a declaration never depends on parse order.
 */
final class Modifiers {

    private Modifiers() {}

    /**
     * Reads a set of parser modifiers.
     *
     * @param modifiers the parser modifiers
     * @return the model modifiers, in natural order
     */
    static Set<Modifier> of(Set<ModifierKind> modifiers) {
        if (modifiers.isEmpty()) {
            return Set.of();
        }
        Set<Modifier> mapped = EnumSet.noneOf(Modifier.class);
        for (ModifierKind modifier : modifiers) {
            mapped.add(map(modifier));
        }
        return EnumSets.ordered(mapped);
    }

    private static Modifier map(ModifierKind modifier) {
        return switch (modifier) {
            case PUBLIC -> Modifier.PUBLIC;
            case PROTECTED -> Modifier.PROTECTED;
            case PRIVATE -> Modifier.PRIVATE;
            case STATIC -> Modifier.STATIC;
            case FINAL -> Modifier.FINAL;
            case ABSTRACT -> Modifier.ABSTRACT;
            case NATIVE -> Modifier.NATIVE;
            case SYNCHRONIZED -> Modifier.SYNCHRONIZED;
            case TRANSIENT -> Modifier.TRANSIENT;
            case VOLATILE -> Modifier.VOLATILE;
            case STRICTFP -> Modifier.STRICTFP;
            case SEALED -> Modifier.SEALED;
            case NON_SEALED -> Modifier.NON_SEALED;
            // Deliberately exhaustive, with no default: a modifier the parser learns and
            // the model does not know must break this build, not be dropped silently.
        };
    }
}
