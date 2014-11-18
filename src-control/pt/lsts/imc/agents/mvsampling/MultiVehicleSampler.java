package pt.lsts.imc.agents.mvsampling;

import info.zepinto.props.Property;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import pt.lsts.imc.Event;
import pt.lsts.imc.agents.FSMAgent;
import pt.lsts.imc.annotations.Agent;
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
	private double surveyDepth = 2;

	private ArrayList<String> slaves = new ArrayList<String>();
	private String myself = null;
	private LinkedHashMap<String, Event> samples = new LinkedHashMap<String, Event>();
	double myLat, myLon;
	
	@EventHandler("Ready")
	public void samplerReady(Event msg) {

		System.out.println("Sampler ready in "+msg.getSourceName());
		if (msg.getSourceName().startsWith("unknown")
				|| slaves.contains(msg.getSourceName()))
			return;

		// myself is always added to the end of the list
		if (msg.getSrc() == getSrcId()) {
			slaves.add(msg.getSourceName());
			myself = msg.getSourceName();
			myLat = Math
					.toDegrees(Double.parseDouble(msg.getData().get("lat")));
			myLon = Math
					.toDegrees(Double.parseDouble(msg.getData().get("lon")));
		} else
			// slaves are always added at the beginning
			slaves.add(0, msg.getSourceName());

		debug("I have " + slaves.size() + " slaves: " + slaves);
	}

	@EventHandler("Sample")
	public void gotSample(Event msg) {
		samples.put(msg.getSourceName(), msg);
	}

	@State
	@InitialState
	public void discoverSamplers() {
		
		if (slaves.size() >= requiredSamplers && myself != null) {
			war("Minimum number of samplers met. Will now send targets.");
			transition("distributeTargets", "SamplersFound");
		}
	}

	private List<Point2D> generateNextWaypoints() {
		double angle = (Math.PI * 2) / requiredSamplers;
		double startAngle = (Math.PI / 2) * Math.random();
		ArrayList<Point2D> points = new ArrayList<Point2D>();
		double centerLat = myLat, centerLon = myLon;

		if (!samples.isEmpty()) {
			double maxSample = -Double.MAX_VALUE;

			for (Entry<String, Event> entry : samples.entrySet()) {
				double sample = Double.parseDouble(entry.getValue().getData()
						.get("value"));
				if (sample > maxSample) {
					maxSample = sample;
					centerLat = Double.parseDouble(entry.getValue().getData()
							.get("lat"));
					centerLon = Double.parseDouble(entry.getValue().getData()
							.get("lon"));
				}
			}
		}

		war("Surveying around (" + centerLat + ", " + centerLon
				+ ")");

		for (int i = 0; i < requiredSamplers; i++) {
			double offsetX = Math.cos(angle * i + startAngle) * cellWidth;
			double offsetY = Math.sin(angle * i + startAngle) * cellWidth;

			double[] pos = WGS84Utilities.WGS84displace(centerLat, centerLon,
					surveyDepth, offsetX, offsetY, 0);
			points.add(new Point2D.Double(Math.toRadians(pos[0]), Math
					.toRadians(pos[1])));
		}

		war("Generated " + points.size() + " waypoints: " + points);

		return points;
	}

	@State
	public void distributeTargets() {

		System.out.println("Entered distribute targets");
		List<Point2D> wpts = generateNextWaypoints();
		samples.clear();

		for (int i = 0; i < wpts.size();) {
			try {
				sendEventReliably("Target", slaves.get(i), 300, "lat", wpts
						.get(i).getX(), "lon", wpts.get(i).getY(), "depth",
						surveyDepth);
				i++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		war("Sent all targets.");
		transition("waitForSamples", "TargetsSent");
	}

	@State
	public void waitForSamples() {
		if (samples.size() == requiredSamplers) {
			war("got all samples. moving on.");
			transition("distributeTargets", "SamplesReceived");
		}
	}
}
