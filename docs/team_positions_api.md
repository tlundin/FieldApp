# Team map needles – API used by the app

The app loads team positions (and stored needle indices) from this endpoint so you can run it manually and inspect what the server returns for `icon`.

## GET team positions (used to draw map needles)

- **URL:** `https://synkserver.net/positions`
- **Method:** `GET`
- **Headers:** none required in the app (plain Volley GET)
- **Query parameters:** none in the app code

### Run with curl

```bash
curl -s -w "\n\nHTTP %{http_code}\n" "https://synkserver.net/positions"
```

Pretty-print JSON (if you have `jq`):

```bash
curl -s "https://synkserver.net/positions" | jq .
```

### Expected response shape

The response must be a **JSON array**. Each element is an object with:

| Field       | Type   | Required | Description |
|------------|--------|----------|-------------|
| `name`     | string | yes      | Display name |
| `uuid`     | string | yes      | User UUID (used to detect "me" and for stable icon id) |
| `timestamp`| number | yes      | Unix time (ms); used for "away" vs "active" default icon |
| `position` | object | yes      | `{ "lat": number, "long": number }` (WGS84 latitude and longitude) |
| `icon`     | number or string | no | Map needle index (0–N). **For "me" the app ignores this and uses the device setting (e.g. 11).** For others, this is what the server last stored (from their device). |

Example:

```json
[
  {
    "name": "t",
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1729680000000,
    "icon": 11,
    "position": { "lat": 59.275, "long": 15.21 }
  },
  {
    "name": "Other",
    "uuid": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "timestamp": 1729680001000,
    "icon": 0,
    "position": { "lat": 59.28, "long": 15.22 }
  }
]
```

When you run the curl command, check:

1. That each entry has an `icon` field if you expect a custom needle.
2. That the entry that corresponds to **you** (your UUID) has the `icon` value you expect (e.g. 11). The app still uses the **device** needle index for "me", but the response shows what the server has stored.
3. Whether there are duplicate `uuid`s or duplicate names that could explain multiple "t" entries with different icons.

---

## POST my position (sends current needle index to server)

The app also **sends** the current user’s position and needle index so the server can store it and return it in `/positions` for other clients.

- **URL:** `https://synkserver.net/position`
- **Method:** `POST`
- **Content-Type:** `application/json; charset=utf-8`
- **Body (JSON):**

```json
{
  "uuid": "<user UUID>",
  "name": "<user name>",
  "timestamp": <Unix ms>,
  "icon": 11,
  "position": { "lat": <number>, "long": <number> }
}
```

Position is WGS84 latitude and longitude. Example curl (replace UUID, name, timestamp, lat, long, and icon as needed):

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST "https://synkserver.net/position" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{"uuid":"YOUR-UUID","name":"t","timestamp":1729680000000,"icon":11,"position":{"lat":59.275,"long":15.21}}'
```

The `icon` value in this POST is `getCurrentUserNeedleIndex()` (e.g. 11). The server should store it and return it in GET `/positions` for that user (other devices will then show that needle for you; your own device always uses the local setting).
