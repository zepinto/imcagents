package pt.lsts.imc.agents.clock;

/**
 * Real-time clock implementation.
 * @author zp
 *
 */
public class RTClock implements Clock {

	@Override
	public long getTimeMillis() {
		return System.currentTimeMillis();
	}
	
	@Override
	public long getDuration(long millis) {
		return millis;
	}

}
