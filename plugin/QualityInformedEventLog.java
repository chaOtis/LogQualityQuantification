package org.processmining.logqualityquantification.plugin;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.logqualityquantification.GUI.LogQualityFrame;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;
import org.processmining.logqualityquantification.plugin.metrics.attributes.DuplicatesWithinActivity;
import org.processmining.logqualityquantification.plugin.metrics.attributes.DuplicatesWithinLog;
import org.processmining.logqualityquantification.plugin.metrics.attributes.DuplicatesWithinTrace;
import org.processmining.logqualityquantification.plugin.metrics.attributes.Format;
import org.processmining.logqualityquantification.plugin.metrics.attributes.FutureEntry;
import org.processmining.logqualityquantification.plugin.metrics.attributes.InfrequentActivityOrdering;
import org.processmining.logqualityquantification.plugin.metrics.attributes.MissingActivity;
import org.processmining.logqualityquantification.plugin.metrics.attributes.MissingEvent;
import org.processmining.logqualityquantification.plugin.metrics.attributes.MissingTimestamp;
import org.processmining.logqualityquantification.plugin.metrics.attributes.MissingTrace;
import org.processmining.logqualityquantification.plugin.metrics.attributes.MixedGranularityActivities;
import org.processmining.logqualityquantification.plugin.metrics.attributes.MixedGranularityLog;
import org.processmining.logqualityquantification.plugin.metrics.attributes.MixedGranularityTraces;
import org.processmining.logqualityquantification.plugin.metrics.attributes.OverlappingActivities;
import org.processmining.logqualityquantification.plugin.metrics.attributes.Precision;

public class QualityInformedEventLog implements WindowListener {
	boolean configSet;
	private UIPluginContext context;
	private XLog log;
	private List<TraceLinkedEvent> eventList = new ArrayList<TraceLinkedEvent>();
	private Set<Metric> metricsList = new LinkedHashSet<Metric>();
	private Set<String> dimensionsList = new TreeSet<>();
	private Map<Metric, List<Metric>> dimensionsMap = new LinkedHashMap<Metric, List<Metric>>();
	private String[] levelsList = {"Log", "Trace", "Activity", "Event"};
	private Map<Metric, List<Metric>> levelsMap = new LinkedHashMap<Metric, List<Metric>>();
	private boolean toolRunning = true;

	public QualityInformedEventLog(UIPluginContext context, XLog log) {
		this.context = context;
		this.log = log;
		init();
		eventReihenfolge(log);
	}

	private void init() {
		metricsList.add(new DuplicatesWithinLog("Duplicates Within Log", "Uniqueness", "Log"));
		metricsList.add(new DuplicatesWithinActivity("Duplicates Within Activity", "Uniqueness", "Activity"));
		metricsList.add(new DuplicatesWithinTrace("Duplicates Within Trace", "Uniqueness", "Trace"));
		metricsList.add(new Format("Format", "Consistency", "Log"));
		metricsList.add(new FutureEntry("Future Entry", "Accuracy", "Event"));
		metricsList.add(new InfrequentActivityOrdering("Infrequent Activity Ordering", "Accuracy", "Trace"));
		metricsList.add(new MissingActivity("Missing Activity", "Completeness", "Trace"));
		metricsList.add(new MissingEvent("Missing Event", "Completeness", "Activity"));
		metricsList.add(new MissingTimestamp("Missing Timestamp", "Completeness", "Event"));
		metricsList.add(new MissingTrace("Missing Trace", "Completeness", "Log"));
		metricsList.add(new MixedGranularityActivities("Mixed Granularity of Activities", "Consistency", "Activity"));
		metricsList.add(new MixedGranularityLog("Mixed Granularity of the Log", "Consistency", "Log"));
		metricsList.add(new MixedGranularityTraces("Mixed Granularity of Traces", "Consistency", "Trace"));
		metricsList.add(new OverlappingActivities("Overlapping Activities per Resource", "Accuracy", "Trace"));
		metricsList.add(new Precision("Precision", "Accuracy", "Event"));
		
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(metricsList.size());
		context.getProgress().setIndeterminate(false);
		
		double starttime = (double) new Date().getTime()/1000;	
		double currenttime;
		
		createEventList(log);
		for (XTrace trace : log) {
			System.out.println(trace.subList(0, (trace.size()-1)));
		}
		
		for (Metric metric : metricsList) {
			context.log("Determining "+ metric.getName()+"...");
			metric.init(eventList);
			currenttime = (double) new Date().getTime()/1000;
			double duration = currenttime - starttime;
			DecimalFormat df = new DecimalFormat("#.####");
			context.log(metric.getName()+" completed! ("+ df.format(duration) + " seconds)");
			context.getProgress().inc();
			starttime = currenttime;
		}
		
		for (String level : levelsList) {
			List<Metric> levelMetricsList = new ArrayList<>();
			for (Metric metric : metricsList) {
				if (metric.getEventLogLevel().equals(level)) {
					levelMetricsList.add(metric);
				}
			}
			Metric levelMetric = new Metric(level+" Level Quality", null, level);
			levelsMap.put(levelMetric, levelMetricsList);
		}
		
		for (Metric metric : metricsList) {
			dimensionsList.add(metric.getDimension());
		}	
		
		for (String dimension : dimensionsList) {
			List<Metric> dimensionMetricsList = new ArrayList<>();
			for (Metric metric : metricsList) {
				if (metric.getDimension().equals(dimension)) {
					dimensionMetricsList.add(metric);
				}
			}
			Metric dimensionMetric = new Metric(dimension, dimension, null);
			dimensionsMap.put(dimensionMetric, dimensionMetricsList);
		}
		
		calculateDimensionScores();
		
		checkPresentQualityInformation();
		JFrame logQualityFrame = new LogQualityFrame(this, log);
		logQualityFrame.addWindowListener(this);
		logQualityFrame.setVisible(true);
		
		while (isToolRunning()) {
			
			try {
				Thread.sleep(200);
			}
			catch(InterruptedException e) {
				
			}
		}
		
	}
	
