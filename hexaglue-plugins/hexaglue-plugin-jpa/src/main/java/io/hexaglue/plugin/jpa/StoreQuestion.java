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

import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What the store makes of one question a port asks.
 *
 * <p>Which store operation answers a port method is read from its <strong>shape</strong> — what it
 * takes, and what it answers with — and never from how the project spelled it. The carrière did the
 * opposite: it parsed the port's method names, so a domain that named things its own way got
 * nothing.</p>
 *
 * <p>One shape is shared by two operations: taking the whole aggregate and answering nothing is
 * either storing it or erasing it. There the port's own word settles it, and only a word the store
 * itself uses counts — never a convention of the project being read. Erasing is never concluded
 * from shape alone: a query that reads the wrong rows can be run again, a deletion that erases them
 * cannot.</p>
 *
 * @param name what the store calls the operation
 * @param taking what the port hands over
 * @param answer what the store answers with
 * @param by the fields of the aggregate the question is about, empty unless it takes fields
 */
record StoreQuestion(String name, Taking taking, Answer answer, List<Field> by) {

    /** The words the store uses for storing something whole. */
    private static final Set<String> STORES = Set.of("save");

    /** The words the store uses for erasing. */
    private static final Set<String> ERASES = Set.of("delete", "deleteById");

    /** What a port method hands the store. */
    enum Taking {
        /** Nothing at all: the question is about everything the store holds. */
        NOTHING,
        /** The aggregate itself. */
        THE_WHOLE,
        /** The identity of the aggregate. */
        THE_IDENTITY,
        /** Values matching fields the aggregate holds. */
        FIELDS
    }

    /** What the store answers with. */
    enum Answer {
        /** Nothing: the method returns void. */
        NOTHING,
        /** The aggregate itself, always. */
        DIRECT,
        /** The aggregate, if there is one. */
        MAYBE,
        /** As many aggregates as there are. */
        MANY,
        /** Whether there is one. */
        TRUTH,
        /** How many there are. */
        COUNT
    }

    /**
     * Validates and copies the components.
     */
    StoreQuestion {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(taking, "taking must not be null");
        Objects.requireNonNull(answer, "answer must not be null");
        Objects.requireNonNull(by, "by must not be null");
        by = List.copyOf(by);
    }

    /**
     * Reads what the store does with a question a port asks.
     *
     * @param method the port method
     * @param aggregate the aggregate the port keeps
     * @return what the store answers it with, or empty when it has no answer for it
     */
    static Optional<StoreQuestion> of(Method method, AggregateRoot aggregate) {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        return answerOf(method.returnType(), aggregate).flatMap(answer -> asked(method, aggregate, answer));
    }

    /** Whether the generated interface has to declare this question rather than inherit it. */
    boolean declared() {
        return taking == Taking.FIELDS;
    }

    private static Optional<StoreQuestion> asked(Method method, AggregateRoot aggregate, Answer answer) {
        List<Parameter> parameters = method.parameters();
        if (parameters.isEmpty()) {
            return aboutEverything(answer);
        }
        if (parameters.size() == 1) {
            TypeRef only = parameters.get(0).type();
            if (isTheWhole(only, aggregate)) {
                return aboutTheWhole(method.name(), answer);
            }
            if (isTheIdentity(only, aggregate)) {
                return aboutTheIdentity(method.name(), answer);
            }
        }
        return aboutFields(parameters, aggregate, answer);
    }

    /** A question about everything the store holds: how many there are, or all of them. */
    private static Optional<StoreQuestion> aboutEverything(Answer answer) {
        return switch (answer) {
            case MANY -> Optional.of(inherited("findAll", Taking.NOTHING, answer));
            case COUNT -> Optional.of(inherited("count", Taking.NOTHING, answer));
            default -> Optional.empty();
        };
    }

    /**
     * A question handing the whole aggregate over. Answering with it is storing it and nothing
     * else; answering nothing is either, so the port's word decides and only the store's words are
     * read.
     */
    private static Optional<StoreQuestion> aboutTheWhole(String asked, Answer answer) {
        if (answer == Answer.DIRECT) {
            return Optional.of(inherited("save", Taking.THE_WHOLE, answer));
        }
        if (answer != Answer.NOTHING) {
            return Optional.empty();
        }
        if (STORES.contains(asked)) {
            return Optional.of(inherited("save", Taking.THE_WHOLE, answer));
        }
        if (ERASES.contains(asked)) {
            return Optional.of(inherited("delete", Taking.THE_WHOLE, answer));
        }
        return Optional.empty();
    }

