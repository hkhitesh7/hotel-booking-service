package com.rupeek.hotelbooking.domain.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/** Immutable monetary amount. All arithmetic is currency-checked and kept at the currency's scale. */
public final class Money implements Comparable<Money> {

    public static final Currency INR = Currency.getInstance("INR");

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        Guard.requireNonNull(amount, "amount");
        Guard.requireNonNull(currency, "currency");
        Guard.require(amount.signum() >= 0, "money amount must not be negative");
        this.currency = currency;
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money inr(String amount) {
        return new Money(new BigDecimal(amount), INR);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        assertSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        assertSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money times(long multiplier) {
        return new Money(amount.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    public Money percentage(int percent) {
        BigDecimal factor = BigDecimal.valueOf(percent).movePointLeft(2);
        return new Money(amount.multiply(factor), currency);
    }

    public Money dividedBy(long divisor) {
        Guard.require(divisor > 0, "divisor must be positive");
        return new Money(amount.divide(BigDecimal.valueOf(divisor), currency.getDefaultFractionDigits(),
                RoundingMode.HALF_UP), currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    @Override
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void assertSameCurrency(Money other) {
        Guard.require(currency.equals(other.currency),
                "currency mismatch: " + currency + " vs " + other.currency);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money other && amount.equals(other.amount) && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }
}
