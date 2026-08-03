# Calculation method — Family Risk Review

**Calculation version:** `1.1.0`  
**Assumption version:** `1.1.0`

## Rate representation

Financial rates are stored as integer **basis points** (`AnnualRateBps`), not unrestricted doubles.

| Example | Basis points | Percent |
|---------|--------------|---------|
| Education default | 800 | 8% |
| Marriage default | 600 | 6% |
| Expense / recurring support default | 600 | 6% |

Validated bounds: `0 … 5000` bps (0% … 50%).

Arithmetic uses `BigDecimal` (DECIMAL64) with **half-up** rounding to whole rupees.

## Defaults by responsibility

| Catalogue | Default inflation |
|-----------|-------------------|
| Child higher education | Education (8%) |
| Marriage support | Marriage (6%) |
| Essential living expenses | Expense (6%) |
| Parent / spouse / special-needs / care replacement | Recurring support (6%) |
| Home loan / other loans | **None (0%)** |
| Buying/completing house | Expense (6%) |
| Custom / Other | **Explicit required** — never silently guessed |

## One-time responsibilities

```
futureValue = currentCost × (1 + inflationRate)^years
```

## Recurring support (v1.1.0 — selected method)

**Beginning-of-year cash-flow model** (not mid-year):

For each year index `y` in `0 until durationYears`:

```
yearOutlay = monthlyAmount × 12 × (1 + inflation)^y
```

Optional discounting when net return `r` is supplied:

```
discounted = yearOutlay / (1 + r)^y
```

Sum years, round half-up to whole rupees.

Payment timing for awareness flow: beginning-of-year annualised monthly outlay. Mid-year timing is **not** used in v1.1.0.

## Derived (stored) indicative values

A stored future indicative amount may be reused only when `DerivedValueMetadata` still matches:

* source current / monthly amounts
* years / duration
* inflation bps
* optional net-return bps
* assumption version
* calculation version

Otherwise recalculate.

## Scenarios

```kotlin
data class CalculationScenario(
    val kind: ScenarioKind,
    val assumptions: CalculationAssumptions,
)
```

Lower / Base / Higher results are generated only when each scenario has documented assumption differences. If only Base is approved, do not imply a meaningful range.

## Indicative Gross Responsibility Value

Sum of indicative values for selected **Must continue** responsibilities. Not recommended life cover.

Aggregate totals use checked `Long` addition (`Math.addExact`) — overflow fails loudly rather than wrapping.

## Calculation snapshots

Persisted `CalculationSnapshot` rows capture:

* review id + revision at snapshot time
* assumption version + calculation version (`ResponsibilityCalculator.CALCULATION_VERSION`)
* scenario kind
* assumptions JSON
* must-continue / adjustable / postponed totals
* per-responsibility indicative lines JSON
* generated-at timestamp

The review’s `summaryStale` flag is cleared when a snapshot is saved and set again whenever calculation inputs change.

