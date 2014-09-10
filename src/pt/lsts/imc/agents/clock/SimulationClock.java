package pt.lsts.imc.agents.clock;

/**
 * A simulated clock that takes, as parameter a time speed multiplier.
 * 
 * @author zp
 *
 */
public class SimulationClock implements Clock {

	private double timeMult;
	private long start;

	public SimulationClock(double timeMult) {
		this.timeMult = timeMult;
		this.start = System.currentTimeMillis();
	}

	@Override
	public long getTimeMillis() {
		return (long) ((System.currentTimeMillis() - start) * timeMult) + start;
	}

	@Override
	public long getDuration(long millis) {
		return (long) (millis / timeMult);
	}

}
