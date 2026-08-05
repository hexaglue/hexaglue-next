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

package io.hexaglue.spi;

import io.hexaglue.model.Modifier;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Optional;

/**
 * How the generated code reaches into a domain type it did not write.
 *
 * <p>This is the one place where a generator touches code somebody else wrote. Everywhere else it
 * emits new files, and a mistake costs a regenerated file; here a mistake costs the user's build.
 * So nothing is assumed: an accessor is found by <strong>shape</strong> — one method, no
 * parameters, answering with the type of the field — and a name is read only to break a tie
 * between several that would do, using the spelling the language itself imposes (a record component
 * answers under its own name, a bean under {@code get} or {@code is}).</p>
 *
 * <p>Building goes the same way: the constructor whose parameters line up with the state of the
 * type, which the canonical constructor of a record always does. When nothing lines up, the answer
 * is nothing at all — a diagnostic rather than a guess that compiles here and breaks there.</p>
 *
 * <p>It sits here rather than inside one backend because the question is the same wherever
 * generated code has to reach a domain type: persistence reads a field to put it in a row, a web
 * layer reads the same field to put it in a response, and the two answering differently would mean
 * one of them is wrong. What each backend does with the answer is its own business.</p>
 *
 * @since 7.0.0
 */
public final class DomainAccess {

    private DomainAccess() {}

    /**
     * Returns the fields that make up the state of a type, in declaration order.
     *
     * @param type the domain type
     * @return its state fields
     */
    public static List<Field> state(ArchType type) {
        return type.structure().fields().stream()
                .filter(field -> !field.modifiers().contains(Modifier.STATIC))
                .toList();
    }

    /**
     * Returns the method that answers with the value of the given field.
     *
     * @param type the domain type holding the field
     * @param field the field to read
     * @return the name of the accessor, or empty when the type does not offer one unambiguously
     */
    public static Optional<String> accessorOf(ArchType type, Field field) {
        List<Method> answering = type.structure().methods().stream()
                .filter(method -> method.parameters().isEmpty())
                .filter(method -> !method.modifiers().contains(Modifier.STATIC))
                .filter(method ->
                        method.returnType().qualifiedName().equals(field.type().qualifiedName()))
                .toList();
        if (answering.size() == 1) {
            return Optional.of(answering.get(0).name());
        }
        return answering.stream()
                .map(Method::name)
                .filter(name -> spellings(field).contains(name))
                .findFirst();
    }

    /**
     * The names the language itself would give this accessor: a record component answers under its
     * own name, a bean under {@code get}, a boolean bean under {@code is}. Nothing here is a
     * convention of the project being read — these are the three spellings Java and its frameworks
     * settled on long ago, and they are only consulted when shape alone leaves a choice.
     */
    private static List<String> spellings(Field field) {
        String capitalised =
                Character.toUpperCase(field.name().charAt(0)) + field.name().substring(1);
        return List.of(field.name(), "get" + capitalised, "is" + capitalised);
    }

    /**
     * Answers whether the type can be rebuilt from its state, that is whether one of its
     * constructors takes exactly the state, in order.
     *
     * @param type the domain type
     * @return true when such a constructor exists
     */
    public static boolean isRebuildable(ArchType type) {
        return type.structure().constructors().stream().anyMatch(constructor -> takesTheState(constructor, type));
    }

    private static boolean takesTheState(Constructor constructor, ArchType type) {
        List<Field> state = state(type);
        List<Parameter> parameters = constructor.parameters();
        if (parameters.size() != state.size()) {
            return false;
        }
        for (int index = 0; index < state.size(); index++) {
            String expected = state.get(index).type().qualifiedName();
            if (!parameters.get(index).type().qualifiedName().equals(expected)) {
                return false;
            }
        }
        return true;
    }
}
