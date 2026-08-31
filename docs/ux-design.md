# UX / Design System

## UX principle

VolumeOK should feel calm, trustworthy, and immediately understandable. It is not a technical mixer, a “senior mode”, or a diagnostic dashboard full of raw metrics.

Primary question:

> **Is this phone ready to ring?**

## Information hierarchy

1. Readiness state.
2. Plain-language explanation.
3. Primary corrective/test action.
4. Protection status.
5. Detailed technical state only when requested.

## Home concept

```text
VolumeOK

      ✓
   EVERYTHING OK

Your phone is ready to ring.

[ TEST NOW ]

Protection   Active
Ringtone     70% ✓
Mode         Sound ✓
Do Not Disturb Off ✓

[ View details ]
```

Localized copy must preserve meaning rather than literal word-for-word translation.

## Readiness visual states

- `READY`: reassuring confirmation; no celebratory excess.
- `ATTENTION`: neutral warning; user can continue but should understand the issue.
- `ACTION_REQUIRED`: strong but non-panicking explanation with one primary action.
- `UNKNOWN`: clear statement that VolumeOK cannot verify a capability/state.

Color must never be the only signal. Always pair icon + text + semantics.

## Fox mascot

The fox is a friendly **guide**, not a childish cartoon that dominates every screen.

Roles:
- onboarding/help;
- safe-test guidance;
- empty/unknown states;
- success acknowledgement;
- occasional educational tip.

Avoid:
- mascot on every card;
- constant animation;
- mascot replacing error text;
- medical-professional claims.

Visual direction:
- warm orange fox;
- optional glasses/headset or subtle technical accessory;
- clean Material-adjacent illustration style;
- high recognizability at app-icon size;
- no excessive gradients/gloss that hurt accessibility.

## Microinteractions

| Event | Interaction |
| --- | --- |
| Snapshot completes READY | subtle check transition + optional light haptic |
| Issue discovered | card expands/fades in; no shaking/pulsing red |
| User corrects issue | button shows bounded progress; app re-reads actual state; result replaces action |
| Protection enabled | shield/check morph + confirmation text |
| Protection paused | shield becomes pause state; remaining duration shown |
| Circuit breaker opens | protection card changes to suspended state with plain-language explanation |
| Safe test starts | speaker/fox subtle animation; immediate Stop action |
| Undoable correction | snackbar with localized Undo |

## Reduced motion

Respect system reduced-motion/animation scale. Essential information must not depend on movement.

## Accessibility baseline

- minimum 48dp touch targets; primary CTAs may use larger 56–64dp targets;
- semantic labels for icons and controls;
- TalkBack reading order tested;
- font scaling to at least 200% without clipping/overlap;
- landscape and multi-window support;
- contrast checked against Material/WCAG guidance;
- no icon-only critical actions;
- focus state visible;
- errors identify both problem and recovery action;
- safe-test audio is never the only feedback channel;
- haptics are supplemental, never required.

## Adaptive/responsive layout

Compose layout decisions follow **window size**, not hardcoded “phone/tablet”.

### Compact
Single pane, vertically ordered.

### Medium/Expanded
Use list-detail or supporting-pane patterns where useful:

```text
┌─────────────────────┬─────────────────────────┐
│ Readiness summary   │ Selected issue/detail   │
│ Test                │ Explanation             │
│ Protection          │ Action / evidence       │
└─────────────────────┴─────────────────────────┘
```

Do not stretch phone cards edge-to-edge across large tablets.

## Internationalization

Initial locales:
- English fallback (`values/`)
- PT-BR (`values-pt-rBR`)
- Spanish (`values-es`)

Requirements:
- no concatenated UI sentences;
- plural resources where needed;
- localized numbers/percentages/time;
- pseudolocale testing;
- text expansion testing;
- avoid culturally specific idioms in core status language.

## Tone

Good:
- “Your ringtone volume is very low.”
- “VolumeOK could not verify Do Not Disturb on this device.”
- “Protection was paused because volume kept changing repeatedly.”

Bad:
- “ERROR CODE 0xA113” as primary copy;
- “Your phone is broken!”;
- “Guaranteed never to miss a call.”

## Design tokens

When implementation starts, centralize:
- spacing;
- typography roles;
- shape radii;
- elevation;
- status semantic colors;
- motion durations;
- icon sizes.

Do not hardcode styling throughout composables.
