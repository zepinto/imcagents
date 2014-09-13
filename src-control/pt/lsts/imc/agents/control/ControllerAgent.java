package pt.lsts.imc.agents.control;

import pt.lsts.imc.DesiredPath;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowRefState;
import pt.lsts.imc.PathControlState;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.Periodic;

@Agent(name="Abstract Controller", publishes={DesiredPath.class, PlanControl.class})
public abstract class ControllerAgent extends ImcAgent {

	protected int entityId;
	protected EstimatedState estimatedState = null;
	protected FollowRefState followRefState = null;
	protected PlanControlState planControlState = null;
	protected PathControlState pathControlState = null;
	
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
	}
	
	@Consume
	protected final void on(PathControlState pathControlState) {
		this.pathControlState = pathControlState;		
	}
	
	public abstract DesiredPath guide();

	@Periodic(millisBetweenUpdates=1000)
	public final void sendReference() {
		DesiredPath ref = guide();
		if (ref != null)
			send(ref);
		else {
			stop();
		}
	}
	
	@Override
	public void init() {
		super.init();
	}
	
	@Override
	public void stop() {
		PlanControl pc = new PlanControl();
		pc.setPlanId("follow_"+getClass().getSimpleName());
		pc.setOp(OP.STOP);
		pc.setRequestId(0);
		pc.setType(TYPE.REQUEST);
		send(pc);
	}
}
