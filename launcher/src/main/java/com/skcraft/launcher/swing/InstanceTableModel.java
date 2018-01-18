/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.swing;

import javax.swing.table.AbstractTableModel;

import com.skcraft.launcher.Instance;
import com.skcraft.launcher.InstanceList;
import com.skcraft.launcher.util.SharedLocale;

import lombok.Getter;

public class InstanceTableModel extends AbstractTableModel {

    private @Getter final InstanceList instances;

    public InstanceTableModel(final InstanceList instances) {
        this.instances = instances;
    }

    public void update() {
        this.instances.sort();
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(final int columnIndex) {
        switch (columnIndex) {
            case 0:
                return SharedLocale.tr("launcher.modpackColumn");
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Instance.class;
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(final Object value, final int rowIndex, final int columnIndex) {
        switch (columnIndex) {
            case 0:
            default:
                break;
        }
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        switch (columnIndex) {
            case 0:
                return false;
            default:
                return false;
        }
    }

    @Override
    public int getRowCount() {
        return this.instances.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Instance getValueAt(final int rowIndex, final int columnIndex) {
        switch (columnIndex) {
            case 0:
            	if (rowIndex<this.instances.size())
            	return this.instances.get(rowIndex);
            default:
                return null;
        }
    }

}
