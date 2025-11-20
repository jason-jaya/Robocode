SampleStudentBot
================

Overview
--------
- Melee bot with guess-factor aim for 1v1 and risk-based movement for crowds.
- Color cycles on scans/wins; starts with adaptive radar lock in melee and spins radar nonstop in 1v1.
- Uses low-risk point selection to keep distance from walls/corners/enemies, with evasive strafing in 1v1.

Targeting
---------
- **Melee:** Simple linear prediction with wall checks; bullet power scales by distance, enemy energy, and our remaining energy, skipping shots when reserves are too low.
- **1v1:** Guess-factor gun with waves segmented by distance and velocity; waves log bins to refine future aim.
- Gun fires as soon as heat is clear and gun turn is within ~5° to increase rate of fire.

Movement
--------
- **Melee:** Samples many candidate points around the bot and picks the lowest-risk location (prefers center over corners, avoids enemies with higher energy).
- **1v1:** Adaptive orbit using battlefield size instead of fixed dimensions; reverses direction when bullets likely or wall pressure rises.
- Keeps a small wall margin to reduce surf bounces and keeps radar spinning if it ever stops.

Energy Management
-----------------
- Caps bullet power when low on energy; 1v1 also clamps wave firepower to leave a small reserve.
- Skips firing if firing would drop energy too low for movement.

How to run in Robocode
----------------------
1) Copy/confirm the compiled class and package path: `student.SampleStudentBot`.
2) In Robocode, click *Battle* → *New* and add `SampleStudentBot` from the student package.
3) Run in melee or 1v1. For the latest code, trigger a rebuild via Robocode’s *Robot* → *Compile* or run your usual build script, then restart the battle.

Notes
-----
- Battlefield-aware 1v1 movement (no hardcoded 800x600).
- Wave stats clamp segment indexes to avoid array errors.
- Radar has a fallback to keep scanning in long melee loops.
