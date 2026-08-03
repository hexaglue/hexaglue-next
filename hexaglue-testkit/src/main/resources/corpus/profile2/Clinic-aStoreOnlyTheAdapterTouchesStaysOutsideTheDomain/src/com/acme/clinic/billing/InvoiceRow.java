package com.acme.clinic.billing;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class InvoiceRow {

    @Id
    private Long id;

    private String reference;

    private long amountInCents;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public long getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(long amountInCents) {
        this.amountInCents = amountInCents;
    }
}
