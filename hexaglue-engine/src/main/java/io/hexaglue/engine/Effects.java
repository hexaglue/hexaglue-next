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

package io.hexaglue.engine;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.UseCase.UseCaseType;
import io.hexaglue.model.code.MethodBodyFacts;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Whether a use case changes anything, read from what the code answering it hands over.
 *
 * <p>A method answering with nothing has done something, and that much the declaration says. The
 * rest of them all look alike: taking the identity of an aggregate and answering with the aggregate
 * is how a hexagon spells both looking one up and putting it through a change. Nothing in the two
 * declarations differs, so the difference is read where it exists — in the body of whatever answers
 * the port.</p>
 *
 * <p>What counts is <strong>handing a type the domain owns</strong> — an aggregate or one of its
 * parts — to a way out. Handing over an identity or a value is how one asks; handing over the thing
 * itself is how one tells. Building an instance changes nothing on its own: a reading that assembles
 * a fresh aggregate to answer with has still only read.</p>
 *
 * <p>The walk follows calls the answering type makes on itself, because a codebase routinely puts
 * the asking behind a hand of its own while telling stays in plain sight — reading one level deep
 * would be right by luck on such a codebase and wrong on the next. Overloads are not told apart by
 * the facts of a body, which carry a target and a name and not the arguments: <strong>if any
 * overload of that name hands something owned over, the use case changed something</strong>. A
 * command read as a query is a GET the web itself will replay.</p>
 */
final class Effects {

    /** What the domain owns, as opposed to what names it or describes it. */
    private static final Set<ArchKind> OWNED = Set.of(ArchKind.AGGREGATE_ROOT, ArchKind.ENTITY);

    private final Links links;
    private final Map<TypeId, Map<String, List<MethodBodyFacts>>> bodies;

    Effects(Links links) {
        this.links = Objects.requireNonNull(links, "links must not be null");
        this.bodies = index(links.code().bodyFacts());
    }

    /**
     * Reads what one use case of a way in does.
     *
     * @param port the way in declaring it
     * @param method the method carrying it
     * @return whether it tells or only asks
     */
    UseCaseType of(TypeNode port, Method method) {
        Objects.requireNonNull(port, "port must not be null");
        Objects.requireNonNull(method, "method must not be null");
        if ("void".equals(method.returnType().qualifiedName())) {
            return UseCaseType.COMMAND;
        }
        return links.answering(port.id()).anyMatch(answering -> changes(answering.id(), method.name()))
                ? UseCaseType.COMMAND
                : UseCaseType.QUERY;
    }

    /**
     * Whether the named method of the answering type ends up handing something owned to a way out,
     * following the calls that type makes on itself.
     */
    private boolean changes(TypeId answering, String name) {
        Deque<String> pending = new ArrayDeque<>(List.of(name));
        Set<String> walked = new HashSet<>();
        while (!pending.isEmpty()) {
            String current = pending.pop();
            if (!walked.add(current)) {
                continue;
            }
            for (MethodBodyFacts.Invocation call : invocations(answering, current)) {
                if (call.target().equals(answering)) {
                    pending.push(call.methodName());
                } else if (isTelling(call)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether one call hands a type the domain owns to a way out. */
    private boolean isTelling(MethodBodyFacts.Invocation call) {
        return links.is(call.target(), ArchKind.DRIVEN_PORT)
                && links.code().type(call.target()).stream()
                        .flatMap(port -> port.methods().stream())
                        .filter(method -> method.name().equals(call.methodName()))
                        .anyMatch(this::takesSomethingOwned);
    }

    private boolean takesSomethingOwned(Method method) {
        return links.namedInPerimeter(method.parameters().stream().map(Parameter::type))
                .anyMatch(named -> OWNED.stream().anyMatch(kind -> links.is(named, kind)));
    }

    private List<MethodBodyFacts.Invocation> invocations(TypeId declaring, String name) {
        return bodies.getOrDefault(declaring, Map.of()).getOrDefault(name, List.of()).stream()
                .map(MethodBodyFacts::invocations)
                .flatMap(List::stream)
                .toList();
    }

    private static Map<TypeId, Map<String, List<MethodBodyFacts>>> index(List<MethodBodyFacts> facts) {
        Map<TypeId, Map<String, List<MethodBodyFacts>>> indexed = new LinkedHashMap<>();
        for (MethodBodyFacts body : facts) {
            indexed.computeIfAbsent(body.declaringType(), type -> new LinkedHashMap<>())
                    .computeIfAbsent(body.methodName(), name -> new ArrayList<>())
                    .add(body);
        }
        return indexed;
    }
}
