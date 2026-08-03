# Product specification — Family Risk Review

This document summarises the product definition for the native Android tablet application. Full master requirements live in the project brief; this file is the in-repo source of truth for implementers.

## Purpose

Help a household understand which people depend on income or unpaid care, which future responsibilities must continue, when money is required, approximate costs, which risks may interrupt income, and why future income is not guaranteed.

Core insight:

> Our responsibilities have planned dates, but the income expected to fund them does not have a guaranteed duration.

## What it is not

Not an exact life-insurance calculator, policy recommender, premium quotation tool, insurer comparison, returns calculator, complete financial planner, or lead-capture form.

## Branding

- **Family Risk Review** is the product identity everywhere (launcher, headers, PDFs, a11y).
- **Dareus One** appears only on the splash screen as “Built by Dareus One” with logo.
- Advisor: **S V Harish** / Certified Insurance Planner / LUGI CIP Completed.
  - TODO(production): Verify final formal credential wording before public release.

## Languages

English, Tamil, Hindi — string resources only; language persists for the review and summary.

## Household model

Household Support Map with Self, Spouse/partner, Child, Parent, Other dependant. Contribution and dependency statuses support dual-income, caregiving, irregular business income, and special-needs dependants. Prefer “household income” / “income contributor” over “breadwinner”.

## Data minimisation

No customer name, phone, email, address, Aadhaar, PAN, medical history, policy, premium, or insurer fields. Optional advisor-only reference (initials, CRM id, private note) must not auto-appear in customer summary.

## Review modes

- **Quick Review** — target 5–8 minutes
- **Guided Review** — target 10–15 minutes

## Customer journey (order)

1. Splash  
2. Welcome + language + mode  
3. Household Support Map  
4. Responsibilities  
5. Prioritisation (Must continue / Adjustable / Postpone)  
6. Responsibility details  
7. Responsibility Timeline  
8. Income-risk education  
9. Key realization  
10. Indicative Gross Responsibility Value  
11. Build Over Time vs Protect From Today  
12. Final awareness summary  
13. Advisor handoff  

## Financial output (v1)

**Indicative Gross Responsibility Value** — must-continue responsibilities before savings, income, assets, or insurance. Not “recommended life cover”.

Formulas and assumptions: see [CALCULATION_METHOD.md](CALCULATION_METHOD.md).

## Device targets

10–13" Android tablets; landscape first; portrait supported; offline during session.
