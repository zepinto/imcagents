package pt.lsts.imc.agents.coords;

public class Pose {

	private Location location = new Location(0, 0);
	private double rollDegs, pitchDegs, yawDegs;
	
	public Pose() {
		this.rollDegs = pitchDegs = yawDegs = 0;
	}
	
	public Pose(Location loc) {
		this();
		this.location = new Location(loc);
	}
	
	public Pose(Location loc, double rollDegs, double pitchDegs, double yawDegs) {
		this(loc);
		this.rollDegs = rollDegs;
		this.pitchDegs = pitchDegs;
		this.yawDegs = yawDegs;
	}
	
	public Location getLocation() {
		return location;
	}
	public void setLocation(Location location) {
		this.location = location;
	}
	public double getRollDegs() {
		return rollDegs;
	}
	public void setRollDegs(double rollDegs) {
		this.rollDegs = rollDegs;
	}
	public double getPitchDegs() {
		return pitchDegs;
	}
	public void setPitchDegs(double pitchDegs) {
		this.pitchDegs = pitchDegs;
	}
	public double getYawDegs() {
		return yawDegs;
	}
	public void setYawDegs(double yawDegs) {
		this.yawDegs = yawDegs;
	}
	
	public void setYawRads(double yawRads) {
		this.yawDegs = Math.toDegrees(yawRads);
	}
	
	public void setRollRads(double rollRads) {
		this.rollDegs = Math.toDegrees(rollRads);
	}
	
	public void setPitchRads(double pitchRads) {
		this.pitchDegs = Math.toDegrees(pitchRads);
	}
	
	public double getYawRads() {
		return Math.toRadians(yawDegs);
	}
		
	public double getRollRads() {
		return Math.toRadians(rollDegs);
	}
	
	public double getPitchRads() {
		return Math.toRadians(pitchDegs);
	}
	
}