	private void checkPresentQualityInformation() {
		for (Entry<String, XAttribute> entry : log.getAttributes().entrySet()) {
			if (entry.getValue().getKey().equals("Timestamp Quality")) {
				for (Entry<Metric, List<Metric>> dimension : dimensionsMap.entrySet()) {
					dimension.getKey().setUsed(false);
					for (Metric metric : dimension.getValue()) {
						metric.setUsed(false);
					}
				}
				for (Entry<String, XAttribute> dimEntry : entry.getValue().getAttributes().entrySet()) {
					for (Metric dimension : dimensionsMap.keySet()) {
						if (dimEntry.getValue().getKey().startsWith(dimension.getName())) {
							dimension.setUsed(true);
							for (Entry<String, XAttribute> metricEntry : dimEntry.getValue().getAttributes().entrySet()) {
								for (Metric metric : dimensionsMap.get(dimension)) {
									if (metricEntry.getValue().getKey().startsWith(metric.getName())) {
										metric.setUsed(true);
										String str = metricEntry.getValue().getKey().substring(metric.getName().length()+10);
										metric.setRelativeWeight(Double.parseDouble(str.substring(0, str.length()-1)));
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public void createQIEL() {

		XFactoryNaiveImpl factory = new XFactoryNaiveImpl();
		for (Entry<String, XAttribute> e : log.getAttributes().entrySet()) {
			if (e.getValue().getKey().equals("Timestamp Quality")) {
				log.getAttributes().remove(e.getKey());
			}
		}
		XAttribute timestampQualityAttribute = factory.createAttributeContainer("Timestamp Quality", null);
		
		for (Entry<Metric, List<Metric>> e : dimensionsMap.entrySet()) {
			
			if (e.getKey().isUsed()) {
				
				XAttribute dimValue = factory.createAttributeContinuous(e.getKey().getName(), e.getKey().getScore(), null);
			
				for (Metric metric : e.getValue()) {

					if (metric.isUsed()) {
						XAttribute metricValue = factory.createAttributeContinuous(metric.getName()+" (weight: " + metric.getRelativeWeight()+")", metric.getScore(), null);
	
						dimValue.getAttributes().put(metric.getName(), metricValue);
					}
					
				}
			timestampQualityAttribute.getAttributes().put(e.getKey().getName(), dimValue);	
			}
		}	
		log.getAttributes().put("Timestamp Quality", timestampQualityAttribute);
	}

	
	public void calculateDimensionScores() {
		for (Entry<Metric, List<Metric>> entry : dimensionsMap.entrySet()) {
			entry.getKey().calculateDimensionScore(entry.getValue());
		}
		for (Entry<Metric, List<Metric>> entry : levelsMap.entrySet()) {
			entry.getKey().calculateDimensionScore(entry.getValue());
		}
	}

	private void createEventList(XLog log) {
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				eventList.add(new TraceLinkedEvent(trace, event));
			}
		}
	}
	private void createTraceGroup(XLog log) { //Problem : es müssen erst für die verscheidenen Tracetypen je eine Liste erzeugt werden
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				
			}
		}
	}
	private void eventReihenfolge(XLog log) {
		for(XTrace trace : log) {
			System.out.println("Trace: " + XConceptExtension.instance().extractName(trace));
			for (XEvent event : trace) {
				System.out.println("Event: "+XConceptExtension.instance().extractName(event)+ " : "+XLifecycleExtension.instance().extractTransition(event));
			}
		}
	}
	
	public Metric getMetric(String metricName) {
		Metric metric = getMetricsList().stream().filter(m -> m.getName().equals(metricName)).findFirst().get();		
		return metric;
	}
		
	public UIPluginContext getContext() {
		return context;
	}

	public void setContext(UIPluginContext context) {
		this.context = context;
	}

	public XLog getLog() {
		return log;
	}

	public void setLog(XLog log) {
		this.log = log;
	}

	public List<TraceLinkedEvent> getEventList() {
		return eventList;
	}

	public void setEventList(List<TraceLinkedEvent> eventList) {
		this.eventList = eventList;
	}

	public Set<Metric> getMetricsList() {
		return metricsList;
	}

	public void setMetricsList(Set<Metric> metricsList) {
		this.metricsList = metricsList;
	}

	public Set<String> getDimensionsList() {
		return dimensionsList;
	}

	public void setDimensionsList(Set<String> dimensionsList) {
		this.dimensionsList = dimensionsList;
	}

	public Map<Metric, List<Metric>> getDimensionsMap() {
		return dimensionsMap;
	}

	public void setDimensionsMap(Map<Metric, List<Metric>> dimensionsMap) {
		this.dimensionsMap = dimensionsMap;
	}

	public Map<Metric, List<Metric>> getLevelsMap() {
		return levelsMap;
	}

	public void setLevelsMap(Map<Metric, List<Metric>> levelsMap) {
		this.levelsMap = levelsMap;
	}

	public boolean isConfigSet() {
		return configSet;
	}

	public void setConfigSet(boolean configSet) {
		this.configSet = configSet;
	}

	public String[] getLevelsList() {
		return levelsList;
	}

	public void setLevelsList(String[] levelsList) {
		this.levelsList = levelsList;
	}

	public boolean isToolRunning() {
		return toolRunning;
	}

	public void setToolRunning(boolean toolRunning) {
		this.toolRunning = toolRunning;
	}

	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosed(WindowEvent e) {
		setToolRunning(false);
		
	}

	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
}
