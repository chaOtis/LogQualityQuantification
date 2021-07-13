package org.processmining.logqualityquantification.plugin.metrics.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;
import org.processmining.logqualityquantification.plugin.sorter.RessourceSorter;
import org.processmining.logqualityquantification.plugin.sorter.TimestampSorter;

public class OverlappingActivities extends Metric {

	public OverlappingActivities(String name, String dimension, String logLevel) {
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
						traceSet.add((((TraceLinkedEvent[]) error.getErrorObject())[0]).getTrace());
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
		getEventList().removeIf(n -> (XLifecycleExtension.instance().extractTransition(n.getEvent()) == null));
		getEventList().removeIf(n -> (XOrganizationalExtension.instance().extractResource(n.getEvent()) == null));
		getEventList().removeIf(n -> (XTimeExtension.instance().extractTimestamp(n.getEvent()) == null));
		String ressource = "ressource undefined";
		Collections.sort(getEventList(), new RessourceSorter().thenComparing(new TimestampSorter()));
		TraceLinkedEvent startEvent = null;
		for (TraceLinkedEvent tle : getEventList()) {
			if (ressource.equals(XOrganizationalExtension.instance().extractResource(tle.getEvent()))) {
				if (XLifecycleExtension.instance().extractTransition(tle.getEvent()).equals("start")) {
					if (startEvent == null) {
						startEvent = new TraceLinkedEvent(tle.getTrace(), tle.getEvent());
					} else {
						String description = XOrganizationalExtension.instance().extractResource(tle.getEvent());
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
						TraceLinkedEvent[] errorSet = { startEvent, tle };
						list.add(new Error(errorSet, false));
						getErrorList().put(keyError, list);
					}
				} else if (XLifecycleExtension.instance().extractTransition(tle.getEvent()).equals("complete")) {
					startEvent = null;
				}
			} else {
				ressource = XOrganizationalExtension.instance().extractResource(tle.getEvent());
				if (XLifecycleExtension.instance().extractTransition(tle.getEvent()).equals("start")) {
					startEvent = tle;
				} else {
					startEvent = null;
				}
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
		errorTableModel.addColumn("Resource");
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
		errorTableModelLayerTwo.addColumn("Running Case");
		errorTableModelLayerTwo.addColumn("Running Event");
		errorTableModelLayerTwo.addColumn("Starting Case");
		errorTableModelLayerTwo.addColumn("Starting Event");
		errorTableModelLayerTwo.addColumn("Timestamp");
		errorTableModelLayerTwo.addColumn("Whitelisting?");
		for (Error error2 : getErrorList().get(error)) {
			errorTableModelLayerTwo.addRow(new Object[] { error, error2,
					XConceptExtension.instance()
							.extractName(((TraceLinkedEvent[]) error2.getErrorObject())[0].getTrace()),
					XConceptExtension.instance()
							.extractName(((TraceLinkedEvent[]) error2.getErrorObject())[0].getEvent()),
					XConceptExtension.instance()
							.extractName(((TraceLinkedEvent[]) error2.getErrorObject())[1].getTrace()),
					XConceptExtension.instance()
							.extractName(((TraceLinkedEvent[]) error2.getErrorObject())[1].getEvent()),
					XTimeExtension.instance()
							.extractTimestamp(((TraceLinkedEvent[]) error2.getErrorObject())[1].getEvent()),
					(boolean) error2.isWhitelisted() });
		}
		return errorTableModelLayerTwo;
	}
}
