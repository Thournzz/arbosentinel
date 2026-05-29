package com.arbosentinel.orange;

// ══════════════════════════════════════════════════════════════════════════════
// ORANGE layer — PAHO Caribbean ETL job
// Source: CMU Delphi Epidata API → https://api.delphi.cmu.edu/epidata/paho_dengue/
//
// LEARNING NOTE — What makes this ETL different from all the others in this project:
//   The CSV-based ETL jobs (Zika, DengAI, WestNile, etc.) READ a file from disk.
//   This ETL job CALLS AN HTTP API. That means:
//   (a) No file path needed — data comes over the network, not from disk
//   (b) We use WebClient to make the HTTP GET request (reactive HTTP client)
//   (c) Jackson deserialises the JSON response into our DelphiApiResponse DTO
//   (d) We call the API once per country — 10 calls total for 10 Caribbean locations
//   (e) Network errors, timeouts, and API rate limits replace CSV parsing errors
//
// LEARNING NOTE — @EventListener(ApplicationReadyEvent.class) @Async pattern:
//   ApplicationReadyEvent fires AFTER the application context is fully initialised,
//   after Flyway migrations run, after all beans are wired.
//   This is the safe moment to run ETL — the DB schema is ready.
//   @Async puts the method on a separate thread from the main startup thread.
//   Without @Async, the ETL job would block the startup sequence — the app wouldn't
//   respond to health checks until the ETL finished.
//   With @Async, startup completes normally on the main thread while ETL runs in
//   the background. Spring Boot's @EnableAsync activates this — check AsyncConfig.
//
// LEARNING NOTE — Idempotency approach (different from other ETL jobs):
//   Other ETL jobs check ingestion_log: "did we already run this job? skip."
//   This job uses a PER-COUNTRY check: countByLocationCode("jm") > 0 → skip Jamaica.
//   This means we can run a PARTIAL load — if Jamaica loaded but Trinidad crashed,
//   the next startup loads Trinidad and skips Jamaica. More granular than checking
//   a single "already ran" flag in ingestion_log.
//   The DB UNIQUE constraint (location_code, epi_week) is the last safety net —
//   even if our check has a race condition, the DB prevents duplicate rows.
//
// LEARNING NOTE — .block() on a reactive WebClient in a non-reactive context:
//   WebClient is Spring's REACTIVE HTTP client — its methods return Mono<T> and Flux<T>
//   which are lazy "promises" that nothing actually calls until someone subscribes.
//   Since this ETL job runs on a regular @Async thread (not in a reactive pipeline),
//   we call .block() to convert the reactive Mono into a plain synchronous value.
//   .block() means: "subscribe to this Mono, wait for the response, return the value."
//   WARNING: Never call .block() on the Spring WebFlux event loop threads (it deadlocks).
//   In an @Async context with a TaskExecutor-managed thread, it is safe.
// ══════════════════════════════════════════════════════════════════════════════

