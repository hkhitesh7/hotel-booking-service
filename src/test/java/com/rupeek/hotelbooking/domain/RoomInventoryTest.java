package com.rupeek.hotelbooking.domain;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.inventory.InventoryKey;
import com.rupeek.hotelbooking.domain.inventory.InventoryUnavailableException;
import com.rupeek.hotelbooking.domain.inventory.RoomInventory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomInventoryTest {

    private static final LocalDate D = LocalDate.of(2026, 12, 10);

    private final RoomInventory inventory = new RoomInventory(new InventoryKey("p", "rt"), 2);

    private static DateRange nights(int fromOffset, int toOffset) {
        return new DateRange(D.plusDays(fromOffset), D.plusDays(toOffset));
    }

    @Test
    void availabilityIsDecidedByTheBusiestNight() {
        inventory.reserve(nights(1, 2), 2);
        assertThat(inventory.availableFor(nights(0, 1))).isEqualTo(2);
        assertThat(inventory.availableFor(nights(0, 3))).isZero();
    }

    @Test
    void preventsDoubleBookingOfOverlappingDates() {
        inventory.reserve(nights(0, 3), 2);
        assertThatThrownBy(() -> inventory.reserve(nights(2, 4), 1))
                .isInstanceOf(InventoryUnavailableException.class);
    }

    @Test
    void failedReservationLeavesLedgerUntouched() {
        inventory.reserve(nights(2, 3), 2);
        assertThatThrownBy(() -> inventory.reserve(nights(0, 3), 1))
                .isInstanceOf(InventoryUnavailableException.class);
        assertThat(inventory.reservedOn(D)).isZero();
    }

    @Test
    void backToBackStaysShareARoom() {
        RoomInventory single = new RoomInventory(new InventoryKey("p", "rt"), 1);
        single.reserve(nights(0, 2), 1);
        single.reserve(nights(2, 4), 1);
        assertThat(single.availableFor(nights(0, 4))).isZero();
    }

    @Test
    void releaseMakesRoomsAvailableAgain() {
        inventory.reserve(nights(0, 2), 2);
        inventory.release(nights(0, 2), 2);
        assertThat(inventory.availableFor(nights(0, 2))).isEqualTo(2);
    }

    @Test
    void cannotReleaseMoreThanReserved() {
        inventory.reserve(nights(0, 1), 1);
        assertThatThrownBy(() -> inventory.release(nights(0, 2), 1)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.reservedOn(D)).isEqualTo(1);
    }
}
