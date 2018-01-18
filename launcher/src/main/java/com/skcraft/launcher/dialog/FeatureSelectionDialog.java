/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.dialog;

import static javax.swing.BorderFactory.*;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.commons.lang.StringUtils;

import com.skcraft.launcher.model.modpack.Feature;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.swing.ActionListeners;
import com.skcraft.launcher.swing.CheckboxTable;
import com.skcraft.launcher.swing.FeatureTableModel;
import com.skcraft.launcher.swing.LinedBoxPanel;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.swing.TextFieldPopupMenu;
import com.skcraft.launcher.util.Environment;
import com.skcraft.launcher.util.Platform;
import com.skcraft.launcher.util.SharedLocale;

import lombok.NonNull;
import net.teamfruit.skcraft.launcher.model.modpack.SupportOS;

public class FeatureSelectionDialog extends JDialog {

    private final List<Feature> features;
    private final JPanel container = new JPanel(new BorderLayout());
    private final JEditorPane descText = new JEditorPane(new HTMLEditorKit().getContentType(), SharedLocale.tr("features.selectForInfo"));
    private final JScrollPane descScroll = new JScrollPane(this.descText);
    private final CheckboxTable componentsTable = new CheckboxTable();
    private final JScrollPane componentsScroll = new JScrollPane(this.componentsTable);
    private final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.componentsScroll, this.descScroll);
    private final LinedBoxPanel buttonsPanel = new LinedBoxPanel(true);
    private final JButton installButton = new JButton(SharedLocale.tr("features.install"));

    public FeatureSelectionDialog(final Window owner, @NonNull final List<Feature> features) {
        super(owner, ModalityType.DOCUMENT_MODAL);

        this.features = features;

        setTitle(SharedLocale.tr("features.title"));
        initComponents();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(500, 400));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        this.componentsTable.setModel(new FeatureTableModel(this.features) {
        	@Override
        	protected boolean checkFeature(Feature feature, boolean newvalue) {
        		if (newvalue&&!isSupportedOS(feature.getDescription()))
        			if (!SwingHelper.confirmDialog(componentsTable, SharedLocale.tr("features.intro.unsupportedOS"), SharedLocale.tr("features.intro.unsupportedOSTitle")))
        				return false;
        		return true;
        	}

            private boolean isSupportedOS(String desc) {
            	String os = StringUtils.substringBetween(desc, "<!--OS:(", ")-->");
            	if (os!=null)
        			try {
        				SupportOS supported = Persistence.getMapper().readValue(os, SupportOS.class);
        				if (supported!=null) {
        					Platform platform = Environment.detectPlatform();
        					List<Platform> allow = supported.getAllow();
        					List<Platform> deny = supported.getDeny();
        					if ((allow!=null&&!allow.contains(platform))||(deny!=null&&deny.contains(platform)))
        						return false;
        				}
        			} catch (Exception e) {
        			}
            	return true;
            }
        });

        this.descScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        this.descText.setOpaque(false);
        this.descText.setBorder(null);
        this.descText.setEditable(false);
        final JLabel descTextLabel = new JLabel();
        final Font descTextFont = new Font(Font.DIALOG, Font.PLAIN, descTextLabel.getFont().getSize());
        final String descTextColor = "#"+Integer.toHexString(descTextLabel.getForeground().getRGB());
        final String descTextBodyRule = String.format("body { font-family: %s; font-size: %spt; color: %s; }", descTextFont.getFamily(), descTextFont.getSize(), descTextColor);
        ((HTMLDocument)this.descText.getDocument()).getStyleSheet().addRule(descTextBodyRule);
        this.descText.setCaretColor(new JLabel().getForeground());
        this.descText.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
					if (e.getURL() != null)
						SwingHelper.openURL(e.getURL(), FeatureSelectionDialog.this);
            }
        });

        SwingHelper.removeOpaqueness(this.descText);
        this.descText.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);

        this.splitPane.setDividerLocation(250);
        this.splitPane.setDividerSize(6);
        SwingHelper.flattenJSplitPane(this.splitPane);

        this.container.setBorder(createEmptyBorder(12, 12, 12, 12));
        this.container.add(this.splitPane, BorderLayout.CENTER);

        this.buttonsPanel.addGlue();
        this.buttonsPanel.addElement(this.installButton);

        final JLabel descLabel = new JLabel(SharedLocale.tr("features.intro"));
        descLabel.setBorder(createEmptyBorder(12, 12, 4, 12));

        SwingHelper.equalWidth(this.installButton, new JButton(SharedLocale.tr("button.cancel")));

        add(descLabel, BorderLayout.NORTH);
        add(this.container, BorderLayout.CENTER);
        add(this.buttonsPanel, BorderLayout.SOUTH);

        this.componentsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
			public void valueChanged(final ListSelectionEvent e) {
                updateDescription();
            }
        });

        this.installButton.addActionListener(ActionListeners.dispose(this));
    }

    private void updateDescription() {
        final Feature feature = this.features.get(this.componentsTable.getSelectedRow());

        if (feature != null)
			this.descText.setText(feature.getDescription());
		else
			this.descText.setText(SharedLocale.tr("features.selectForInfo"));
    }

}
