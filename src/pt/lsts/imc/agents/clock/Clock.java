package pt.lsts.imc.agents.clock;

/**
 * This interface describes a Clock that converts an time durations into real
 * time.
 * 
 * @author zp
 *
 */
public interface Clock {

	/**
	 * Retrieve the current (logical) time.
	 * 
	 * @return the current (logical) time.
	 */
	public long getTimeMillis();

	/**
	 * Transform logical time into real-time
	 * 
	 * @param millis
	 *            The logical time, in milliseconds to be converted to real-time
	 *            milliseconds
	 * @return real-time milliseconds. <br/>
	 *         Example: A simulated clock that has time speed multiplied by two,
	 *         will return <code>millis/2</code>.
	 */
	public long getDuration(long millis);
}
