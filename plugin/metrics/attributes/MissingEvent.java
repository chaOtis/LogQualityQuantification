package org.processmining.logqualityquantification.plugin.metrics.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;
import org.processmining.logqualityquantification.plugin.sorter.EventNameSorter;
import org.processmining.logqualityquantification.plugin.sorter.TimestampSorter;
import org.processmining.logqualityquantification.plugin.sorter.TraceSorter;

public class MissingEvent extends Metric {

	int missingTransitionCount = 0;

	public MissingEvent(String name, String dimension, String logLevel) {
		super(name, dimension, logLevel);
	}

	public void calculateScore() {
		if (getEventList().size() + missingTransitionCount > 0) {
			int affectedEvents = 0;
			int affectedTraces = 0;
			int totalEvents = getEventList().size() + missingTransitionCount;
			int totalTraces;
			HashSet<XTrace> traceSet = new HashSet<>();
			for (TraceLinkedEvent tle : getEventList()) {
				traceSet.add(tle.getTrace());
			}
			totalTraces = traceSet.size();
			HashSet<XTrace> errorTraceSet = new HashSet<>();
			for (List<Error> list : getErrorList().values()) {
				for (Error error : list) {
					if (!error.isWhitelisted()) {
						errorTraceSet.add((((TraceLinkedEvent) error.getErrorObject())).getTrace());
					}
				}
			}
			affectedTraces = errorTraceSet.size();
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
		setScoreWeight(4);
		getEventList().removeIf(n -> (XTimeExtension.instance().extractTimestamp(n.getEvent()) == null));
		Collections.sort(getEventList(),
				new TraceSorter().thenComparing(new EventNameSorter()).thenComparing(new TimestampSorter()));
		ListIterator<TraceLinkedEvent> lit = getEventList().listIterator();
		for (TraceLinkedEvent tle : getEventList()) {
			if (XLifecycleExtension.instance().extractTransition(tle.getEvent()) == null) {
				missingTransitionCount++;
				String description = XConceptExtension.instance().extractName(tle.getEvent()) + " (Transition missing)";
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
				getErrorList().put(keyError, list);
			}
		}
		getEventList().removeIf(n -> (XLifecycleExtension.instance().extractTransition(n.getEvent()) == null));
		int eventStartIndex = 0;
		int eventEndIndex = 0;
		lit = getEventList().listIterator();
		while (lit.hasNext()) {
			lit.next();
			if (!lit.hasNext()
					|| !XConceptExtension.instance().extractName(getEventList().get(lit.nextIndex()).getTrace())
							.equals(XConceptExtension.instance()
									.extractName(getEventList().get(lit.previousIndex()).getTrace()))
					|| !XConceptExtension.instance().extractName(getEventList().get(lit.nextIndex()).getEvent())
							.equals(XConceptExtension.instance()
									.extractName(getEventList().get(lit.previousIndex()).getEvent()))) {
				eventEndIndex = lit.nextIndex() - 1;
				List<TraceLinkedEvent> startList = new ArrayList<>();
				for (int i = eventStartIndex; i <= eventEndIndex; i++) {
					if (XLifecycleExtension.instance().extractTransition(getEventList().get(i).getEvent())
							.equals("start")) {
						startList.add(getEventList().get(i));
					} else if (XLifecycleExtension.instance().extractTransition(getEventList().get(i).getEvent())
							.equals("complete")) {
						if (startList.size() > 0) {
							if (XOrganizationalExtension.instance()
									.extractResource(getEventList().get(i).getEvent()) != null) {
								for (TraceLinkedEvent tle : startList) {
									if (XOrganizationalExtension.instance()
											.extractResource(getEventList().get(i).getEvent())
											.equals(XOrganizationalExtension.instance()
													.extractResource(tle.getEvent()))) {
										startList.remove(tle);
										break;
									}
								}
							} else {
								for (TraceLinkedEvent tle : startList) {
									if (XOrganizationalExtension.instance().extractResource(tle.getEvent()) == null) {
										startList.remove(tle);
										break;
									}
								}
							}
						} else {
							String description = XConceptExtension.instance()
									.extractName(getEventList().get(i).getEvent()) + " (related start event missing)";
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
							list.add(new Error(getEventList().get(i), false));
							getErrorList().put(keyError, list);
						}
					}
				}
				for (TraceLinkedEvent tle : startList) {
					String description = XConceptExtension.instance().extractName(tle.getEvent())
							+ " (related complete event missing)";
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
					getErrorList().put(keyError, list);
				}
				startList.clear();
				eventStartIndex = lit.nextIndex();
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
		errorTableModel.addColumn("Errortype");
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
		errorTableModelLayerTwo.addColumn("Missing element");
		errorTableModelLayerTwo.addColumn("Timestamp");
		errorTableModelLayerTwo.addColumn("Whitelisting?");
		String missingElement;
		for (Error error2 : getErrorList().get(error)) {
			if (XLifecycleExtension.instance()
					.extractTransition(((TraceLinkedEvent) error2.getErrorObject()).getEvent()) != null) {
				if (XLifecycleExtension.instance()
						.extractTransition(((TraceLinkedEvent) error2.getErrorObject()).getEvent()).equals("start")) {
					missingElement = "related complete event";
				} else {
					missingElement = "related start event";
				}
			} else {
				missingElement = "transition completely missing";
			}
			errorTableModelLayerTwo.addRow(new Object[] { error, error2,
					XConceptExtension.instance().extractName(((TraceLinkedEvent) error2.getErrorObject()).getTrace()),
					XConceptExtension.instance().extractName(((TraceLinkedEvent) error2.getErrorObject()).getEvent()),
					missingElement,
					XTimeExtension.instance().extractTimestamp(((TraceLinkedEvent) error2.getErrorObject()).getEvent()),
					(boolean) error2.isWhitelisted() });
		}
		return errorTableModelLayerTwo;
	}

	public void eventAusgabe() {
		if (getErrorList().size() != 0) {
			System.out.println(XConceptExtension.instance().extractName(
					((TraceLinkedEvent) getErrorList().entrySet().iterator().next().getValue().get(0).getErrorObject())
							.getEvent()));
		} else {
			System.out.println("Errorlist empty");
		}

	}

	public Date getTimestamp() { //momentan zu Testzwecken so modifiziert, dass es den Timestamp des Events zurückgibt, um diesen dann im Missing Timestamp einzufügen
		TraceLinkedEvent tle = ((TraceLinkedEvent) getErrorList().entrySet().iterator().next().getValue().get(0)
				.getErrorObject());
		XEvent eventOne = tle.getEvent();
		return XTimeExtension.instance().extractTimestamp(eventOne);
	}
}
