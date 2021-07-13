package org.processmining.logqualityquantification.GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.WindowConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.processmining.framework.util.ui.widgets.ProMTable;

public class ErrorFrame extends JFrame implements TableModelListener {

	private ProMTable errorTable;
	private DefaultTableModel defaultTableModel;
	private String description;
	
	public ErrorFrame(DefaultTableModel defaultTableModel, String description) {
		this.defaultTableModel = defaultTableModel;
		this.description = description;
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Errortable: "+description);
		setLayout(new BorderLayout());
		createTable(defaultTableModel);
		add(errorTable, BorderLayout.CENTER);
		setVisible(true);
		setSize(new Dimension(1200, 450));
	}
	
	private void createTable(DefaultTableModel defaultTableModel) {
		errorTable = new ProMTable(defaultTableModel);
		errorTable.getColumnModel().getColumn(0).setMinWidth(0);
		errorTable.getColumnModel().getColumn(0).setMaxWidth(0);
		errorTable.getColumnModel().getColumn(0).setWidth(0);
		errorTable.getColumnModel().getColumn(1).setMinWidth(0);
		errorTable.getColumnModel().getColumn(1).setMaxWidth(0);
		errorTable.getColumnModel().getColumn(1).setWidth(0);
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(errorTable.getTable().getModel());
		errorTable.setRowSorter(sorter);
		List<RowSorter.SortKey> sortKeys = new ArrayList<>(25);
		for (int i = 0; i < defaultTableModel.getColumnCount(); i++) {
			sortKeys.add(new RowSorter.SortKey(i, SortOrder.ASCENDING));
		}
		sorter.setSortKeys(sortKeys);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	
	public ProMTable getErrorTable() {
		return errorTable;
	}

	public void setErrorTable(ProMTable errorTable) {
		this.errorTable = errorTable;
	}
	
	public DefaultTableModel getDefaultTableModel() {
		return defaultTableModel;
	}

	public void setDefaultTableModel(DefaultTableModel defaultTableModel) {
		this.defaultTableModel = defaultTableModel;
	}

	public void tableChanged(TableModelEvent e) {
		int row = e.getFirstRow();
        int column = e.getColumn();
        int j = defaultTableModel.getColumnCount() - 1;
        TableModel model = (TableModel)e.getSource();
        if (model.getColumnName(column) == "Whitelisting?" && !this.isFocused() && model.getValueAt(row, 0) == defaultTableModel.getValueAt(0, 0)) {
    		boolean bool = (boolean) model.getValueAt(row, column);
    		System.out.println(bool);
    		for (int i = 0; i < defaultTableModel.getRowCount(); i++) {
				defaultTableModel.setValueAt(bool, i, j);
			}
        }		
	}
}
