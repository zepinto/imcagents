package pt.lsts.imc.agents.coords;

import pt.lsts.util.WGS84Utilities;

public class Location {

	private double latDegrees, lonDegrees, depth;
	
	public Location(Location other) {
		this.latDegrees = other.latDegrees;
		this.lonDegrees = other.lonDegrees;
	}
	
	public Location(double latDegs, double lonDegs) {
		this.latDegrees = latDegs;
		this.lonDegrees = lonDegs;
	}
	
	public double getHorizontalDistance(Location otherLoc) {
		return WGS84Utilities.distance(latDegrees, lonDegrees, otherLoc.latDegrees, otherLoc.lonDegrees);
	}
	
	public double[] offsetFrom(Location otherLoc) {
		double offsets[] = WGS84Utilities.WGS84displacement(otherLoc.latDegrees, otherLoc.lonDegrees, otherLoc.depth, latDegrees, lonDegrees, depth);
		offsets[2] = getDepth() - otherLoc.getDepth();
		return offsets;
	}
	
	public double angleTo(Location otherLoc) {
		double[] offsets = otherLoc.offsetFrom(this);
		return Math.atan2(offsets[1], offsets[0]);
	}
	
	public void translate(double northing, double easting, double depth) {
		double [] disp = WGS84Utilities.WGS84displace(latDegrees, lonDegrees, this.depth, northing, easting, depth);
		latDegrees = disp[0];
		lonDegrees = disp[1];
		this.depth = disp[2];				
	}

	public double getLatRads() {
		return Math.toRadians(latDegrees);
	}
	
	public double getLatDegrees() {
		return latDegrees;
	}

	public void setLatDegrees(double latDegrees) {
		this.latDegrees = latDegrees;
	}

	public double getLonDegrees() {
		return lonDegrees;
	}
	
	public double getLonRads() {
		return Math.toRadians(lonDegrees);
	}

	public void setLonDegrees(double lonDegrees) {
		this.lonDegrees = lonDegrees;
	}

	public double getDepth() {
		return depth;
	}

	public void setDepth(double depth) {
		this.depth = depth;
	}
	
	@Override
	public String toString() {
		return String.format("%.6f, %.6f, %.2f", latDegrees, lonDegrees, depth);
	}
		
}
