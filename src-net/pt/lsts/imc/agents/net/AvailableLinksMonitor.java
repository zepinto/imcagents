package pt.lsts.imc.agents.net;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import pt.lsts.imc.Announce;
import pt.lsts.imc.Event;
import pt.lsts.imc.agents.AgentContext;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.Periodic;

@Agent(name = "Links Monitor", publishes = Event.class)
public class AvailableLinksMonitor extends ImcAgent {

	private LinkedHashMap<String, Announce> lastAnnounceBySystem = new LinkedHashMap<String, Announce>();

	@Consume
	public void on(Announce ann) {
		if (!lastAnnounceBySystem.containsKey(ann.getSysName())) {
			sendEvent("LinkCreated", "name", ann.getSysName(), "type", ann
					.getSysType().toString());
		}
		lastAnnounceBySystem.put(ann.getSysName(), ann);
	}

	@Periodic(millisBetweenUpdates = 5000)
	public void updateLinks() {
		ArrayList<String> toBeRemoved = new ArrayList<>();

		for (Entry<String, Announce> ann : lastAnnounceBySystem.entrySet()) {
			if (AgentContext.instance().getTime()
					- ann.getValue().getTimestampMillis() > 30000) {
				toBeRemoved.add(ann.getKey());
			}
		}
		for (String s : toBeRemoved) {
			Announce ann = lastAnnounceBySystem.get(s);
			lastAnnounceBySystem.remove(s);
			sendEvent("LinkDropped", "name", ann.getSysName(), "type", ann
					.getSysType().toString());
		}
	}
}
