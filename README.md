# SafePlate: Smart Online Food Ordering System with Secure Digital Food Profile

Spring Boot 3 modular monolith (Java 17, Spring Data JPA, Spring Security, Thymeleaf, Bootstrap 5, MySQL 8).

## Run

1. Install JDK 17+, Maven 3.9+ and MySQL 8.
2. Set the database credentials (defaults in `application.yml`):
   `DB_HOST`, `DB_PORT`, `DB_NAME` (default `smart_food`, created automatically), `DB_USER`, `DB_PASSWORD`.
3. `mvn spring-boot:run` then open http://localhost:8080
4. Tests (use an in-memory H2 database): `mvn test`

On an empty database the app seeds roles, allergens, ingredients, restaurants and demo accounts
(disable with `app.seed.enabled=false`). `ExtraRestaurantSeeder` adds six more demo restaurants (36 dishes) on startup,
skipping any that already exist by name, so it also works on a database you created earlier:

| Role | Email | Password |
|---|---|---|
| Administrator | admin@smartfood.local | `Admin@12345` (override with `ADMIN_PASSWORD`) |
| Restaurant manager | manager@smartfood.local | `Demo@12345` |
| Kitchen staff | kitchen@smartfood.local | `Demo@12345` |
| Customer (peanut allergy, lactose-free) | customer@smartfood.local | `Demo@12345` |

Change or remove the demo accounts before any real deployment.

## How the safety rules work

| Warning | Meaning | Effect |
|---|---|---|
| KNOWN_CONFLICT | item declares an allergen on the customer's allergy list | cannot be added to the cart or ordered |
| POSSIBLE_CONFLICT | cross-contact declared, or an ingredient commonly carries the allergen | must be acknowledged at checkout |
| DIETARY_MISMATCH | ingredient breaks a diet, or is on the avoid list | must be acknowledged |
| INFORMATION_INCOMPLETE | restaurant has not confirmed ingredient/allergen data | must be acknowledged |

Checks run on the server at add-to-cart and again at checkout. Warnings, item allergens, prices and a profile summary are
stored as snapshots with each order.

## Security notes

- BCrypt (cost 12), session login with fixation protection, CSRF on, lock for 15 minutes after 5 failed logins.
- URL rules plus `@PreAuthorize` on controllers. All customer data is looked up by the logged-in user id, never by an id from the browser.
- Restaurant staff can only see and change their own restaurant's menu and orders. Kitchen staff see allergen warnings but not the customer's full profile.
- Audit log for logins, profile changes, orders, status changes and denied access.
- Payments use a sandbox simulator behind the `PaymentGateway` interface. No real money moves.
