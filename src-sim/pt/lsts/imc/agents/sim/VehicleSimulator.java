package pt.lsts.imc.agents.sim;

import info.zepinto.props.Property;
import pt.lsts.imc.DesiredPath;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Heartbeat;
import pt.lsts.imc.PathControlState;
import pt.lsts.imc.agents.AgentContext;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.agents.coords.Location;
import pt.lsts.imc.agents.coords.Pose;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.Periodic;

@Agent(name="Abstract Vehicle Simulator", publishes={Heartbeat.class, PathControlState.class, EstimatedState.class})
public abstract class VehicleSimulator extends ImcAgent {

	protected UnicycleModel model = new UnicycleModel();
	protected DesiredPath desiredPath = null;
	private PathControlState pathControlState = null;
	private long lastTime = 0;
	
	@Property
	public double latDegrees = 41.184483, lonDegrees = -8.7057, depth = 5;
	
	@Property
	public double rollDegs = 0, pitchDegs = 0, yawDegs = 0;
	
	@Periodic(millisBetweenUpdates=1000)
	public final void update() {
		pathControlState = update(desiredPath, model);
		long now = AgentContext.instance().getTime();
		
		model.advance((now - lastTime)/1000.0);
		lastTime 	= now;
		Pose pose 	= model.getState();
		latDegrees 	= pose.getLocation().getLatDegrees();
		lonDegrees 	= pose.getLocation().getLonDegrees();
		depth 		= pose.getLocation().getDepth();
		rollDegs 	= pose.getRollDegs();
		pitchDegs 	= pose.getPitchDegs();
		yawDegs 	= pose.getYawDegs();
		
		EstimatedState state = new EstimatedState();
		state.setLat(Math.toRadians(latDegrees));
		state.setLon(Math.toRadians(lonDegrees));
		state.setDepth(depth);
		state.setPhi(Math.toRadians(rollDegs));
		state.setTheta(Math.toRadians(pitchDegs));
		state.setPsi(Math.toRadians(yawDegs));
		state.setU(model.getSpeedMPS());
		state.setVx(Math.cos(model.getYawRad()) * model.getSpeedMPS());
		state.setVy(Math.sin(model.getYawRad()) * model.getSpeedMPS());
		state.setVz(Math.cos(model.getPitchRad()) * model.getSpeedMPS());
		send(state);
		send(new Heartbeat());
		if (pathControlState != null)
			send(pathControlState);
	}
	
	@Consume
	protected void on(DesiredPath path) {
		this.desiredPath = path;
		long now = AgentContext.instance().getTime();
		//model.advance(now - lastTime);
		pathControlState = update(desiredPath, model);
		lastTime 	= now;		
	}
	
	public abstract void resetPose(Pose pose);
	
	public abstract PathControlState update(DesiredPath path, UnicycleModel model); 
	
	@Override
	public void init() {
		model.setLocation(new Location(latDegrees, lonDegrees));
		model.setDepth(depth);
		model.setRollRad(Math.toRadians(rollDegs));
		model.setPitchRad(Math.toRadians(pitchDegs));
		model.setYawRad(Math.toRadians(yawDegs));
		model.setSpeedMPS(0);
		lastTime = AgentContext.instance().getTime();
	}
}
