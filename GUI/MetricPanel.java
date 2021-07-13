package org.processmining.logqualityquantification.GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.processmining.framework.util.ui.widgets.ProMTable;
import org.processmining.logqualityquantification.plugin.metrics.Metric;
import org.processmining.logqualityquantification.plugin.metrics.Metric.Error;

public class MetricPanel extends JPanel implements TableModelListener {

	private Metric metric;
	private JButton detailsButton = new JButton("details");
	protected ProMTable errorTable;
	private JCheckBox metricUsed;
	private JTextField metricWeight;
	private JLabel scoreValueLabel = new JLabel();
	private JLabel scoreLevelLabel = new JLabel();
	private JLabel metricName = new JLabel();
	private JPanel scoreLevelPanel = new JPanel();
	private MetricPanel metricPanel;
	private LogQualityFrame logQualityFrame;

	public MetricPanel(Metric metric, LogQualityFrame logQualityFrame) {
		this.logQualityFrame = logQualityFrame;
		this.metric = metric;
		this.metricPanel = this;
		createPanel();
	}

	private void createPanel() {
		setLayout(new BorderLayout());
		setBorder(new javax.swing.border.LineBorder(Color.BLACK, 2));

		metricName.setText(metric.getName());
		metricName.setFont(new Font("Georgia", Font.BOLD, 12));
		metricName.setHorizontalAlignment(JLabel.CENTER);
		add(metricName, BorderLayout.NORTH);

		scoreValueLabel.setText("score: " + Double.toString(Math.round(metric.getScore() * 10000.00) / 10000.00));
		scoreValueLabel.setHorizontalAlignment(JLabel.CENTER);
		add(scoreValueLabel, BorderLayout.CENTER);

		scoreLevelLabel.setText(metric.getQualityLevel());
		scoreLevelLabel.setHorizontalAlignment(JLabel.CENTER);

		scoreLevelPanel.setLayout(new BorderLayout());
		scoreLevelPanel.add(scoreLevelLabel, BorderLayout.CENTER);
		scoreLevelPanel.setBackground(getBackgroundColor(metric.getQualityLevel()));
		add(scoreLevelPanel, BorderLayout.EAST);

		if (getMetric().getEventLogLevel() != null && getMetric().getDimension() != null) {
			getDetailsButton().setHorizontalAlignment(JLabel.CENTER);
			add(getDetailsButton(), BorderLayout.SOUTH);
		}
	}

