/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.swing;

import javax.swing.*;
import javax.swing.border.Border;

import com.skcraft.launcher.creator.model.creator.RecentEntry;

import java.awt.*;

public class BorderCellRenderer implements ListCellRenderer<RecentEntry> {

    private final Border border;
    private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

    public BorderCellRenderer(Border border) {
        this.border = border;
    }

    public Component getListCellRendererComponent(JList<? extends RecentEntry> list, RecentEntry value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        renderer.setBorder(border);
        return renderer;
    }

}
