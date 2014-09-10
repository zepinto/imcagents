package pt.lsts.imc.agents.coords;

import pt.lsts.util.WGS84Utilities;

/**
 * This class holds geographic coordinates
 * 
 * @author zp
 *
 */
public class Location {

	private double latDegrees, lonDegrees, depth;

	/**
	 * Create a new location by copying the coordinates of the given location
	 * 
	 * @param other
	 *            Another location
	 */
	public Location(Location other) {
		this.latDegrees = other.latDegrees;
		this.lonDegrees = other.lonDegrees;
		this.depth = other.depth;
	}

	/**
	 * Creates a location with given latitude and longitude. Depth will be set
	 * to 0.
	 * 
	 * @param latDegs
	 *            Latitude, in decimal degrees.
	 * @param lonDegs
	 *            Longitude, in decimal degrees.
	 */
	public Location(double latDegs, double lonDegs) {
		this.latDegrees = latDegs;
		this.lonDegrees = lonDegs;
	}

	/**
	 * Calculate the horizontal distance to another location, in meters.
	 * 
	 * @param otherLoc
	 *            Another location
	 * @return The distance, in meters to the other location.
	 */
	public double getHorizontalDistance(Location otherLoc) {
		return WGS84Utilities.distance(latDegrees, lonDegrees,
				otherLoc.latDegrees, otherLoc.lonDegrees);
	}

	/**
	 * Calculates the cartesian offsets from another location
	 * 
	 * @param otherLoc
	 *            Another location
	 * @return The cartesian offsets return as an array (a[]) of coordinates
	 *         where:
	 * 
	 *         <pre>
	 * a[0] -> northing offset, in meters.
	 * a[1] -> easting offset, in meters.
	 * a[2] -> depth offset, in meters.
	 * </pre>
	 */
	public double[] offsetFrom(Location otherLoc) {
		double offsets[] = WGS84Utilities.WGS84displacement(
				otherLoc.latDegrees, otherLoc.lonDegrees, otherLoc.depth,
				latDegrees, lonDegrees, depth);
		offsets[2] = getDepth() - otherLoc.getDepth();
		return offsets;
	}

	/**
	 * Calculates the angle (on the earth ground) to another location
	 * 
	 * @param otherLoc
	 *            another location
	 * @return The angle, in radians, to the other location. 0 means
	 *         <code>otherLoc</code> is exactly towards north.
	 */
	public double angleTo(Location otherLoc) {
		double[] offsets = otherLoc.offsetFrom(this);
		return Math.atan2(offsets[1], offsets[0]);
	}

	/**
	 * Moves this location by applying the given cartesian offsets. 
 	 * @see {@link #offsetFrom(Location)}
	 * @param northing The northing offset, in meters.
	 * @param easting The easting offset, in meters.
	 * @param depth The depth offset, in meters.
	 */
	public void translate(double northing, double easting, double depth) {
		double[] disp = WGS84Utilities.WGS84displace(latDegrees, lonDegrees,
				this.depth, northing, easting, depth);
		latDegrees = disp[0];
		lonDegrees = disp[1];
		this.depth = disp[2];
	}

	/**
	 * @return The latitude coordinate, in radians.
	 */
	public double getLatRads() {
		return Math.toRadians(latDegrees);
	}

	/**
	 * @return The latitude coordinate, in decimal degrees.
	 */
	public double getLatDegrees() {
		return latDegrees;
	}

	/**
	 * Set the latitude coordinate
	 * @param latDegrees new value for latitude, in degrees.
	 */
	public void setLatDegrees(double latDegrees) {
		this.latDegrees = latDegrees;
	}

	/**
	 * @return The longitude coordinate, in decimal degrees.
	 */
	public double getLonDegrees() {
		return lonDegrees;
	}

	/**
	 * @return The longitude coordinate, in radians.
	 */
	public double getLonRads() {
		return Math.toRadians(lonDegrees);
	}

	/**
	 * Set the longitude coordinate
	 * @param latDegrees new value for latitude, in degrees.
	 */
	public void setLonDegrees(double lonDegrees) {
		this.lonDegrees = lonDegrees;
	}

	/**
	 * Retrieve the depth from surface.
	 * @return depth from surface, in meters. Negative for altitude above ground / surface.
	 */
	public double getDepth() {
		return depth;
	}

	/**
	 * Set the depth from surface.
	 * @param depth depth from surface, in meters. Negative for altitude above ground / surface.
	 */
	public void setDepth(double depth) {
		this.depth = depth;
	}

	@Override
	public String toString() {
		return String.format("%.6f, %.6f, %.2f", latDegrees, lonDegrees, depth);
	}

}
