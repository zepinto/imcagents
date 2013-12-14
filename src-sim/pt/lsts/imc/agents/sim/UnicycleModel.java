package pt.lsts.imc.agents.sim;

import pt.lsts.imc.agents.coords.Location;
import pt.lsts.imc.agents.coords.Pose;

/**
 * This class implements the Unicycle Model dynamics to be used for a rough preview 
 * of vehicle's behavior 
 * @author zp
 *
 */
public class UnicycleModel {

	protected double latRad, lonRad, x, y, rollRad, pitchRad, yawRad, depth, speedMPS;
	protected double targetLatRad, targetLonRad, maxSteeringRad = Math.toRadians(7);
	protected boolean arrived = true;


	public Location getCurrentPosition() {
		Location loc = new Location(Math.toDegrees(latRad), Math.toDegrees(lonRad));
		loc.setDepth(depth);
		loc.translate(x, y, 0);
		return loc;
	}

	public Pose getState() {
		return new Pose(getCurrentPosition(), Math.toRadians(rollRad), Math.toRadians(pitchRad), Math.toRadians(yawRad));        
	}

	public void setState(Pose state) {
		if (state == null) {            
			return;
		}

		Location pos = state.getLocation();
		latRad = pos.getLatRads();
		lonRad = pos.getLonRads();
		x = y = 0;
		depth = pos.getDepth();
		rollRad = state.getRollRads();
		pitchRad = state.getPitchRads();
		yawRad = state.getYawRads();        
	}

	/**
	 * Reset the state of the vehicle to be at the given position
	 * @param loc The new vehicle location. New heading will be calculated as a jump from previous location
	 */
	public void setLocation(Location loc) {
		Location old = getCurrentPosition();
		latRad = loc.getLatRads();
		lonRad = loc.getLonRads();
		depth = loc.getDepth();
		x = y = 0;
		pitchRad = rollRad = 0;
		yawRad = old.angleTo(loc);
	}

	/**
	 * Advance the given time by integrating the vehicle position (with current heading and speed)
	 * @param timestepSecs
	 */
	public void advance(double timestepSecs) {
		double angle = yawRad;
		x += speedMPS * timestepSecs * Math.sin(angle);
		y += speedMPS * timestepSecs * Math.cos(angle);
		depth += speedMPS * timestepSecs * Math.sin(pitchRad);

		if (depth > 0)
			depth -= 0.05* timestepSecs;

	}

	/**
	 * Guide the vehicle to a certain location. This method will compute a new heading and speed that will guide the vehicle to the given location.<br/>
	 * If the vehicle is already at or near the target location, the speed is set to 0 and the method returns <b>true</b>.  
	 * @param loc The target location
	 * @param speed Desired speed
	 * @return <b>true</b> if the vehicle is arrived
	 */
	public boolean guide(Location loc, double speed) {
		if (loc.getHorizontalDistance(getCurrentPosition()) < speed) {
			speedMPS = rollRad = pitchRad = 0;
			return true;            
		}            

		speedMPS = speed;
		if (loc.getDepth() > depth+0.1)
			pitchRad = Math.toRadians(12);
		else if (loc.getDepth() < depth-0.1)
			pitchRad = -Math.toRadians(12);
		else {
			depth = loc.getDepth();
			pitchRad = 0;
		}

		double ang = getCurrentPosition().angleTo(loc);

		double diffAng = yawRad - ang;

		while (diffAng > Math.PI)
			diffAng -= Math.PI * 2;
		while (diffAng < -Math.PI)
			diffAng += Math.PI * 2;

		if (Math.abs(diffAng) < maxSteeringRad)
			yawRad = ang;
		else if (diffAng > 0)
			yawRad -= maxSteeringRad;
		else
			yawRad += maxSteeringRad;

		return false;    
	}

	/**
	 * @return the latRad
	 */
	public double getLatRad() {
		return latRad;
	}

	/**
	 * @param latRad the latRad to set
	 */
	public void setLatRad(double latRad) {
		this.latRad = latRad;
	}

	/**
	 * @return the lonRad
	 */
	public double getLonRad() {
		return lonRad;
	}

	/**
	 * @param lonRad the lonRad to set
	 */
	public void setLonRad(double lonRad) {
		this.lonRad = lonRad;
	}

	/**
	 * @return the x
	 */
	public double getX() {
		return x;
	}

	/**
	 * @param x the x to set
	 */
	public void setX(double x) {
		this.x = x;
	}

	/**
	 * @return the y
	 */
	public double getY() {
		return y;
	}

	/**
	 * @param y the y to set
	 */
	public void setY(double y) {
		this.y = y;
	}

	/**
	 * @return the depth
	 */
	public double getDepth() {
		return depth;
	}

	/**
	 * @param depth the depth to set
	 */
	public void setDepth(double depth) {
		this.depth = depth;
	}

	/**
	 * @return the rollRad
	 */
	public double getRollRad() {
		return rollRad;
	}

	/**
	 * @param rollRad the rollRad to set
	 */
	public void setRollRad(double rollRad) {
		this.rollRad = rollRad;
	}

	/**
	 * @return the pitchRad
	 */
	public double getPitchRad() {
		return pitchRad;
	}

	/**
	 * @param pitchRad the pitchRad to set
	 */
	public void setPitchRad(double pitchRad) {
		this.pitchRad = pitchRad;
	}

	/**
	 * @return the yawRad
	 */
	public double getYawRad() {
		return yawRad;
	}

	/**
	 * @param yawRad the yawRad to set
	 */
	public void setYawRad(double yawRad) {
		this.yawRad = yawRad;
	}

	/**
	 * @return the maxSteeringRad
	 */
	public double getMaxSteeringRad() {
		return maxSteeringRad;
	}

	/**
	 * @param maxSteeringRad the maxSteeringRad to set
	 */
	public void setMaxSteeringRad(double maxSteeringRad) {
		this.maxSteeringRad = maxSteeringRad;
	}
}
