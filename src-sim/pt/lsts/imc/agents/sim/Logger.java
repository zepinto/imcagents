package pt.lsts.imc.agents.sim;

import info.zepinto.props.Property;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.lsf.LsfMessageLogger;

public class Logger extends ImcAgent {

	@Property
	boolean print = false;
	
	@Consume
	public void on(IMCMessage m) {
		
		try {
			LsfMessageLogger.log(m);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		if(print)
			System.out.println(m);
	}
}
