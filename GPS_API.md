# GPS / Location API

Existing Driver / Owner / Booking / Parking endpoints are unchanged. New Parking columns
(`parking_name`, `latitude`, `longitude`) are nullable; with `ddl-auto=update` Hibernate adds them
and old rows keep working (they simply never appear in nearby results until given coordinates).

The user's live position is used only to compute distance. It is not stored and not logged.

## Auth
All endpoints below need `Authorization: Bearer <jwt>` (see SECURITY.md). Roles/ownership:
- `GET /api/parkings/nearby`, `GET /api/parkings/{id}`: any logged-in user.
- `POST /api/parkings`, `PUT /api/parkings/{id}`: role OWNER (or ADMIN). The owner is taken from the JWT's
  linked Owner profile, never from the request body. `PUT` returns **403** if the parking belongs to another owner.
- Missing/invalid token -> **401**; wrong role or not your parking -> **403**.

## POST /api/parkings   (owner; owner comes from auth, never the body)
```json
{ "parkingName": "City Mall Parking", "location": "Bhopal", "latitude": 23.2599,
  "longitude": 77.4126, "totalSlots": 100, "pricePerHour": 20 }
```
201 ->
```json
{ "parkingId": 1, "parkingName": "City Mall Parking", "location": "Bhopal", "latitude": 23.2599,
  "longitude": 77.4126, "totalSlots": 100, "availableSlots": 100, "pricePerHour": 20.0,
  "status": "OPEN", "ownerId": 3, "ownerName": "Arpit" }
```
Errors: 400 bad/half coordinates, 401 no/invalid token, 403 not an owner. The old `POST /api/parkings/add/{ownerId}` still works and also accepts latitude/longitude.

## PUT /api/parkings/{id}   (owner of that parking only)
Partial update, null/omitted = unchanged. Coordinates must be sent as a pair. No `ownerId` field exists.
```json
{ "latitude": 23.2601, "longitude": 77.4130 }
```
200 -> same shape as above. 403 if not the owner, 404 unknown id, 400 invalid coordinates.
Changing `totalSlots` shifts `availableSlots` by the same delta (never below 0).

## GET /api/parkings/{id}
Same response as above, now including `parkingName`, `latitude`, `longitude` (null for legacy rows).
(Id field is still `parkingId`, as before, to avoid breaking existing clients.)

## GET /api/parkings/nearby
| param | required | default | notes |
|---|---|---|---|
| latitude | yes | – | -90..90 |
| longitude | yes | – | -180..180 |
| radius | no | 5 | km, >0 and <=100 |
| availableOnly | no | **false** | `true` => only `availableSlots > 0` |

`GET /api/parkings/nearby?latitude=23.2599&longitude=77.4126&radius=5&availableOnly=true`
```json
{ "success": true, "message": "Nearby parking locations fetched successfully",
  "data": [ { "id": 1, "parkingName": "City Mall Parking", "address": "Bhopal",
              "latitude": 23.2599, "longitude": 77.4126, "distanceKm": 1.4,
              "totalSlots": 100, "availableSlots": 25, "pricePerHour": 20.0, "status": "OPEN" } ] }
```
Sorted nearest first; empty list (not an error) when nothing is in range.
`parkingName` falls back to `location` for legacy rows. Errors: 400 with `{status, message}`.

## Pieces
- `util/DistanceCalculator` – the only Haversine implementation (+ bounding box for the DB pre-filter)
- `service/LocationService` – coordinate validation + nearby search
- `security/CurrentOwnerProvider` – auth seam
- `static/map.html` – Leaflet + OpenStreetMap UI at `/map.html` (no API key needed; Google Maps is only
  used via a plain directions URL built in the browser). If you later add a key, put it in an env var.
- Geocoding (address -> coordinates) was intentionally not added; owners enter coordinates.
