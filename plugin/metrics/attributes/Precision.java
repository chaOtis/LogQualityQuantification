package org.processmining.logqualityquantification.plugin.metrics.attributes;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;

public class Precision extends Metric {

	public Precision(String name, String dimension, String logLevel) {
		super(name, dimension, logLevel);
	}

	public void calculateScore() {
		if (getEventList().size() > 0) {
			double score = getEventList().size();
			for (Error error : getErrorList().keySet()) {
				for (Error error2 : getErrorList().get(error)) {
					if (!error2.isWhitelisted()) {
						score = score - (double) 1 / 6;
					}
				}
			}
			setScore(score / getEventList().size());
		} else {
			setScore(1);
		}
	}

	public void detectErrors() {
		setScoreWeight(2);
		getEventList().removeIf(n -> (XTimeExtension.instance().extractTimestamp(n.getEvent()) == null));
		String granularity[] = {"Month", "Day", "Hour", "Minute", "Second", "Millisecond"};
		for (TraceLinkedEvent tle : getEventList()) {
			Date d = XTimeExtension.instance().extractTimestamp(tle.getEvent());
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			c.setTime(d);
			int index;
			if (c.get(Calendar.MILLISECOND) == 0) {
				if (c.get(Calendar.SECOND) == 0) {
					if (c.get(Calendar.MINUTE) == 0) {
						if (c.get(Calendar.HOUR_OF_DAY) == 0) {
							if (c.get(Calendar.DAY_OF_MONTH) == c.getActualMaximum(Calendar.DAY_OF_MONTH)) {
								if (c.get(Calendar.MONTH) == c.getActualMaximum(Calendar.MONTH) - 1) {
									index = 0;
								} else {
									index = 1;
								}
							} else {
								index = 2;
							}
						} else {
							index = 3;
						}
					} else {
						index = 4;
					}
				} else {
					index = 5;
				}
			} else {
				continue;
			}
			String description = XConceptExtension.instance().extractName(tle.getEvent());
			for (int i = index; i < granularity.length; i++) {		
				List<String> key = new ArrayList<String>();
				key.add(description);
				key.add(granularity[i]);
				List<Error> list = new ArrayList<Error>();
				Error keyError = new Error(key, false);
				boolean descriptionExists = false;
				for (Error error : getErrorList().keySet()) {
					if (((ArrayList<String>) error.getErrorObject()).get(0).equals(description)
							&& ((ArrayList<String>) error.getErrorObject()).get(1).equals(granularity[i])) {
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
		errorTableModel.addColumn("Missing granularity");
		errorTableModel.addColumn("Occurences");
		errorTableModel.addColumn("Whitelisting?");
		errorTableModel.addColumn("");
		for (Error errorType : getErrorList().keySet()) {
			int occurences = getErrorList().get(errorType).size();
			errorTableModel.addRow(new Object[] { errorType, ((ArrayList<String>) errorType.getErrorObject()).get(0),
					((ArrayList<String>) errorType.getErrorObject()).get(1), occurences, errorType.isWhitelisted(),
					"errorlist" });
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
					XTimeExtension.instance().extractTimestamp(((TraceLinkedEvent) error2.getErrorObject()).getEvent()),
					(boolean) error2.isWhitelisted() });
		}
		return errorTableModelLayerTwo;
	}
}
