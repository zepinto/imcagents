package pt.lsts.imc.agents.tests;

import info.zepinto.props.Property;

import java.util.Arrays;

import pt.lsts.imc.AgentCommand;
import pt.lsts.imc.AgentCommand.CMD;
import pt.lsts.imc.Announce;
import pt.lsts.imc.agents.AgentContext;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import scala.util.Random;

@Agent(name="RemoteInst", publishes=AgentCommand.class)
public class RemoteInstantiationExample extends ImcAgent {

	@Property
	int request_id = new Random().nextInt(1000);

	long migrationStarted = 0;
	
	@Consume
	public void on(Announce announce) {
		if (migrationStarted != 0) {
			if ((AgentContext.instance().getTime() - migrationStarted) < 10000)
				return;
			err("Last migration failed.");
			migrationStarted = 0;
			request_id++;
		}

		if (announce.getSrc() != getSrcId()
				&& announce.getServices().contains("agents")) {

			war("Trying to migrate to " + announce.getSourceName());
			migrationStarted = AgentContext.instance().getTime();

			AgentCommand cmd = new AgentCommand();
			cmd.setCmd(CMD.INSTANTIATION_REQUEST);
			cmd.setArgs(Arrays.asList(serializeToImc()));
			cmd.setRequestId(request_id);
			try {
				sendReliably(announce.getSourceName(), cmd, 2000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Consume
	public void on(AgentCommand cmd) {
		
		if (cmd.getCmd() == CMD.INSTANTIATION_SUCCESS
				&& cmd.getRequestId() == request_id) {
			war("Agent migrated successfully. Terminating this instance.");
			terminate();
		}
	}

	@Override
	public void init() {
		super.init();
		System.out.println("initialized");
		request_id++;
		war("initialized request id with value " + request_id);
	}
}
