package com.rupeek.hotelbooking.domain;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.InvalidRequestException;
import com.rupeek.hotelbooking.domain.common.Money;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    private static final LocalDate D1 = LocalDate.of(2026, 12, 10);

    @Nested
    class DateRangeRules {

        @Test
        void countsNightsExcludingCheckOutDay() {
            assertThat(new DateRange(D1, D1.plusDays(3)).nights()).isEqualTo(3);
            assertThat(new DateRange(D1, D1.plusDays(3)).nightsStream()).containsExactly(D1, D1.plusDays(1),
                    D1.plusDays(2));
        }

        @Test
        void rejectsEmptyOrInvertedRanges() {
            assertThatThrownBy(() -> new DateRange(D1, D1)).isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> new DateRange(D1, D1.minusDays(1))).isInstanceOf(InvalidRequestException.class);
        }

        @Test
        void rejectsOverlyLongStays() {
            assertThatThrownBy(() -> new DateRange(D1, D1.plusDays(DateRange.MAX_NIGHTS + 1)))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        void backToBackStaysDoNotOverlap() {
            DateRange first = new DateRange(D1, D1.plusDays(2));
            assertThat(first.overlaps(new DateRange(D1.plusDays(2), D1.plusDays(4)))).isFalse();
            assertThat(first.overlaps(new DateRange(D1.plusDays(1), D1.plusDays(4)))).isTrue();
        }
    }

    @Nested
    class MoneyRules {

        @Test
        void arithmeticKeepsCurrencyScale() {
            Money price = Money.inr("1000");
            assertThat(price.times(3).percentage(50)).isEqualTo(Money.inr("1500.00"));
            assertThat(Money.inr("100").dividedBy(3)).isEqualTo(Money.inr("33.33"));
        }

        @Test
        void rejectsNegativeAmountsAndCurrencyMismatch() {
            assertThatThrownBy(() -> Money.inr("-1")).isInstanceOf(InvalidRequestException.class);
            Money usd = Money.zero(Currency.getInstance("USD"));
            assertThatThrownBy(() -> Money.inr("1").plus(usd)).isInstanceOf(InvalidRequestException.class);
        }
    }
}
