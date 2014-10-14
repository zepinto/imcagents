package pt.lsts.imc.agents.control;

import info.zepinto.props.Property;
import pt.lsts.imc.DesiredSpeed;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowRefState;
import pt.lsts.imc.FollowReference;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.DesiredSpeed.SPEED_UNITS;
import pt.lsts.imc.DesiredZ.Z_UNITS;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.Reference;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.Periodic;

@Agent(name = "Abstract Controller", publishes = { Reference.class,
		PlanControl.class })
public abstract class WaypointController extends ImcAgent {

	@Property
	String vehicle = null;

	@Property
	String ctrl_id = getClass().getSimpleName();

	@Property
	int timeout = 30;

	protected enum STATE {
		Idle, Connecting, Controlling, Finished
	};

	protected EstimatedState estimatedState = null;
	protected FollowRefState followRefState = null;
	protected PlanControlState planControlState = null;
	protected STATE currentState;

	@Consume
	protected final void on(EstimatedState estate) {
		if (!estate.getSourceName().equals(vehicle))
			return;
		this.estimatedState = estate;
	}

	@Consume
	protected final void on(FollowRefState frefstate) {
		if (!frefstate.getSourceName().equals(vehicle))
			return;
		this.followRefState = frefstate;	
	}

	@Consume
	protected final void on(PlanControlState planControlState) {
		
		if (!planControlState.getSourceName().equals(vehicle))
			return;
		this.planControlState = planControlState;
	}

	protected boolean arrivedXY() {
		return followRefState != null && (followRefState.getProximity() & FollowRefState.PROX_XY_NEAR) != 0;
	}

	protected boolean arrivedZ() {
		return followRefState != null && (followRefState.getProximity() & FollowRefState.PROX_Z_NEAR) != 0;
	}

	protected boolean arrived() {
		return arrivedZ() && arrivedXY();
	}

	protected Reference waypoint(double latRadians, double lonRadians,
			double depth, double speed) {
		return new Reference()
		.setSpeed(
				new DesiredSpeed().setSpeedUnits(SPEED_UNITS.METERS_PS)
				.setValue(speed))
				.setZ(new DesiredZ().setZUnits(Z_UNITS.DEPTH).setValue(depth))
				.setLat(latRadians).setLon(lonRadians).setRadius(0);
	}
	
	private PlanControl createStartRequest() {

		FollowReference fref = new FollowReference()
		.setControlEnt((short) getEntityId()).setControlSrc(getSrcId())
		.setTimeout(timeout).setLoiterRadius(10);

		return new PlanControl().setType(TYPE.REQUEST).setOp(OP.START)
				.setPlanId(ctrl_id).setRequestId(0).setArg(fref).setFlags(0);
	}

	@Periodic(millisBetweenUpdates = 2000)
	public void update() {

		if (currentState == STATE.Finished) {
			stop();
			return;
		}
		if (planControlState == null || followRefState == null) {
			currentState = STATE.Connecting;
		}
		else {

			if (planControlState.getPlanId().equals(ctrl_id)
					&& followRefState.getControlSrc() == getSrcId()
					&& followRefState.getControlEnt() == getEntityId()) {
				currentState = STATE.Controlling;
			}
		}

		switch (currentState) {
		case Connecting:
			if (planControlState != null && planControlState.getState() == PlanControlState.STATE.READY)
				send(vehicle, createStartRequest());
			break;
		case Controlling:
			Reference wpt = guide();
			if (wpt == null || (wpt.getFlags() & Reference.FLAG_MANDONE) != 0) {
				currentState = STATE.Finished;
				stop();
			} else
				send(vehicle, wpt);
			break;
		default:
			break;
		}
	}

	@Override
	public void init() {
		super.init();
		currentState = STATE.Connecting;
	}

	@Override
	public void stop() {
		PlanControl pc = new PlanControl().setPlanId(ctrl_id).setOp(OP.STOP)
				.setFlags(0).setRequestId(0).setType(TYPE.REQUEST);
		send(vehicle, pc);
	}

	public abstract Reference guide();

}
