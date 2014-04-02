package pt.lsts.imc.agents.sim;

import info.zepinto.props.Property;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.llf.LLFMessageLogger;

public class Logger extends ImcAgent {

	@Property
	boolean print = false;
	
	@Property
	String log = "log/$day/$time";
	
	private LLFMessageLogger logger;
	
	@Override
	public void init() {
		super.init();
		SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat time = new SimpleDateFormat("HHmmss");
		String dir = log.replaceAll("\\$time", time.format(new Date()));
		dir = dir.replaceAll("\\$day", date.format(new Date()));
		new File(dir).mkdirs();
		logger = new LLFMessageLogger(dir);		
	}
	
	@Consume
	public void on(IMCMessage m) {
		
		try {
			logger.logMessage(m);
			logger.flushLogs();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		if(print)
			System.out.println(m);
	}
}
