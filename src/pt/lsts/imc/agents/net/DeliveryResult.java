package pt.lsts.imc.agents.net;

import pt.lsts.imc.IMCMessage;

public class DeliveryResult {

	private Exception error = null;
	private IMCMessage m;
	
	public DeliveryResult(IMCMessage m, Exception error) {
		this.error = error;
		this.m = m;
	}
	
	public void getException() throws Exception {
		if (error != null)
			throw error;
	}
	
	public int getRequestNumber() {
		return m.getInteger("__reliable");
	}
	
	public boolean isSuccess() {
		return error == null;
	}
}
