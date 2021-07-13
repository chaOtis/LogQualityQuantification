package org.processmining.logqualityquantification.GUI;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.logqualityquantification.plugin.QualityInformedEventLog;
import org.processmining.logqualityquantification.plugin.metrics.Metric;

import Repair.RepairPanel;

public class LogQualityFrame extends JFrame implements WindowListener {

	private QualityInformedEventLog qieLog;
	private JPanel quantificationPanel = new JPanel();
	private JPanel errorTable = new JPanel();
	private JPanel detailsPanel = new JPanel();
	private JPanel mainPanel = new JPanel();
	private JScrollPane scrollPane;
	private JFrame configFrame;
	private List<MetricPanel> metricPanels = new ArrayList<>();
	private List<MetricPanel> dimensionPanels = new ArrayList<>();
	private List<MetricPanel> logLevelPanels = new ArrayList<>();
	private GridBagConstraints gbc = new GridBagConstraints();
	private JPanel leftPanel = new JPanel();
	private JButton configButton = new JButton("Configuration");
	private RepairPanel repairPanel;
	private JButton createQIELButton = new JButton("create QIEL");
	private XLog log;
	public LogQualityFrame(QualityInformedEventLog qieLog,XLog log) {
		this.qieLog = qieLog;
		setSize(1400,800);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Log Quality Quantification: "+XConceptExtension.instance().extractName(qieLog.getLog()));
		
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3,3,3,3);
				
		configFrame = new ConfigurationFrame(qieLog);
		configFrame.addWindowListener(this); 
		