	public JPanel getErrorTable() {
		if (metric.getErrorTableModel() == null) {
			JPanel errorTable = new JPanel();
			errorTable.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.GRAY, Color.BLACK));
			errorTable.setSize(new Dimension(300, 300));
			errorTable.add(new JLabel("click details"));
			return errorTable;
		} else {
			metric.getErrorTableModel().addTableModelListener(this);
			errorTable = new ProMTable(metric.getErrorTableModel());
			errorTable.getColumnModel().getColumn(0).setMinWidth(0);
			errorTable.getColumnModel().getColumn(0).setMaxWidth(0);
			errorTable.getColumnModel().getColumn(0).setWidth(0);
			try {
				errorTable.getTable().getColumn("").setCellRenderer(new ButtonRenderer());
				errorTable.getTable().getColumn("").setCellEditor(new ButtonEditor(new JCheckBox(), this));
			} catch (Exception e) {

			}
			TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(errorTable.getTable().getModel());
			errorTable.setRowSorter(sorter);
			List<RowSorter.SortKey> sortKeys = new ArrayList<>(25);
			for (int i = 0; i < metric.getErrorTableModel().getColumnCount(); i++) {
				sortKeys.add(new RowSorter.SortKey(i, SortOrder.ASCENDING));
			}
			sorter.setSortKeys(sortKeys);
			return errorTable;
		}
	}

	public void tableChanged(TableModelEvent e) {
		int row = e.getFirstRow();
		int column = e.getColumn();
		TableModel model = (TableModel) e.getSource();
		String columnName = model.getColumnName(column);
		if (columnName == "Whitelisting?") {
			boolean data = (boolean) model.getValueAt(row, column);
			if (logQualityFrame.isFocused()) {
				((Error) model.getValueAt(row, 0)).setWhitelisted(data);
				try {
					for (Error error2 : metric.getErrorList().get(model.getValueAt(row, 0))) {
						error2.setWhitelisted(data);
					}
				} catch (Exception e2) {
					// TODO: handle exception
				}
			} else if (model != metric.getErrorTableModel()) {
				((Error) model.getValueAt(row, 1)).setWhitelisted(data);
				if (!data) {
					((Error) model.getValueAt(row, 0)).setWhitelisted(data);
					for (int i = 0; i < metric.getErrorTableModel().getRowCount(); i++) {
						if (metric.getErrorTableModel().getValueAt(i, 0) == model.getValueAt(row, 0)) {
							metric.getErrorTableModel().setValueAt(data, i,
									metric.getErrorTableModel().getColumnCount() - 2);
							break;
						}
					}
				} else {
					boolean allWhitelisted = true;
					for (Error error : metric.getErrorList().get(model.getValueAt(row, 0))) {
						if (!error.isWhitelisted()) {
							allWhitelisted = false;
							break;
						}
					}
					if (allWhitelisted) {
						for (int i = 0; i < metric.getErrorTableModel().getRowCount(); i++) {
							if (metric.getErrorTableModel().getValueAt(i, 0) == model.getValueAt(row, 0)) {
								metric.getErrorTableModel().setValueAt(data, i,
										metric.getErrorTableModel().getColumnCount() - 2);
								break;
							}
						}
					}
				}
			}
			update();
		}
	}

	private void update() {
		metric.calculateScore();
		logQualityFrame.getQieLog().calculateDimensionScores();
		scoreValueLabel.setText("score: " + Double.toString(Math.round(metric.getScore() * 10000.00) / 10000.00));
		scoreLevelLabel.setText(metric.getQualityLevel());
		scoreLevelPanel.setBackground(getBackgroundColor(metric.getQualityLevel()));
		for (MetricPanel metricPanel : logQualityFrame.getDimensionPanels()) {
			if (metric.getDimension() == metricPanel.getMetric().getDimension()) {
				metricPanel.getScoreValueLabel().setText("score: "
						+ Double.toString(Math.round(metricPanel.getMetric().getScore() * 10000.00) / 10000.00));
				metricPanel.getScoreLevelLabel().setText(metricPanel.getMetric().getQualityLevel());
				metricPanel.getScoreLevelPanel()
						.setBackground(getBackgroundColor(metricPanel.getMetric().getQualityLevel()));
			}
		}
		for (MetricPanel metricPanel : logQualityFrame.getLogLevelPanels()) {
			if (metric.getDimension() == metricPanel.getMetric().getDimension()) {
				metricPanel.getScoreValueLabel().setText("score: "
						+ Double.toString(Math.round(metricPanel.getMetric().getScore() * 10000.00) / 10000.00));
				metricPanel.getScoreLevelLabel().setText(metricPanel.getMetric().getQualityLevel());
				metricPanel.getScoreLevelPanel()
						.setBackground(getBackgroundColor(metricPanel.getMetric().getQualityLevel()));
			}
		}
	}

	public JPanel getDetailsWindow() {
		JPanel detailsWindow = new JPanel();
		errorTable.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.GRAY, Color.BLACK));
		errorTable.setSize(new Dimension(300, 300));
		errorTable.add(new JLabel("click details"));
		return detailsWindow;
	}

	private Color getBackgroundColor(String qualityLevel) {
		Color color;
		switch (qualityLevel) {
			case "High" :
				color = Color.GREEN;
				break;
			case "Medium" :
				color = Color.YELLOW;
				break;
			default :
				color = Color.RED;
		}
		return color;
	}

	public void setErrorTable(ProMTable errorTable) {
		this.errorTable = errorTable;
	}

	public JButton getDetailsButton() {
		return detailsButton;
	}

	public void setDetailsButton(JButton detailsButton) {
		this.detailsButton = detailsButton;
	}

	public JCheckBox getMetricUsed() {
		return metricUsed;
	}

	public void setMetricUsed(JCheckBox metricUsed) {
		this.metricUsed = metricUsed;
	}

	public JTextField getMetricWeight() {
		return metricWeight;
	}

	public void setMetricWeight(JTextField metricWeight) {
		this.metricWeight = metricWeight;
	}

	public Metric getMetric() {
		return metric;
	}

	public void setMetric(Metric metric) {
		this.metric = metric;
	}

	public JLabel getScoreValueLabel() {
		return scoreValueLabel;
	}

	public void setScoreValueLabel(double score) {
		this.scoreValueLabel = new JLabel("Score " + Double.toString(Math.round(score * 10000.00) / 10000.00));
	}

	public JLabel getMetricName() {
		return metricName;
	}

	public void setMetricName(JLabel metricName) {
		this.metricName = metricName;
	}

	public JPanel getScoreLevelPanel() {
		return scoreLevelPanel;
	}

	public void setScoreLevelPanel(JPanel scoreLevelPanel) {
		this.scoreLevelPanel = scoreLevelPanel;
	}

	public JLabel getScoreLevelLabel() {
		return scoreLevelLabel;
	}

	public void setScoreLevelLabel(JLabel scoreLevelLabel) {
		this.scoreLevelLabel = scoreLevelLabel;
	}

	class ButtonRenderer extends JButton implements TableCellRenderer {

		public ButtonRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(UIManager.getColor("Button.background"));
			}
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}

	class ButtonEditor extends DefaultCellEditor {

		protected JButton button;
		private String label;
		private boolean isPushed;
		private JTable table;
		int row;

		public ButtonEditor(JCheckBox checkBox, MetricPanel metricPanel) {
			super(checkBox);
			button = new JButton();
			button.setOpaque(true);
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					fireEditingStopped();
				}
			});
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			this.table = table;
			this.row = row;
			if (isSelected) {
				button.setForeground(table.getSelectionForeground());
				button.setBackground(table.getSelectionBackground());
			} else {
				button.setForeground(table.getForeground());
				button.setBackground(table.getBackground());
			}
			label = (value == null) ? "" : value.toString();
			button.setText(label);
			isPushed = true;
			return button;
		}

		@Override
		public Object getCellEditorValue() {
			if (isPushed) {
				ErrorFrame errorFrame = new ErrorFrame(
						metric.createErrorTableModelLayerTwo((Error) table.getValueAt(row, 0)),
						(String) table.getValueAt(row, 1));
				errorFrame.getDefaultTableModel().addTableModelListener(metricPanel);
				metric.getErrorTableModel().addTableModelListener(errorFrame);
			}
			isPushed = false;
			return label;
		}

		@Override
		public boolean stopCellEditing() {
			isPushed = false;
			return super.stopCellEditing();
		}
	}
}