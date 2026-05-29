package com.arbosentinel.green.dto;

// ══════════════════════════════════════════════════════════════════════════════
// GREEN layer — Jackson deserialization DTOs for the CMU Delphi Epidata API
//
// The Delphi API returns JSON in this envelope structure:
//
//   {
//     "result": 1,
//     "message": "success",
//     "epidata": [
//       {
//         "location":       "jm",
//         "epiweek":        202301,
//         "total_pop":      2961167,
//         "num_dengue":     50,
//         "incidence_rate": 1.69,
//         "serotype":       null
//       },
//       ...
//     ]
//   }
//
// These two classes let Jackson automatically map that JSON into Java objects.
// The ETL job (PahoEtlJob) calls the API and deserialises into DelphiApiResponse,
// then iterates over the epidata list to build PahoCaribCase entities.
//
// LEARNING NOTE — Jackson @JsonProperty:
//   The Delphi API uses snake_case JSON keys (total_pop, num_dengue, epiweek).
//   Java convention is camelCase (totalPop, numDengue, epiweek).
//   @JsonProperty("total_pop") tells Jackson: "when you see the JSON key 'total_pop',
//   map it to the Java field 'totalPop'".
//   Without this, Jackson would look for a JSON key named "totalPop" and find nothing,
//   leaving the field null even when the API sent a value.
//
// LEARNING NOTE — Why two classes (wrapper + record)?
//   The JSON has TWO levels:
//     Level 1 — outer envelope: { "result": 1, "message": "...", "epidata": [...] }
//     Level 2 — each data row inside the epidata array
//   We need a class for each level. DelphiApiResponse holds the envelope,
//   EpidataRecord holds one row. Jackson fills them both automatically.
//
// LEARNING NOTE — Why NOT use @JsonIgnoreProperties(ignoreUnknown = true)?
//   We don't need it here because we're mapping the complete Delphi response shape.
//   It's useful when an external API adds new fields and you don't want Jackson
//   to throw an error because the Java class doesn't have a matching field.
//   For future-proofing we could add it — kept out here to stay minimal.
// ══════════════════════════════════════════════════════════════════════════════

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

// Outer envelope — matches the top-level JSON object
@JsonIgnoreProperties(ignoreUnknown = true)  // Ignore any new fields Delphi might add
public class DelphiApiResponse {

    // LEARNING NOTE — Public fields vs getters for Jackson:
    //   Jackson can deserialise into either public fields OR private fields with getters/setters.
    //   We use public fields here to keep this class as simple as possible —
    //   it's a pure data carrier, never mutated after deserialisation.

    @JsonProperty("result")
    public Integer result;          // 1 = success, -1 = no data, -2 = too many rows

    @JsonProperty("message")
    public String message;          // "success", "no results", etc.

    @JsonProperty("epidata")
    public List<EpidataRecord> epidata;   // The actual case data rows

    // ── Inner class — one row of PAHO dengue surveillance data ────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EpidataRecord {

        @JsonProperty("location")
        public String location;         // ISO-like country code: "jm", "tt", "bb"

        @JsonProperty("epiweek")
        public Integer epiweek;         // YYYYWW integer: 202301 = year 2023, week 1

        // LEARNING NOTE — total_pop → Long:
        //   JSON numbers without decimals can deserialise to Integer or Long.
        //   We use Long because Caribbean island populations can exceed Integer.MAX_VALUE
        //   if the API ever includes larger jurisdictions. Safer to use Long here.
        @JsonProperty("total_pop")
        public Long totalPop;           // Registered population for this country

        @JsonProperty("num_dengue")
        public Integer numDengue;       // Reported dengue cases this epi-week (nullable)

        // LEARNING NOTE — incidence_rate → BigDecimal:
        //   The JSON value is a decimal (e.g. 1.69). Jackson can map it to double,
        //   float, or BigDecimal. We use BigDecimal to avoid floating-point rounding
        //   errors — same reason the database stores it as NUMERIC(10,4).
        @JsonProperty("incidence_rate")
        public BigDecimal incidenceRate; // Cases per 100,000 population (nullable)

        @JsonProperty("serotype")
        public String serotype;         // DENV-1 through DENV-4 — almost always null
    }
}
