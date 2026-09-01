# ADR-005 — Treat the lowest non-zero ringtone step as attention

Status: Accepted  
Date: 2026-09-01

## Context

Motorola M0B evidence showed that ringtone `1/7`, normal ringer mode, and DND off rendered `READY`. Public APIs prove
that the step is non-zero, but neither automation nor that observation proved that it is reasonably audible. VolumeOK's
accessibility goal makes a silent acceptance of the lowest step misleading.

Percentage thresholds are not comparable across OEM volume curves. A personalized minimum would add persistence and
policy concepts before M2 needs them.

## Decision

M2 uses ordinal platform steps:

- `0` is muted and produces `ACTION_REQUIRED`;
- the lowest non-zero step, `1`, produces `ATTENTION`;
- `2` or higher has no volume-level issue, subject to all other mandatory evidence;
- invalid or unavailable volume evidence produces `UNKNOWN`.

`READY` means that current public evidence indicates a configuration capable of ringing. It does not guarantee human
audibility, telephony delivery, carrier behavior, active routing, or future ringing.

The first one-tap correction is deliberately narrow: when the current value is `1`, the platform maximum is greater
than `2`, and nominal write capability exists, the user may explicitly request `1 -> 2`. Success requires fresh
readback. Zero, silent/vibrate mode, DND, and any correction whose only target is platform maximum receive guidance,
not an automatic CTA in M2.

## Consequences

- The physical `1/7` result changes from `READY` to `ATTENTION` without inventing a percentage.
- The rule remains deterministic and understandable across different platform maxima.
- A future user-configured protection/readiness minimum may supersede this baseline through a separate persisted-policy
  decision; M2 does not add DataStore for it.
- M3 audible confirmation remains the only user evidence that the local test sound was actually heard.
