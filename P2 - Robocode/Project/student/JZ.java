package student;

import robocode.*;
import java.awt.Color;

public class JZ extends TeamRobot {

    enum Mode { WALL, REVERSE_WALL, RECOVER }
    Mode mode = Mode.WALL;

    int stuckCounter = 0;
    int sideHitCount = 0;
    long lastSideHitTime = 0;


    private String currentTarget = null;

    public void run() {

        setBodyColor(Color.yellow);
        setGunColor(Color.yellow);
        setRadarColor(Color.yellow);
        setBulletColor(Color.yellow);
        setScanColor(Color.yellow);

        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        setTurnRadarRight(Double.POSITIVE_INFINITY);

        findWall();

        while (true) {
            if (getRadarTurnRemaining() == 0) {
                setTurnRadarRight(Double.POSITIVE_INFINITY);
            }
            if (mode == Mode.WALL) {
                wallMode();
            }
            else if (mode == Mode.REVERSE_WALL) {
                reverseWallMode();
            }
            else if (mode == Mode.RECOVER) {
                execute();

                if (Math.abs(getDistanceRemaining()) < 1 &&
                    Math.abs(getTurnRemaining()) < 1) {
                    if (modeBeforeRecover == Mode.REVERSE_WALL) {
                        mode = Mode.REVERSE_WALL;
                    } else {
                        mode = Mode.WALL;
                    }
                }
            }
        }
    }

    Mode modeBeforeRecover = Mode.WALL;

    public void wallMode() {
        double wallMargin = 30;
        double heading = getHeading();
        double x = getX();
        double y = getY();
        double w = getBattleFieldWidth();
        double h = getBattleFieldHeight();
        // North
        if (heading < 20 || heading > 340) {
            setAhead(h - y - wallMargin);
        }
        // East
        else if (heading > 70 && heading < 110) {
            setAhead(w - x - wallMargin);
        }
        // South
        else if (heading > 160 && heading < 200) {
            setAhead(y - wallMargin);
        }
        // West
        else if (heading > 250 && heading < 290) {
            setAhead(x - wallMargin);
        }
        if (Math.abs(getDistanceRemaining()) < 5) {
            turnRight(90);
            stuckCounter = 0;
        } else {
            stuckCounter++;
        }

        if (stuckCounter > 50) {
            setAhead(50);
            execute();
            stuckCounter = 0;
        }
        execute();
    }

    public void reverseWallMode() {

        double wallMargin = 30;
        double heading = getHeading();
        double x = getX();
        double y = getY();
        double w = getBattleFieldWidth();
        double h = getBattleFieldHeight();

        double dist;

        // North
        if (heading < 20 || heading > 340) {
            dist = y - wallMargin;
            setBack(dist);
        }
        // East 
        else if (heading > 70 && heading < 110) {
            dist = x - wallMargin;
            setBack(dist);
        }
        // South 
        else if (heading > 160 && heading < 200) {
            dist = h - y - wallMargin; 
            setBack(dist);
        }
        // West
        else if (heading > 250 && heading < 290) {
            dist = w - x - wallMargin; 
            setBack(dist);
        }

        if (Math.abs(getDistanceRemaining()) < 10) {
            turnLeft(90);
            stuckCounter = 0;
        } else {
            stuckCounter++;
        }

        if (stuckCounter > 50) {
            setAhead(50);
            execute();
            stuckCounter = 0;
        }

        execute();
    }

