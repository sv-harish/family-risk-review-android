# Calculation method — Family Risk Review

**Calculation version:** `1.0.0`  
**Assumption version:** `1.0.0`

## Defaults

| Assumption | Annual rate |
|------------|-------------|
| Education inflation | 8% |
| Marriage support inflation | 6% |
| Essential expense inflation | 6% |

Assumptions are stored versioned and editable from settings / calculation details. Never hard-code rates in composables.

## One-time responsibilities

```
futureValue = currentCost × (1 + inflationRate)^years
```

Store: current cost, years, inflation rate, future value, assumption version, calculation version.

## Recurring support (v1.0.0)

Indicative total = sum over each year `y` in `0 .. durationYears-1` of:

```
monthlyAmount × 12 × (1 + inflationRate)^y
```

Optional discounting (detailed-gap later): divide each year’s outlay by `(1 + expectedNetReturn)^y` when a net-return assumption is supplied.

Do not multiply monthly expenses by an unexplained duration without documenting the method.

## Indicative Gross Responsibility Value

Sum of indicative values for selected responsibilities with priority **Must continue**.

Adjustable and postponed items are shown separately and excluded from the primary figure.

This is **not** recommended life cover, final protection gap, or guaranteed corpus.

## Scenarios

Ranges only when representing documented Lower-cost / Base / Higher-cost scenarios — never ± arbitrary percentage.

## Future detailed-gap inputs (not in initial awareness flow)

Existing dedicated savings, existing insurance, surviving household income, asset income, liabilities, support duration, expected net return, inflation, emergency costs. Domain models should remain extensible without shipping these questions in v1 awareness unless explicitly enabled.
