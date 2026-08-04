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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * A generated column whose name an engine refuses turns a build that passed into an application
 * that will not start. The escaping is therefore not a nicety of the generator: it is the reason
 * there is one way of writing a name here rather than two.
 */
class SqlNamesTest {

    @Nested
    @DisplayName("writes a Java name in snake case")
    class WritesSnakeCase {

        @ParameterizedTest
        @CsvSource({
            "id, id",
            "firstName, first_name",
            "totalAmount, total_amount",
            "customerBillingAddress, customer_billing_address"
        })
        @DisplayName("breaking at each capital")
        void breakingAtEachCapital(String name, String expected) {
            assertThat(SqlNames.snake(name)).isEqualTo(expected);
        }

        /**
         * The reading the carrière lost by having a second, shorter implementation: one of its two
         * conversions handled runs of capitals and the other did not, and the one that did not was
         * the one every value object's columns went through.
         */
        @ParameterizedTest
        @CsvSource({"XMLParser, xml_parser", "HTTPStatus, http_status", "ID, id", "IBANCode, iban_code"})
        @DisplayName("and at the end of a run of capitals, not in the middle of it")
        void andAtTheEndOfARunOfCapitals(String name, String expected) {
            assertThat(SqlNames.snake(name)).isEqualTo(expected);
        }

        @Test
        @DisplayName("keeping a digit with what it belongs to")
        void keepingADigitWithWhatItBelongsTo() {
            assertThat(SqlNames.snake("address2")).isEqualTo("address2");
            assertThat(SqlNames.snake("address2Line")).isEqualTo("address2_line");
        }

        @Test
        @DisplayName("and refusing a name that is none")
        void andRefusingANameThatIsNone() {
            assertThatThrownBy(() -> SqlNames.snake(" ")).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("keeps a reserved word out of the way")
    class KeepsReservedWordsOutOfTheWay {

        static java.util.stream.Stream<String> reserved() {
            return SqlNames.reservedWords().stream();
        }

        /**
         * Every one of them, because the list is the whole of what protects a build from an engine
         * refusing its own DDL, and a word quietly dropped from it would be found by a user rather
         * than by this test.
         */
        @ParameterizedTest
        @MethodSource("reserved")
        @DisplayName("suffixing a column, so what it holds keeps its name")
        void suffixingAColumn(String word) {
            assertThat(SqlNames.column(word)).isEqualTo(word + "_col");
        }

        /**
         * The invariant is the one that matters — whatever comes out, an engine takes it. The
         * carrière's rule failed it twice: it left {@code values} alone for already ending in an s,
         * and turned {@code value} into {@code values}, which is reserved in its own right.
         */
        @ParameterizedTest
        @MethodSource("reserved")
        @DisplayName("and moving a table out of the way, whichever word it was")
        void andMovingATableOutOfTheWay(String word) {
            assertThat(SqlNames.table(word, "")).matches(name -> !SqlNames.isReserved(name));
        }

        @Test
        @DisplayName("by pluralising it where that is enough")
        void byPluralisingItWhereThatIsEnough() {
            assertThat(SqlNames.table("Order", "")).isEqualTo("orders");
            assertThat(SqlNames.table("Group", "")).isEqualTo("groups");
        }

        @Test
        @DisplayName("and by naming it a table where it is not")
        void andByNamingItATableWhereItIsNot() {
            assertThat(SqlNames.table("Value", "")).isEqualTo("value_tbl");
            assertThat(SqlNames.table("Values", "")).isEqualTo("values_tbl");
        }

        @Test
        @DisplayName("and leaving alone what no engine minds")
        void andLeavingAloneWhatNoEngineMinds() {
            assertThat(SqlNames.column("firstName")).isEqualTo("first_name");
            assertThat(SqlNames.table("Customer", "")).isEqualTo("customer");
        }

        @Test
        @DisplayName("even when the word only appears once the name is in snake case")
        void evenWhenTheWordOnlyAppearsInSnakeCase() {
            assertThat(SqlNames.column("Order")).isEqualTo("order_col");
            assertThat(SqlNames.table("Order", "")).isEqualTo("orders");
        }
    }

    @Nested
    @DisplayName("prefixes every table a project asks it to")
    class PrefixesTables {

        @Test
        @DisplayName("before the escaping, so the prefix is part of the name and not of the fix")
        void beforeTheEscaping() {
            assertThat(SqlNames.table("Customer", "shop_")).isEqualTo("shop_customer");
            assertThat(SqlNames.table("Order", "shop_")).isEqualTo("shop_orders");
        }
    }
}