    public void findWall() {

        double x = getX();
        double y = getY();
        double w = getBattleFieldWidth();
        double h = getBattleFieldHeight();
        double margin = 30;
        double heading = getHeading();

        // Distances to each wall
        
        double distTop = h - y - margin;
        double distBottom = y - margin;
        double distRight = w - x - margin;
        double distLeft = x - margin;
        distTop = Math.max(0, distTop);
        distBottom = Math.max(0, distBottom);
        distRight = Math.max(0, distRight);
        distLeft = Math.max(0, distLeft);

        // Find minimum distance
        double minDist = Math.min(Math.min(distTop, distBottom), Math.min(distRight, distLeft));

        // Snap heading to nearest 90Â° before driving
        turnLeft(getHeading() % 90);

        // Move toward the closest wall
        if (minDist == distTop) {
            // TOP wall
            turnTo(0);
            ahead(distTop);
        }
        else if (minDist == distBottom) {
            // BOTTOM wall
            turnTo(180);
            ahead(distBottom);
        }
        else if (minDist == distRight) {
            // RIGHT wall
            turnTo(90);
            ahead(distRight);
        }
        else if (minDist == distLeft) {
            // LEFT wall
            turnTo(270);
            ahead(distLeft);
        }
    }

    public void turnTo(double targetAngle) {
        double turn = targetAngle - getHeading();
        while (turn > 180) turn -= 360;
        while (turn < -180) turn += 360;
        turnRight(turn);
    }

    public void onScannedRobot(ScannedRobotEvent e) {

        if (isTeammate(e.getName())) {
    		return;  // Skip shooting teammate
		}

		if (currentTarget == null)
    		currentTarget = e.getName();

		if (!e.getName().equals(currentTarget)) {
    		return; // Ignore all other robots
		}

        double radarTurn = getHeading() + e.getBearing() - getRadarHeading();
        radarTurn = normalize(radarTurn);
        setTurnRadarRight(radarTurn + (radarTurn > 0 ? 10 : -10));

        double absBearing = getHeading() + e.getBearing();
        double gunTurn = normalize(absBearing - getGunHeading());
        setTurnGunRight(gunTurn);

        double dist = e.getDistance();
        double power = (dist < 150) ? 3 :
                       (dist < 300) ? 2 : 1.2;

        if (getGunHeat() == 0 && Math.abs(gunTurn) < 10)
            fire(power);
    }

    private double normalize(double ang) {
        while (ang > 180) ang -= 360;
        while (ang < -180) ang += 360;
        return ang;
    }

    public void onHitRobot(HitRobotEvent e) {

        double b = e.getBearing();

        if (b > -90 && b < 90) {
            mode = Mode.REVERSE_WALL;
            modeBeforeRecover = Mode.REVERSE_WALL;
        } else {
            mode = Mode.WALL;
            modeBeforeRecover = Mode.WALL;
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
        double b = e.getBearing();
        long time = getTime();

        if (b > -90 && b < 90) {
            mode = Mode.REVERSE_WALL;
            modeBeforeRecover = Mode.REVERSE_WALL;

            sideHitCount = 0;
            return;
        }

        if ((b >= 90 && b <= 135) || (b <= -90 && b >= -135)) {

            if (time - lastSideHitTime > 5) {
                sideHitCount++;
            }
            lastSideHitTime = time;

            if (sideHitCount >= 1) {
                if (mode == Mode.WALL) {
                    mode = Mode.REVERSE_WALL;
                    modeBeforeRecover = Mode.REVERSE_WALL;
                } else {
                    mode = Mode.WALL;
                    modeBeforeRecover = Mode.WALL;
                }

                sideHitCount = 0; 
            }
            return;
        }
        mode = Mode.WALL;
        modeBeforeRecover = Mode.WALL;
        sideHitCount = 0;
    }


    //Receives broadcast function
    public void onMessageReceived(MessageEvent e) {
        Object msg = e.getMessage();
        if (msg instanceof String s && s.startsWith("TARGET:")) {
            String targetName = s.substring(7);
            System.out.println("Now targeting: " + targetName);
            // Store the target for use in onScannedRobot
            this.currentTarget = targetName;
        }
    }

    @Override
    public void onWin(WinEvent e) {
        for (int i = 0; i < 30; i++) {
            setAhead(0);
            turnRight(30);
            turnLeft(30);
            fire(2);
        }
    }

}
