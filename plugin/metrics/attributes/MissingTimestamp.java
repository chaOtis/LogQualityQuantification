package org.processmining.logqualityquantification.plugin.metrics.attributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;

public class MissingTimestamp extends Metric {

	public MissingTimestamp(String name, String dimension, String logLevel) {
		super(name, dimension, logLevel);
	}

	public void calculateScore() {
		if (getEventList().size() > 0) {
			int affectedEvents = 0;
			int affectedTraces = 0;
			int totalEvents = getEventList().size();
			int totalTraces;
			HashSet<XTrace> traceSet = new HashSet<>();
			for (TraceLinkedEvent tle : getEventList()) {
				traceSet.add(tle.getTrace());
			}
			totalTraces = traceSet.size();
			traceSet.clear();
			for (List<Error> list : getErrorList().values()) {
				for (Error error : list) {
					if (!error.isWhitelisted()) {
						traceSet.add(((TraceLinkedEvent) error.getErrorObject()).getTrace());
					}
				}
			}
			affectedTraces = traceSet.size();
			for (List<Error> list : getErrorList().values()) {
				for (Error error : list) {
					if (!error.isWhitelisted()) {
						affectedEvents++;
					}
				}
			}
			double score = 1 - ((double) affectedEvents / totalEvents + (double) affectedTraces / totalTraces) / 2;
			setScore(score);
		} else {
			setScore(1);
		}
	}

	public void detectErrors() {
		setScoreWeight(2);
		for (TraceLinkedEvent tle : getEventList()) {
			if (XTimeExtension.instance().extractTimestamp(tle.getEvent()) == null) {
				String description = XConceptExtension.instance().extractName(tle.getEvent());
				List<Error> list = new ArrayList<Error>();
				Error keyError = new Error(description, false);
				boolean descriptionExists = false;
				for (Error error : getErrorList().keySet()) {
					if (error.getErrorObject().equals(description)) {
						descriptionExists = true;
						keyError = error;
						break;
					}
				}
				if (descriptionExists) {
					list = getErrorList().get(keyError);
				}
				list.add(new Error(tle, false));
				getErrorList().put(keyError,
						list); /* keyError (Description, Error-liste) */
			}
		}
	}

	public void createErrorTableModel() {
		DefaultTableModel errorTableModel = new DefaultTableModel();
		if (getErrorList().size() > 0) {
			errorTableModel = new DefaultTableModel() {
				public Class<?> getColumnClass(int colIndex) {
					return getValueAt(0, colIndex).getClass();
				}
			};
		}
		errorTableModel.addColumn("Errortype");
		errorTableModel.addColumn("Activity name");
		errorTableModel.addColumn("Occurences");
		errorTableModel.addColumn("Whitelisting?");
		errorTableModel.addColumn("");
		for (Error errorType : getErrorList().keySet()) {
			int occurences = getErrorList().get(errorType).size();
			errorTableModel.addRow(new Object[] { errorType, errorType.getErrorObject(), occurences,
					errorType.isWhitelisted(), "errorlist" });
		}
		setErrorTableModel(errorTableModel);
	}

	public DefaultTableModel createErrorTableModelLayerTwo(Error error) {
		DefaultTableModel errorTableModelLayerTwo = new DefaultTableModel() {
			public Class<?> getColumnClass(int colIndex) {
				return getValueAt(0, colIndex).getClass();
			}
		};
		errorTableModelLayerTwo.addColumn("Errortype");
		errorTableModelLayerTwo.addColumn("Error");
		errorTableModelLayerTwo.addColumn("Case ID");
		errorTableModelLayerTwo.addColumn("Event");
		errorTableModelLayerTwo.addColumn("Timestamp");
		errorTableModelLayerTwo.addColumn("Whitelisting?");
		for (Error error2 : getErrorList().get(error)) {
			errorTableModelLayerTwo.addRow(new Object[] { error, error2,
					XConceptExtension.instance().extractName(((TraceLinkedEvent) error2.getErrorObject()).getTrace()),
					XConceptExtension.instance().extractName(((TraceLinkedEvent) error2.getErrorObject()).getEvent()),
					" ", (boolean) error2.isWhitelisted() });
		}
		return errorTableModelLayerTwo;
	}

	public void eventAusgabe() {
		if (getErrorList().size() != 0) {
			XEvent event = ((TraceLinkedEvent) getErrorList().entrySet().iterator().next().getValue().get(0)
					.getErrorObject()).getEvent();
			System.out.println(XConceptExtension.instance().extractName(event) + ": "
					+ XTimeExtension.instance().extractTimestamp(event));
		} else {
			System.out.println("Errorlist empty");
		}
	}

	public Date getTimestamp() { //Holt sich aktuell den Timestamp des ersten Events aus der Errorliste zu MissingTimestamps
		TraceLinkedEvent tle = ((TraceLinkedEvent) getErrorList().entrySet().iterator().next().getValue().get(0)
				.getErrorObject());
		XEvent eventOne = tle.getEvent();
		return XTimeExtension.instance().extractTimestamp(eventOne);

	}

	public void errorListElements(XLog log) {
		System.out.println("Amount of Errortypes: " + getErrorList().size());
		for (Error errorType : getErrorList().keySet()) {
			int i = 0;
			System.out.println("Amount of Errors in Errortype " + errorType.getErrorObject() + ": "
					+ getErrorList().get(errorType).size());
			for (Error errorElement : getErrorList().get(errorType)) {
				XTrace errorTrace = ((TraceLinkedEvent) errorElement.getErrorObject()).getTrace();
				XEvent errorEvent = ((TraceLinkedEvent) errorElement.getErrorObject()).getEvent();
				testMethode(errorTrace, log);
				System.out.println(i + ". " + XConceptExtension.instance().extractName(errorTrace) + " (Trace), "
						+ XConceptExtension.instance().extractName(errorEvent)+ " (Event), "
						+ XTimeExtension.instance().extractTimestamp(errorEvent)+ " (Timestamp)");
				i++;
				
			}
		}
	}
	
	public void aenderTimestamp(Date date) { //Methode bekommt ein Date übergeben, um dieses in das fehlerhafte Event einzufügen
		XFactoryNaiveImpl factory = new XFactoryNaiveImpl();
		if (getTimestamp() == null) {
			XAttribute timestamp = factory.createAttributeTimestamp("time:timestamp", date, null);
			XEvent eventOne = ((TraceLinkedEvent) getErrorList().entrySet().iterator().next().getValue().get(0)
					.getErrorObject()).getEvent();
			eventOne.getAttributes().put("time:timestamp", timestamp);
		}
	}

	public void changeTimestamp(XLog log) {
		XFactoryNaiveImpl factory = new XFactoryNaiveImpl();
		Date date = new Date();
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				XAttribute timestamp = factory.createAttributeTimestamp("time:timestamp", date, null);
				event.getAttributes().replace("time:timestamp", timestamp);
				break;
			}
		}
	}
	public void testMethode(XTrace fehlerTrace ,XLog testLog) {
		for (XTrace testTrace : testLog) {
			System.out.println("Trace: "+XConceptExtension.instance().extractName(testTrace)+" "+fehlerTrace.equals(testTrace));
		}
	}
}