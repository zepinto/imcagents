package pt.lsts.imc.agents.net;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import info.zepinto.props.Property;
import pt.lsts.imc.Announce;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.agents.AgentContext;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.net.IMCProtocol;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;

@Agent(name = "IMC Protocol", publishes = IMCMessage.class)
public class ImcProtocol extends ImcAgent {

	@Property
	String local_name = "IMCAgents";

	@Property
	int bind_port = 8448;

	private IMCProtocol proto;

	private LinkedHashMap<String, Announce> lastAnnounces = new LinkedHashMap<String, Announce>();

	@Override
	public void init() {
		super.init();
		proto = new IMCProtocol(local_name, bind_port);
		AgentContext.instance().setUid(proto.getLocalId());
		proto.addMessageListener(
				new MessageListener<MessageInfo, IMCMessage>() {
					@Override
					public void onMessage(MessageInfo info, IMCMessage msg) {
						lastAnnounces.put(msg.getSourceName(), (Announce) msg);
					}
				}, Arrays.asList("Announce"));

		proto.addMessageListener(new MessageListener<MessageInfo, IMCMessage>() {
			@Override
			public void onMessage(MessageInfo info, IMCMessage msg) {
				postInternally(msg);
			}
		});
	}

	@Override
	public void stop() {
		super.stop();
		proto.stop();
	}

	@Consume
	void dispatch(IMCMessage msg) {
		if (msg.getDst() != 0 && msg.getDst() != 65535) {
			String name = IMCDefinition.getInstance().getResolver()
					.resolve(msg.getDst());
			proto.sendMessage(name, msg);
		} else {
			for (Entry<String, Announce> ann : lastAnnounces.entrySet()) {
				// send message to all peers (the ones that announced themselves
				// recently)
				if (System.currentTimeMillis()
						- ann.getValue().getTimestampMillis() < 30000) {
					proto.sendMessage(ann.getValue().getSysName(), msg);
				}
			}
		}
	}
}
