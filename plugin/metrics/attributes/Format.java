package org.processmining.logqualityquantification.plugin.metrics.attributes;

import java.util.Calendar;
import java.util.Date;

import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;
import org.processmining.logqualityquantification.plugin.metrics.Metric;

public class Format extends Metric {

	public Format(String name, String dimension, String logLevel) {
		super(name, dimension, logLevel);
	}

	public void calculateScore() {
		if (getEventList().size() > 0 && !getErrorList().keySet().stream().findFirst().get().isWhitelisted()) {
			int toTwelveCount = 0;
			for (TraceLinkedEvent tle : getEventList()) {
				Date date = XTimeExtension.instance().extractTimestamp(tle.getEvent());
				Calendar c = Calendar.getInstance();
				c.setTime(date);
				int dayValue = c.get(Calendar.DAY_OF_MONTH);
				if (dayValue < 12) {
					toTwelveCount++;
				}
			}
			double toTwelveExpected = 12 * (double) 12 / 365.25;
			double toTwelveObserved = (double) toTwelveCount / getEventList().size();
			double score = 1;
			setScore(score);
			if (toTwelveObserved > toTwelveExpected) {
				setScore((1.0 - toTwelveObserved) / (1.0 - toTwelveExpected));
			}
		} else {
			setScore(1);
		}
	}

	public void detectErrors() {
		setScoreWeight(2);
		getEventList().removeIf(n -> (XTimeExtension.instance().extractTimestamp(n.getEvent()) == null));
		Object object = new Object();
		Error error = new Error(object, false);
		getErrorList().put(error, null);
	}

	public void createErrorTableModel() {
		DefaultTableModel errorTableModel = new DefaultTableModel() {
			public Class<?> getColumnClass(int colIndex) {
				return getValueAt(0, colIndex).getClass();
			}
		};
		errorTableModel.addColumn("Errortype");
		errorTableModel.addColumn("Errortype");
		errorTableModel.addColumn("Whitelisting?");
		Error error = getErrorList().keySet().stream().findFirst().get();
		errorTableModel.addRow(new Object[] { error, "Format error present?", (boolean) error.isWhitelisted() });
		setErrorTableModel(errorTableModel);
	}

}
