package pt.lsts.imc.agents.clock;

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
