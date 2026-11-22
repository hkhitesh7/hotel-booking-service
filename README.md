# Hotel Booking Service

Backend for a hotel booking platform: discover properties, onboard owners and properties,
book a room type for a date range, pay, and cancel. Built with Java 17+ / Spring Boot 3.3 and
in-memory persistence kept behind repository interfaces.

## Build & run

```bash
mvn test                                  # 70 unit, service, concurrency and HTTP tests
mvn spring-boot:run                       # or: mvn package && java -jar target/hotel-booking-1.0.0.jar
```

* `http://localhost:8080/` redirects to the **Swagger UI**, where every endpoint can be tried
  from the browser with "Try it out". The UI is also at `/swagger-ui.html`, and the OpenAPI
  JSON is at `/v3/api-docs`.
* API: `http://localhost:8080/api/v1/...`
* Demo data is seeded on startup (`app.seed-demo-data=true`): a 2-property **chain** and a
  1-property **individual** owner in Bangalore. Their ids are printed in the startup log.

Requires a full JDK 17+ (a JRE-only install can't compile).

> **Browser tip:** a browser's address bar only sends `GET` requests. Search and the other `GET`
> endpoints open fine there. Booking, payment and cancellation are `POST`s, so use Swagger's
> "Try it out" or curl for those.

### Try it

```bash
H=http://localhost:8080/api/v1
curl "$H/properties/search?city=Bangalore&checkIn=2026-12-10&checkOut=2026-12-12&guests=2&amenities=WIFI&minStars=3&maxPrice=4000"

curl -X POST $H/bookings -H 'Content-Type: application/json' -d '{
  "guestId":"g1","propertyId":"<propertyId>","roomTypeId":"<roomTypeId>",
  "checkIn":"2026-12-10","checkOut":"2026-12-12","guests":2}'

curl -X POST $H/bookings/<bookingId>/payments -H 'Idempotency-Key: 7f3c...' \
     -H 'Content-Type: application/json' -d '{"method":"UPI","details":{"vpa":"guest@okbank"}}'

curl -X POST $H/bookings/<bookingId>/cancellation
```

| Operation | Endpoint (relative to `/api/v1` unless noted) |
|---|---|
| Onboard owner (+ properties) | `POST /owners` |
| Add property to owner | `POST /owners/{ownerId}/properties` |
| Get owner / property | `GET /owners/{id}`, `GET /properties/{id}` |
| Search | `GET /properties/search?city&locality&checkIn&checkOut&guests&minPrice&maxPrice&amenities&minStars` |
| Book (hold) | `POST /bookings` |
| Get booking | `GET /bookings/{id}` |
| Pay | `POST /bookings/{id}/payments` (header `Idempotency-Key` required) |
| Cancel | `POST /bookings/{id}/cancellation` |
| API docs | `GET /` at the server root → redirects to `/swagger-ui.html` |

Payment details by method: `CARD {cardNumber}` (checked with Luhn), `UPI {vpa}`,
`WALLET {provider, walletId}`. The mock gateway **declines** card numbers ending in `0002`
(e.g. `4000000000000002`) and any VPA or wallet id containing `decline`.

Errors are returned as RFC 7807 `ProblemDetail`: 400 for validation, 404 for unknown ids,
409 for conflicts (no availability, invalid state transition, idempotency-key misuse), and
502 for gateway failures.

## Package layout

```
domain/            framework-free model, no Spring imports
  common/          Money, DateRange (value objects), exception hierarchy, Guard
  property/        PropertyOwner (portfolio), Property, RoomType, Location, StarRating, Amenity, repos
  inventory/       RoomInventory (per-night ledger), InventoryKey, repo
  booking/         Booking aggregate + BookingStatus state machine, repo
  payment/         Payment, PaymentProcessor (strategy), PaymentGateway (port), repo
  cancellation/    CancellationPolicy (strategy), TieredCancellationPolicy, NonRefundable
  pricing/         PricingStrategy (decorators), PriceCalculator
  search/          SearchCriteria, SearchFilter (specification) + filters
application/       use-case services: onboarding, search, booking, payment, cancellation, inventory
infrastructure/    adapters: in-memory repos, mock gateway + card/UPI/wallet processors, hold-expiry job
api/               REST controllers, DTOs, filter assembly, exception mapping
config/            Spring wiring of domain strategies, OpenAPI, demo seeder
```

Dependencies point inward: `api`/`infrastructure` → `application` → `domain`.

## Key design decisions

* **Single vs multi-property.** There is one structure: a `PropertyOwner` holds a portfolio of
  properties. A standalone hotel is a portfolio of one, onboarded through the same
  `onboardOwner` / `addProperty` path. `OwnerType` is descriptive only and does not branch any logic.
* **Inventory = room type × night.** `RoomInventory` keeps a reserved-count per night. A stay can
  be booked only if *every* night has capacity, so the busiest night decides. Stays are
  `[checkIn, checkOut)`, which means back-to-back stays don't conflict.
* **Double-booking and concurrency.** `InventoryService` does check-and-reserve atomically under a
  striped lock keyed by `InventoryKey`, so different room types still book in parallel. A
  separate shared `bookingLocks` serialises pay, cancel and expire on the same booking. The lock
  order is always booking → inventory, so they can't deadlock. Search reads without locks; at
  worst it sees a slightly stale count, and booking re-checks under the lock. Tests race 32
  threads for 3 rooms and assert exactly 3 bookings succeed.
* **Booking lifecycle.** `PENDING_PAYMENT → CONFIRMED → CANCELLED`, `PENDING_PAYMENT → CANCELLED`,
  `PENDING_PAYMENT → EXPIRED`. Every transition goes through `Booking`, which rejects illegal
  moves with `InvalidStateTransitionException` (409). A new booking is a **time-boxed hold**
  (`booking.hold-duration`, default 15 min). A scheduled job expires unpaid holds and releases
  their rooms, and a payment attempt on a lapsed hold expires it on the spot.
* **The payment outcome drives booking state.** Success confirms the booking. A decline records a
  `FAILED` payment and leaves the booking pending, so the guest can retry while the hold lasts.
* **Pluggable payment methods.** `PaymentMethod` is an open value object, not an enum. Adding a
  method means writing one `@Component` that implements `PaymentProcessor` (or extends
  `GatewayPaymentProcessor`). `PaymentProcessorRegistry` discovers it, and nothing else changes.
  The external provider sits behind the `PaymentGateway` port, and `MockPaymentGateway` implements it.
* **Idempotent payments.** `Idempotency-Key` is required. Repeating a key returns the original
  payment (`replayed: true`) without charging again. Reusing a key on another booking returns 409.
  The repository enforces key uniqueness the way a DB unique constraint would, and the mock
  gateway is idempotent too.
* **Pluggable cancellation policy.** Each property references a policy code (`FLEXIBLE`,
  `MODERATE`, `NON_REFUNDABLE`). `TieredCancellationPolicy` maps "days before check-in" to a refund
  percentage. A new policy is a new `CancellationPolicy` bean. The refund goes back through the
  original payment method, and the rooms are released.
* **Pluggable pricing.** `PricingStrategy` prices one room-night, and strategies compose as
  decorators (`WeekendSurchargePricingStrategy` wraps `BaseRatePricingStrategy`, +20% on Fri/Sat
  nights by default). `PriceCalculator` sums the nights. Seasonal or occupancy-based pricing
  would be another decorator in `DomainConfig`.
* **Extensible search.** The search service runs location → availability → `List<SearchFilter>`
  → sort by cheapest. Filters are specification objects. A new filter is a new class plus one
  line in `SearchFilterAssembler` (the API mapping), and the service itself stays unchanged.
* **Testability.** `Clock` and `IdGenerator` are injected. Service tests wire real services over
  in-memory adapters by hand (`testsupport/Fixture`) with a controllable clock, with no Spring and
  no mocks of our own code. One `@SpringBootTest` drives the full journey over HTTP.

## Assumptions

* Single currency (INR). `Money` is currency-aware, so adding currencies is straightforward.
* Guests are identified by an opaque `guestId`, since authentication is out of scope.
* One booking = one room type and N rooms. If `rooms` is omitted, it is the minimum needed for
  the party (`ceil(guests / maxOccupancy)`).
* Search shows only room types that can host the whole party within one room type for the dates.
  Mixed room types per booking are out of scope.
* Check-in can't be in the past, and stays are capped at 30 nights. Cancellation is allowed up to
  and including the check-in date. Same-day cancellation falls in the lowest refund tier.
* Dates use the server clock's zone. A real system would use each property's time zone.
* Properties are immutable after onboarding (no edit or deactivate endpoints).

## What I'd do with more time

* Real persistence (JPA/Postgres): inventory rows per (room type, night) with optimistic locking
  or `SELECT … FOR UPDATE`, a unique index on the idempotency key, and transactions in place of
  in-process locks. That is needed to run more than one instance.
* An outbox or saga for the payment/refund ↔ booking steps, so a crash between the gateway call
  and the state save can be reconciled. Also webhook handling for async payment methods (UPI collect).
* Search: index by city/locality, pagination, sorting options, and caching availability.
* Owner-facing APIs: edit properties, change room counts (with checks against existing bookings),
  blackout dates, and per-date rate overrides.
* Partial cancellation (fewer rooms or nights), modifications, and guest booking history.
* Observability: metrics on holds, expiries, declines, and lock contention.
