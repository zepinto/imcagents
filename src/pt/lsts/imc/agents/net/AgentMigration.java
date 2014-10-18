package pt.lsts.imc.agents.net;

import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import pt.lsts.imc.AgentCommand;
import pt.lsts.imc.AgentCommand.CMD;
import pt.lsts.imc.AnnounceService;
import pt.lsts.imc.agents.AgentContext;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.pattern.Patterns;

@Agent(name = "MigrationSystem", publishes = { AgentCommand.class,
		AnnounceService.class })
public class AgentMigration extends ImcAgent {

	@Consume
	public void on(AgentCommand cmd) {
		
		if (cmd.getSrc() == getSrcId())
			return;
				
		switch (cmd.getCmd()) {
		case STATE_REQUEST:
			Vector<Future<Object>> futureStates = new Vector<Future<Object>>();
			for (ActorRef ref : AgentContext.instance().getActors())
				futureStates.add(Patterns.ask(ref, "state", 1000));

			Vector<pt.lsts.imc.Agent> states = new Vector<pt.lsts.imc.Agent>();

			for (Future<Object> f : futureStates) {
				try {
					states.add((pt.lsts.imc.Agent) Await.result(f,
							Duration.create(1500, TimeUnit.MILLISECONDS)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			send(cmd.getSourceName(),
					new AgentCommand().setCmd(CMD.STATE_REPLY).setArgs(states)
							.setRequestId(cmd.getRequestId()));

			break;
		case INSTANTIATION_REQUEST:
			try {				
				// Validate agents
				for (pt.lsts.imc.Agent ag : cmd.getArgs()) {
					try {
						Class.forName(ag.getAgentClass());
					} catch (Exception e) {
						e.printStackTrace();
						send(cmd.getSourceName(),
								new AgentCommand()
										.setCmd(CMD.INSTANTIATION_FAILURE)
										.setInfo(
												e.getClass().getSimpleName()
														+ ": " + e.getMessage())
										.setRequestId(cmd.getRequestId()));

						return;
					}
				}

				try {
					sendReliably(
							cmd.getSourceName(),
							new AgentCommand()
									.setCmd(CMD.INSTANTIATION_SUCCESS)
									.setInfo(
											cmd.getArgs().size()
													+ " agents instantiated.")
									.setRequestId(cmd.getRequestId()), 5000);					
				} catch (Exception e) {
					e.printStackTrace();
					
					send(cmd.getSourceName(),
							new AgentCommand()
									.setCmd(CMD.INSTANTIATION_FAILURE)
									.setInfo(
											e.getClass().getSimpleName() + ": "
													+ e.getMessage())
									.setRequestId(cmd.getRequestId()));
					return;
				}

				Vector<ActorRef> newActors = new Vector<ActorRef>();
				for (pt.lsts.imc.Agent ag : cmd.getArgs()) {
					Class<?> c = Class.forName(ag.getAgentClass());
					Properties props = new Properties();
					props.putAll(ag.getAgentState());
					newActors.add(AgentContext.instance().bootstrap(c, props));
				}

				for (ActorRef ref : newActors)
					ref.tell("init", getSelf());

			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void init() {
		super.init();
		postInternally(new AnnounceService("imc+any://agents",
				AnnounceService.SRV_TYPE_EXTERNAL));
	}
}
