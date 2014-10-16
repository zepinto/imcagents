package pt.lsts.imc.agents.net;

import info.zepinto.props.Property;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.agents.AgentContext;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.net.UDPTransport;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;

@Agent(name = "UDP Transport", publishes = { IMCMessage.class })
public class ImcUdp extends ImcAgent {

	@Property
	int local_port;

	@Property
	int remote_port;

	@Property
	String remote_host;

	UDPTransport transport;

	@Override
	public void init() {
		super.init();
		transport = new UDPTransport(local_port, 1);
		transport.addMessageListener(new MessageListener<MessageInfo, IMCMessage>() {
			@Override
			public void onMessage(MessageInfo info, IMCMessage msg) {
				send(msg);
			}
		});
	}

	@Override
	public void stop() {
		super.stop();
		transport.stop();
	}

	@Consume
	public void forward(IMCMessage m) {
		m.setSrc(AgentContext.instance().getUid());
		m.setTimestampMillis(AgentContext.instance().getTime());
		if (m.getSrcEnt() == 0)
			m.setSrcEnt(255);
		transport.sendMessage(remote_host, remote_port, m);
	}
}
