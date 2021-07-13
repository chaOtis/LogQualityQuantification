package org.processmining.logqualityquantification.plugin.metrics.attributes;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.table.DefaultTableModel;

import org.apache.commons.collections.IteratorUtils;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;
import org.processmining.logqualityquantification.plugin.sorter.TimestampSorter;
import org.processmining.logqualityquantification.plugin.sorter.TraceSorter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Table;

public class InfrequentActivityOrdering extends Metric {

	public InfrequentActivityOrdering(String name, String dimension, String logLevel) {
		super(name, dimension, logLevel);
	}

	public void calculateScore() {
		if (getEventList().size() > 0) {
			int affectedTraces = 0;
			int totalTraces = 0;
			HashSet<XTrace> traceSet = new HashSet<>();
			for (TraceLinkedEvent tle : getEventList()) {
				traceSet.add(tle.getTrace());
			}
			totalTraces = traceSet.size();
			HashSet<XTrace> errorTraceSet = new HashSet<>();
			for (List<Error> list : getErrorList().values()) {
				for (Error error : list) {
					if (!error.isWhitelisted()) {
						errorTraceSet.add((((TraceLinkedEvent[]) error.getErrorObject())[0]).getTrace());
					}
				}
			}
			affectedTraces = errorTraceSet.size();
			double score = 1 - (double) affectedTraces / totalTraces;
			setScore(score);
		} else {
			setScore(1);
		}
	}

	public void detectErrors() {
		setScoreWeight(3);
		boolean transitionExists = true;
		for (TraceLinkedEvent tle : getEventList()) {
			if (XLifecycleExtension.instance().extractTransition(tle.getEvent()) == null) {
				transitionExists = false;
			}
		}
		if (transitionExists == true) {
			getEventList()
					.removeIf(n -> (XLifecycleExtension.instance().extractTransition(n.getEvent()).equals("start")));
		}
		getEventList().removeIf(n -> (XTimeExtension.instance().extractTimestamp(n.getEvent()) == null));
		if (getEventList().size() > 0) {

			Collections.sort(getEventList(), new TraceSorter().thenComparing(new TimestampSorter()));

			//create and fill 2x2 table with preceding events in rows and following events in columns based on eventList
			Table<String, String, Integer> followsMatrix = createFollowsMatrix(getEventList());

			//get Infrequent relations based on followsMatrix
			List<Map<String, Collection<String>>> relations = getRelations(followsMatrix, getEventList());
			Map<String, Collection<String>> infrequentRelations = relations.get(0);
			Map<String, Collection<String>> frequentRelations = relations.get(1);

			//add infrequent relations to errorList if frequent follower does occur in the trace
			List<TraceLinkedEvent> traceList = new ArrayList<>();
			TraceLinkedEvent precedor = null;
			TraceLinkedEvent follower = null;

			int traceStartIndex = 0;
			int traceEndIndex = 0;
			ListIterator<TraceLinkedEvent> lit = IteratorUtils.toListIterator(getEventList().iterator());
			do {
				lit.next();
				if (!lit.hasNext() || !XConceptExtension.instance()
						.extractName(getEventList().get(lit.nextIndex()).getTrace()).equals(XConceptExtension.instance()
								.extractName(getEventList().get(lit.previousIndex()).getTrace()))) {
					traceEndIndex = lit.nextIndex() - 1;
					for (int i = traceStartIndex; i < traceEndIndex; i++) {
						if (infrequentRelations.containsKey(
								XConceptExtension.instance().extractName(getEventList().get(i).getEvent())) == true) {
							if (infrequentRelations
									.get(XConceptExtension.instance().extractName(getEventList().get(i).getEvent()))
									.contains(XConceptExtension.instance()
											.extractName(getEventList().get(i + 1).getEvent()))) {
								for (int k = traceStartIndex; k <= traceEndIndex; k++) {
									if (frequentRelations
											.get(XConceptExtension.instance().extractName(getEventList().get(i).getEvent()))
											.contains(XConceptExtension.instance()
													.extractName(getEventList().get(k).getEvent()))) {

										String description = XConceptExtension.instance()
												.extractName(getEventList().get(i).getEvent()) + " -> "
												+ XConceptExtension.instance().extractName(getEventList().get(k).getEvent());
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
										TraceLinkedEvent[] errorSet = { getEventList().get(i), getEventList().get(k) };
										list.add(new Error(errorSet, false));
										getErrorList().put(keyError, list);
										break;
									}
								}
							}
						}
					}
					traceStartIndex = lit.nextIndex();
				}

			} while (lit.hasNext());
		}
	}

