package com.rupeek.hotelbooking.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Drives the full guest journey over HTTP: onboard -> search -> book -> pay -> cancel. */
@SpringBootTest(properties = "app.seed-demo-data=false")
@AutoConfigureMockMvc
class BookingFlowIntegrationTest {

    private static final String CITY = "Mysore";

    @Autowired
    private MockMvc mvc;

    private final LocalDate checkIn = LocalDate.now().plusDays(10);
    private final LocalDate checkOut = checkIn.plusDays(2);
    private String locality;
    private String propertyId;
    private String roomTypeId;

    @BeforeEach
    void onboardSingleRoomProperty() throws Exception {
        locality = "Locality-" + System.nanoTime();
        String body = """
                {"name": "Palace Owner", "type": "INDIVIDUAL", "properties": [{
                   "name": "Palace View", "stars": 4, "amenities": ["WIFI", "POOL"],
                   "location": {"city": "%s", "locality": "%s"},
                   "cancellationPolicy": "FLEXIBLE",
                   "roomTypes": [{"name": "Standard", "maxOccupancy": 2, "totalRooms": 1, "nightlyRate": 3000}]
                }]}""".formatted(CITY, locality);
        String owner = postJson("/api/v1/owners", body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        propertyId = JsonPath.read(owner, "$.propertyIds[0]");
        String property = mvc.perform(get("/api/v1/properties/" + propertyId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        roomTypeId = JsonPath.read(property, "$.roomTypes[0].id");
    }

    @Test
    void fullGuestJourney() throws Exception {
        search().andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].offers[0].roomsAvailable").value(1));

        String booking = createBooking().andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn().getResponse().getContentAsString();
        String bookingId = JsonPath.read(booking, "$.id");
        Number total = JsonPath.read(booking, "$.totalAmount.amount");

        createBooking().andExpect(status().isConflict());
        search().andExpect(jsonPath("$", hasSize(0)));

        pay(bookingId, "pay-key-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.replayed").value(false))
                .andExpect(jsonPath("$.booking.status").value("CONFIRMED"));
        pay(bookingId, "pay-key-1").andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true));

        mvc.perform(post("/api/v1/bookings/" + bookingId + "/cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booking.status").value("CANCELLED"))
                // Check-in is 10 days out, so the FLEXIBLE policy refunds in full.
                .andExpect(jsonPath("$.refundAmount.amount").value(total));

        search().andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(post("/api/v1/bookings/" + bookingId + "/cancellation")).andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        postJson("/api/v1/bookings", """
                {"guestId": "g", "propertyId": "%s", "roomTypeId": "%s",
                 "checkIn": "%s", "checkOut": "%s", "guests": 2}""".formatted(propertyId, roomTypeId, checkOut, checkIn))
                .andExpect(status().isBadRequest());
        postJson("/api/v1/bookings", "{\"guestId\": \"\"}").andExpect(status().isBadRequest());
        postJson("/api/v1/bookings/unknown/payments", "{\"method\": \"UPI\"}").andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/bookings/unknown")).andExpect(status().isNotFound());
    }

    @Test
    void rootRedirectsToApiDocs() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui.html"));
    }

    private ResultActions search() throws Exception {
        return mvc.perform(get("/api/v1/properties/search")
                        .param("city", CITY).param("locality", locality)
                        .param("checkIn", checkIn.toString()).param("checkOut", checkOut.toString())
                        .param("guests", "2").param("amenities", "WIFI").param("minStars", "3"))
                .andExpect(status().isOk());
    }

    private ResultActions createBooking() throws Exception {
        return postJson("/api/v1/bookings", """
                {"guestId": "guest-7", "propertyId": "%s", "roomTypeId": "%s",
                 "checkIn": "%s", "checkOut": "%s", "guests": 2}"""
                .formatted(propertyId, roomTypeId, checkIn, checkOut));
    }

    private ResultActions pay(String bookingId, String idempotencyKey) throws Exception {
        return mvc.perform(post("/api/v1/bookings/" + bookingId + "/payments")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\": \"card\", \"details\": {\"cardNumber\": \"4111 1111 1111 1111\"}}"));
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
