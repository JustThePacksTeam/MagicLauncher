package net.teamfruit.skcraft.launcher.swing;

import java.awt.Component;

import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.skcraft.launcher.Instance;

public class InstanceCellFactory implements TableCellRenderer, ListCellRenderer<Instance> {

	@Override
	public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
		return getCellComponent(table, value, isSelected);
	}

	@Override
	public Component getListCellRendererComponent(final JList<? extends Instance> list, final Instance value, final int index, final boolean isSelected, final boolean cellHasFocus) {
		return getCellComponent(list, value, isSelected);
	}

	public InstanceTableCellPanel getCellComponent(final JComponent component, @Nullable final Object value, final boolean isSelected) {
		final InstanceTableCellPanel tablecell = new InstanceTableCellPanel(component);

		if (value instanceof Instance) {
			final Instance instance = (Instance) value;
			tablecell.setTitle(instance.getTitle());
			tablecell.setShowPlayIcon(isSelected);
			tablecell.setShowSelected(isSelected);
			tablecell.setInstance(instance);
		}

		return tablecell;
	}
}
