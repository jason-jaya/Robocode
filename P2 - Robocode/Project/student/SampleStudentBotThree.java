package student;

import robocode.*;
import robocode.util.*;
import java.awt.geom.Point2D;
import java.util.*;

public class SampleStudentBotThree extends TeamRobot {

    private double previousEnemyEnergy = 100;
	private boolean moveForward = true;

    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

		while (true) {
			// Your main robot logic here
			setTurnRadarRight(Double.POSITIVE_INFINITY);
			checkWalls();
			execute();
		}
	}

	public double normalize(double angle) {
		while (angle > 180) angle -= 360;
		while (angle < -180) angle += 360;
		return angle;
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		double bulletPower = Math.min(3.0,getEnergy());
		double myX = getX();
		double myY = getY();
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyVelocity = e.getVelocity();


		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), 
			battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while((++deltaTime) * (20.0 - 3.0 * bulletPower) < 
			Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
			predictedX += Math.sin(enemyHeading) * enemyVelocity;	
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			if(	predictedX < 18.0 
				|| predictedY < 18.0
				|| predictedX > battleFieldWidth - 18.0
				|| predictedY > battleFieldHeight - 18.0){
				predictedX = Math.min(Math.max(18.0, predictedX), 
							battleFieldWidth - 18.0);	
				predictedY = Math.min(Math.max(18.0, predictedY), 
							battleFieldHeight - 18.0);
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(
			predictedX - getX(), predictedY - getY()));

		setTurnRadarRightRadians(
			Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
		setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
		fire(bulletPower);


		double enemyEnergy = e.getEnergy();  
    	System.out.println("Enemy energy: " + enemyEnergy);
		if (enemyEnergy != previousEnemyEnergy) {
			double change = previousEnemyEnergy - enemyEnergy;
			if (change != 0) { 
				System.out.println("change detected: " + change);
				double dodgeAngle = Math.random() < 0.5 ? 90 : -90; 
				setTurnRight(normalize(e.getBearing() + dodgeAngle));
				setAhead(moveForward ? 100 : -100);
				System.out.println("Dodging queued");
				moveForward = !moveForward;
			}
		previousEnemyEnergy = enemyEnergy;
		}
	}

	private void checkWalls() {
        double margin = 100; // distance from wall
        if (getX() < margin) {
            setTurnRight(90);
            setAhead(50);
        } else if (getX() > getBattleFieldWidth() - margin) {
            setTurnRight(90);
            setAhead(50);
        }
        if (getY() < margin) {
            setTurnRight(90);
            setAhead(50);
        } else if (getY() > getBattleFieldHeight() - margin) {
            setTurnRight(90);
            setAhead(50);
        }
    }

	
}