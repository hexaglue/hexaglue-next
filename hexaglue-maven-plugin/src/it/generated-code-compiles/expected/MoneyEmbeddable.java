package com.example.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import javax.annotation.processing.Generated;

/**
 * How Money is stored.
 *
 * <p>Written from the classified model. Anything changed here is lost the next time the sources are read.</p>
 */
@Generated("io.hexaglue.jpa")
@Embeddable
public class MoneyEmbeddable {
    @Column(
            name = "amount"
    )
    private BigDecimal amount;

    @Column(
            name = "currency"
    )
    private String currency;

    /**
     * For the persistence provider.
     */
    protected MoneyEmbeddable() {
    }

    /**
     * Builds a row from what the domain holds.
     * @param amount the stored amount
     * @param currency the stored currency
     */
    public MoneyEmbeddable(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    /**
     * @return the stored amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * @return the stored currency
     */
    public String getCurrency() {
        return currency;
    }
}
