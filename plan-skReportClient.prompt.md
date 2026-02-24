# Plan: Create SkReportClient for Slovak Avalanche Reports

The SK avalanche page (https://avalanches.sk/bulletin/latest) contains **multiple regional reports**, each with **two avalanche levels** (above/below a certain elevation boundary) and **avalanche problem types** — fundamentally different from the single-report TOPR client. The elevation boundary is not always treeline — it can be e.g. "1700m" or another altitude depending on conditions. This requires either extending the existing `AvalancheReportClient` interface or creating a new multi-region-aware interface, plus adapting the data model.

## Steps

### 1. Extend `AvalancheReportClient` interface

File: `src/main/java/walaniam/avalanches/client/AvalancheReportClient.java`

Add a `List<ReportFetchResult> fetchAll(ExecutionContext)` method with a default implementation that wraps the existing single `fetch()` so the SK client can return one `ReportFetchResult` per region, while `ToprReportClient` remains backward-compatible.

### 2. Extend `AvalancheReport` and `ReportId` models

File: `src/main/java/walaniam/avalanches/persistence/AvalancheReport.java`

Add the following fields:
- `String region` — the region name (e.g. "Západné Tatry")
- `int avalancheLevelLow` — avalanche level below the elevation boundary; the existing `avalancheLevel` always holds the **higher** of the two values (consistent with how TOPR uses it — TOPR always sets `avalancheLevel` to the higher value)
- `String elevationBoundary` — label for the elevation boundary (e.g. "Treeline", "1700m"); nullable for TOPR reports which have a single level
- `List<String> avalancheProblems` — problem types (e.g. "New snow", "Persistent weak layers")

File: `src/main/java/walaniam/avalanches/persistence/ReportId.java`

Add `String region` field so each region's report has a unique identity.

### 3. Handle SK date format in `DateTimeUtils`

File: `src/main/java/walaniam/avalanches/common/time/DateTimeUtils.java`

The SK page may use a different date format than TOPR's `yyyy-MM-dd HH:mm`. Add an overloaded `parseDate` method that accepts a custom `DateTimeFormatter` pattern, to be used by `SkReportClient`. The SK timezone should be `Europe/Bratislava`.

### 4. Create `SkReportClient`

New file: `src/main/java/walaniam/avalanches/client/SkReportClient.java`

Implement `AvalancheReportClient`, using HtmlUnit to fetch https://avalanches.sk/bulletin/latest. Create an inner `PageWrapper` class (following the TOPR pattern) that:

- Finds all `div.bulletin-map-details` elements on the page.
- For each region div:
  - Extracts the region name from `span.bulletin-report-region-name-region`.
  - Parses the two avalanche levels from `<text>` elements inside the SVG (the first `<text>` is the below-boundary level, the second is the above-boundary level). The **higher** of the two values is stored in `avalancheLevel` (consistent with TOPR convention), the lower-elevation value in `avalancheLevelLow`.
  - Extracts the elevation boundary label from the `<span>` adjacent to the SVG (e.g. "Treeline", "1700m") and stores it in `elevationBoundary`.
  - Collects avalanche problem captions from `div.picto-caption` elements.
  - Extracts report/validity dates from the page header (the SK page likely has a shared publication/validity date).
- Builds one `AvalancheReport` and `ReportFetchResult` per region, with `reportedBy = "HZS"`.

The HTML structure for each region looks like:

```html
<div style="z-index: 1000;" class="bulletin-map-details top-right js-active">
  <ul class="list-plain">
    <li class="bulletin-report-picto">
      <a tabindex="-1" href="/education/danger-scale?lang=en">
        <svg ...>
          <!-- SVG with avalanche level triangles -->
          <text ...>2</text>  <!-- below-boundary level → avalancheLevelLow -->
          <text ...>3</text>  <!-- above-boundary level → avalancheLevel (always the higher value) -->
        </svg>
        <span>Treeline</span>  <!-- elevation boundary label; can also be e.g. "1700m" → elevationBoundary -->
      </a>
    </li>
    <li>
      <div class="bulletin-report-picto avalanche-situation">
        <a class="img" href="/education/avalanche-problems#new_snow">
          <div class="picto-caption">New snow</div>
        </a>
      </div>
    </li>
    <!-- more avalanche problems... -->
  </ul>
  <p class="bulletin-report-region-name">
    <span class="bulletin-report-region-name-region">Západné Tatry</span>
  </p>
</div>
```

Additionally, download the PDF bulletin from `https://static.laviny.sk/bulletins/{date}/{date}_SK_en.pdf` where `{date}` is the report date formatted as `yyyy-MM-dd` (e.g. `https://static.laviny.sk/bulletins/2026-02-23/2026-02-23_SK_en.pdf`). The PDF is shared across all regions, so it should be fetched once and attached as the same `BinaryReport` to every `ReportFetchResult`.

Override `fetchAll(ExecutionContext)` to return a `List<ReportFetchResult>`, one per region. The single `fetch(ExecutionContext)` method can throw `UnsupportedOperationException` or return the first region's result.

### 5. Create `SkReportClientIT` integration test

New file: `src/test/java/walaniam/avalanches/client/SkReportClientIT.java`

Mirror the structure of `ToprReportClientIT.java`, asserting that `fetchAll()` returns multiple `ReportFetchResult` entries, each with:
- Valid region name (non-blank)
- `avalancheLevel` (higher value) in range 1–5
- `avalancheLevelLow` (lower-elevation value) in range 1–5, less than or equal to `avalancheLevel`
- Non-blank `elevationBoundary` (e.g. "Treeline", "1700m")
- Non-empty `avalancheProblems` list
- Non-null dates
- `reportedBy` equal to `"HZS"`
- `binaryReport` is non-null, with non-empty `bytes` and `contentType` of `"application/pdf"`; all regions share the same `BinaryReport` instance

## Further Considerations

1. **Single vs. multi-report interface**: The current `AvalancheReportClient.fetch()` returns one `ReportFetchResult`. Adding a default `fetchAll()` returning a `List<ReportFetchResult>` to the existing interface keeps things simpler and backward-compatible — the default wraps `fetch()` in a singleton list.

2. **Model extension vs. new model**: Adding `region`, `avalancheLevelLow`, `elevationBoundary`, and `avalancheProblems` to the existing `AvalancheReport` makes fields optional/unused for TOPR reports. An alternative is creating a separate `SkAvalancheReport` subclass — but this complicates the shared persistence layer. Extending the existing model with nullable fields is recommended.

3. **Future `ToprReportClient` extension** *(not in scope now)*: Once the new model fields are in place, `ToprReportClient` could be extended to also parse and populate `avalancheLevelLow`, `elevationBoundary`, and `avalancheProblems` from the TOPR page. This is not part of the current implementation — the new fields should simply remain null/empty for TOPR reports for now.

4. **Shared PDF for SK**: The SK PDF bulletin is available at `https://static.laviny.sk/bulletins/{date}/{date}_SK_en.pdf` (date formatted as `yyyy-MM-dd`). Since this PDF covers all regions, it should be downloaded once per `fetchAll()` invocation and the same `BinaryReport` instance attached to every `ReportFetchResult`. If the PDF download fails (e.g. 404 for days without a bulletin), `binaryReport` should be set to null and the failure logged as a warning — it should not prevent the avalanche level data from being returned.

