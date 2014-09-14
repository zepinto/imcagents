package pt.lsts.imc.agents.control;

import java.util.LinkedHashMap;

import info.zepinto.props.Property;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowRefState;
import pt.lsts.imc.FollowReference;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.Reference;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.EventHandler;
import pt.lsts.imc.annotations.Periodic;

@Agent(name = "Abstract Controller", publishes = { Reference.class, PlanControl.class })
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
		this.estimatedState = estate;
	}

	@Consume
	protected final void on(FollowRefState frefstate) {
		this.followRefState = frefstate;
	}

	@Consume
	protected final void on(PlanControlState planControlState) {
		this.planControlState = planControlState;

		switch (currentState) {
		case Connecting:
			if (planControlState.getPlanId().equals(ctrl_id))
				break;

		default:
			break;
		}
	}
	
	@EventHandler("setBehavior")
	void event(LinkedHashMap<String, ?> data) {
		if ((""+data.get("ctrl_id")).equals(ctrl_id)) {
			//I AM ACTIVE!			
		}
	}

	private PlanControl createStartRequest() {
		FollowReference fref = new FollowReference()
				.setControlEnt((short) getEntityId()).setControlSrc(getSrcId())
				.setTimeout(timeout).setLoiterRadius(10);

		return new PlanControl().setType(TYPE.REQUEST).setOp(OP.START)
				.setPlanId(ctrl_id).setRequestId(0).setArg(fref);
	}

	@Periodic(millisBetweenUpdates = 1000)
	public void update() {

		if (currentState == STATE.Finished) {
			stop();
			return;
		}
		if (planControlState == null || followRefState == null) {
			currentState = STATE.Connecting;
		}
		else if (planControlState.getPlanId().equals(ctrl_id) 
				&& followRefState.getControlSrc() == getSrcId()
				&& followRefState.getControlEnt() == getEntityId()) {
			currentState = STATE.Controlling;
		}
		
		System.out.println("STATE: "+currentState);

		switch (currentState) {
		case Connecting:
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
		PlanControl pc = new PlanControl()
		.setPlanId(ctrl_id)
		.setOp(OP.STOP)
		.setFlags(0)
		.setRequestId(0)
		.setType(TYPE.REQUEST);
		send(pc);
	}

	public abstract Reference guide();

}
