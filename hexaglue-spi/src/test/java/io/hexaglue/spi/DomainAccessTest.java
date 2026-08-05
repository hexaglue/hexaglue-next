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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * How the generated code reaches into a type somebody else wrote — the one place a mistake costs
 * the user's build rather than a regenerated file, and the reason every rule here is stated
 * against the smallest type that can break it.
 */
class DomainAccessTest {

    private static final TypeRef TEXT = TypeRef.of("java.lang.String");
    private static final TypeRef AMOUNT = TypeRef.of("java.math.BigDecimal");
    private static final TypeRef TRUTH = TypeRef.of("boolean");

    private static ArchType type(List<Field> fields, List<Method> methods, List<Constructor> constructors) {
        return new ValueObject(
                TypeId.of("com.shop.Money"),
                TypeStructure.builder(TypeNature.CLASS)
                        .fields(fields)
                        .methods(methods)
                        .constructors(constructors)
                        .build(),
                Classification.builder(
                                ArchKind.VALUE_OBJECT,
                                Confidence.HIGH,
                                Basis.INFERRED,
                                ProofNode.fact("VALUE_OBJECT by fixture"))
                        .build());
    }

    private static Field field(String name, TypeRef held) {
        return Field.builder(name, held).build();
    }

    private static Field constant(String name, TypeRef held) {
        return Field.builder(name, held)
                .modifiers(Set.of(Modifier.STATIC, Modifier.FINAL))
                .build();
    }

    @Nested
    @DisplayName("what makes up the state of a type")
    class State {

        @Test
        @DisplayName("is what belongs to an instance, in the order it was declared")
        void isWhatBelongsToAnInstance() {
            ArchType money = type(
                    List.of(constant("ZERO", AMOUNT), field("amount", AMOUNT), field("currency", TEXT)),
                    List.of(),
                    List.of());

            assertThat(DomainAccess.state(money)).extracting(Field::name).containsExactly("amount", "currency");
        }
    }

    @Nested
    @DisplayName("the method a value is read through")
    class Accessors {

        @Test
        @DisplayName("is the one whose shape answers with it, whatever the project called it")
        void isTheOneWhoseShapeAnswersWithIt() {
            ArchType money = type(
                    List.of(field("amount", AMOUNT)),
                    List.of(Method.of("howMuchItIs", AMOUNT), Method.of("currency", TEXT)),
                    List.of());

            assertThat(DomainAccess.accessorOf(money, field("amount", AMOUNT))).contains("howMuchItIs");
        }

        @Test
        @DisplayName("never one that has to be handed something first")
        void neverOneThatTakesSomething() {
            ArchType money = type(
                    List.of(field("amount", AMOUNT)),
                    List.of(Method.builder("amount", AMOUNT)
                            .parameters(List.of(Parameter.of("rounding", TEXT)))
                            .build()),
                    List.of());

            assertThat(DomainAccess.accessorOf(money, field("amount", AMOUNT))).isEmpty();
        }

        @Test
        @DisplayName("nor one that belongs to the class rather than to the value")
        void norAStaticOne() {
            ArchType money = type(
                    List.of(field("amount", AMOUNT)),
                    List.of(Method.builder("amount", AMOUNT)
                            .modifiers(Set.of(Modifier.STATIC))
                            .build()),
                    List.of());

            assertThat(DomainAccess.accessorOf(money, field("amount", AMOUNT))).isEmpty();
        }

        @Test
        @DisplayName("nor one answering with something else")
        void norOneAnsweringWithSomethingElse() {
            ArchType money = type(List.of(field("amount", AMOUNT)), List.of(Method.of("amount", TEXT)), List.of());

            assertThat(DomainAccess.accessorOf(money, field("amount", AMOUNT))).isEmpty();
        }

        /**
         * Two fields of the same type leave the shape undecided. Only then is a name read, and only
         * the three spellings the language itself imposes — never a convention of the project.
         */
        @Test
        @DisplayName("and when two would do, the spelling the language imposes settles it")
        void whenTwoWouldDoTheLanguageSettlesIt() {
            ArchType money = type(
                    List.of(field("amount", AMOUNT), field("tax", AMOUNT)),
                    List.of(Method.of("amount", AMOUNT), Method.of("getTax", AMOUNT)),
                    List.of());

            assertThat(DomainAccess.accessorOf(money, field("amount", AMOUNT))).contains("amount");
            assertThat(DomainAccess.accessorOf(money, field("tax", AMOUNT))).contains("getTax");
        }

        @Test
        @DisplayName("a truth being read the way a bean reads one")
        void aTruthIsReadTheWayABeanReadsOne() {
            ArchType money = type(
                    List.of(field("settled", TRUTH), field("archived", TRUTH)),
                    List.of(Method.of("isSettled", TRUTH), Method.of("isArchived", TRUTH)),
                    List.of());

            assertThat(DomainAccess.accessorOf(money, field("settled", TRUTH))).contains("isSettled");
        }

        @Test
        @DisplayName("and nothing at all when no spelling breaks the tie")
        void nothingWhenNoSpellingBreaksTheTie() {
            ArchType money = type(
                    List.of(field("amount", AMOUNT), field("tax", AMOUNT)),
                    List.of(Method.of("first", AMOUNT), Method.of("second", AMOUNT)),
                    List.of());

            assertThat(DomainAccess.accessorOf(money, field("amount", AMOUNT))).isEmpty();
        }
    }

    @Nested
    @DisplayName("what makes a type rebuildable from a row")
    class Rebuilding {

        private final List<Field> state = List.of(field("amount", AMOUNT), field("currency", TEXT));

        @Test
        @DisplayName("is a constructor taking its state, in order")
        void isAConstructorTakingItsState() {
            ArchType money = type(
                    state,
                    List.of(),
                    List.of(Constructor.of(List.of(Parameter.of("amount", AMOUNT), Parameter.of("currency", TEXT)))));

            assertThat(DomainAccess.isRebuildable(money)).isTrue();
        }

        @Test
        @DisplayName("and not one taking the same things in another order")
        void notOneTakingThemInAnotherOrder() {
            ArchType money = type(
                    state,
                    List.of(),
                    List.of(Constructor.of(List.of(Parameter.of("currency", TEXT), Parameter.of("amount", AMOUNT)))));

            assertThat(DomainAccess.isRebuildable(money)).isFalse();
        }

        @Test
        @DisplayName("nor one taking fewer of them")
        void norOneTakingFewer() {
            ArchType money = type(state, List.of(), List.of(Constructor.of(List.of(Parameter.of("amount", AMOUNT)))));

            assertThat(DomainAccess.isRebuildable(money)).isFalse();
        }

        @Test
        @DisplayName("nor a type offering none")
        void norATypeOfferingNone() {
            assertThat(DomainAccess.isRebuildable(type(state, List.of(), List.of())))
                    .isFalse();
        }

        @Test
        @DisplayName("but one among several is enough")
        void oneAmongSeveralIsEnough() {
            ArchType money = type(
                    state,
                    List.of(),
                    List.of(
                            Constructor.of(List.of(Parameter.of("amount", AMOUNT))),
                            Constructor.of(List.of(Parameter.of("amount", AMOUNT), Parameter.of("currency", TEXT)))));

            assertThat(DomainAccess.isRebuildable(money)).isTrue();
        }
    }
}
