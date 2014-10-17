package pt.lsts.imc.agents.sim;

import java.text.SimpleDateFormat;
import java.util.Date;

import pt.lsts.imc.LogBookEntry;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;

@Agent(name = "LogBook", publishes = {})
public class LogBookDisplay extends ImcAgent {

	private SimpleDateFormat fmt = new SimpleDateFormat("YYYY/MM/dd HH:mm:ss");

	@Consume
	private void on(LogBookEntry entry) {

		System.out.println("["
				+ fmt.format(new Date((long) (1000 * entry.getHtime())))
				+ "] - " + entry.getType() + " [" + entry.getContext() + "] "
				+ entry.getText());
	}
}
