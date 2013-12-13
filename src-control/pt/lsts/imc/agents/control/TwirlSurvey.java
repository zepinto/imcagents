package pt.lsts.imc.agents.control;

import info.zepinto.props.Property;

import java.util.Date;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowRefState;
import pt.lsts.imc.Reference;
import pt.lsts.imc.agents.AgentContext;

public class TwirlSurvey extends ControllerAgent {

	@Property
	double radius = 100;
	
	@Override
	public Reference guide(EstimatedState estimatedState,
			FollowRefState followRefState) {
		System.out.println(new Date(AgentContext.instance().getTime()));
		return new Reference();
	}

}
