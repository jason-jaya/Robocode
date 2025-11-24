package student;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import robocode.Condition;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.WinEvent;
import robocode.util.Utils;


public class SampleStudentBot extends TeamRobot {
    static double BULLET_POWER = 2.8;

    class Robot extends Point2D.Double {
        public long scanTime;
        public boolean alive = true;
        public double energy;
        public String name;
        public double gunHeadingRadians;
        public double absoluteBearingRadians;
        public double velocity;
        public double heading;
        public double lastHeading;
        public double shootAbleScore;
        public double dist;
    }

    public static class Utility {
        static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }

        static double randomBetween(double min, double max) {
            return min + Math.random() * (max - min);
        }

        static Point2D project(Point2D sourceLocation, double angle, double length) {
            return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
                    sourceLocation.getY() + Math.cos(angle) * length);
        }

        static double absoluteBearing(Point2D source, Point2D target) {
            return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
        }

        static int sign(double v) {
            return v < 0 ? -1 : 1;
        }
    }

    class Movement1v1 {
        private static final double MAX_TRY_TIME = 125;
        private static final double REVERSE_TUNER = 0.43;
        private static final double DEFAULT_EVASION = 1.25;
        private static final double WALL_BOUNCE_TUNER = 0.71;
        private final TeamRobot robot;
        private double direction = 0.45;

        Movement1v1(TeamRobot _robot) {
            this.robot = _robot;
        }

        private Rectangle2D buildFireField() {
            double width = robot.getBattleFieldWidth();
            double height = robot.getBattleFieldHeight();
            return new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
                    Math.max(50, width - WALL_MARGIN * 2), Math.max(50, height - WALL_MARGIN * 2));
        }

        public void onScannedRobot(ScannedRobotEvent e) {
            Robot enemy = new Robot();
            enemy.absoluteBearingRadians = robot.getHeadingRadians() + e.getBearingRadians();
            enemy.dist = e.getDistance();
            Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
            Point2D enemyLocation = Utility.project(robotLocation, enemy.absoluteBearingRadians, enemy.dist);
            Point2D robotDestination;
            Rectangle2D fireField = buildFireField();
            double tryTime = 0;
            while (!fireField.contains(
                    robotDestination = Utility.project(enemyLocation, enemy.absoluteBearingRadians + Math.PI + direction,
                            enemy.dist * (DEFAULT_EVASION - tryTime / 100.0)))
                    && tryTime < MAX_TRY_TIME) {
                tryTime++;
            }
            if ((Math.random() < (Rules.getBulletSpeed(BULLET_POWER) / REVERSE_TUNER) / enemy.dist
                    || tryTime > (enemy.dist / Rules.getBulletSpeed(BULLET_POWER) / WALL_BOUNCE_TUNER))) {
                direction = -direction;
            }
            double angle = Utility.absoluteBearing(robotLocation, robotDestination) - robot.getHeadingRadians();
            robot.setAhead(Math.cos(angle) * 110);
            robot.setTurnRightRadians(Math.tan(angle));
        }
    }

    static class Wave extends Condition {
        Point2D targetLocation;
        double bulletPower;
        Point2D gunLocation;
        double bearing;
        double lateralDirection;
        private static final double MAX_DISTANCE = 900;
        private static final int DISTANCE_INDEXES = 5;
        private static final int VELOCITY_INDEXES = 5;
        private static final int BINS = 27;
        private static final int MIDDLE_BIN = (BINS - 1) / 2;
        private static final double MAX_ESCAPE_ANGLE = 0.7;
        private static final double BIN_WIDTH = MAX_ESCAPE_ANGLE / (double) MIDDLE_BIN;
        private static final int[][][][] statBuffers = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][BINS];
        private int[] buffer;
        private double distanceTraveled;
        private final TeamRobot robot;

        Wave(TeamRobot _robot) {
            this.robot = _robot;
        }

        public boolean test() {
            advance();
            if (hasArrived()) {
                buffer[currentBin()]++;
                robot.removeCustomEvent(this);
            }
            return false;
        }

        double mostVisitedBearingOffset() {
            return (lateralDirection * BIN_WIDTH) * (mostVisitedBin() - MIDDLE_BIN);
        }

        void setSegmentations(double distance, double velocity, double lastVelocity) {
            int distanceIndex = (int) (distance / (MAX_DISTANCE / DISTANCE_INDEXES));
            int velocityIndex = (int) Math.abs(velocity / 2);
            int lastVelocityIndex = (int) Math.abs(lastVelocity / 2);
            distanceIndex = (int) Utility.clamp(distanceIndex, 0, DISTANCE_INDEXES - 1);
            velocityIndex = (int) Utility.clamp(velocityIndex, 0, VELOCITY_INDEXES - 1);
            lastVelocityIndex = (int) Utility.clamp(lastVelocityIndex, 0, VELOCITY_INDEXES - 1);
            buffer = statBuffers[distanceIndex][velocityIndex][lastVelocityIndex];
        }

        private void advance() {
            distanceTraveled += Rules.getBulletSpeed(bulletPower);
        }

        private boolean hasArrived() {
            return distanceTraveled > gunLocation.distance(targetLocation) - WALL_MARGIN;
        }

        private int currentBin() {
            int bin = (int) Math.round(((Utils.normalRelativeAngle
                    (SampleStudentBot.Utility.absoluteBearing(gunLocation, targetLocation) - bearing)) /
                    (lateralDirection * BIN_WIDTH)) + MIDDLE_BIN);
            return (int) Utility.clamp(bin, 0, BINS - 1);
        }

        private int mostVisitedBin() {
            int mostVisited = MIDDLE_BIN;
            for (int i = 0; i < BINS; i++) {
                if (buffer[i] > buffer[mostVisited]) {
                    mostVisited = i;
                }
            }
            return mostVisited;
        }
    }

    static Random random = new Random();

    private static final Color TEAM_COLOR = Color.black;
    private boolean flashToggle;

    private boolean isFriendlyScan(ScannedRobotEvent event) {
        // Teammates are painted black; skip any scan that resolves to a teammate to avoid friendly fire.
        return isTeammate(event.getName());
    }

    private void changeColor() {
        Color on = flashToggle ? Color.white : Color.black;
        Color off = flashToggle ? Color.black : Color.white;
        flashToggle = !flashToggle;
        setColors(on, off, TEAM_COLOR, TEAM_COLOR, TEAM_COLOR);
    }

    public void onWin(WinEvent event) {
        while (true) {
            changeColor();
            turnRadarRight(360);
        }
    }

    static final int AMOUNT_PREDICTED_POINTS = 150;
    static final double WALL_MARGIN = 18;
    HashMap<String, Robot> enemyList = new HashMap<>();
    Robot myRobot = new Robot();
    Robot targetBot;
    List<Point2D.Double> possibleLocations = new ArrayList<>();
    Point2D.Double targetPoint = new Point2D.Double(60, 60);
    Rectangle2D.Double battleField = new Rectangle2D.Double();
    int idleTime = 24;
    private static double lateralDirection;
    private static double preEnemyVelocity;
    private static Movement1v1 movement1v1;

    {
        movement1v1 = new Movement1v1(this);
    }

    public void run() {
        battleField.height = getBattleFieldHeight();
        battleField.width = getBattleFieldWidth();
        myRobot.x = getX();
        myRobot.y = getY();
        myRobot.energy = getEnergy();
        targetPoint.x = myRobot.x;
        targetPoint.y = myRobot.y;
        targetBot = new Robot();
        targetBot.alive = false;
        changeColor();
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        if (getOthers() > 1) {
            updateListLocations(AMOUNT_PREDICTED_POINTS);
            setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            while (true) {
                myRobot.lastHeading = myRobot.heading;
                myRobot.heading = getHeadingRadians();
                myRobot.x = getX();
                myRobot.y = getY();
                myRobot.energy = getEnergy();
                myRobot.gunHeadingRadians = getGunHeadingRadians();
                Iterator<Robot> enemiesIterator = enemyList.values().iterator();
                while (enemiesIterator.hasNext()) {
                    Robot r = enemiesIterator.next();
                    if (getTime() - r.scanTime > 25) {
                        r.alive = false;
                        if (targetBot.name != null && r.name.equals(targetBot.name)) {
                            targetBot.alive = false;
                        }
                    }
                }
                movement();
                if (targetBot.alive) {
                    shooting();
                }
                if (getRadarTurnRemainingRadians() == 0) {
                    setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
                }
                execute();
            }
        } else {
            lateralDirection = 1;
            preEnemyVelocity = 0;
            while (true) {
                turnRadarRightRadians(Double.POSITIVE_INFINITY);
            }
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        changeColor();
        if (isFriendlyScan(e)) {
            if (getRadarTurnRemainingRadians() == 0) {
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            }
            return;
        }
        if (getOthers() > 1) {
            Robot en = enemyList.get(e.getName());
            if (en == null) {
                en = new Robot();
                enemyList.put(e.getName(), en);
            }
            changeColor();
            en.absoluteBearingRadians = e.getBearingRadians();
            en.setLocation(new Point2D.Double(
                    myRobot.x + e.getDistance() * Math.sin(getHeadingRadians() + en.absoluteBearingRadians),
                    myRobot.y + e.getDistance() * Math.cos(getHeadingRadians() + en.absoluteBearingRadians)));
            en.lastHeading = en.heading;
            en.name = e.getName();
            en.energy = e.getEnergy();
            en.alive = true;
            en.scanTime = getTime();
            en.velocity = e.getVelocity();
            en.heading = e.getHeadingRadians();
            en.shootAbleScore = en.energy < 25 ? (en.energy < 5 ?
                    (en.energy == 0 ? Double.MIN_VALUE : en.distance(myRobot) * 0.1) :
                    en.distance(myRobot) * 0.65) : en.distance(myRobot);
            if (getOthers() == 1) {
                setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
            }
            if (!targetBot.alive || en.shootAbleScore < targetBot.shootAbleScore) {
                targetBot = en;
            }
        } else {
            changeColor();
            Robot enemy = new Robot();
            enemy.absoluteBearingRadians = getHeadingRadians() + e.getBearingRadians();
            enemy.dist = e.getDistance();
            enemy.velocity = e.getVelocity();
            if (enemy.velocity != 0) {
                lateralDirection = Utility.sign(enemy.velocity * Math.sin(e.getHeadingRadians() - enemy.absoluteBearingRadians));
            }
            Wave wave = new Wave(this);
            wave.gunLocation = new Point2D.Double(getX(), getY());
            wave.targetLocation = Utility.project(wave.gunLocation, enemy.absoluteBearingRadians, enemy.dist);
            wave.lateralDirection = lateralDirection;
            wave.setSegmentations(enemy.dist, enemy.velocity, preEnemyVelocity);
            preEnemyVelocity = enemy.velocity;
            wave.bearing = enemy.absoluteBearingRadians;
            setTurnGunRightRadians(Utils.normalRelativeAngle(
                    enemy.absoluteBearingRadians - getGunHeadingRadians() + wave.mostVisitedBearingOffset()));
            BULLET_POWER = Math.min(3, Math.min(this.getEnergy(), e.getEnergy()) / 3.8);
            double firePower = BULLET_POWER;
            if (getEnergy() < 15) {
                firePower = Utility.clamp(firePower, 0.1, 1.5);
            }
            if (getEnergy() < 2.2 && e.getDistance() < 500) {
                firePower = 0.15;
            } else if (e.getDistance() >= 500) {
                firePower = Math.min(firePower, 1.2);
            }
            firePower = Math.min(firePower, Math.max(0.1, getEnergy() - 0.4));
            wave.bulletPower = firePower;
            if (firePower > 0.1 && getEnergy() > firePower) {
                addCustomEvent(wave);
                setFire(firePower);
            }
            movement1v1.onScannedRobot(e);
            setTurnRadarRightRadians(Utils.normalRelativeAngle(enemy.absoluteBearingRadians - getRadarHeadingRadians()) * 2);
        }
    }

    public void onRobotDeath(RobotDeathEvent event) {
        if (enemyList.containsKey(event.getName())) {
            enemyList.get(event.getName()).alive = false;
        }
        if (event.getName().equals(targetBot.name)) {
            targetBot.alive = false;
        }
    }

    private double selectMeleePower(double distance, Robot target) {
        double distanceFactor = Utility.clamp(500 / (distance + 200), 0.25, 2.4);
        double enemyFactor = target != null ? Utility.clamp(target.energy / 3d, 0.15, 2.4) : distanceFactor;
        double survivalCap = myRobot.energy < 12 ? 0.35 : myRobot.energy < 25 ? 1.25 : 3.0;
        return Utility.clamp(Math.min(distanceFactor, Math.min(enemyFactor + 0.2, survivalCap)), 0.1, 3.0);
    }

    private boolean hasEnergyForShot(double power) {
        return myRobot.energy - power > 0.2;
    }

    public void shooting() {
        if (targetBot != null && targetBot.alive) {
            double dist = myRobot.distance(targetBot);
            double power = selectMeleePower(dist, targetBot);
            if (!hasEnergyForShot(power)) {
                return;
            }
            long deltaHitTime;
            Point2D.Double shootAt = new Point2D.Double();
            double head, deltaHead, bulletSpeed;
            double predictX, predictY;
            predictX = targetBot.getX();
            predictY = targetBot.getY();
            head = targetBot.heading;
            deltaHead = head - targetBot.lastHeading;
            shootAt.setLocation(predictX, predictY);
            deltaHitTime = 0;
            do {
                predictX += Math.sin(head) * targetBot.velocity;
                predictY += Math.cos(head) * targetBot.velocity;
                head += deltaHead;
                deltaHitTime++;
                Rectangle2D.Double fireField = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
                        battleField.width - WALL_MARGIN, battleField.height - WALL_MARGIN);
                if (!fireField.contains(predictX, predictY)) {
                    bulletSpeed = shootAt.distance(myRobot) / deltaHitTime;
                    power = Utility.clamp((20 - bulletSpeed) / 3.0, 0.1, 3.0);
                    break;
                }
                shootAt.setLocation(predictX, predictY);
            } while ((int) Math.round((shootAt.distance(myRobot) - WALL_MARGIN) / Rules.getBulletSpeed(power)) > deltaHitTime);
            shootAt.setLocation(Utility.clamp(predictX, 34, getBattleFieldWidth() - 34),
                    Utility.clamp(predictY, 34, getBattleFieldHeight() - 34));
            power = Utility.clamp(power, 0.1, 3.0);
            if ((getGunHeat() == 0.0) && (Math.abs(getGunTurnRemaining()) < 5.0) && (power > 0.0) && hasEnergyForShot(power)) {
                setFire(power);
            }
            setTurnGunRightRadians(Utils.normalRelativeAngle(((Math.PI / 2) - Math.atan2(shootAt.y - myRobot.getY(),
                    shootAt.x - myRobot.getX())) - getGunHeadingRadians()));
        }
    }

    public void movement() {
        if (targetPoint.distance(myRobot) < 15 || idleTime > 25) {
            idleTime = 0;
            updateListLocations(AMOUNT_PREDICTED_POINTS);
            Point2D.Double lowRiskP = null;
            double lowestRisk = Double.MAX_VALUE;
            for (Point2D.Double p : possibleLocations) {
                double currentRisk = evaluatePoint(p);
                if (currentRisk <= lowestRisk || lowRiskP == null) {
                    lowestRisk = currentRisk;
                    lowRiskP = p;
                }
            }
            targetPoint = lowRiskP;
        } else {
            idleTime++;
            double angle = Utility.absoluteBearing(myRobot, targetPoint) - getHeadingRadians();
            double direction = 1;
            if (Math.cos(angle) < 0) {
                angle += Math.PI;
                direction *= -1;
            }
            setMaxVelocity(10 - (4 * Math.abs(getTurnRemainingRadians())));
            setAhead(myRobot.distance(targetPoint) * direction);
            angle = Utils.normalRelativeAngle(angle);
            setTurnRightRadians(angle);
        }
    }

    public void updateListLocations(int n) {
        possibleLocations.clear();
        final int xRange = (int) (125 * 1.5);
        for (int i = 0; i < n; i++) {
            double randXMod = Utility.randomBetween(-xRange, xRange);
            double yRange = Math.sqrt(xRange * xRange - randXMod * randXMod);
            double randYMod = Utility.randomBetween(-yRange, yRange);
            double y = Utility.clamp(myRobot.y + randYMod, 75, battleField.height - 75);
            double x = Utility.clamp(myRobot.x + randXMod, 75, battleField.width - 75);
            possibleLocations.add(new Point2D.Double(x, y));
        }
    }

    public double evaluatePoint(Point2D.Double p) {
        double rickValue = Utility.randomBetween(1, 2.25) / p.distanceSq(myRobot);
        rickValue += (6 * (getOthers() - 1)) / p.distanceSq(battleField.width / 2, battleField.height / 2);
        double cornerFactor = getOthers() <= 5 ? getOthers() == 1 ? 0.25 : 0.5 : 1;
        rickValue += cornerFactor / p.distanceSq(0, 0);
        rickValue += cornerFactor / p.distanceSq(battleField.width, 0);
        rickValue += cornerFactor / p.distanceSq(0, battleField.height);
        rickValue += cornerFactor / p.distanceSq(battleField.width, battleField.height);
        if (targetBot.alive) {
            double robotAngle = Utils.normalRelativeAngle(Utility.absoluteBearing(p, targetBot) - Utility.absoluteBearing(myRobot, p));
            Iterator<Robot> enemiesIterator = enemyList.values().iterator();
            while (enemiesIterator.hasNext()) {
                Robot en = enemiesIterator.next();
                rickValue += (en.energy / myRobot.energy) * (1 / p.distanceSq(en)) * (1.0 + ((1 - (Math.abs(Math.sin(robotAngle)))) +
                        Math.abs(Math.cos(robotAngle))) / 2) * (1 + Math.abs(Math.cos(Utility.absoluteBearing(myRobot, p) - Utility.absoluteBearing(en, p))));
            }
        } else if (enemyList.values().size() >= 1) {
            Iterator<Robot> enIterator = enemyList.values().iterator();
            while (enIterator.hasNext()) {
                Robot en = enIterator.next();
                rickValue += (en.energy / myRobot.energy) * (1 / p.distanceSq(en)) * (1 + Math.abs(Math.cos(Utility.absoluteBearing(myRobot, p) - Utility.absoluteBearing(en, p))));
            }
        } else {
            rickValue += (1 + Math.abs(Utility.absoluteBearing(myRobot, targetPoint) - getHeadingRadians()));
        }
        return rickValue;
    }
}
