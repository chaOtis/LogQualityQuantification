package org.processmining.logqualityquantification.plugin.metrics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.model.XTrace;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;

public class Metric {

	private  List<TraceLinkedEvent> errorSet = new ArrayList<>();
	private  LinkedHashMap<Error, List<Error>> errorList = new LinkedHashMap<Error, List<Error>>();
	private double score;
	private String qualityLevel;
	private String eventLogLevel;
	private String dimension;
	private String name;
	private DefaultTableModel errorTableModel;
	private DefaultTableModel errorTableModelLayerTwo;
	private String details;
	private int scoreWeight = 1;
	private boolean used = true;
	private double relativeWeight = 1;
	private List<TraceLinkedEvent> eventList = new ArrayList<TraceLinkedEvent>();
	private List<XTrace> errorTraceList = new ArrayList<XTrace>();

	public Metric(String name, String dimension, String eventLogLevel) {
		super();
		this.name = name;
		this.dimension = dimension;
		this.eventLogLevel = eventLogLevel;
	}
	
	public void init(List<TraceLinkedEvent> eventList) {
		setEventList(eventList);
		detectErrors();
		createErrorTableModel();
		calculateScore();
		createErrorTraceList();
	}

	private void createErrorTraceList() {
		for (List<Error> list : getErrorList().values()) {
			if (list != null) {
				for (Error error : list) {
					try {
						XTrace errorTrace = ((TraceLinkedEvent) error.getErrorObject()).getTrace();
						errorTraceList.add(errorTrace);
					} catch (Exception e) {

					}					
				}
			}
		}
		// TODO Auto-generated method stub
		
	}

	public void calculatePreliminaryAttributeScore(List<TraceLinkedEvent> eventList) {
		// TODO Auto-generated method stub
	}

	public void calculateScore() {
		// TODO Auto-generated method stub
		
	}

	public void detectErrors() {
		// TODO Auto-generated method stub
		
	}

	public void calculateDimensionScore(List<Metric> metricsList) {
		double scoreSum = 0;
		double weightSum = 0;
		for (Metric metric : metricsList) {
			if (metric.isUsed()) {
				scoreSum = scoreSum + metric.relativeWeight*metric.getScore();
				weightSum = weightSum + metric.relativeWeight;
			}
		}
		setScore(scoreSum/weightSum);
		setQualityLevel(score);
	
	}
	
	public void setQualityLevel(double score) {
		if (score > (double) 3/4) {
			this.qualityLevel = "High";
		}
		else if (score > (double) 1/4) {
			this.qualityLevel = "Medium";
		} 
		else {
			this.qualityLevel = "Low";
		}
	}
	
	public void createErrorTableModel() {
		
	}
	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		if (score > 1) {
			this.score= 1;
		}
		else if (score < 0) {
			this.score = 0;
		} 
		else {
			this.score = (double) Math.round(Math.pow(score, scoreWeight) * 1000000d)/1000000d;
		}
		setQualityLevel(this.score);
	}
	
	public DefaultTableModel createErrorTableModelLayerTwo(Error error) {
		return errorTableModelLayerTwo;
	}
	
	public DefaultTableModel getErrorTableModelLayerTwo() {
		return errorTableModelLayerTwo;
	}

	public void setErrorTableModelLayerTwo(DefaultTableModel errorTableModelLayerTwo) {
		this.errorTableModelLayerTwo = errorTableModelLayerTwo;
	}

	public String getQualityLevel() {
		return qualityLevel;
	}

	public int getScoreWeight() {
		return scoreWeight;
	}


	public void setScoreWeight(int scoreWeight) {
		this.scoreWeight = scoreWeight;
	}

	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	public double getRelativeWeight() {
		return relativeWeight;
	}

	public void setRelativeWeight(double relativeWeight) {
		this.relativeWeight = relativeWeight;
	}

	public void setQualityLevel(String qualityLevel) {
		this.qualityLevel = qualityLevel;
	}

	public String getEventLogLevel() {
		return eventLogLevel;
	}

	public void setEventLogLevel(String eventLogLevel) {
		this.eventLogLevel = eventLogLevel;
	}

	public String getDimension() {
		return dimension;
	}

	public void setDimension(String dimension) {
		this.dimension = dimension;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DefaultTableModel getErrorTableModel() {
		return errorTableModel;
	}

	public void setErrorTableModel(DefaultTableModel errorTableModel) {
		this.errorTableModel = errorTableModel;
	}
	
	public List<TraceLinkedEvent> getErrorSet() {
		return errorSet;
	}

	public void setErrorSet(List<TraceLinkedEvent> errorSet) {
		this.errorSet = errorSet;
	}

	public LinkedHashMap<Error, List<Error>> getErrorList() {
		return errorList;
	}

	public void setErrorList(LinkedHashMap<Error, List<Error>> errorList) {
		this.errorList = errorList;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public List<TraceLinkedEvent> getEventList() {
		return eventList;
	}

	public void setEventList(List<TraceLinkedEvent> eventList) {
		this.eventList = new ArrayList(eventList);
	}

	public class Error {
		private Object errorObject;
		private boolean isWhitelisted = false;
		
		public Error(Object errorObject, boolean isWhitelisted) {
			super();
			this.setErrorObject(errorObject);
			this.setWhitelisted(isWhitelisted);
		}

		public Object getErrorObject() {
			return errorObject;
		}

		public void setErrorObject(Object errorObject) {
			this.errorObject = errorObject;
		}

		public boolean isWhitelisted() {
			return isWhitelisted;
		}

		public void setWhitelisted(boolean isWhitelisted) {
			this.isWhitelisted = isWhitelisted;
		}
	}	
}
