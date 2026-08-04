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

import com.palantir.javapoet.CodeBlock;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.ArchModel;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * How one value crosses between the domain and its row, asked of the same shop the whole backend
 * is read against. Both the mapper and the adapter ask this, so what it answers about a value has
 * to be the same answer in both directions.
 */
class CrossingTest {

    private final Crossing crossing = new Crossing(ShopFixture.model(), JpaOptions.defaults());

    private static CodeBlock value() {
        return CodeBlock.of("value");
    }

    private static String written(Optional<CodeBlock> crossed) {
        return crossed.map(CodeBlock::toString).orElse("");
    }

    @Test
    @DisplayName("a plain value goes over untouched, both ways")
    void aPlainValueGoesOverUntouched() {
        TypeRef text = TypeRef.of("java.lang.String");

        assertThat(written(crossing.outward(text, value()))).isEqualTo("value");
        assertThat(written(crossing.inward(text, value()))).isEqualTo("value");
    }

    @Test
    @DisplayName("an identity as the single value it is written around, and is rebuilt from it")
    void anIdentityAsTheValueItWrapsAround() {
        TypeRef identity = ShopFixture.ref(ShopFixture.ORDER_ID);

        assertThat(written(crossing.outward(identity, value()))).isEqualTo("value.value()");
        assertThat(written(crossing.inward(identity, value()))).isEqualTo("new com.shop.domain.OrderId(value)");
    }

    /**
     * An identity the analysis could not see inside has no way across: the generated code would
     * have to invent how to open it, and inventing that is how a store ends up keyed by something
     * no query matches.
     */
    @Test
    @DisplayName("but an identity nothing could see inside has no way across at all")
    void anIdentityNothingCouldSeeInsideHasNoWayAcross() {
        TypeRef opaque = ShopFixture.ref(ShopFixture.TAG_ID);

        assertThat(crossing.outward(opaque, value())).isEmpty();
        assertThat(crossing.inward(opaque, value())).isEmpty();
        assertThat(crossing.crosses(opaque)).isFalse();
    }

    @Test
    @DisplayName("a domain value through the mapper written for it, rather than opened here")
    void aDomainValueThroughItsOwnMapper() {
        TypeRef money = ShopFixture.ref(ShopFixture.MONEY);

        assertThat(written(crossing.outward(money, value()))).isEqualTo("com.shop.domain.MoneyMapper.toEntity(value)");
        assertThat(written(crossing.inward(money, value()))).isEqualTo("com.shop.domain.MoneyMapper.toDomain(value)");
    }

    @Test
    @DisplayName("and nothing at all for a value no mapper was written for")
    void nothingForAValueNoMapperWasWrittenFor() {
        TypeRef unreachable = ShopFixture.ref(ShopFixture.ORDER_LINE);

        assertThat(crossing.crosses(unreachable)).isFalse();
    }

    /**
     * Anything with a life of its own is named by its identity, which is the reference a store
     * keeps: handing a whole aggregate across a field would be storing it twice.
     */
    @Test
    @DisplayName("nothing with a life of its own crosses as a field")
    void nothingWithALifeOfItsOwnCrossesAsAField() {
        assertThat(crossing.crosses(ShopFixture.ref(ShopFixture.INVOICE))).isFalse();
        assertThat(crossing.crosses(ShopFixture.ref(ShopFixture.ORDER_PLACED))).isFalse();
    }

    @Test
    @DisplayName("and neither does a container, whichever kind")
    void neitherDoesAContainer() {
        TypeRef lines = TypeRef.parameterized("java.util.List", ShopFixture.ref(ShopFixture.ORDER_LINE));
        TypeRef maybe = TypeRef.parameterized("java.util.Optional", ShopFixture.ref(ShopFixture.MONEY));
        TypeRef byName = TypeRef.parameterized(
                "java.util.Map", TypeRef.of("java.lang.String"), ShopFixture.ref(ShopFixture.MONEY));

        assertThat(crossing.crosses(lines)).isFalse();
        assertThat(crossing.crosses(maybe)).isFalse();
        assertThat(crossing.crosses(byName)).isFalse();
        assertThat(crossing.inward(lines, value())).isEmpty();
    }

    @Test
    @DisplayName("and the mapper of a type is named after it, written or not")
    void theMapperOfATypeIsNamedAfterIt() {
        assertThat(crossing.mapperFor(ShopFixture.ref(ShopFixture.MONEY)).toString())
                .isEqualTo("com.shop.domain.MoneyMapper");
    }

    @Test
    @DisplayName("a type the analysis never reached crosses as itself, for want of anything to say")
    void aTypeTheAnalysisNeverReachedCrossesAsItself() {
        ArchModel model = ShopFixture.model();
        TypeRef unknown = TypeRef.of("java.time.Instant");

        assertThat(model.type(io.hexaglue.model.TypeId.of(unknown.qualifiedName())))
                .isEmpty();
        assertThat(crossing.crosses(unknown)).isTrue();
    }
}
