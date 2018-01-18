/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.model.swing;

import com.skcraft.launcher.model.modpack.Feature.Recommendation;

import javax.swing.*;

public class RecommendationComboBoxModel extends AbstractListModel<Recommendation> implements ComboBoxModel<Recommendation> {

    private Recommendation selection;

    @Override
    public void setSelectedItem(Object anItem) {
        selection = (Recommendation) anItem;
    }

    @Override
    public Recommendation getSelectedItem() {
        return selection;
    }

    @Override
    public int getSize() {
        return Recommendation.values().length + 1;
    }

    @Override
    public Recommendation getElementAt(int index) {
        if (index == 0) {
            return null;
        } else {
            return Recommendation.values()[index - 1];
        }
    }
}