	private List<Map<String, Collection<String>>> getRelations(Table<String, String, Integer> followsMatrix,
			List<TraceLinkedEvent> eventList) {
		ListMultimap<String, String> infrequentRelations = ArrayListMultimap.create();
		ListMultimap<String, String> frequentRelations = ArrayListMultimap.create();
		Set<String> eventNamesSet = new HashSet<>();
		for (TraceLinkedEvent tle : eventList) {
			eventNamesSet.add(XConceptExtension.instance().extractName(tle.getEvent()));
		}
		for (String eventName : eventNamesSet) {
			int eventCountRow = followsMatrix.row(eventName).values().stream().reduce(0, Integer::sum);
			Map<String, Integer> matrixRowSorted = followsMatrix.row(eventName).entrySet().stream()
					.sorted(comparingByValue())
					.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
			double thresholdRow = 0;
			for (Entry e : matrixRowSorted.entrySet()) {
				if (matrixRowSorted.get(e.getKey()) != 0) {
					thresholdRow = thresholdRow + ((double) matrixRowSorted.get(e.getKey()) / eventCountRow);
					if (thresholdRow <= 0.2) {
						int eventCountColumn = followsMatrix.column((String) e.getKey()).values().stream().reduce(0,
								Integer::sum);
						double thresholdColumn = ((double) matrixRowSorted.get(e.getKey()) / eventCountColumn);
						if (thresholdColumn <= 0.2) {
							infrequentRelations.put(eventName, (String) e.getKey());
						}
					} else {
						frequentRelations.put(eventName, (String) e.getKey());
					}
				}
			}
		}
		List<Map<String, Collection<String>>> relations = new ArrayList<>();
		relations.add(infrequentRelations.asMap());
		relations.add(frequentRelations.asMap());
		return relations;
	}

	private Table<String, String, Integer> createFollowsMatrix(List<TraceLinkedEvent> eventList) {
		Set<String> eventNames = new HashSet<>();
		for (TraceLinkedEvent tle : eventList) {
			eventNames.add(XConceptExtension.instance().extractName(tle.getEvent()));
		}
		Table<String, String, Integer> followsMatrix = HashBasedTable.create();
		for (String i : eventNames) {
			for (String k : eventNames) {
				followsMatrix.put(i, k, 0);
			}
		}
		TraceLinkedEvent precedor = null;
		TraceLinkedEvent follower = null;
		for (TraceLinkedEvent tle : eventList) {
			follower = tle;
			if (precedor != null && XConceptExtension.instance().extractName(precedor.getTrace()) == XConceptExtension
					.instance().extractName(follower.getTrace())) {
				followsMatrix.put(XConceptExtension.instance().extractName(precedor.getEvent()),
						XConceptExtension.instance().extractName(follower.getEvent()),
						followsMatrix.get(XConceptExtension.instance().extractName(precedor.getEvent()),
								XConceptExtension.instance().extractName(follower.getEvent())) + 1);
			}
			precedor = follower;
		}
		return followsMatrix;
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
		errorTableModelLayerTwo.addColumn("Predecing Event");
		errorTableModelLayerTwo.addColumn("Expected Following Event");
		errorTableModelLayerTwo.addColumn("Timestamp");
		errorTableModelLayerTwo.addColumn("Whitelisting?");
		for (Error error2 : getErrorList().get(error)) {
			errorTableModelLayerTwo.addRow(new Object[] { error, error2,
					XConceptExtension.instance()
							.extractName(((TraceLinkedEvent[]) error2.getErrorObject())[0].getTrace()),
					XConceptExtension.instance()
							.extractName(((TraceLinkedEvent[]) error2.getErrorObject())[0].getEvent()),
					XConceptExtension.instance()
							.extractName(((TraceLinkedEvent[]) error2.getErrorObject())[1].getEvent()),
					XTimeExtension.instance()
							.extractTimestamp(((TraceLinkedEvent[]) error2.getErrorObject())[0].getEvent()),
					(boolean) error2.isWhitelisted() });
		}
		return errorTableModelLayerTwo;
	}
}
