package pt.lsts.imc.agents.transport;

import java.util.Vector;

import info.zepinto.props.Property;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.net.TcpTransport;

public class TcpAgent extends ImcAgent {

	@Property
	private int bindPort = 8040;

	@Property
	private String messagesToSend = "Event";
	
	private TcpTransport transport;
	private Vector<Integer> messagesToForward;
	
	@Override
	public void init() {
		super.init();
		messagesToForward = new Vector<Integer>();
		for (String s : messagesToSend.split(","))
			messagesToForward.add(IMCDefinition.getInstance().getMessageId(s.trim()));
		
		try {
			transport.bind(bindPort);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Consume
	public void on(IMCMessage msg) {
		
	}
	
	@Override
	public void stop() {
		super.stop();
	}
	
	
}
