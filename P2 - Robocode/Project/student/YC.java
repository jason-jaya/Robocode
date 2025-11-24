package student;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;

//simple team bot: strafes, avoids edges, predicts shots
public class YC extends TeamRobot {
	//movement flags
	private boolean movingForward = true;
	private boolean nearBoundary = false;
	//enemy energy snapshot
	private double lastEnemyEnergy = 100;
	//edge margin
	private static final double EDGE_MARGIN = 80;
	//strafe intervals
	private static final int FORWARD_INTERVAL = 36;
	private static final int REVERSE_INTERVAL = 28;

	private String currentTarget = null;

	@Override
	public void run() {
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		setBodyColor(Color.YELLOW);
		setGunColor(Color.YELLOW);
		setRadarColor(Color.YELLOW);
		setBulletColor(Color.YELLOW);

		setAhead(5000);
		movingForward = true;
		setTurnRadarRight(Double.POSITIVE_INFINITY);

		while (true) {
			checkEdges();

			int period = movingForward ? FORWARD_INTERVAL : REVERSE_INTERVAL;
			if (getTime() % period == 0) {
				toggleDirection();
			}

			if (getRadarTurnRemaining() == 0) {
				setTurnRadarRight(Double.POSITIVE_INFINITY);
			}
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		//skip teammates
		if (isTeammate(e.getName())) {
    		return; 
		}
		
		//
		if (currentTarget == null)
    		currentTarget = e.getName();

		if (!e.getName().equals(currentTarget)) {
    		return; // Ignore all other robots
		}
		//pick bullet power by distance
		double dist = e.getDistance();
		double power = dist > 350 ? 1 : (dist > 120 ? 2 : 3);

		//predictive aiming
		double absBearing = getHeadingRadians() + e.getBearingRadians();
		double bulletSpeed = 20 - 3 * power;
		double lateral = e.getVelocity() * Math.sin(e.getHeadingRadians() - absBearing);
		double gunTurn = Utils.normalRelativeAngle((absBearing - getGunHeadingRadians()) + (lateral / bulletSpeed));
		setTurnGunRightRadians(gunTurn);

		//lock radar
		setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()));

		//fire if cool and enough energy
		if (getGunHeat() == 0 && getEnergy() > power + 0.1) {
			setFire(power);
		}

		//perpendicular circling
		double strafe = movingForward ? 1.35 : 1.6; // ~77deg / 92deg
		setTurnRightRadians(Utils.normalRelativeAngle(e.getBearingRadians() + strafe));

		//close evade
		if (dist < 110) {
			if (Math.abs(e.getBearingRadians()) < Math.PI / 2) setBack(140); else setAhead(140);
		}

		//dodge on enemy energy drop (likely fired)
		double drop = lastEnemyEnergy - e.getEnergy();
		if (drop > 0 && drop <= 3) toggleDirection();
		lastEnemyEnergy = e.getEnergy();

		//rescan when aligned
		if (Math.abs(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians())) < 0.01) scan();
	}

	@Override
	public void onHitWall(HitWallEvent e) {
		toggleDirection();
	}

	@Override
	public void onHitRobot(HitRobotEvent e) {
		if (e.isMyFault()) toggleDirection();
	}

	private void toggleDirection() {
		if (movingForward) { setBack(5000); } else { setAhead(5000); }
		movingForward = !movingForward;
	}

	//edge detection flips direction once when entering margin
	private void checkEdges() {
		boolean close = getX() < EDGE_MARGIN || getY() < EDGE_MARGIN ||
					getBattleFieldWidth() - getX() < EDGE_MARGIN ||
					getBattleFieldHeight() - getY() < EDGE_MARGIN;
		if (close && !nearBoundary) {
			toggleDirection();
			nearBoundary = true;
		} else if (!close && nearBoundary) {
			nearBoundary = false;
		}
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
