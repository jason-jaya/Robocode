package student;

import robocode.*;
import robocode.util.*;
import java.awt.Color;
import java.awt.geom.Point2D;

//Frank Zhang
//TUID: 916158049
//Robocode Project Code

public class FZ extends TeamRobot {

    private double previousEnemyEnergy = 100;
	private boolean moveForward = true;

    public void run() {
		setBodyColor(Color.YELLOW);
		setGunColor(Color.YELLOW);
		setRadarColor(Color.YELLOW);
		setBulletColor(Color.YELLOW);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

		while (true) {
			setTurnRadarRight(Double.POSITIVE_INFINITY);
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		// if ((e.getName().equals("student.AsianRobotTwo*") ||
        //     e.getName().equals("student.AsianRobotFour*")) ||
        //     e.getName().equals("student.AsianRobotThree*")) {
		// 	return; 
		// }
		if (isTeammate(e.getName())) {
    		return;  // Skip shooting teammate
		}
		
		String targetName = e.getName();
		try {
			broadcastMessage("TARGET:" + targetName);
		} catch (Exception ex) {
			System.out.println("Broadcast failed (no teammates?)");
		}
		System.out.println("Targeting: " + targetName);

		double distance = e.getDistance();
		double bulletPower; 
		if (getEnergy() < 20) {
			bulletPower = 1;  
		} else if (distance < 200) {
			bulletPower = 3;  
		} else if (distance < 400) {
			bulletPower = 2;  
		} else {
			bulletPower = 1; 
		}
		double myX = getX();
		double myY = getY();
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyVelocity = e.getVelocity();
		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
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
		double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
		setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
		setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
		fire(bulletPower);
		System.out.println(bulletPower);

		double enemyEnergy = e.getEnergy();
		double drop = previousEnemyEnergy - enemyEnergy;
		if (nearWall()) {
			setTurnRightRadians(Utils.normalRelativeAngle(Math.atan2(getBattleFieldWidth()/2 - getX(),getBattleFieldHeight()/2 - getY()) - getHeadingRadians()));
			setAhead(120);
		} else {
			// Regular perpendicular strafing
			double perpendicular = e.getBearingRadians() + Math.PI/2;
			setTurnRightRadians(perpendicular);
			// Reverse direction only when bullet fired
			if (drop > 0 && drop <= 3.0) {
				moveForward = !moveForward;
			}
			setAhead(moveForward ? 150 : -150);
		}
		previousEnemyEnergy = enemyEnergy;
	}

	private boolean nearWall() {
		double margin = 120;
		return (getX() < margin ||
				getX() > getBattleFieldWidth() - margin ||
				getY() < margin ||
				getY() > getBattleFieldHeight() - margin);
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