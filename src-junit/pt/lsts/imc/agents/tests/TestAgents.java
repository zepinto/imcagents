package pt.lsts.imc.agents.tests;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.reflections.Reflections;

import akka.actor.ActorSystem;
import akka.actor.Props;
import pt.lsts.imc.annotations.Agent;

public class TestAgents {

	@Test
	public void testEmptyConstructors() {
		Reflections reflections = new Reflections();
		Set<Class<?>> agents = reflections.getTypesAnnotatedWith(Agent.class);
		ActorSystem system = ActorSystem.create("TestAgents");
		
		for (Class<?> agent : agents) {
			try {
				system.actorOf(Props.create(agent));
				System.out.println(agent.getSimpleName()+": OK");
			}
			catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Cannot instantiate "+agent.getSimpleName());
			}
		}
	}
	
	
	
}
