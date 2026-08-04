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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Which store operation answers a question a port asks. Read from what the method takes and what
 * it answers with — and where two operations share a shape, from the word the store itself uses,
 * never from a convention of the project being read.
 */
class StoreQuestionTest {

    private static final TypeRef INVOICE = ShopFixture.ref(ShopFixture.INVOICE);
    private static final TypeRef IDENTITY = ShopFixture.ref(ShopFixture.INVOICE_ID);
    private static final TypeRef TEXT = TypeRef.of("java.lang.String");
    private static final TypeRef VOID = TypeRef.of("void");

    private static final AggregateRoot INVOICES = ShopFixture.model()
            .all(AggregateRoot.class)
            .filter(aggregate -> aggregate.id().equals(ShopFixture.INVOICE))
            .findFirst()
            .orElseThrow();

    private static Optional<StoreQuestion> asked(String name, TypeRef answer, TypeRef... taken) {
        Method.Builder method = Method.builder(name, answer);
        List<Parameter> parameters = new java.util.ArrayList<>();
        for (int index = 0; index < taken.length; index++) {
            parameters.add(Parameter.of("argument" + index, taken[index]));
        }
        return StoreQuestion.of(method.parameters(parameters).build(), INVOICES);
    }

    private static String answering(String name, TypeRef answer, TypeRef... taken) {
        return asked(name, answer, taken).map(StoreQuestion::name).orElse("");
    }

    @Nested
    @DisplayName("what the store already answers")
    class Inherited {

        @Test
        @DisplayName("one aggregate by its identity, or whether there is one")
        void oneByItsIdentity() {
            assertThat(answering("byId", TypeRef.parameterized("java.util.Optional", INVOICE), IDENTITY))
                    .isEqualTo("findById");
            assertThat(answering("has", TypeRef.of("boolean"), IDENTITY)).isEqualTo("existsById");
            assertThat(answering("has", TypeRef.of("java.lang.Boolean"), IDENTITY))
                    .isEqualTo("existsById");
        }

        @Test
        @DisplayName("all of them, or how many there are, in the widths the store counts in")
        void allOfThemOrHowMany() {
            assertThat(answering("all", TypeRef.parameterized("java.util.List", INVOICE)))
                    .isEqualTo("findAll");
            assertThat(answering("all", TypeRef.parameterized("java.util.Collection", INVOICE)))
                    .isEqualTo("findAll");
            assertThat(answering("all", TypeRef.parameterized("java.lang.Iterable", INVOICE)))
                    .isEqualTo("findAll");
            assertThat(answering("count", TypeRef.of("long"))).isEqualTo("count");
            assertThat(answering("count", TypeRef.of("java.lang.Long"))).isEqualTo("count");
        }

        /**
         * A shape the store cannot answer in the terms the port asked for is refused rather than
         * converted: narrowing a count or reshaping a collection would be the generator deciding
         * something nobody asked it to decide.
         */
        @Test
        @DisplayName("and nothing when the port asks for a shape the store does not answer in")
        void nothingForAShapeItDoesNotAnswerIn() {
            assertThat(asked("count", TypeRef.of("int"))).isEmpty();
            assertThat(asked("all", TypeRef.parameterized("java.util.Set", INVOICE)))
                    .isEmpty();
            assertThat(asked("all", TypeRef.parameterized("java.util.stream.Stream", INVOICE)))
                    .isEmpty();
            assertThat(asked("all", TypeRef.parameterized("java.util.List", TEXT)))
                    .isEmpty();
            assertThat(asked("describe", TEXT, IDENTITY)).isEmpty();
            assertThat(asked("theOne", INVOICE, IDENTITY)).isEmpty();
            assertThat(asked("nothing", VOID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("the one shape two operations share")
    class TakingTheWhole {

        @Test
        @DisplayName("answering with the aggregate is storing it, whatever the port called it")
        void answeringWithItIsStoringIt() {
            assertThat(answering("keep", INVOICE, INVOICE)).isEqualTo("save");
        }

        @Test
        @DisplayName("answering nothing is either, so the store's own word settles it")
        void answeringNothingIsSettledByTheStoresWord() {
            assertThat(answering("save", VOID, INVOICE)).isEqualTo("save");
            assertThat(answering("delete", VOID, INVOICE)).isEqualTo("delete");
        }

        @Test
        @DisplayName("and a word the store does not use settles nothing at all")
        void aWordTheStoreDoesNotUseSettlesNothing() {
            assertThat(asked("archive", VOID, INVOICE)).isEmpty();
            assertThat(asked("store", VOID, INVOICE)).isEmpty();
        }

        @Test
        @DisplayName("erasing by identity being read the same way, and never from shape alone")
        void erasingByIdentityIsReadTheSameWay() {
            assertThat(answering("delete", VOID, IDENTITY)).isEqualTo("deleteById");
            assertThat(answering("deleteById", VOID, IDENTITY)).isEqualTo("deleteById");
            assertThat(asked("touch", VOID, IDENTITY)).isEmpty();
        }

        @Test
        @DisplayName("and any other answer to the whole aggregate is refused")
        void anyOtherAnswerToTheWholeIsRefused() {
            assertThat(asked("save", TypeRef.of("boolean"), INVOICE)).isEmpty();
            assertThat(asked("save", TypeRef.parameterized("java.util.List", INVOICE), INVOICE))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("what the generated interface has to declare")
    class Declared {

        @Test
        @DisplayName("a question about a field the aggregate holds, named from that field")
        void aQuestionAboutAFieldItHolds() {
            Optional<StoreQuestion> question =
                    asked("withReference", TypeRef.parameterized("java.util.List", INVOICE), TEXT);

            assertThat(question).isPresent();
            assertThat(question.orElseThrow().name()).isEqualTo("findByReference");
            assertThat(question.orElseThrow().declared()).isTrue();
            assertThat(question.orElseThrow().by())
                    .singleElement()
                    .satisfies(field -> assertThat(field.name()).isEqualTo("reference"));
        }

        @Test
        @DisplayName("the shape of the answer saying which of the three questions it is")
        void theAnswerSaysWhichQuestionItIs() {
            assertThat(answering("any", TypeRef.of("boolean"), TEXT)).isEqualTo("existsByReference");
            assertThat(answering("many", TypeRef.of("long"), TEXT)).isEqualTo("countByReference");
            assertThat(answering("one", TypeRef.parameterized("java.util.Optional", INVOICE), TEXT))
                    .isEqualTo("findByReference");
        }

        @Test
        @DisplayName("and nothing at all about a value the aggregate does not hold")
        void nothingAboutAValueItDoesNotHold() {
            assertThat(asked("at", TypeRef.parameterized("java.util.List", INVOICE), TypeRef.of("java.time.Instant")))
                    .isEmpty();
        }

        /**
         * Asking by identity is what the store already answers, so the identity field is never one
         * of the fields a declared query is named after.
         */
        @Test
        @DisplayName("never naming itself after the identity, which the store already answers by")
        void neverNamedAfterTheIdentity() {
            assertThat(asked("byIdentity", TypeRef.parameterized("java.util.List", INVOICE), IDENTITY))
                    .isEmpty();
        }
    }
}
