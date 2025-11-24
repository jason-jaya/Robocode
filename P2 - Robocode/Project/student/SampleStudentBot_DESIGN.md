# SampleStudentBot Design
created by Jason Jaya.

## Overview
- Team-capable melee and 1v1 robot focused on evasive movement, wave-surfer inspired targeting in duels, and risk-based positioning in melee.
- Emphasizes survivability by capping firepower based on distance, enemy energy, and self-energy.
- Uses a consistent yellow palette for team identification and avoids friendly fire with teammate checks.

## Main Loop
- Initializes battlefield dimensions, own state, and sets colors once at startup.
- Melee path: continually updates enemy list, picks a low-risk destination, moves, shoots at the current target, and keeps radar sweeping.
- 1v1 path: dedicated radar lock, wave collection, and adaptive gun aiming based on the most visited bearing offsets.

## Targeting
- **Melee:** Chooses target by a shootable score weighted by enemy energy and distance; predictive linear/extrapolated aim with wall corrections.
- **1v1:** Builds a `Wave` per scan; segments by distance and velocity; fires where historical bins show highest hit likelihood.
- Firepower selection: clamps to survivability thresholds; uses softer shots when energy is low or range is long.

## Movement
- **Melee:** Generates random candidate points around the bot, evaluates risk (enemy proximity, center bias, corner penalties), and moves toward the lowest-risk point with smoothing.
- **1v1:** Lateral, direction-reversing movement relative to enemy bearing; avoids walls via a shrinking fire field; reverses based on bullet travel/spacing heuristics.

## Radar
- In melee, keeps radar sweeping; if scan is stale or locked to one opponent, resets sweep.
- In 1v1, uses double-angle radar turns for tight locks.

## Energy & Survival
- Will not fire if post-shot energy would dip below a small reserve.
- Reduces power when energy is scarce or when predicted lead shot risks exiting the battlefield.

## Team Behavior
- Skips shots on teammates via `isFriendlyScan` using `isTeammate`.
- Single setColor call establishes yellow body/gun/radar/bullet stripe for team recognition.

## Extensibility Notes
- Wave data: segmentation bins are static; can be expanded for finer granularity.
- Movement: risk scoring is heuristic; can incorporate enemy headings/velocities directly or add wall smoothing to candidate generation.
- Communication: currently no messaging; can broadcast enemy positions to allies if desired.

## Risks / Next Steps
- Wave buffer is global/static; long battles may overweight early data—consider decay.
- Predictive melee aim assumes roughly linear motion; agile enemies may dodge—could add circular or pattern-matching aim.
- Add tests/simulations around risk scoring to ensure it behaves on small/odd-shaped maps.
