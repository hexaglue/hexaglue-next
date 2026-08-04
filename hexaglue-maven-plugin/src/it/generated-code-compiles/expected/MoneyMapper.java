package com.example.shop;

import javax.annotation.processing.Generated;

/**
 * Between Money and the row that stores it.
 *
 * <p>Written from the classified model. Anything changed here is lost the next time the sources are read.</p>
 */
@Generated("io.hexaglue.jpa")
public final class MoneyMapper {
    private MoneyMapper() {
    }

    /**
     * @param domain what to store
     * @return the row storing it
     */
    public static MoneyEmbeddable toEntity(Money domain) {
        return new MoneyEmbeddable(domain.amount(), domain.currency());
    }

    /**
     * @param row what was stored
     * @return what the domain makes of it
     */
    public static Money toDomain(MoneyEmbeddable row) {
        return new Money(row.getAmount(), row.getCurrency());
    }
}
