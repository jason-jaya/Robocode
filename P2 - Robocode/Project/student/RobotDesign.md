Robot Design Document – SampleStudentBot
=======================================

This document describes the AI design and behavior of `student.SampleStudentBot` (single‑robot focus; team mechanics excluded).

Overall Approach (High-Level)
-----------------------------
- Hybrid melee/duel logic: a guess-factor gun with waves in 1v1; linear-predictive aiming with risk-based movement in melee.
- Defensive bias: energy-aware firing to stay mobile; movement favors low-risk positions away from walls, corners, and high-energy enemies.
- Continuous radar lock: full-spin radar in 1v1 and continuous sweep in melee to keep targeting data fresh.
- Visual effect: black/white flashing body/gun with fixed black radar/bullet/scan for visibility.

Architecture
------------
Primary classes/structures:
- `SampleStudentBot` (extends `AdvancedRobot`): orchestrates run loop, radar, firing, and movement.
- Nested `Robot` struct: per-enemy state (energy, heading, velocity, absolute bearing, last scan time).
- `Wave` (extends `Condition`): captures guess-factor statistics per bullet (distance/velocity segmentation) for 1v1 targeting.
- `Movement1v1`: dueling movement with adaptive orbit and direction flips (uses live battlefield size).
- `Utility`: math helpers (clamp, projection, absoluteBearing, sign).

Control Flow (per tick)
-----------------------
```
initialize battlefield, colors, radar decoupling
if (melee) {
    seed candidate points for movement
    loop:
        refresh self/enemy states
        drop stale enemies (>25 ticks)
        movement() -> pick low-risk point and head toward it
        shooting() if a target is alive (linear prediction)
        radar fallback spin if needed
        execute()
} else { // 1v1
    loop:
        radar sweeps infinitely
}
onScannedRobot(melee):
    update/track enemy state, choose best target by shootable score
onScannedRobot(1v1):
    spawn Wave, aim via guess-factor offset, energy-aware firepower, orbit movement
```

Targeting
---------
- **1v1 Guess-Factor Gun:** On scan, create a `Wave` with gun position, enemy bearing/distance, lateral direction. Segment stats by distance and current/previous velocity (clamped indexes). When a wave “arrives” (custom event), increment the hit bin. Firing angle = absolute bearing + most-visited bin offset. This adapts to enemy lateral movement patterns.
- **Melee Linear Prediction:** Predict target position with heading/turn-rate extrapolation; adjust bullet power if predicted point exits safe field. Uses dynamic power based on distance, enemy energy, and remaining self-energy.
- **Fire Rate:** Fires when gun heat is clear and gun turn is within ~5°; energy guard leaves ~0.2 energy buffer and caps power when low.

Movement
--------
- **Melee Risk Map:** Sample ~150 candidate points around current position. Score combines inverse distance to self (stay moving), attraction to center, corner penalties (scaled by live enemy count), and enemy proximity/angle weighting (higher-energy enemies produce more risk). Choose minimum-risk point and drive toward it with max velocity scaled by turn remaining.
- **1v1 Orbit/Anti-Gravity:** Evasive strafe around enemy bearing with direction flips triggered by bullet-distance heuristics and wall proximity. Uses a battlefield-aware rectangle (no fixed 800x600) to keep within margins.
- **Wall Margin:** Consistent 18px margin when projecting positions for firing and movement; clamps predicted positions to stay inside field.

Radar
-----
- Radar decoupled from gun/body. In melee, spins right infinitely (failsafe if radar stops). In 1v1, full-spin locks to rescan quickly; lateralDirection updates on enemy velocity sign.

Energy Management
-----------------
- Bullet power capped by both self and enemy energy; in low-energy states firePower is clamped (<=1.5, or 0.15 when very low and close).
- In melee, `selectMeleePower` scales with distance and enemy energy; skips shots that would drop below a small reserve.

State & Data Structures
-----------------------
- `HashMap<String,Robot> enemyList` keyed by name; pruning when scans are >25 ticks stale.
- `Wave.statBuffers[DIST][VEL][LAST_VEL][BIN]` accumulates guess-factor hits; bins centered at middle (escape angle ~0.7).
- `possibleLocations`: list of candidate melee movement points regenerated when reaching target or after idle ticks.

Key Algorithms/Influences
-------------------------
- Guess-Factor (waves + lateral direction bins) inspired by classic Robocode papers/bots (e.g., Raiko variants, CassiusClay style segmentation).
- Risk-based melee movement: sampled point scoring reminiscent of “Minimum Risk Movement” introduced in Robocode community articles.
- Direction flipping in 1v1 uses bullet flight time heuristics (reverse tuner) and wall bounce tuner, similar to orbiting strafe patterns covered in class.

Behavior Summary
----------------
- In melee: constantly evaluates safer spots, fires cautiously to preserve energy, keeps radar spinning, and avoids corners/high-energy opponents.
- In 1v1: strafes at variable radius, adapts aim via guess-factor stats, conserves energy when low, and fires as soon as alignment/heat allow.

Simple Structural Sketch
------------------------
```
SampleStudentBot
 ├─ Robot (enemy state)
 ├─ Wave (GF stats, custom event)
 ├─ Movement1v1 (orbit/evasion)
 └─ Utility (math helpers)
```

Testing Notes
-------------
- Uses `javac` with `robocode.jar` on classpath; deploy target: `env/robots/student/*.class`.
- Requires custom event processing (waves) to remain registered; radar must keep scanning to populate segments quickly.

Limitations / Future Work
-------------------------
- Melee gun is linear-only—could add segmented linear or simple GF-lite for crowds.
- No adaptive anti-ram logic; close-quarters evasion could be strengthened.
- No surfing of incoming waves—movement is evasive/orbit-based, not true wave surfing.

Word count: ~560
