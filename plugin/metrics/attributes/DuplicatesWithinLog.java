package org.processmining.logqualityquantification.plugin.metrics.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;
import org.processmining.logqualityquantification.plugin.sorter.TimestampSorter;

public class DuplicatesWithinLog extends Metric {

	public DuplicatesWithinLog(String name, String dimension, String logLevel) {
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
		getEventList().removeIf(n -> (XTimeExtension.instance().extractTimestamp(n.getEvent()) == null));
		Collections.sort(getEventList(), new TimestampSorter());
		HashSet<TraceLinkedEvent> sameTimeErrorSet = new HashSet<>();
		int eventCounter = 0;
		for (TraceLinkedEvent tle : getEventList()) {
			if (eventCounter != 0 && XTimeExtension.instance().extractTimestamp(tle.getEvent()).equals(
					XTimeExtension.instance().extractTimestamp(getEventList().get(eventCounter - 1).getEvent()))) {
				sameTimeErrorSet.add(getEventList().get(eventCounter - 1));
				sameTimeErrorSet.add(tle);
			}
			if (eventCounter == getEventList().size() - 1
					|| !XTimeExtension.instance().extractTimestamp(tle.getEvent()).equals(XTimeExtension.instance()
							.extractTimestamp(getEventList().get(eventCounter + 1).getEvent()))) {
				HashSet<String> traceNames = new HashSet<>();
				for (TraceLinkedEvent error : sameTimeErrorSet) {
					traceNames.add(XConceptExtension.instance().extractName(error.getTrace()));
				}
				if (traceNames.size() > 1) {
					String description = null;
					for (TraceLinkedEvent error : sameTimeErrorSet) {
						if (description == null) {
							description = XConceptExtension.instance().extractName(error.getEvent());
						} else {
							description = description + ", "
									+ XConceptExtension.instance().extractName(error.getEvent());
						}
					}
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
					for (TraceLinkedEvent error : sameTimeErrorSet) {
						list.add(new Error(error, false));
					}
					getErrorList().put(keyError, list);
				}
				traceNames.clear();
				sameTimeErrorSet.clear();
			}
			eventCounter++;
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
		errorTableModel.addColumn("Batched Event Group");
		errorTableModel.addColumn("Affected traces");
		errorTableModel.addColumn("Whitelisting?");
		errorTableModel.addColumn("");
		for (Error errorType : getErrorList().keySet()) {
			HashSet<XTrace> traceSet = new HashSet<>();
			for (Error error : getErrorList().get(errorType)) {
				traceSet.add(((TraceLinkedEvent) error.getErrorObject()).getTrace());
			}
			int affectedTraces = traceSet.size();
			errorTableModel.addRow(new Object[] { errorType, errorType.getErrorObject(), affectedTraces,
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
		errorTableModelLayerTwo.addColumn("Transition");
		errorTableModelLayerTwo.addColumn("Whitelisting?");
		for (Error error2 : getErrorList().get(error)) {
			String transition;
			if (XLifecycleExtension.instance()
					.extractTransition(((TraceLinkedEvent) error2.getErrorObject()).getEvent()) != null) {
				transition = XLifecycleExtension.instance()
						.extractTransition(((TraceLinkedEvent) error2.getErrorObject()).getEvent());
			} else {
				transition = " ";
			}
			errorTableModelLayerTwo.addRow(new Object[] { error, error2,
					XConceptExtension.instance().extractName(((TraceLinkedEvent) error2.getErrorObject()).getTrace()),
					XConceptExtension.instance().extractName(((TraceLinkedEvent) error2.getErrorObject()).getEvent()),
					XTimeExtension.instance().extractTimestamp(((TraceLinkedEvent) error2.getErrorObject()).getEvent()),
					transition, (boolean) error2.isWhitelisted() });
		}
		return errorTableModelLayerTwo;
	}
}