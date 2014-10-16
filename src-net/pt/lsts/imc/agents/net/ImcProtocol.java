package pt.lsts.imc.agents.net;

import info.zepinto.props.Property;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

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
		
		boolean reliably = msg.getInteger("__reliable") != 0;
		
		if (reliably) {
			System.out.println("[IMCProtocol] Send message of type "+msg.getAbbrev()+" reliably to "+msg.getString("__dst")+" in less than "+msg.getInteger("__timeout")+" milliseconds.");
			try {
				proto.sendReliably(msg.getString("__dst"), msg, msg.getInteger("__timeout"));
				getSender().tell(new DeliveryResult(msg, null), self());
			}
			catch (Exception e) {
				e.printStackTrace();
				getSender().tell(new DeliveryResult(msg, e), self());
			}
			return;
		}
		
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
