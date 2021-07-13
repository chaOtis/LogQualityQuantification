package org.processmining.logqualityquantification.GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.processmining.logqualityquantification.plugin.QualityInformedEventLog;
import org.processmining.logqualityquantification.plugin.metrics.Metric;

public class ConfigurationFrame extends JFrame {
	
	JButton defaultValues = new JButton("set default values");
	JButton applyChanges = new JButton("Apply changes");
	QualityInformedEventLog qieLog;
	JPanel metricsPanel = new JPanel();	
	GridBagConstraints gbc = new GridBagConstraints();
	Map<Metric, List<JComponent>> metricComponents = new HashMap<Metric, List<JComponent>>();
	JPanel quantificationPanel;
	
	public ConfigurationFrame(QualityInformedEventLog qieLog) {
		this.qieLog = qieLog;
		NumberFormat textFieldFormat = DecimalFormat.getInstance();
		textFieldFormat.setMinimumFractionDigits(2);
		textFieldFormat.setMaximumFractionDigits(2);
		textFieldFormat.setRoundingMode(RoundingMode.HALF_UP);
		for (Metric dimension : qieLog.getDimensionsMap().keySet()) {
			List<JComponent> componentsList = new ArrayList<JComponent>();
			JLabel dimensionName = new JLabel(dimension.getName());
			dimensionName.setFont(new Font("Arial", Font.BOLD, 14));
			componentsList.add(dimensionName);
			componentsList.add(new JCheckBox());
			metricComponents.put(dimension, componentsList);
		}
		for (List<Metric> metricList : qieLog.getDimensionsMap().values()) {
			for (Metric metric : metricList) {
				List<JComponent> componentsList = new ArrayList<JComponent>();
				JLabel metricName = new JLabel(metric.getName());
				componentsList.add(metricName);
				componentsList.add(new JCheckBox());
				componentsList.add(new JFormattedTextField(textFieldFormat));
				metricComponents.put(metric, componentsList);
			}
		}
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("User Configuration");
		setSize(new Dimension(450, qieLog.getMetricsList().size()*30));
		setLayout(new BorderLayout());
				
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(1,2));
		
		buttonsPanel.add(defaultValues);
		buttonsPanel.add(applyChanges);
		
		add(buttonsPanel, BorderLayout.SOUTH);
		
		createMetricsPanel();
		
		defaultValues.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				metricsPanel.removeAll();
				for (Entry<Metric, List<Metric>> entry : qieLog.getDimensionsMap().entrySet()) {
					entry.getKey().setRelativeWeight(1);
					entry.getKey().setUsed(true);
					for (Metric metric : entry.getValue()) {
						metric.setRelativeWeight(1);
						metric.setUsed(true);
					}
				}
				createMetricsPanel();
			}
		});
		
		applyChanges.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(Metric metric : metricComponents.keySet()) {
					metric.setUsed(((JCheckBox) metricComponents.get(metric).get(1)).isSelected());
					if (metricComponents.get(metric).size() == 3) {
						System.out.println(((JFormattedTextField) metricComponents.get(metric).get(2)).getValue());
						if (((JFormattedTextField) metricComponents.get(metric).get(2)).getValue() instanceof Double) {
							metric.setRelativeWeight((double) ((JFormattedTextField) metricComponents.get(metric).get(2)).getValue());
						}
						else {
							Long weightlong = (long) ((JFormattedTextField) metricComponents.get(metric).get(2)).getValue();
							double weight = weightlong;
							metric.setRelativeWeight(weight);
						}
					}
				}
				qieLog.setConfigSet(true);
				dispose();
			}
		});
	}
	
	private void createMetricsPanel() {
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		JLabel dimensionNameCaption = new JLabel("Dimension name");
		metricsPanel.add(dimensionNameCaption, gbc);
		gbc.gridx++;
		JLabel dimensionUsedCaption = new JLabel("Used?");
		metricsPanel.add(dimensionUsedCaption, gbc);
		gbc.gridx++;
		JLabel metricNameCaption = new JLabel("Metric name");
		metricsPanel.add(metricNameCaption, gbc);
		gbc.gridx++;
		JLabel metricUsedCaption = new JLabel("Used?");
		metricsPanel.add(metricUsedCaption, gbc);
		gbc.gridx++;
		JLabel metricWeightCaption = new JLabel("Weight");
		metricsPanel.add(metricWeightCaption, gbc);
		
		add(metricsPanel, BorderLayout.CENTER);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		metricsPanel.setLayout(new GridBagLayout());
				
		for (Entry<Metric, List<Metric>> entry : qieLog.getDimensionsMap().entrySet()) {
			metricsPanel.add(metricComponents.get(entry.getKey()).get(0), gbc);
			gbc.gridx++;
			if (entry.getKey().isUsed()) {
				((JCheckBox) metricComponents.get(entry.getKey()).get(1)).setSelected(true);
			}
			metricsPanel.add(metricComponents.get(entry.getKey()).get(1), gbc);
			gbc.gridx++;
			for (Metric metric : entry.getValue()) {
				metricsPanel.add(metricComponents.get(metric).get(0), gbc);
				gbc.gridx++;
				if (metric.isUsed()) {
					((JCheckBox) metricComponents.get(metric).get(1)).setSelected(true);
				}
				((JCheckBox) metricComponents.get(entry.getKey()).get(1)).addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (((JCheckBox) metricComponents.get(entry.getKey()).get(1)).isSelected()) {
							((JCheckBox) metricComponents.get(metric).get(1)).setSelected(true);
						}
						else {
							((JCheckBox) metricComponents.get(metric).get(1)).setSelected(false);
						}
					}
				});
				((JCheckBox) metricComponents.get(metric).get(1)).addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						System.out.println("Action");
						boolean activeMetric = false;
						for (Metric metric : entry.getValue()) {
							if (((JCheckBox) metricComponents.get(metric).get(1)).isSelected()) {
								activeMetric = true;
							}
						}
						if (activeMetric == false) {
							((JCheckBox) metricComponents.get(entry.getKey()).get(1)).setSelected(false);
						}
						else {
							((JCheckBox) metricComponents.get(entry.getKey()).get(1)).setSelected(true);
						}
					}
				});
				metricsPanel.add(metricComponents.get(metric).get(1), gbc);
				gbc.gridx++;
				((JFormattedTextField) metricComponents.get(metric).get(2)).setValue(metric.getRelativeWeight());
				((JFormattedTextField) metricComponents.get(metric).get(2)).setColumns(3);
				metricsPanel.add(metricComponents.get(metric).get(2), gbc);
				gbc.gridx = 2;
				gbc.gridy++;
			}
			gbc.gridx = 0;		
		}
	metricsPanel.validate();
	metricsPanel.repaint();	
	}
}
