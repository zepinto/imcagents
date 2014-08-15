package pt.lsts.imc.agents.control;

import info.zepinto.props.Property;
import pt.lsts.imc.DesiredPath;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.DesiredZ.Z_UNITS;
import pt.lsts.imc.PathControlState;
import pt.lsts.imc.agents.AgentContext;
import pt.lsts.util.WGS84Utilities;

public class TwirlSurvey extends ControllerAgent {

	@Property
	double latDegrees = 41.1844833;
	
	@Property
	double lonDegrees = -8.7057833;
	
	@Property
	double radius = 100;
	
	@Property
	double minZ = 2;
	
	@Property
	double maxZ = 4;
	
	@Property
	double dvn = 0;
	
	@Property
	double dve = 0;
	
	@Property
	double speed = 1.25;
	
	@Property
	DesiredPath.SPEED_UNITS speedUnits = DesiredPath.SPEED_UNITS.METERS_PS;
	
	private DesiredZ z = new DesiredZ(0, Z_UNITS.DEPTH);
	private boolean descend = true;
	private long start = 0;
	private double loiteringTime = 0;
	private long lastLoiterTime = -1;

	@Override
	public DesiredPath guide() {
		if (estimatedState == null)
			return null;
		
		double ellapsedTime = loiteringTime;
		
		if (pathControlState != null) {
			boolean loitering = (pathControlState.getFlags() & PathControlState.FL_LOITERING) != 0;
			
			if (AgentContext.instance().getTime() - lastLoiterTime < 10000 && loitering)
				loiteringTime += (AgentContext.instance().getTime() - lastLoiterTime) / 1000.0;
			
			if (loitering)
				lastLoiterTime = AgentContext.instance().getTime();
		}
		
		double[] dest = WGS84Utilities.WGS84displace(latDegrees, lonDegrees, 0, dvn*ellapsedTime, dve*ellapsedTime, 0);
		
		if (start == 0 && pathControlState != null && ((pathControlState.getFlags() & PathControlState.FL_LOITERING) != 0))
			start = AgentContext.instance().getTime();
		
		DesiredPath ref = new DesiredPath();
		
		boolean nearBottom = estimatedState.getAlt() != -1 && estimatedState.getAlt() < 3;
		
		if (descend && (estimatedState.getDepth() > maxZ || nearBottom)) {
			descend = false;
			z.setValue(minZ);
		}
		else if (!descend && estimatedState.getDepth() < minZ) {
			descend = true;
			z.setValue(maxZ);
		}
		
		ref.setEndLat(Math.toRadians(dest[0]));
		ref.setEndLon(Math.toRadians(dest[1]));
		ref.setEndZ(z.getValue());
		ref.setSpeed(speed);
		ref.setSpeedUnits(speedUnits);
		ref.setLradius(radius);
		return ref;
	}

}