		errorTable.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.GRAY, Color.BLACK));
		errorTable.add(new JLabel("click details"));
		
		//detailsPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.GRAY, Color.BLACK));
		//detailsPanel.add(new JLabel("click details"));
		
		leftPanel.setLayout(new BorderLayout());	
		
		quantificationPanel.setLayout(new GridBagLayout());
		quantificationPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.GRAY, Color.BLACK));
		setQuantificationPanel();
		
		leftPanel.add(quantificationPanel, BorderLayout.CENTER);
		leftPanel.add(errorTable, BorderLayout.SOUTH);
		
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(leftPanel, BorderLayout.CENTER);
		//mainPanel.add(detailsPanel, BorderLayout.EAST);
		
		scrollPane = new JScrollPane(mainPanel);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add("Detection & Quantification", scrollPane);
		repairPanel = new RepairPanel(qieLog,log);
		tabbedPane.add("Repair", repairPanel);
		
		this.add(tabbedPane);

	}

	
	private void setQuantificationPanel() {
		metricPanels.clear();
		quantificationPanel.removeAll();
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		gbc.gridx = 1;
		gbc.gridy = 1; 
		
		for (Metric metric : qieLog.getDimensionsMap().keySet()) {
			if (metric.isUsed()) {
				MetricPanel metricPanel = new MetricPanel(metric, this);
				dimensionPanels.add(metricPanel);				
				quantificationPanel.add(metricPanel, gbc);
				gbc.gridx++;
			}
		}
		
		gbc.gridx = 0;
		gbc.gridy = 2;
		for (Metric metric : qieLog.getLevelsMap().keySet()) {
			MetricPanel metricPanel = new MetricPanel(metric, this);
			logLevelPanels.add(metricPanel);				
			quantificationPanel.add(metricPanel, gbc);
			gbc.gridy++;
		}
		
		for (Metric metric : qieLog.getMetricsList()) {
			if (metric.isUsed()) {
				metricPanels.add(new MetricPanel(metric, this));
			}
		}
		
		for(MetricPanel metric : metricPanels) {
			metric.getDetailsButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
					leftPanel.remove(errorTable);
					errorTable = metric.getErrorTable();
					leftPanel.add(errorTable, BorderLayout.SOUTH);
					revalidate();
					repaint();
				}
			});	
		}
		
		gbc.gridy = 2;
		for (Metric metricLevel : qieLog.getLevelsMap().keySet()) {
			gbc.gridx = 1;
			for (Metric metricDimension : qieLog.getDimensionsMap().keySet()) {
				if (metricDimension.isUsed()) {
					List<MetricPanel> cellMetrics = new ArrayList<>();
					for (MetricPanel metric : metricPanels) {
						if (metric.getMetric().getDimension().equals(metricDimension.getDimension()) && metric.getMetric().getEventLogLevel().equals(metricLevel.getEventLogLevel())) {
							cellMetrics.add(metric);
						}
					}
					if (cellMetrics.size() > 0) {
						JPanel cellPanel = new JPanel();
						cellPanel.setLayout(new GridLayout(cellMetrics.size(), 1));
						for (MetricPanel cellMetric : cellMetrics) {
							
							int height = 120;
							if (cellMetrics.size() > 1) {
								height = 60;
							}
							cellMetric.setPreferredSize(new Dimension(300,height));
							cellPanel.add(cellMetric);
						}
						quantificationPanel.add(cellPanel, gbc);
					} else {
						quantificationPanel.add(new JLabel(" "), gbc);
					}
					gbc.gridx++;
				}
			}
			gbc.gridy++;
		}
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		quantificationPanel.add(configButton, gbc);		
		configButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				configFrame.setVisible(true);
			}
		});
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		quantificationPanel.add(createQIELButton, gbc);		
		createQIELButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				qieLog.createQIEL();
			}
		});
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = qieLog.getDimensionsMap().size();
		quantificationPanel.add(createLogQualityPanel(), gbc);
		gbc.gridwidth = 1;
		
		quantificationPanel.validate();
		quantificationPanel.repaint();
	}

	private JPanel createLogQualityPanel() {
		JPanel overallQualityPanel = new JPanel();
		//String s = Double.toString(Math.round(score*10000.00)/10000.00);
		JLabel scoreLabel = new JLabel("Event Log Timestamp Quality_v2");
		scoreLabel.setHorizontalAlignment(JLabel.CENTER);
		scoreLabel.setFont(new Font("Georgia", Font.BOLD, 24));
		overallQualityPanel.setBackground(Color.GRAY);
		overallQualityPanel.add(scoreLabel);
		return overallQualityPanel;
	}

	public QualityInformedEventLog getQieLog() {
		return qieLog;
	}

	public void setQieLog(QualityInformedEventLog qieLog) {
		this.qieLog = qieLog;
	}

	public JPanel getQuantificationPanel() {
		return quantificationPanel;
	}

	public void setQuantificationPanel(JPanel quantificationPanel) {
		this.quantificationPanel = quantificationPanel;
	}

	public JPanel getErrorTable() {
		return errorTable;
	}

	public void setErrorTable(JPanel errorTable) {
		this.errorTable = errorTable;
	}

	public JPanel getDetailsPanel() {
		return detailsPanel;
	}

	public void setDetailsPanel(JPanel detailsPanel) {
		this.detailsPanel = detailsPanel;
	}

	public List<MetricPanel> getMetricPanels() {
		return metricPanels;
	}
	
	public List<MetricPanel> getDimensionPanels() {
		return dimensionPanels;
	}


	public void setDimensionPanels(List<MetricPanel> dimensionPanels) {
		this.dimensionPanels = dimensionPanels;
	}

	public List<MetricPanel> getLogLevelPanels() {
		return logLevelPanels;
	}


	public void setLogLevelPanels(List<MetricPanel> logLevelPanels) {
		this.logLevelPanels = logLevelPanels;
	}


	public void setMetricPanels(List<MetricPanel> metricPanels) {
		this.metricPanels = metricPanels;
	}

	public JPanel getLeftPanel() {
		return leftPanel;
	}

	public void setLeftPanel(JPanel leftPanel) {
		this.leftPanel = leftPanel;
	}

	public JButton getConfigButton() {
		return configButton;
	}

	public void setButtonConfig(JButton configButton) {
		this.configButton = configButton;
	}


	public void windowClosed(WindowEvent e) {
		qieLog.calculateDimensionScores();
		setQuantificationPanel();
	}


	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}
}
