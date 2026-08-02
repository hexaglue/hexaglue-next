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

import io.hexaglue.model.Modifier;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Field;
import java.util.List;

/**
 * What a declaration's own shape says, read once for every rule that needs it.
 *
 * <p>Two rules ask the same question for different reasons — one to read a value object, another to
 * tell an event a port publishes from a request it carries — and asking it twice would let the two
 * answers drift apart. There is one reading of immutability in the engine, and it is here.</p>
 *
 * @since 7.0.0
 */
final class Shapes {

    private Shapes() {}

    /**
     * Returns the fields that make up the state of a declaration. A static field belongs to the
     * type, not to its instances, and says nothing about what the instances are.
     *
     * @param type the declaration to read
     * @return the instance fields, in declaration order
     */
    static List<Field> state(TypeNode type) {
        return type.fields().stream()
                .filter(field -> !field.modifiers().contains(Modifier.STATIC))
                .toList();
    }

    /**
     * Returns whether the state of a declaration cannot change. A type holding no state at all is
     * not immutable in any sense worth reading: vacuous immutability is the absence of a signal,
     * not one.
     *
     * @param type the declaration to read
     * @return true when the declaration has state and none of it can change
     */
    static boolean isImmutable(TypeNode type) {
        List<Field> state = state(type);
        return switch (type.nature()) {
            case RECORD -> !state.isEmpty();
            case ENUM -> true;
            case CLASS ->
                !state.isEmpty()
                        && state.stream().allMatch(field -> field.modifiers().contains(Modifier.FINAL));
            // An interface holds no state and an annotation is a declaration about other
            // declarations: neither has a shape that says what it is.
            case INTERFACE, ANNOTATION -> false;
        };
    }
}
