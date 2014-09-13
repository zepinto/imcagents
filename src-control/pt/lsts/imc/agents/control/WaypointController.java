package pt.lsts.imc.agents.control;

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
import pt.lsts.imc.annotations.Periodic;

@Agent(name = "Abstract Controller", publishes = { Reference.class, PlanControl.class })
public abstract class WaypointController extends ImcAgent {

	@Property
	String vehicle = null;

	@Property
	String plan_id = "WaypointController";

	@Property
	int timeout = 30;

	enum STATE {
		Connecting, Controlling, Finished
	};

	protected EstimatedState estimatedState = null;
	protected FollowRefState followRefState = null;
	protected PlanControlState planControlState = null;
	private STATE currentState;

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
			if (planControlState.getPlanId().equals(plan_id))
				break;

		default:
			break;
		}
	}

	private PlanControl createStartRequest() {
		FollowReference fref = new FollowReference()
				.setControlEnt((short) getEntityId()).setControlSrc(getSrcId())
				.setTimeout(timeout).setLoiterRadius(10);

		return new PlanControl().setType(TYPE.REQUEST).setOp(OP.START)
				.setPlanId(plan_id).setRequestId(0).setArg(fref);
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
		else if (planControlState.getPlanId().equals(plan_id) 
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
		PlanControl pc = new PlanControl();
		pc.setPlanId(plan_id);
		pc.setOp(OP.STOP);
		pc.setRequestId(0);
		pc.setType(TYPE.REQUEST);
		send(pc);
	}

	public abstract Reference guide();

}
