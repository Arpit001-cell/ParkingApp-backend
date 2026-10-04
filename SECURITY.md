# Security: Spring Security + JWT + Google OAuth2

## Design
- New `User` table (`users`) holds login identity: email, BCrypt hash, role, provider (LOCAL/GOOGLE), providerId.
- `Owner` and `Driver` keep their tables; each got ONE new nullable column `user_id` (unique FK to `users`).
  Existing rows are untouched (user_id = NULL). This link is how a JWT maps to "which Owner/Driver am I".
- Roles: `USER`, `DRIVER`, `OWNER`, `ADMIN` -> authorities `ROLE_USER` ... (one `ROLE_` prefix; `hasRole("OWNER")` adds it).
- JWT: HS256 built with Nimbus (already inside Spring Security, so no extra JWT library / no Jackson clash with Boot 4).
  Claims: `sub`=email, `userId`, `role`, `iat`, `exp`, `iss`. No password/secret inside.
  The filter re-loads the user from the DB on each request, so a disabled user or changed role takes effect immediately.
- Stateless; CSRF off (no cookies, token in header). Google login uses a short-lived HTTP session ONLY to carry the
  OAuth `state`; it is invalidated before redirecting.

## Files
Created: `entity/{User,Role,AuthProvider}`, `repository/UserRepository`, `dto/{RegisterRequest,LoginRequest,AuthResponse,UserResponse,OwnerProfileRequest,DriverProfileRequest}`,
`security/{AppUserDetails,CustomUserDetailsService,JwtService,JwtAuthenticationFilter,RestSecurityHandlers,AccessGuard,OAuth2AuthenticationSuccessHandler,OAuth2AuthenticationFailureHandler}`,
`config/{SecurityConfig,AdminBootstrap}`, `service/AuthService`, `controller/AuthController`,
`exception/{ConflictException,BadRequestException,OAuth2LoginException}`, `static/login.html`, `test/.../JwtServiceTest`.

Modified: `pom.xml` (3 deps + test dep), `application.properties`, `CorsConfig` (now a CorsConfigurationSource bean, no `*`),
`GlobalExceptionHandler` (401/403/409/400 JSON; AccessDenied -> 403 instead of falling into the RuntimeException->400 catch-all),
`Owner`/`Driver` (+user link), `Owner/Driver repositories` (+findByUser_Id), `Parking/Owner/Driver/Booking controllers + Parking/Booking services` (ownership checks).

