package pt.lsts.imc.agents.control;

import info.zepinto.props.Property;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowRefState;
import pt.lsts.imc.Reference;

public class TwirlSurvey extends ControllerAgent {

	@Property
	double radius = 100;
	
	@Override
	public Reference guide(EstimatedState estimatedState,
			FollowRefState followRefState) {
		return new Reference();
	}

}
