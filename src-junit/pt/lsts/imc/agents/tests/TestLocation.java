/**
 * 
 */
package pt.lsts.imc.agents.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pt.lsts.imc.agents.coords.Location;

/**
 * @author zp
 *
 */
public class TestLocation {

	@Test
	public void testOffsets() {
		Location loc1 = new Location(41, -8);
		Location loc2 = new Location(loc1);
		loc2.translate(220, 180, 2);
		double[] offsets = loc2.offsetFrom(loc1);
			
		assertEquals(220, offsets[0], 	0.01);
		assertEquals(180, offsets[1], 	0.01);
		assertEquals(2, offsets[2], 	0.01);
		
	}
	
	@Test
	public void testDistance() {
		Location loc1 = new Location(41, -8);
		Location loc2 = new Location(36.7, -8.6);
		assertEquals(loc1.getHorizontalDistance(loc2), loc2.getHorizontalDistance(loc1), 1.0);
	}
	
	@Test
	public void testAngles() {
		Location loc1 = new Location(41, -8);
		Location loc2 = new Location(loc1);
		loc2.translate(100, 0, 0);
		Location loc3 = new Location(loc1);
		loc3.translate(0, 100, 0);
		Location loc4 = new Location(loc1);
		loc4.translate(100, 100, 0);
		
		assertEquals(0, Math.toDegrees(loc1.angleTo(loc2)), 0.01);
		assertEquals(90, Math.toDegrees(loc1.angleTo(loc3)), 0.01);
		assertEquals(45, Math.toDegrees(loc1.angleTo(loc4)), 0.01);
	}
	

}
