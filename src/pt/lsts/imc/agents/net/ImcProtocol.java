package pt.lsts.imc.agents.net;

import info.zepinto.props.Property;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import pt.lsts.imc.Announce;
import pt.lsts.imc.AnnounceService;
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
	
	@Property
	String autoconnect = ".*";

	private IMCProtocol proto;

	private LinkedHashMap<String, Announce> lastAnnounces = new LinkedHashMap<String, Announce>();

	@Override
	public void init() {
		super.init();
		proto = new IMCProtocol(local_name, bind_port);
		proto.setAutoConnect(autoconnect);
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
	private void on(AnnounceService serv) {
		if (serv.getSrc() == 0 || serv.getSrc() == getSrcId()) {
			proto.addService(serv.getService());
			war("Added service: "+serv.getService());
		}
		
	}

	@Consume
	void dispatch(IMCMessage msg) {
		boolean reliably = msg.getInteger("__reliable") != 0;

		if (reliably) {
			try {
				proto.sendReliably(msg.getString("__dst"), msg,
						msg.getInteger("__timeout"));
				getSender().tell(new DeliveryResult(msg, null), self());
			} catch (Exception e) {
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

				if (ann.getValue().getSrc() == getSrcId())
					continue;

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
