package pt.lsts.imc.agents.clock;

public interface Clock {

	public long getTimeMillis();
	public long getDuration(long millis);
}
