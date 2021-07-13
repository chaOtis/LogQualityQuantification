package org.processmining.logqualityquantification.plugin.metrics.attributes;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.stream.IntStream;

import javax.swing.table.DefaultTableModel;

import org.apache.commons.collections.IteratorUtils;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;
import org.processmining.logqualityquantification.plugin.sorter.EventNameSorter;

public class MixedGranularityActivities extends Metric {

	private Map<String, Integer> eventCount = new HashMap<>();

	public MixedGranularityActivities(String name, String dimension, String logLevel) {
		super(name, dimension, logLevel);
	}

	public void calculateScore() {
		if (getEventList().size() > 0) {
			double score = 1;
			Map<String, List<Double>> expectedEventGranularity = new HashMap<>();
			for (Entry<Error, List<Error>> e : getErrorList().entrySet()) {
				int max = 0;
				int j = 0;
				for (Error error : e.getValue()) {
					if ((Double) error.getErrorObject() > (Double) ((Error) e.getValue().toArray()[max])
							.getErrorObject()) {
						max = j;
					}
					j++;
				}
				double[] nullRatio = { 1, (double) 11 / 12, 353.25 / 365.25, (double) 23 / 24, (double) 59 / 60,
						(double) 59 / 60, (double) 999 / 1000 };
				double[] expectedPrecision = new double[7];
				expectedPrecision[max] = nullRatio[max];
				double factor = 1 - nullRatio[max];
				for (int i = max - 1; i >= 0; i--) {
					expectedPrecision[i] = nullRatio[i] * (factor);
					factor = (1 - nullRatio[i]) * factor;
				}
				List<Double> expectedPrecisionList = new ArrayList<Double>();
				for (int i = 0; i < 7; i++) {
					expectedPrecisionList.add(expectedPrecision[i]);
				}
				expectedEventGranularity.put((String) e.getKey().getErrorObject(), expectedPrecisionList);
			}
			for (Entry<String, Integer> entry : eventCount.entrySet()) {
				double partialScore = 0;
				for (Entry<Error, List<Error>> e : getErrorList().entrySet()) {
					if (!e.getKey().isWhitelisted() && e.getKey().getErrorObject().equals(entry.getKey())) {
						for (int i = 0; i < 7; i++) {
							partialScore = partialScore
									+ Math.abs((Double) ((Error) e.getValue().toArray()[i]).getErrorObject()
											- expectedEventGranularity.get(entry.getKey()).get(i));

						}
					}
				}
				partialScore = partialScore * entry.getValue() / getEventList().size();
				score = score - 0.5 * partialScore;
			}
			setScore(score);
		} else {
			setScore(1);
		}
	}

	public void detectErrors() {
		getEventList().removeIf(n -> (XTimeExtension.instance().extractTimestamp(n.getEvent()) == null));
		setScoreWeight(2);
		Collections.sort(getEventList(), new EventNameSorter());
		int[] precision = new int[7];
		for (int i = 0; i < 7; i++) {
			precision[i] = 0;
		}
		ListIterator<TraceLinkedEvent> lit = IteratorUtils.toListIterator(getEventList().iterator());
		while (lit.hasNext()) {
			lit.next();
			Date d = XTimeExtension.instance().extractTimestamp(getEventList().get(lit.previousIndex()).getEvent());
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			c.setTime(d);
			if (c.get(Calendar.MILLISECOND) == 0) {
				if (c.get(Calendar.SECOND) == 0) {
					if (c.get(Calendar.MINUTE) == 0) {
						if (c.get(Calendar.HOUR_OF_DAY) == 0) {
							if (c.get(Calendar.DAY_OF_MONTH) == c.getActualMaximum(Calendar.DAY_OF_MONTH)) {
								if (c.get(Calendar.MONTH) == c.getActualMaximum(Calendar.MONTH) - 1) {
									precision[0]++;
								} else {
									precision[1]++;
								}
							} else {
								precision[2]++;
							}
						} else {
							precision[3]++;
						}
					} else {
						precision[4]++;
					}
				} else {
					precision[5]++;
				}
			} else {
				precision[6]++;
			}
			if (!lit.hasNext() || !XConceptExtension.instance()
					.extractName(getEventList().get(lit.nextIndex()).getEvent()).equals(XConceptExtension.instance()
							.extractName(getEventList().get(lit.previousIndex()).getEvent()))) {
				List<Error> errorList = new ArrayList<Error>();
				eventCount.put(
						XConceptExtension.instance().extractName(getEventList().get(lit.previousIndex()).getEvent()),
						IntStream.of(precision).sum());

				for (int i = 0; i < 7; i++) {
					Error error = new Error((double) precision[i] / eventCount.get(XConceptExtension.instance()
							.extractName(getEventList().get(lit.previousIndex()).getEvent())), false);
					errorList.add(error);
				}
				Error error = new Error(
						XConceptExtension.instance().extractName(getEventList().get(lit.previousIndex()).getEvent()),
						false);
				getErrorList().put(error, errorList);
				for (int i = 0; i < 7; i++) {
					precision[i] = 0;
				}
			}
		}
	}

	public void createErrorTableModel() {
		DefaultTableModel errorTableModel = new DefaultTableModel() {
			public Class<?> getColumnClass(int colIndex) {
				return getValueAt(0, colIndex).getClass();
			}
		};
		errorTableModel.addColumn("Errortype");
		errorTableModel.addColumn("Activity");
		errorTableModel.addColumn("Year");
		errorTableModel.addColumn("Month");
		errorTableModel.addColumn("Day");
		errorTableModel.addColumn("Hour");
		errorTableModel.addColumn("Minute");
		errorTableModel.addColumn("Second");
		errorTableModel.addColumn("Millisecond");
		errorTableModel.addColumn("Whitelisting?");
		for (Entry<Error, List<Error>> entry : getErrorList().entrySet()) {
			int count = eventCount.get(entry.getKey().getErrorObject());
			errorTableModel.addRow(new Object[] { entry.getKey(), entry.getKey().getErrorObject(),
					count * (Double) ((Error) entry.getValue().toArray()[0]).getErrorObject(),
					count * (Double) ((Error) entry.getValue().toArray()[1]).getErrorObject(),
					count * (Double) ((Error) entry.getValue().toArray()[2]).getErrorObject(),
					count * (Double) ((Error) entry.getValue().toArray()[3]).getErrorObject(),
					count * (Double) ((Error) entry.getValue().toArray()[4]).getErrorObject(),
					count * (Double) ((Error) entry.getValue().toArray()[5]).getErrorObject(),
					count * (Double) ((Error) entry.getValue().toArray()[6]).getErrorObject(),
					(boolean) entry.getKey().isWhitelisted() });
		}
		setErrorTableModel(errorTableModel);
	}
}
