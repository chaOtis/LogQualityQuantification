package org.processmining.logqualityquantification.plugin.metrics.attributes;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.swing.table.DefaultTableModel;

import org.apache.commons.collections.IteratorUtils;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;

public class MixedGranularityLog extends Metric {

	public MixedGranularityLog(String name, String dimension, String logLevel) {
		super(name, dimension, logLevel);
	}

	public void calculateScore() {
		if (getEventList().size() > 0 && !getErrorList().keySet().iterator().next().isWhitelisted()) {
			double score = 1;
			double[] expectedEventGranularity = new double[7];
			int max = 0;
			int j = 0;
			List<Error> errorList = getErrorList().values().iterator().next();
			for (Error error : errorList) {
				if ((Double) error.getErrorObject() > (Double) ((Error) errorList.toArray()[max]).getErrorObject()) {
					max = j;
				}
				j++;
			}
			double[] nullRatio = { 1, (double) 11 / 12, (double) 353.25 / 365.25, (double) 23 / 24, (double) 59 / 60,
					(double) 59 / 60, (double) 999 / 1000 };
			expectedEventGranularity[max] = (double) nullRatio[max];
			double factor = 1 - nullRatio[max];
			for (int i = max - 1; i >= 0; i--) {
				expectedEventGranularity[i] = nullRatio[i] * (factor);
				factor = (double) (1 - nullRatio[i]) * factor;
			}
			double partialScore = 0;
			for (int i = 0; i < 7; i++) {
				partialScore = partialScore + Math
						.abs((Double) ((Error) errorList.toArray()[i]).getErrorObject() - expectedEventGranularity[i]);
			}
			score = 1 - 0.5 * partialScore;
			setScore(score);
		} else {
			setScore(1);
		}
	}

	public void detectErrors() {
		setScoreWeight(2);
		getEventList().removeIf(n -> (XTimeExtension.instance().extractTimestamp(n.getEvent()) == null));
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
		}
		List<Error> errorList = new ArrayList<Error>();
		for (int i = 0; i < 7; i++) {
			Error error = new Error((double) precision[i] / getEventList().size(), false);
			errorList.add(error);
		}
		String description = " ";
		getErrorList().put(new Error(description, false), errorList);
	}

	public void createErrorTableModel() {
		DefaultTableModel errorTableModel = new DefaultTableModel() {
			public Class<?> getColumnClass(int colIndex) {
				return getValueAt(0, colIndex).getClass();
			}
		};
		errorTableModel.addColumn("Errortype");
		errorTableModel.addColumn(" ");
		errorTableModel.addColumn("Year");
		errorTableModel.addColumn("Month");
		errorTableModel.addColumn("Day");
		errorTableModel.addColumn("Hour");
		errorTableModel.addColumn("Minute");
		errorTableModel.addColumn("Second");
		errorTableModel.addColumn("Millisecond");
		errorTableModel.addColumn("Whitelisting?");
		for (Entry<Error, List<Error>> entry : getErrorList().entrySet()) {
			int count = getEventList().size();
			errorTableModel.addRow(new Object[] { entry.getKey(), "Granularity distribution",
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
