# cloud-itonami-isco-8160

Open Occupation Blueprint for **ISCO-08 8160**: Food and Related Products Machine Operators.

This repository designs a forkable OSS business for an independent food processing machine operator: a monitoring robot performs temperature checks and sampling under a governor-gated actor, so the operator keeps their own food-safety and batch records instead of renting a closed food-safety SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a food-line monitoring robot performs temperature checks, sample collection and line-clearance verification under an actor that proposes
actions and an independent **Food Processing Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near cutting/mixing equipment, or handling allergen cross-contact zones) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
production order + food-safety plan + batch specification
        |
        v
Process Advisor -> Food Processing Governor -> process/monitor, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8160`). Required capabilities:

- :robotics
- :telemetry
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

Like `cloud-itonami-isco-6130`, this repository implements the **full
itonami Actor pattern** from CLAUDE.md's Actors section: a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/food_processing/store.cljc` — `Store` protocol + `MemStore`:
  registered batches (`allergen-cross-contact-risk?`), committed
  records, an append-only audit ledger.
- `src/food_processing/advisor.cljc` — `Advisor` protocol; `mock-advisor`
  (deterministic, default) proposes a processing operation from a
  request; `llm-advisor` wraps a `langchain.model/ChatModel` — either
  way the advisor only ever produces a `:propose`-effect proposal,
  never a committed record, and LLM parse failures always yield
  `confidence 0.0` (forces escalation, never fabricated confidence).
- `src/food_processing/governor.cljc` — `ProcessingGovernor/check`: a
  pure function, wired as its own `:govern` node. Hard invariants
  (unregistered batch, a proposal whose `:effect` isn't `:propose`)
  always route to `:hold`. The escalation invariant — a `:release` op
  on a batch flagged `allergen-cross-contact-risk?` — always routes to
  `:request-approval` (a batch can never reach market without an
  explicit human allergen-labelling review), as does low advisor
  confidence.
- `src/food_processing/actor.cljc` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.

```bash
clojure -M:test   # 10 tests, 26 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) —
the second `cloud-itonami-isco-*` occupation actor built on the full
`langgraph.graph`/`langchain` Actor pattern (after `-6130`; the
remaining 23 implemented occupations use the lighter Store+governor-only
pattern).

## License

AGPL-3.0-or-later.
