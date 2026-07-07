# fuel-event

Detects fuel **refuel** and **theft** events from raw, continuous fuel-sensor
readings, uploaded as an Excel file.

## What it does

Each sheet in the uploaded Excel file must contain a single column of raw,
continuous sensor readings belonging to **one device/vehicle**. For each
sheet, the service returns an ordered list of detected events:

- **negative value** → fuel stolen (level dropped and stayed down)
- **positive value** → fuel added (level rose and stayed up)

Example response:

```json
{
  "events": {
    "DUT standard": [494.0, 497.0, 412.0],
    "FL standard": [18.0, 23.0, 15.0, 19.0, 26.0, 17.0, 17.0],
    "FL Steal": [-6.0, 11.0, 35.0],
    "Volvo standard": []
  }
}
```

## API

### `POST /fuel-event/api/fuel-events`

| Param | Type | Required | Notes |
|---|---|---|---|
| `file` | multipart file (.xlsx) | yes | Each sheet = one device's continuous readings |
| `threshold` | number | no | Noise threshold override (raw sensor units). Must be `> 0`. Defaults to `fuel-event.detection.default-threshold` |

**Responses**

| Status | When |
|---|---|
| `200` | Returns `{ "events": { sheetName: [deltas...] } }` |
| `400` | Empty/missing file, no sheets, no numeric data, or non-positive `threshold` |
| `500` | Unexpected server error |

Swagger UI (port/context-path from `application.yml`):
`http://localhost:8082/fuel-event/swagger-ui/index.html`

## Configuration (`application.yml`)

```yaml
server:
  port: 8082
  servlet:
    context-path: /fuel-event

spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

fuel-event:
  detection:
    default-threshold: 5.0    # used when the caller doesn't pass ?threshold=
    hold-samples: 3           # samples checked to confirm an event doesn't revert
    group-gap: 3              # max gap between jumps still treated as one ramp
    max-ramp-extension: 10    # max samples followed to settle a gradual ramp
```

`default-threshold` is a business-meaningful value that can vary a lot by
sensor type (see limitations below) — it's exposed per-request. The other
three are internal algorithm tuning constants and are **not** meant to be
set per-request by clients.

## Project structure

```
com.geekbro.fuelevent.detector     FuelEventDetector — pure algorithm, no Spring dependency
com.geekbro.fuelevent.service      FuelEventService — reads the Excel file (Apache POI), validates it
com.geekbro.fuelevent.controller   FuelEventController — REST endpoint
com.geekbro.fuelevent.dto          FuelEventResponse — API response shape
com.geekbro.fuelevent.config       FuelEventDetectionProperties — binds fuel-event.detection.* from YAML
com.geekbro.fuelevent.exception    GlobalExceptionHandler — centralized error handling (@RestControllerAdvice)
```

`FuelEventDetector` is intentionally stateless and framework-free — the
threshold is passed per call, not fixed at construction — so it can be
unit-tested directly without a Spring context.

## Known limitations

- **No timestamp** → can't distinguish a fast drop (likely theft) from slow
  normal consumption by rate; relies on "settles at a new level" instead.
- **No calibration table** → raw sensor units are reported, not liters
  (tank shape is non-linear in real hardware).
- **Threshold is sensor-dependent** — a single global default won't fit
  every sensor type well (e.g. DUT sensors need a higher threshold than FL
  sensors to avoid false positives from noise). Pass `threshold` explicitly
  per sheet/sensor type when known.
- **Large files** are read fully into memory via `XSSFWorkbook`. Fine for
  files in the tens of MB range; a SAX-based streaming reader would be
  needed for much larger files.

## Running tests

```
mvn test
```

`FuelEventDetectorTest` covers: glitch filtering, simple theft/refuel,
gradual ramp settling, multiple mixed events in one stream, no-event case,
and null/too-short input.
