package pt.lsts.imc.agents.mvsampling;

import info.zepinto.props.Property;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Event;
import pt.lsts.imc.agents.FSMAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.EventHandler;
import pt.lsts.imc.annotations.InitialState;
import pt.lsts.imc.annotations.State;
import pt.lsts.util.WGS84Utilities;

@Agent(name = "Multi-Vehicle Sampler", publishes = { Event.class })
public class MultiVehicleSampler extends FSMAgent {

	@Property
	private int requiredSamplers = 3;

	@Property
	private double cellWidth = 50;

	@Property
	private double surveyDepth = 3;
	
	private ArrayList<String> slaves = new ArrayList<String>(); 
	private String myself = null;
	private LinkedHashMap<String, Event> samples = new LinkedHashMap<String, Event>();
	double myLat, myLon;
	
	@Consume
	public void on(EstimatedState state) {
		//check this is my loc
		if (state.getSrc() == getSrcId()) {
			double[] pos = WGS84Utilities.toLatLonDepth(state);
			myLat = pos[0];
			myLon = pos[1];
		}
	}
	@EventHandler("Ready")
	public void samplerReady(Event msg) {
		// myself is always added to the end of the list
		if (msg.getSrc() == getSrcId()) {
			slaves.add(msg.getSourceName());
			myself = msg.getSourceName();
		}
		else
			// slaves are always added at the beginning
			slaves.add(0, msg.getSourceName());
	}
	
	@EventHandler("Sample")
	public void gotSample(Event msg) {
		samples.put(msg.getSourceName(), msg);
	}

	
	@State
	@InitialState
	public void discoverSamplers() {
		if (slaves.size() >= requiredSamplers-1 && myself != null) {
			System.out
					.println("Minimum number of samplers met. Will now send targets.");
			transition("distributeTargets", "SamplersFound");
		}
	}
	
	private List<Point2D> generateNextWaypoints() {
		double angle = (Math.PI*2)/requiredSamplers;
		ArrayList<Point2D> points = new ArrayList<Point2D>();
		
		if (samples.isEmpty()) {
			// initial point
			for (int i = 0; i < requiredSamplers; i++) {
				double offsetX = Math.cos(angle*i) * cellWidth;
				double offsetY = Math.sin(angle*i) * cellWidth;
				double[] pos = WGS84Utilities.WGS84displace(myLat, myLon, surveyDepth, offsetX, offsetY, 0);
				points.add(new Point2D.Double(Math.toRadians(pos[0]), Math.toRadians(pos[1])));
			}
		}
		else {
			// FIXME check highest value from samples and generate based on that
			for (int i = 0; i < requiredSamplers; i++) {
				double offsetX = Math.cos(angle*i) * cellWidth;
				double offsetY = Math.sin(angle*i) * cellWidth;
				double[] pos = WGS84Utilities.WGS84displace(myLat, myLon, surveyDepth, offsetX, offsetY, 0);
				points.add(new Point2D.Double(Math.toRadians(pos[0]), Math.toRadians(pos[1])));
			}
		}
		
		System.out.println("Generated "+points.size()+" waypoints");
		
		return points;
	}
	

	@State
	public void distributeTargets() {
		
		System.out.println("Entered distribute targets");
		List<Point2D> wpts = generateNextWaypoints();
		samples.clear();
		
		for (int i = 0; i < wpts.size(); i++) {
			sendEvent("Target", "vehicle", slaves.get(i), "lat", wpts.get(i).getX(), "lon", wpts.get(i).getY(),
					"depth", surveyDepth);
		}
		
		System.out.println("Sent all targets.");
		transition("waitForSamples", "TargetsSent");
	}

	@State
	public void waitForSamples() {
		if (samples.size() == requiredSamplers) {
			System.out.println("got all samples. moving on.");
			transition("distributeTargets", "SamplesReceived");
		}		
	}
}