    /**
     * A question about one aggregate, named by its identity. Answering with it always is a promise
     * the store cannot make — there may be no such row — so only the shapes that allow for that are
     * answered.
     */
    private static Optional<StoreQuestion> aboutTheIdentity(String asked, Answer answer) {
        return switch (answer) {
            case MAYBE -> Optional.of(inherited("findById", Taking.THE_IDENTITY, answer));
            case TRUTH -> Optional.of(inherited("existsById", Taking.THE_IDENTITY, answer));
            case NOTHING ->
                ERASES.contains(asked)
                        ? Optional.of(inherited("deleteById", Taking.THE_IDENTITY, answer))
                        : Optional.empty();
            default -> Optional.empty();
        };
    }

    /**
     * A question about the fields the aggregate holds. Every value handed over has to match one of
     * them that is not its identity — asking by identity is what the store already answers — and
     * the shape of the answer says which of the three questions it is.
     */
    private static Optional<StoreQuestion> aboutFields(
            List<Parameter> parameters, AggregateRoot aggregate, Answer answer) {
        List<Field> matched = new ArrayList<>();
        for (Parameter parameter : parameters) {
            Optional<Field> field = fieldHolding(parameter.type(), aggregate);
            if (field.isEmpty()) {
                return Optional.empty();
            }
            matched.add(field.orElseThrow());
        }
        return verbFor(answer)
                .map(verb -> new StoreQuestion(verb + "By" + joined(matched), Taking.FIELDS, answer, matched));
    }

    private static Optional<String> verbFor(Answer answer) {
        return switch (answer) {
            case TRUTH -> Optional.of("exists");
            case COUNT -> Optional.of("count");
            case MAYBE, MANY -> Optional.of("find");
            default -> Optional.empty();
        };
    }

    private static StoreQuestion inherited(String name, Taking taking, Answer answer) {
        return new StoreQuestion(name, taking, answer, List.of());
    }

    /**
     * What the store can answer with. A count is answered in the width the store counts in, and a
     * collection in a shape a list satisfies: narrowing or converting either would be the generator
     * deciding something the port did not ask for.
     */
    private static Optional<Answer> answerOf(TypeRef answer, AggregateRoot aggregate) {
        String named = answer.qualifiedName();
        if ("void".equals(named)) {
            return Optional.of(Answer.NOTHING);
        }
        if ("boolean".equals(named) || "java.lang.Boolean".equals(named)) {
            return Optional.of(Answer.TRUTH);
        }
        if ("long".equals(named) || "java.lang.Long".equals(named)) {
            return Optional.of(Answer.COUNT);
        }
        String kept = aggregate.id().qualifiedName();
        if (kept.equals(named)) {
            return Optional.of(Answer.DIRECT);
        }
        if (!kept.equals(answer.unwrapElement().qualifiedName())) {
            return Optional.empty();
        }
        if (answer.isOptionalLike()) {
            return Optional.of(Answer.MAYBE);
        }
        return holdsAList(named) ? Optional.of(Answer.MANY) : Optional.empty();
    }

    private static boolean holdsAList(String named) {
        return "java.util.List".equals(named)
                || "java.util.Collection".equals(named)
                || "java.lang.Iterable".equals(named);
    }

    private static boolean isTheWhole(TypeRef type, AggregateRoot aggregate) {
        return type.qualifiedName().equals(aggregate.id().qualifiedName());
    }

    private static boolean isTheIdentity(TypeRef type, AggregateRoot aggregate) {
        return aggregate
                .identityField()
                .filter(identity -> identity.type().qualifiedName().equals(type.qualifiedName()))
                .isPresent();
    }

    /** The field of the aggregate a value of this type would be matched against, if any. */
    private static Optional<Field> fieldHolding(TypeRef type, AggregateRoot aggregate) {
        return aggregate.structure().fields().stream()
                .filter(field -> !field.modifiers().contains(Modifier.STATIC))
                .filter(field -> !field.isIdentity())
                .filter(field -> field.type().qualifiedName().equals(type.qualifiedName()))
                .findFirst();
    }

    private static String joined(List<Field> fields) {
        return fields.stream()
                .map(StoreQuestion::capitalised)
                .reduce("", (all, one) -> all.isEmpty() ? one : all + "And" + one);
    }

    private static String capitalised(Field field) {
        String name = field.name();
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }
}
