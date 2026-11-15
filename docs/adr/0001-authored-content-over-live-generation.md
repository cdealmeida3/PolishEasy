# Exercises are authored ahead of time, not generated live

The New Material in every Daily Set is Authored Content: written and reviewed before release and shipped with the app. We rejected generating exercises with an AI model while the Learner uses the app, because live generation can produce wrong Polish that a beginner cannot detect, needs a network connection and a server, and costs money per use. AI may help draft content, but nothing reaches a Learner unchecked: see ADR-0002.

## Consequences

- The app works offline and needs no backend for content.
- The amount of course material is limited by how much has been authored; new Units arrive through releases.
- Only the Review part of a Daily Set varies per Learner, and it is assembled from Authored Content, never generated.