import com.arbosentinel.blue.entity.PahoCaribCase;
import com.arbosentinel.green.dto.DelphiApiResponse;
import com.arbosentinel.purple.PahoCaribCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PahoEtlJob {

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final PahoCaribCaseRepository pahoRepo;

    // LEARNING NOTE — @Qualifier for two beans of the same type:
    //   Spring has TWO WebClient beans in the application context:
    //     - "mlServiceClient"  → points to Python FastAPI on port 8000
    //     - "pahoApiClient"    → points to https://api.delphi.cmu.edu
    //   Without @Qualifier, Spring would throw:
    //     "NoUniqueBeanDefinitionException: expected single matching bean but found 2"
    //   @Qualifier("pahoApiClient") tells Spring: "inject the bean named pahoApiClient".
    //   We use field injection here (not constructor injection) because Lombok's
    //   @RequiredArgsConstructor generates a constructor from final fields only —
    //   adding @Qualifier to a Lombok-generated constructor parameter requires extra config.
    //   Field injection with @Autowired + @Qualifier is the clean workaround.
    @Autowired
    @Qualifier("pahoApiClient")
    private WebClient pahoApiClient;

    // ── Configuration ─────────────────────────────────────────────────────────

    // LEARNING NOTE — Epiweek start default:
    //   PAHO data in the Delphi API typically starts around 2010.
    //   "201001" = year 2010, week 1 (YYYYWW format).
    //   We request all data from 2010 to a future epiweek (202660 = year 2026, week 60).
    //   Week 60 never exists — Delphi returns data up to the most recent available week.
    @Value("${paho.delphi.epiweek.start:201001}")
    private String epiweekStart;

    @Value("${arbosentinel.etl.batch-size:500}")
    private int batchSize;

    // ── Caribbean country codes ────────────────────────────────────────────────

    // LEARNING NOTE — Why a static final List (not a DB table or config file)?
    //   These 10 PAHO Caribbean locations are stable — PAHO has reported on these
    //   same countries for decades. A hardcoded list is appropriate.
    //   If PAHO added a new country to their API, we'd add it here — one line change.
    //   We intentionally exclude mainland Latin America (Colombia, Venezuela) to keep
    //   this focused on the Caribbean island region per Dr. Sandiford's directive.
    private static final List<String> CARIBBEAN_LOCATIONS = List.of(
            "jm",   // Jamaica
            "tt",   // Trinidad and Tobago
            "bb",   // Barbados
            "cu",   // Cuba
            "do",   // Dominican Republic
            "ht",   // Haiti
            "pr",   // Puerto Rico (US territory — PAHO reports it separately)
            "bs",   // Bahamas
            "gy",   // Guyana
            "bz"    // Belize
    );

    // Maps Delphi short codes to full country names — used when building entities
    private static final Map<String, String> LOCATION_NAMES = Map.of(
            "jm", "Jamaica",
            "tt", "Trinidad and Tobago",
            "bb", "Barbados",
            "cu", "Cuba",
            "do", "Dominican Republic",
            "ht", "Haiti",
            "pr", "Puerto Rico",
            "bs", "Bahamas",
            "gy", "Guyana",
            "bz", "Belize"
    );

    // ── Startup trigger ────────────────────────────────────────────────────────

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void loadOnStartup() {
        log.info("PAHO ETL: startup check — scanning Caribbean locations");

        int totalLoaded = 0;

        for (String locationCode : CARIBBEAN_LOCATIONS) {
            // Check if we already have data for this country
            // countByLocationCode returns 0 if no rows exist
            long existingCount = pahoRepo.countByLocationCode(locationCode);

            if (existingCount > 0) {
                // Data already loaded — skip on this startup
                // On next manual refresh, the controller endpoint will
                // fetch only new weeks (incremental update)
                log.info("PAHO ETL: {} already has {} rows — skipping startup load",
                        LOCATION_NAMES.getOrDefault(locationCode, locationCode),
                        existingCount);
                continue;
            }

            // No data for this country — load its full history
            log.info("PAHO ETL: loading full history for {}",
                    LOCATION_NAMES.getOrDefault(locationCode, locationCode));

            try {
                // Build epiweek range: from our configured start to end of current year + buffer
                int currentYear = LocalDate.now().getYear();
                String epiweekEnd = (currentYear + 1) + "60";  // Next year week 60 — safe upper bound
                String epiweekRange = epiweekStart + "-" + epiweekEnd;

                int inserted = fetchAndSaveCountry(locationCode, epiweekRange);
                totalLoaded += inserted;
                log.info("PAHO ETL: {} — inserted {} rows", locationCode, inserted);

                // LEARNING NOTE — Thread.sleep between API calls:
                //   We pause 500ms between each country request to be a respectful API client.
                //   Hitting the Delphi API with 10 rapid sequential requests risks triggering
                //   rate limiting (HTTP 429 Too Many Requests). A 500ms gap is courteous.
                //   This is "ETL throttling" — standard practice for batch API consumption.
                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // Restore interrupt flag — Java best practice
                log.warn("PAHO ETL: interrupted during {}", locationCode);
                break;
            } catch (Exception e) {
                // One country failing should NOT stop the rest
                // Log and continue to the next country
                log.error("PAHO ETL: failed loading {} — {}", locationCode, e.getMessage());
            }
        }

        log.info("PAHO ETL startup complete — total rows inserted: {}", totalLoaded);
    }

    // ── Core fetch-and-save method ─────────────────────────────────────────────

    // LEARNING NOTE — @Transactional here vs on the startup method:
    //   @Transactional wraps the method in a DB transaction: either ALL inserts
    //   for one country succeed, or ALL roll back if something goes wrong mid-batch.
    //   We put it here (on fetchAndSaveCountry) not on loadOnStartup() because:
    //   (a) loadOnStartup processes 10 countries in a loop — we want per-country
    //       transactions, not one giant transaction for all 10 countries at once
    //   (b) If Jamaica's insert fails, we don't want to roll back Trinidad's data
    //   Per-country transactionality = better isolation and easier debugging.
    @Transactional
    public int fetchAndSaveCountry(String locationCode, String epiweekRange) {

        // ── Step 1: Call the Delphi API ────────────────────────────────────────

        // LEARNING NOTE — WebClient URI builder:
        //   .uri(uriBuilder -> uriBuilder.path(...).queryParam(...).build())
        //   is Spring's type-safe way to construct URLs with query parameters.
        //   It handles URL encoding automatically — if locationCode contained special
        //   characters, they'd be encoded as %xx. This is safer than String.format().
        //
        //   The resulting URL looks like:
        //   https://api.delphi.cmu.edu/epidata/paho_dengue/?locations=jm&epiweeks=201001-202760
        //
        // LEARNING NOTE — .retrieve() vs .exchangeToMono():
        //   .retrieve() auto-throws WebClientResponseException for 4xx/5xx responses.
        //   .exchangeToMono() gives full control over the response, including status.
        //   We use .retrieve() because for this API any non-200 is a failure we want to
        //   propagate as an exception to the caller (loadOnStartup handles it with try/catch).
        //
        // LEARNING NOTE — .bodyToMono(DelphiApiResponse.class):
        //   Tells WebClient to parse the response body as JSON into a DelphiApiResponse.
        //   Jackson handles the actual parsing using the @JsonProperty annotations we
        //   defined on DelphiApiResponse.EpidataRecord.
        //
        // LEARNING NOTE — .block(Duration.ofSeconds(60)):
        //   .block() converts the reactive Mono into a blocking call.
        //   Duration.ofSeconds(60) is a TIMEOUT — if the API doesn't respond within
        //   60 seconds, block() throws a RuntimeException. Without a timeout,
        //   a hung API request would hang this thread forever.
        log.debug("PAHO ETL: calling Delphi API for {} with epiweeks {}", locationCode, epiweekRange);

        DelphiApiResponse response;
        try {
            response = pahoApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/epidata/paho_dengue/")
                            .queryParam("locations", locationCode)
                            .queryParam("epiweeks", epiweekRange)
                            .build())
                    .retrieve()
                    .bodyToMono(DelphiApiResponse.class)
                    .block(Duration.ofSeconds(60));

        } catch (WebClientResponseException e) {
            // LEARNING NOTE — WebClientResponseException:
            //   Thrown when the server returns an HTTP error status (4xx or 5xx).
            //   e.getStatusCode() gives the HTTP status, e.getResponseBodyAsString()
            //   gives the raw response body — useful for diagnosing 429 rate limits
            //   or 400 Bad Request (e.g. wrong epiweek format).
            log.error("PAHO API returned {} for {}: {}",
                    e.getStatusCode(), locationCode, e.getResponseBodyAsString());
            throw e;
        }

        // ── Step 2: Validate the response ─────────────────────────────────────

        // Delphi API result codes: 1 = data found, -1 = no data for this query
        if (response == null || response.result == null || response.result != 1) {
            String msg = response != null ? response.message : "null response";
            log.warn("PAHO ETL: no data for {} — Delphi says: {}", locationCode, msg);
            return 0;
        }

        if (response.epidata == null || response.epidata.isEmpty()) {
            log.warn("PAHO ETL: empty epidata array for {}", locationCode);
            return 0;
        }

        log.info("PAHO ETL: received {} records from Delphi for {}",
                response.epidata.size(), locationCode);

        // ── Step 3: Map API records to entities and save in batches ───────────

        // LEARNING NOTE — Batch inserts vs individual saves:
        //   saveAll(list) is a BULK INSERT — Hibernate sends one INSERT statement
        //   with many rows (or a prepared statement that it executes many times
        //   in a single DB roundtrip, depending on the JDBC batch settings).
        //   This is MUCH faster than calling save(entity) for each record individually
        //   because it reduces the number of DB roundtrips:
        //     500 records × individual save = 500 roundtrips
        //     saveAll([500 records]) = 1 roundtrip
        //   We batch in groups of batchSize (default 500) to keep memory manageable
        //   for large result sets.
        List<PahoCaribCase> batch = new ArrayList<>();
        String countryName = LOCATION_NAMES.getOrDefault(locationCode, locationCode);
        int inserted = 0;
        int skipped = 0;

        for (DelphiApiResponse.EpidataRecord record : response.epidata) {

            // Skip malformed records (epiweek or location code missing)
            if (record.epiweek == null || record.location == null) {
                skipped++;
                continue;
            }

            // LEARNING NOTE — Idempotency check at record level:
            //   Even though countByLocationCode prevents re-loading a country on startup,
            //   the incremental refresh endpoint (POST /api/paho/etl/refresh) can call
            //   this method for date ranges that overlap with existing data.
            //   existsByLocationCodeAndEpiWeek performs:
            //     SELECT EXISTS(SELECT 1 FROM paho_caribbean_cases
            //                   WHERE location_code = ? AND epi_week = ?)
            //   One query per record is expensive for the initial load, but ensures
            //   correctness. For the startup path, the countByLocationCode guard above
            //   means this check is only reached for records in date ranges that
            //   genuinely might overlap with existing data.
            if (pahoRepo.existsByLocationCodeAndEpiWeek(record.location, record.epiweek)) {
                skipped++;
                continue;
            }

            // LEARNING NOTE — @Builder pattern:
            //   .builder() starts the builder, each .field(value) sets a field,
            //   .build() constructs the final immutable PahoCaribCase object.
            //   We never call the constructor directly — the builder makes it obvious
            //   which field each value is being assigned to, preventing argument order mistakes.
            PahoCaribCase entity = PahoCaribCase.builder()
                    .locationCode(record.location)
                    .countryName(countryName)
                    .epiWeek(record.epiweek)
                    // LEARNING NOTE — Integer arithmetic to extract year and week:
                    //   epiweek = YYYYWW, so:
                    //   year       = 202301 / 100 = 2023  (integer division discards remainder)
                    //   weekOfYear = 202301 % 100 = 1     (modulo gives the remainder)
                    .year(record.epiweek / 100)
                    .weekOfYear(record.epiweek % 100)
                    .totalPopulation(record.totalPop)
                    .dengueCases(record.numDengue)
                    .incidenceRate(record.incidenceRate)
                    .serotype(record.serotype)
                    .dataSource("PAHO/DELPHI")
                    .ingestedAt(LocalDateTime.now())
                    .build();

            batch.add(entity);
            inserted++;

            // Flush to DB when batch is full — avoids keeping too much in memory
            if (batch.size() >= batchSize) {
                pahoRepo.saveAll(batch);
                batch.clear();
                log.debug("PAHO ETL: flushed batch of {} for {}", batchSize, locationCode);
            }
        }

        // Flush the final partial batch (records that didn't fill a complete batchSize)
        if (!batch.isEmpty()) {
            pahoRepo.saveAll(batch);
        }

        if (skipped > 0) {
            log.debug("PAHO ETL: {} — skipped {} records (already exist or malformed)", locationCode, skipped);
        }

        return inserted;
    }

    // ── Incremental refresh ────────────────────────────────────────────────────

    // LEARNING NOTE — Why a separate incremental refresh method:
    //   The startup method (loadOnStartup) only loads countries with zero records.
    //   But once we have historical data, we need to add NEW weeks as they're published.
    //   This method is called by PahoController's POST /api/paho/etl/refresh endpoint.
    //   It finds each country's most recent epiweek in our DB, then fetches everything
    //   AFTER that from the Delphi API — getting only the new weeks.
    public int refreshAllCountries() {
        log.info("PAHO ETL: starting incremental refresh for all Caribbean locations");
        int total = 0;

        int currentYear = LocalDate.now().getYear();
        String epiweekEnd = (currentYear + 1) + "60";

        for (String locationCode : CARIBBEAN_LOCATIONS) {
            try {
                // Find the most recent epiweek we already have for this country
                PahoCaribCase latest = pahoRepo.findFirstByLocationCodeOrderByEpiWeekDesc(locationCode);

                // If we have no data at all, fetch from the beginning
                int startEpiweek;
                if (latest == null) {
                    startEpiweek = Integer.parseInt(epiweekStart);  // e.g. 201001
                } else {
                    // Start from the week AFTER our last record
                    // epiWeek = YYYYWW, add 1 to week. Handle year rollover (week 53 → next year week 1):
                    int lastWeek = latest.getEpiWeek() % 100;
                    int lastYear = latest.getEpiWeek() / 100;
                    if (lastWeek >= 53) {
                        startEpiweek = (lastYear + 1) * 100 + 1;  // Roll to next year week 1
                    } else {
                        startEpiweek = lastYear * 100 + (lastWeek + 1);
                    }
                }

                String epiweekRange = startEpiweek + "-" + epiweekEnd;
                log.info("PAHO ETL refresh: {} — fetching epiweeks {}", locationCode, epiweekRange);

                int inserted = fetchAndSaveCountry(locationCode, epiweekRange);
                total += inserted;

                Thread.sleep(500);  // Respectful pause between API calls

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("PAHO ETL refresh: interrupted during {}", locationCode);
                break;
            } catch (Exception e) {
                log.error("PAHO ETL refresh: failed for {} — {}", locationCode, e.getMessage());
            }
        }

        log.info("PAHO ETL refresh complete — {} new rows inserted", total);
        return total;
    }
}
