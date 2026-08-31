# Observability / AIOps / LLMOps

## MVP observability

VolumeOK is a local-first Android app, so observability must be proportional.

Baseline:
- structured local diagnostic events;
- explicit failure codes;
- Android Vitals / Play Console crash and ANR signals after distribution;
- no user-content logging;
- no behavioral analytics required for core operation.

Suggested event fields:

```text
event
appVersion
androidApi
oem/deviceBucket
capability
previousStateBucket
newStateBucket
protectionRuntimeState
failureCode
elapsedMs
```

Avoid contact names, phone numbers, free-form user content, secrets, and raw identifiers unless a future privacy architecture explicitly justifies them.

## Product health signals

- crash/ANR rate;
- readiness-evaluation failures;
- correction write-not-effective rate;
- protection runtime stopped/degraded rate;
- circuit-breaker frequency;
- OEM/API-specific failure distribution;
- battery/performance regressions;
- accessibility/user-reported false-green cases.

## AIOps

**Not required for MVP.**

AIOps becomes useful only when enough opt-in/aggregate operational data exists. Future automation may:
- detect anomaly spikes by OEM/API/app version;
- cluster recurring failure codes;
- create/triage issues;
- suggest likely regression ranges;
- recommend rollback investigation.

AIOps must not autonomously alter user protection policies, bypass permissions, or publish releases without the normal deterministic/human release gates.

## LLMOps

**Not applicable to MVP.** The readiness/protection engine is deterministic.

If an LLM is later added for optional explanation/support, it must be a separate bounded capability with:
- model/version tracking;
- prompt/version tracking;
- structured output schema;
- golden and adversarial evals;
- hallucination/grounding checks;
- privacy/redaction policy;
- latency/cost budgets;
- deterministic fallback;
- explicit statement that LLM output cannot authorize or execute core protection decisions.

No LLM should decide:
- whether phone is READY;
- whether auto-restore occurs;
- volume bounds;
- permission/capability state;
- circuit-breaker transitions.
