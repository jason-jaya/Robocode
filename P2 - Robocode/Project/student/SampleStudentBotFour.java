package student;

import robocode.*;
import java.awt.Color;

public class SampleStudentBotFour extends TeamRobot
{
    enum Mode { WALL, RUN, RECOVER }
    Mode mode = Mode.WALL;

    boolean reverseMode = false;

	public void run() {

        setBodyColor(Color.yellow);
		setGunColor(Color.yellow);
		setRadarColor(Color.yellow);
		setBulletColor(Color.yellow);
		setScanColor(Color.yellow);

        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setTurnRadarRight(Double.POSITIVE_INFINITY);
        
        setAdjustGunForRobotTurn(true);

        findWall();
        
        while (true) {
            if (mode == Mode.WALL) {
                wallMode();
            }
            else if (mode == Mode.RUN) {
                // only run runAway once per event
                execute();
            }
            else if (mode == Mode.RECOVER) {
                // wait until movement finishes
                execute();
                if (Math.abs(getDistanceRemaining()) < 1 &&
                    Math.abs(getTurnRemaining()) < 1) 
                {
                    mode = Mode.WALL;
                }
            }
        }
	}

    public void wallMode(){
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
        }
        execute();
    }

    public void runAway(double bearing) {

        double dodgeAngle = (bearing > 0) ? -90 : 90;

        setTurnRight(dodgeAngle);
        setAhead(200);
        execute();

        while (Math.abs(getDistanceRemaining()) > 1 ||
            Math.abs(getTurnRemaining()) > 1) {
            execute();
        }

        setTurnRight(45);
        setAhead(120);

        while (Math.abs(getDistanceRemaining()) > 1 ||
            Math.abs(getTurnRemaining()) > 1) {
            execute();
        }

        reverseMode = !reverseMode;

        findWall();

        mode = Mode.WALL;
    }




    public void findWall(){
        turnLeft(getHeading() % 90);

        double heading = getHeading();
        double x = getX();
        double y = getY();
        double w = getBattleFieldWidth();
        double h = getBattleFieldHeight();
        double wallMargin = 30; 

        // North
        if (heading > 355 || heading < 5) {
            setAhead(h - y - wallMargin);
            out.println("move north " + (h - y - wallMargin));
        }
        // East
        else if (heading > 85 && heading < 95) {
            setAhead(w - x - wallMargin);
            out.println("move east " + (w - x - wallMargin));
        }
        // South
        else if (heading > 175 && heading < 185) {
            setAhead(y - wallMargin);
            out.println("move south " + (y - wallMargin));
        }
        // West
        else if (heading > 265 && heading < 275) {
            setAhead(x - wallMargin);
            out.println("move west " + (x - wallMargin));
        }

        turnGunRight(90);
		turnRight(90);
    }

	public void onScannedRobot(ScannedRobotEvent e) {
		double radarTurn =
        getHeading() + e.getBearing() - getRadarHeading();
    
        while (radarTurn > 180) radarTurn -= 360;
        while (radarTurn < -180) radarTurn += 360;

        setTurnRadarRight(radarTurn + (radarTurn > 0 ? 10 : -10));

        double absoluteBearing = getHeading() + e.getBearing();

        double gunTurn = absoluteBearing - getGunHeading();

        while (gunTurn > 180) gunTurn -= 360;
        while (gunTurn < -180) gunTurn += 360;

        setTurnGunRight(gunTurn);

        double distance = e.getDistance();
        double power;

        if (distance < 150)
            power = 3;
        else if (distance < 300)
            power = 2;
        else
            power = 1.2;

        if (getGunHeat() == 0 && Math.abs(gunTurn) < 10) {
            fire(power);
        }
	}

    public void onHitRobot(HitRobotEvent e) {
		mode = Mode.RUN;
        runAway(e.getBearing());
	}

    public void onHitByBullet(HitByBulletEvent e) {
        mode = Mode.RUN;
        runAway(e.getBearing());
    }

    public void onHitWall(HitWallEvent e) {
        mode = Mode.RECOVER;
        setBack(40);
        setTurnRight(90);
        setAhead(200);
    }

}