## Access rules
| Endpoint | Who |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login`, `/oauth2/**`, `/login/oauth2/**`, `/login.html`, `/map.html` | public |
| Legacy `POST /api/drivers/register|login`, `POST /api/owners/register|login` | public (kept for compatibility) |
| `GET /api/auth/me`, `POST /api/auth/profile/owner|driver` | any logged-in user |
| `POST /api/parkings`, `/api/parkings/add/{ownerId}`, `PUT/DELETE /api/parkings/**` | OWNER (own parkings only) or ADMIN |
| `GET /api/parkings/**` (list, nearby, search, details...) | any logged-in user |
| `GET /api/parkings/my-parkings/{ownerId}`, `/api/owners/{id}` (GET/PUT/DELETE), `/api/bookings/owner/{ownerId}` | that owner or ADMIN |
| `/api/drivers/{id}` (GET/PUT/DELETE), `/api/bookings/driver/{driverId}` | that driver or ADMIN |
| `POST /api/bookings/book` | DRIVER or ADMIN; `driverId` in the body is ignored and replaced by the caller's own |
| `GET /api/bookings/{id}`, `PUT /api/bookings/cancel/{id}` | the booking's driver, the parking's owner, or ADMIN |
| `GET /api/owners/phone/{phone}`, `GET /api/drivers/vehicle/{v}` | ADMIN only |

## Changed / deprecated endpoints (read this)
- Everything except the public list above now returns **401 without a token**. This is the intended behaviour change.
- `POST /api/owners/login` and `/api/drivers/login` are **deprecated**: they still return the profile but issue no token and grant no access. Use `/api/auth/login`.
- `POST /api/owners/register` and `/api/drivers/register` still work but create profiles with **no login attached**; only an ADMIN can access those.
  Use `POST /api/auth/register` with `"role":"OWNER"` (+`phone`) or `"role":"DRIVER"` (+`phone`,`vehicleNumber`) to create login + profile together.
- Google sign-ups start as `USER`. They call `POST /api/auth/profile/owner` or `/profile/driver` once, which creates the profile, changes the role and returns a fresh token.
- Existing, previously-registered owners/drivers have no login. To migrate one, register an account and (manually, in SQL) set `owners.user_id` / `drivers.user_id` to the new `users.id`.
- One account has one role (OWNER or DRIVER, not both).
- A Google login whose email already exists as a **password** account is refused (`account_exists_use_password_login`) instead of silently merged, to prevent account pre-hijacking.

## Environment variables
| Var | Required | Meaning |
|---|---|---|
| `JWT_SECRET` | yes | >= 32 chars. `openssl rand -base64 48`. App refuses to start without it. |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | for Google login | from Google Cloud |
| `FRONTEND_URL` | no (`http://localhost:3000`) | CORS origin(s), comma separated |
| `OAUTH2_REDIRECT_URI` | no (`$FRONTEND_URL/oauth2/redirect`) | where the browser lands after Google. To use the bundled page: `http://localhost:8089/login.html` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | no | creates the first ADMIN at startup |

Linux/macOS: `export JWT_SECRET=...` before `./mvnw spring-boot:run`. Windows PowerShell: `$env:JWT_SECRET="..."`.
In Eclipse/STS: Run Configurations -> Environment tab.

## Google Cloud setup
1. https://console.cloud.google.com -> create/select a project.
2. APIs & Services -> OAuth consent screen: User type External, fill app name + email, add scopes `openid`, `email`, `profile`; while in Testing add your Gmail as a test user.
3. Credentials -> Create credentials -> OAuth client ID -> Web application.
4. Authorized redirect URI: `http://localhost:8089/login/oauth2/code/google` (Spring's default path `/login/oauth2/code/{registrationId}`).
5. Copy Client ID / Secret into `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`. Never commit them.

## Postman sequence (base `http://localhost:8089`)
1. Register owner: `POST /api/auth/register` body
   `{"name":"Arpit","email":"arpit@example.com","password":"StrongPass123","role":"OWNER","phone":"9876543210"}` -> 201, copy `token`, note `user.ownerId`.
2. Login: `POST /api/auth/login` `{"email":"arpit@example.com","password":"StrongPass123"}` -> 200 + token. Wrong password -> 401.
3. In Postman: Authorization tab -> Bearer Token -> paste token (header `Authorization: Bearer <token>`).
4. Protected call: `GET /api/auth/me` -> 200. Create a parking: `POST /api/parkings` `{"parkingName":"City Mall","location":"Bhopal","latitude":23.2599,"longitude":77.4126,"totalSlots":50,"pricePerHour":20}` -> 201.
5. No token: `GET /api/parkings` with no header -> **401** `{"success":false,"message":"Authentication required"}`.
6. 403: register a second account with role `DRIVER` (`phone`, `vehicleNumber`), use its token on `POST /api/parkings` -> **403**; or `PUT /api/parkings/{id}` with another owner's token -> 403; or `GET /api/drivers/vehicle/MP04AB1234` as non-admin -> 403.
7. Driver books: `POST /api/bookings/book` `{"parkingId":1,"startTime":"10:00:00","durationHours":2}` with the DRIVER token.
8. Nearby: `GET /api/parkings/nearby?latitude=23.2599&longitude=77.4126&radius=5&availableOnly=true`.
9. Google: open `http://localhost:8089/login.html` in a browser -> "Continue with Google" -> browser goes to `/oauth2/authorization/google` -> Google login + consent -> `/login/oauth2/code/google` -> app creates/finds the user -> redirect to `OAUTH2_REDIRECT_URI#token=<jwt>` -> `login.html` stores it and opens `/map.html`.

## Frontend guidance
- After login/register, store `token` and send `Authorization: Bearer <token>` on every API call. On any 401, clear it and show login.
- localStorage is simple but readable by any XSS; keep the frontend free of untrusted HTML. (A cookie-based design would need CSRF protection, so it was not used.)
- "Continue with Google": navigate (full page, not fetch/XHR) to `http://localhost:8089/oauth2/authorization/google`. On your redirect page read `location.hash` (`#token=...` or `#error=...`), save the token, then `history.replaceState` to remove it from the URL.
- Use `GET /api/auth/me` after login to get `ownerId` / `driverId` for the existing path-based endpoints.

## Not verified
This was written without Maven/Internet access, so it has not been compiled or run. Run `mvn clean test` and start the app,
then walk the Postman sequence above.
