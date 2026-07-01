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

## License

AGPL-3.0-or-later.
