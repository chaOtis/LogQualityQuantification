package org.processmining.logqualityquantification.plugin.metrics.attributes;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;
import org.processmining.logqualityquantification.plugin.sorter.TimestampSorter;
import org.processmining.logqualityquantification.plugin.sorter.TraceSorter;

public class MissingTrace extends Metric {

	public MissingTrace(String name, String dimension, String logLevel) {
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
			for (Error error : getErrorList().keySet()) {
				if (!error.isWhitelisted()) {
					try {
						errorTraceSet.add(((TraceLinkedEvent[]) error.getErrorObject())[0].getTrace());
					} catch (Exception e) {
						errorTraceSet.add(((TraceLinkedEvent[]) error.getErrorObject())[1].getTrace());
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
		getEventList().removeIf(n -> (XTimeExtension.instance().extractTimestamp(n.getEvent()) == null));
		Collections.sort(getEventList(), new TraceSorter().thenComparing(new TimestampSorter()));
		Map<XTrace, Long> sortedTraces = createSortedTracesMap(getEventList());
		List<TraceDistance> sameDayTraceDistances = createTraceDistanceList(sortedTraces);
		double tsdMediumValue = calculateMediumValue(sameDayTraceDistances);
		double tsdStandardDeviation = calculateStandardDeviation(tsdMediumValue, sameDayTraceDistances);
		Map<XTrace, Long> firstTraceOfDayList = createFirstTraceOfDayList(sortedTraces);
		double fttMediumValue = calculateFTTMediumValue(firstTraceOfDayList);
		double fttStandardDeviation = calculateFTTStandardDeviation(fttMediumValue, firstTraceOfDayList);
		Map<XTrace, Long> lastTraceOfDayList = createLastTraceOfDayList(sortedTraces);
		double lttMediumValue = calculateLTTMediumValue(lastTraceOfDayList);
		double lttStandardDeviation = calculateLTTStandardDeviation(lttMediumValue, lastTraceOfDayList);

		//testSameDayTraces
		testSameDayTraces(tsdMediumValue, tsdStandardDeviation, sameDayTraceDistances);

		//testFirstTraceOfDay
		testFirstTraceTime(tsdMediumValue, tsdStandardDeviation, fttMediumValue, fttStandardDeviation,
				firstTraceOfDayList);

		//testLastTraceOfDay
		testLastTraceTime(tsdMediumValue, tsdStandardDeviation, lttMediumValue, lttStandardDeviation,
				lastTraceOfDayList);
	}

	private void testLastTraceTime(double tsdMediumValue, double tsdStandardDeviation, double lttMediumValue,
			double lttStandardDeviation, Map<XTrace, Long> lastTraceOfDayList) {
		for (Entry entry : lastTraceOfDayList.entrySet()) {
			if ((long) entry.getValue() > (tsdMediumValue + lttMediumValue
					+ 2 * (Math.sqrt(Math.pow(tsdStandardDeviation, 2) + Math.pow(lttStandardDeviation, 2))))) {
				TraceLinkedEvent[] errorSet = { new TraceLinkedEvent((XTrace) entry.getKey(), null), null };
				getErrorList().put(new Error(errorSet, false), null);
			}
		}

	}

	private double calculateLTTStandardDeviation(double lttMediumValue, Map<XTrace, Long> lastTraceOfDayList) {
		double lttStandardDeviation;
		double lttDeviationSum = 0;
		for (Entry entry : lastTraceOfDayList.entrySet()) {
			lttDeviationSum = lttDeviationSum + Math.pow(((long) entry.getValue() - lttMediumValue), 2);
		}
		lttStandardDeviation = Math.sqrt(lttDeviationSum / lastTraceOfDayList.size());
		return lttStandardDeviation;
	}

	private double calculateLTTMediumValue(Map<XTrace, Long> lastTraceOfDayList) {
		double lttMediumValue = 0;
		long lttMediumValueSum = 0;
		for (Entry entry : lastTraceOfDayList.entrySet()) {
			lttMediumValueSum = lttMediumValueSum + (long) entry.getValue();
		}
		lttMediumValue = (double) lttMediumValueSum / lastTraceOfDayList.size();
		return lttMediumValue;
	}

	private Map<XTrace, Long> createLastTraceOfDayList(Map<XTrace, Long> sortedTraces) {
		Map<XTrace, Long> lastTraceOfDayList = new HashMap<>();
		int precedorTraceDay = 0;
		int followerTraceDay = 0;
		XTrace precedorTrace = null;
		long precedorMillies = 0;
		for (Entry entry : sortedTraces.entrySet()) {
			Date d = new Date((long) entry.getValue());
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			followerTraceDay = c.get(Calendar.DAY_OF_MONTH);
			if (precedorTraceDay != followerTraceDay && precedorTrace != null) {
				lastTraceOfDayList.put(precedorTrace, precedorMillies);
			}
			precedorMillies = TimeUnit.DAYS.toMillis(1) - (TimeUnit.HOURS.toMillis(c.get(Calendar.HOUR_OF_DAY))
					+ TimeUnit.MINUTES.toMillis(c.get(Calendar.MINUTE))
					+ TimeUnit.SECONDS.toMillis(c.get(Calendar.SECOND)) + c.get(Calendar.MILLISECOND));
			precedorTrace = (XTrace) entry.getKey();
			precedorTraceDay = followerTraceDay;
		}
		return lastTraceOfDayList;
	}

	private void testFirstTraceTime(double tsdMediumValue, double tsdStandardDeviation, double fttMediumValue,
			double fttStandardDeviation, Map<XTrace, Long> firstTraceOfDayList) {
		for (Entry entry : firstTraceOfDayList.entrySet()) {
			if ((long) entry.getValue() > (tsdMediumValue + fttMediumValue
					+ 2 * (Math.sqrt(Math.pow(tsdStandardDeviation, 2) + Math.pow(fttStandardDeviation, 2))))) {
				TraceLinkedEvent[] errorSet = { null, new TraceLinkedEvent((XTrace) entry.getKey(), null) };
				System.out.println("Error");
				getErrorList().put(new Error(errorSet, false), null);
			}
		}
	}

	private double calculateFTTStandardDeviation(double fttMediumValue, Map<XTrace, Long> firstTraceOfDayList) {
		double fttStandardDeviation;
		double fttDeviationSum = 0;
		for (Entry entry : firstTraceOfDayList.entrySet()) {
			fttDeviationSum = fttDeviationSum + Math.pow(((long) entry.getValue() - fttMediumValue), 2);
		}
		fttStandardDeviation = Math.sqrt(fttDeviationSum / firstTraceOfDayList.size());
		return fttStandardDeviation;
	}

	private double calculateFTTMediumValue(Map<XTrace, Long> firstTraceOfDayList) {
		double fttMediumValue = 0;
		long fttMediumValueSum = 0;
		for (Entry entry : firstTraceOfDayList.entrySet()) {
			fttMediumValueSum = fttMediumValueSum + (long) entry.getValue();
		}
		fttMediumValue = (double) fttMediumValueSum / firstTraceOfDayList.size();
		return fttMediumValue;
	}

	private Map<XTrace, Long> createFirstTraceOfDayList(Map<XTrace, Long> sortedTraces) {
		Map<XTrace, Long> firstTraceOfDayList = new HashMap<>();
		int precedorTraceDay = 0;
		int followerTraceDay = 0;
		for (Entry entry : sortedTraces.entrySet()) {
			long followerMillies = (long) entry.getValue();
			Date d = new Date(followerMillies);
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			followerTraceDay = c.get(Calendar.DAY_OF_MONTH);
			followerMillies = c.get(Calendar.MILLISECOND) + 1000
					* (c.get(Calendar.SECOND) + 60 * (c.get(Calendar.MINUTE) + 60 * (c.get(Calendar.HOUR_OF_DAY))));
			if (precedorTraceDay != followerTraceDay) {
				firstTraceOfDayList.put((XTrace) entry.getKey(), followerMillies);
			}
			precedorTraceDay = followerTraceDay;
		}
		return firstTraceOfDayList;
	}

	private void testSameDayTraces(double mediumValue, double standardDeviation,
			List<TraceDistance> sameDayTraceDistances) {
		for (TraceDistance td : sameDayTraceDistances) {
			if (td.diffInMillies > (2 * mediumValue + 2 * Math.sqrt(2) * standardDeviation)) {
				TraceLinkedEvent[] errorSet = { new TraceLinkedEvent(td.trace1, null),
						new TraceLinkedEvent(td.trace2, null) };
				System.out.println("Error");
				getErrorList().put(new Error(errorSet, false), null);
			}
		}
	}

	private double calculateStandardDeviation(double mediumValue, List<TraceDistance> traceDistances) {
		double standardDeviation;
		double deviationSum = 0;
		for (TraceDistance td : traceDistances) {
			deviationSum = deviationSum + Math.pow(((double) td.diffInMillies - mediumValue), 2);
		}
		standardDeviation = Math.sqrt((double) (deviationSum / traceDistances.size()));
		return standardDeviation;
	}

	private double calculateMediumValue(List<TraceDistance> traceDistances) {
		double mediumValue;
		long valueSum = 0;
		for (TraceDistance td : traceDistances) {
			valueSum = valueSum + td.diffInMillies;
		}
		mediumValue = (double) valueSum / traceDistances.size();
		//mediumValue = mediumValue * (1-0.1);
		return mediumValue;
	}

	private Map<XTrace, Long> createSortedTracesMap(List<TraceLinkedEvent> eventList) {
		Map<XTrace, Long> sortedTraces = new HashMap<>();
		for (TraceLinkedEvent tle : eventList) {
			if (!sortedTraces.containsKey(tle.getTrace())) {
				Date d = new Date();
				d = XTimeExtension.instance().extractTimestamp(tle.getEvent());
				long dateInMillies = d.getTime();
				sortedTraces.put(tle.getTrace(), dateInMillies);
			}
		}
		sortedTraces = sortedTraces.entrySet().stream().sorted(comparingByValue())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
		return sortedTraces;
	}

	private List<TraceDistance> createTraceDistanceList(Map<XTrace, Long> sortedTraces) {
		List<TraceDistance> sameDayTraceDistances = new ArrayList<TraceDistance>();
		long precedorMillies = 0;
		XTrace precedorTrace = null;
		long followerMillies = 0;
		XTrace followerTrace = null;
		int precedorDay = 0;
		int followerDay = 0;
		for (Entry st : sortedTraces.entrySet()) {
			followerMillies = (long) st.getValue();
			followerTrace = (XTrace) st.getKey();
			Date d = new Date(followerMillies);
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			followerDay = c.get(Calendar.DAY_OF_MONTH);
			boolean sameDayTrace = false;

			if (precedorDay == followerDay) {
				sameDayTrace = true;
			}
			if (sameDayTrace == true) {
				long diffInMillies = followerMillies - precedorMillies;
				sameDayTraceDistances.add(new TraceDistance(precedorTrace, followerTrace, diffInMillies));
			}
			precedorMillies = followerMillies;
			precedorTrace = followerTrace;
			precedorDay = followerDay;
		}
		return sameDayTraceDistances;
	}

	class TraceDistance {
		XTrace trace1;
		XTrace trace2;
		long diffInMillies;

		public TraceDistance(XTrace trace1, XTrace trace2, long diffInMillies) {
			this.trace1 = trace1;
			this.trace2 = trace2;
			this.diffInMillies = diffInMillies;
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
		errorTableModel.addColumn("Preceding Case");
		errorTableModel.addColumn("Following Case");
		errorTableModel.addColumn("Whitelisting?");
		for (Error error : getErrorList().keySet()) {
			String trace1;
			String trace2;
			try {
				trace1 = XConceptExtension.instance()
						.extractName(((TraceLinkedEvent[]) error.getErrorObject())[0].getTrace());
			} catch (Exception e) {
				trace1 = "Start of day";
			}
			try {
				trace2 = XConceptExtension.instance()
						.extractName(((TraceLinkedEvent[]) error.getErrorObject())[1].getTrace());
			} catch (Exception e) {
				trace2 = "End of day";
			}
			errorTableModel.addRow(new Object[] { error, trace1, trace2, (boolean) error.isWhitelisted() });
		}
		setErrorTableModel(errorTableModel);
	}
}
