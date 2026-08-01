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

package io.hexaglue.model.code;

import io.hexaglue.model.TypeId;
import java.util.List;
import java.util.Objects;

/**
 * Facts extracted from one method body in a single AST traversal: which types it invokes and which
 * it instantiates. Present in the code model only when the method-bodies capability was requested.
 *
 * @param declaringType the type declaring the method
 * @param methodName the method name
 * @param invocations the invoked members, in occurrence order
 * @param instantiations the instantiated types, in occurrence order
 * @since 7.0.0
 */
public record MethodBodyFacts(
        TypeId declaringType, String methodName, List<Invocation> invocations, List<Instantiation> instantiations) {

    /**
     * Validates the method identity and copies the fact lists.
     */
    public MethodBodyFacts {
        Objects.requireNonNull(declaringType, "declaringType must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(invocations, "invocations must not be null");
        Objects.requireNonNull(instantiations, "instantiations must not be null");
        if (methodName.isBlank()) {
            throw new IllegalArgumentException("methodName must not be blank");
        }
        invocations = List.copyOf(invocations);
        instantiations = List.copyOf(instantiations);
    }

    /**
     * One invocation observed in a method body.
     *
     * @param target the type whose member is invoked, possibly an external stub
     * @param methodName the invoked method name
     * @since 7.0.0
     */
    public record Invocation(TypeId target, String methodName) {

        /**
         * Validates the target and the method name.
         */
        public Invocation {
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(methodName, "methodName must not be null");
            if (methodName.isBlank()) {
                throw new IllegalArgumentException("methodName must not be blank");
            }
        }
    }

    /**
     * One instantiation observed in a method body.
     *
     * @param target the instantiated type, possibly an external stub
     * @since 7.0.0
     */
    public record Instantiation(TypeId target) {

        /**
         * Validates the target.
         */
        public Instantiation {
            Objects.requireNonNull(target, "target must not be null");
        }
    }
}
