package student;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;


/**
 * Second team member using the same behavior as SampleStudentBot.
 */
public class SampleStudentBotTwo extends TeamRobot {

    // --- Enemy data container ---
    private static class Enemy {
        String name;
        double distance;
        double bearingRadians; // relative bearing
        double absBearingRadians; // absolute bearing
        double energy;
        double headingRadians;
        double velocity;
        long scanTime;
        double x;
        double y;
    }

    // --- Utility helpers ---
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private static double projectX(double x, double angle, double dist){ return x + Math.sin(angle)*dist; }
    private static double projectY(double y, double angle, double dist){ return y + Math.cos(angle)*dist; }

    private Enemy target = null;
    private double lateralDirection = 1; // sign for perpendicular movement
    private static final double WALL_MARGIN = 40;
    private int radarDirection = 1; // flips to keep radar sweeping if lock lost
    // Wall stuck handling
    private int wallStuckTicks = 0;
    private static final int WALL_STUCK_THRESHOLD = 5;

    @Override
    public void run() {
        setBodyColor(Color.YELLOW);
        setGunColor(Color.YELLOW);
        setRadarColor(Color.YELLOW);
        setBulletColor(Color.YELLOW);
        setScanColor(Color.YELLOW);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        // Start spinning radar indefinitely
        setTurnRadarRight(Double.POSITIVE_INFINITY);
        while (true) {
            doMovement();
            doAimingAndFiring();
            // Maintain radar lock; if it stopped, restart sweep
            if (getRadarTurnRemaining() == 0) {
                setTurnRadarRight(Double.POSITIVE_INFINITY * radarDirection);
            }
            execute();
        }
    }

    // --- Event: update enemy info ---
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        double absBearing = getHeadingRadians() + e.getBearingRadians();
        Enemy en;
        if (target == null || e.getName().equals(target.name)) {
            en = target == null ? new Enemy() : target;
        } else {
            // Prefer closest enemy if multiple
            if (target != null && target.scanTime == getTime() && target.distance < e.getDistance()) {
                return; // keep current target
            }
            en = new Enemy();
        }
        en.name = e.getName();
        en.distance = e.getDistance();
        en.bearingRadians = e.getBearingRadians();
        en.absBearingRadians = absBearing;
        en.energy = e.getEnergy();
        en.headingRadians = e.getHeadingRadians();
        en.velocity = e.getVelocity();
        en.scanTime = getTime();
        en.x = projectX(getX(), absBearing, en.distance);
        en.y = projectY(getY(), absBearing, en.distance);
        target = en;

        // Radar lock: turn radar toward (and a bit past) enemy for persistent tracking
        double radarTurn = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
        double extra = (radarTurn < 0 ? -1 : 1) * Math.toRadians(20); // overshoot
        setTurnRadarRightRadians(radarTurn + extra);
    }

    // --- Movement: perpendicular strafe + wall avoidance ---
    private void doMovement() {
        if (target == null) return;
        // Update lateral direction based on enemy movement for variability
        if (target.velocity != 0) {
            lateralDirection = Math.signum(target.velocity * Math.sin(target.headingRadians - target.absBearingRadians));
            if (lateralDirection == 0) lateralDirection = 1;
        }
        // Desired perpendicular angle
        double perpAngle = target.absBearingRadians + Math.PI/2 * lateralDirection;
        double turnAngle = Utils.normalRelativeAngle(perpAngle - getHeadingRadians());
        setTurnRightRadians(turnAngle);

        // Distance band control: try to stay roughly 200-450 from enemy
        double desired = target.distance < 180 ? -120 : (target.distance > 450 ? 160 : 60);
        setAhead(desired);

        // Wall avoidance: if near wall, bias heading toward center
        double bfW = getBattleFieldWidth();
        double bfH = getBattleFieldHeight();
        boolean nearWall = getX() < WALL_MARGIN || getX() > bfW - WALL_MARGIN || getY() < WALL_MARGIN || getY() > bfH - WALL_MARGIN;
        if (nearWall) {
            wallStuckTicks++;
        } else {
            wallStuckTicks = 0;
        }
        // Escape if stuck: enemy close & multiple consecutive ticks near wall
        if (nearWall && target.distance < 260 && wallStuckTicks >= WALL_STUCK_THRESHOLD) {
            performWallEscape(bfW, bfH);
            return; // skip normal movement this tick
        }
        if (nearWall) {
            double centerX = bfW / 2.0;
            double centerY = bfH / 2.0;
            double centerBearing = Math.atan2(centerX - getX(), centerY - getY());
            double adjust = Utils.normalRelativeAngle(centerBearing - getHeadingRadians());
            setTurnRightRadians(adjust * 0.4); // gentle correction
            setAhead(120); // push inward without overriding distance band too much
        }
    }

    // Forceful escape: turn to center and burst inward, flipping lateral direction for next cycles
    private void performWallEscape(double bfW, double bfH) {
        double centerX = bfW / 2.0;
        double centerY = bfH / 2.0;
        double centerBearing = Math.atan2(centerX - getX(), centerY - getY());
        double turn = Utils.normalRelativeAngle(centerBearing - getHeadingRadians());
        setTurnRightRadians(turn);
        // Burst ahead (or back if angle awkward) to detach from wall
        double push = 220;
        if (Math.cos(turn) < 0) {
            push = -push; // if facing away, reverse
        }
        setAhead(push);
        // Flip lateral direction to vary path after escape
        lateralDirection = -lateralDirection;
        // Slight radar adjustment to ensure continued scans
        setTurnRadarRightRadians(Double.POSITIVE_INFINITY * (radarDirection));
    }

    // --- Firing & Aiming ---
    private void doAimingAndFiring() {
        if (target == null || getGunHeat() > 0) return;
        // Predict simple linear future position
        double power = selectFirePower(target.distance, getEnergy(), target.energy);
        double bulletSpeed = 20 - 3 * power;
        double time = target.distance / bulletSpeed;
        if (time > 3) time = 3; // clamp
        double futureX = target.x + Math.sin(target.headingRadians) * target.velocity * time;
        double futureY = target.y + Math.cos(target.headingRadians) * target.velocity * time;
        double futureBearing = Math.atan2(futureX - getX(), futureY - getY());
        double gunTurn = Utils.normalRelativeAngle(futureBearing - getGunHeadingRadians());
        setTurnGunRightRadians(gunTurn);
        // Fire only if roughly aligned
        if (Math.abs(getGunTurnRemaining()) < 6) {
            setFire(power);
        }
    }

    private double selectFirePower(double distance, double myEnergy, double enemyEnergy) {
        double base;
        if (distance < 140) base = 2.8;
        else if (distance < 250) base = 2.2;
        else if (distance < 400) base = 1.6;
        else if (distance < 600) base = 1.1;
        else base = 0.9;
        // Conserve if low energy
        if (myEnergy < 18) base = Math.min(base, 1.4);
        if (myEnergy < 8) base = Math.min(base, 0.9);
        // Avoid overkill if enemy almost dead
        if (enemyEnergy < 5 && distance > 150) base = Math.min(base, 1.2);
        return clamp(base, 0.1, 3.0);
    }

    // --- Minor cosmetic win animation ---
    @Override
    public void onWin(WinEvent e) {
        for (int i = 0; i < 30; i++) {
            setBodyColor(new Color((int)(Math.random()*255),0,0));
            turnGunRight(45);
            turnRadarRight(45);
        }
    }
}
