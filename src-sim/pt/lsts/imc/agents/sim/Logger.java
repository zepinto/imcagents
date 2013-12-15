package pt.lsts.imc.agents.sim;

import info.zepinto.props.Property;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Consume;

public class Logger extends ImcAgent {

	@Property
	String print = "*";
	
	@Property
	String log = "";
	
	@Consume
	public void on(IMCMessage m) {
		System.out.println(m);
	}
}
