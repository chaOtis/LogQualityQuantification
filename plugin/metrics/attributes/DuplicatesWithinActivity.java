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
import org.processmining.logqualityquantification.plugin.sorter.EventNameSorter;
import org.processmining.logqualityquantification.plugin.sorter.TimestampSorter;
import org.processmining.logqualityquantification.plugin.sorter.TraceSorter;

public class DuplicatesWithinActivity extends Metric {

	public DuplicatesWithinActivity(String name, String dimension, String eventLogLevel) {
		super(name, dimension, eventLogLevel);
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
		Collections.sort(getEventList(),
				new TraceSorter().thenComparing(new EventNameSorter()).thenComparing(new TimestampSorter()));
		HashSet<TraceLinkedEvent> errorSet = new HashSet<TraceLinkedEvent>();
		int eventCounter = 0;
		for (TraceLinkedEvent tle : getEventList()) {
			if (eventCounter != 0
					&& XConceptExtension.instance().extractName(tle.getEvent()).equals(
							XConceptExtension.instance().extractName(getEventList().get(eventCounter - 1).getEvent()))
					&& XConceptExtension.instance().extractName(tle.getTrace()).equals(
							XConceptExtension.instance().extractName(getEventList().get(eventCounter - 1).getTrace()))
					&& XTimeExtension.instance().extractTimestamp(tle.getEvent()).equals(XTimeExtension.instance()
							.extractTimestamp(getEventList().get(eventCounter - 1).getEvent()))) {
				errorSet.add(tle);
				errorSet.add(getEventList().get(eventCounter - 1));
			}
			eventCounter++;
		}
		if (errorSet.size() > 1) {
			for (TraceLinkedEvent tle : errorSet) {
				String description = XConceptExtension.instance().extractName(tle.getEvent());
				List<Error> list = new ArrayList<Error>();
				Error keyError = getErrorList().keySet().stream()
						.filter(error -> description.equals(error.getErrorObject())).findFirst().orElse(null);
				if (keyError == null) {
					keyError = new Error(description, false);
				} else {
					list = getErrorList().get(keyError);
				}
				list.add(new Error(tle, false));
				getErrorList().put(keyError, list);
			}
		}
		errorSet.clear();
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
		errorTableModel.addColumn("Affected activities");
		errorTableModel.addColumn("Whitelisting?");
		errorTableModel.addColumn("");
		for (Error errorType : getErrorList().keySet()) {
			errorTableModel.addRow(new Object[] { errorType, errorType.getErrorObject(),
					getErrorList().get(errorType).size(), errorType.isWhitelisted(), "errorlist" });
		}
		setErrorTableModel(errorTableModel);
	}

	public DefaultTableModel createErrorTableModelLayerTwo(Error error) {
		DefaultTableModel errorTableModel = new DefaultTableModel();
		errorTableModel.addColumn("Errortype");
		errorTableModel.addColumn("Error");
		errorTableModel.addColumn("Case ID");
		errorTableModel.addColumn("Event");
		errorTableModel.addColumn("Timestamp");
		errorTableModel.addColumn("Transition");
		errorTableModel.addColumn("Whitelisting?");
		for (Error error2 : getErrorList().get(error)) {
			String transition;
			if (XLifecycleExtension.instance()
					.extractTransition(((TraceLinkedEvent) error2.getErrorObject()).getEvent()) != null) {
				transition = XLifecycleExtension.instance()
						.extractTransition(((TraceLinkedEvent) error2.getErrorObject()).getEvent());
			} else {
				transition = " ";
			}
			errorTableModel.addRow(new Object[] { error, error2,
					XConceptExtension.instance().extractName(((TraceLinkedEvent) error2.getErrorObject()).getTrace()),
					XConceptExtension.instance().extractName(((TraceLinkedEvent) error2.getErrorObject()).getEvent()),
					XTimeExtension.instance().extractTimestamp(((TraceLinkedEvent) error2.getErrorObject()).getEvent()),
					transition, (boolean) error2.isWhitelisted() });
		}
		return errorTableModel;
	}
}
