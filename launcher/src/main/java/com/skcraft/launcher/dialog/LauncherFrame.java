/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.dialog;

import static com.skcraft.launcher.util.SharedLocale.*;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.WeakReference;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.basic.BasicPanelUI;

import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.launcher.Instance;
import com.skcraft.launcher.InstanceList;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.launch.LaunchListener;
import com.skcraft.launcher.launch.LaunchOptions;
import com.skcraft.launcher.launch.LaunchOptions.UpdatePolicy;
import com.skcraft.launcher.swing.ActionListeners;
import com.skcraft.launcher.swing.DoubleClickToButtonAdapter;
import com.skcraft.launcher.swing.InstanceTable;
import com.skcraft.launcher.swing.InstanceTableModel;
import com.skcraft.launcher.swing.PopupMouseAdapter;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.swing.WebpagePanel;
import com.skcraft.launcher.util.SharedLocale;
import com.skcraft.launcher.util.SwingExecutor;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import net.teamfruit.skcraft.launcher.TipList;
import net.teamfruit.skcraft.launcher.appicon.AppIcon;
import net.teamfruit.skcraft.launcher.swing.BoardPanel;
import net.teamfruit.skcraft.launcher.swing.InstanceCellFactory;
import net.teamfruit.skcraft.launcher.swing.InstanceTableCellPanel;
import net.teamfruit.skcraft.launcher.swing.TipsPanel;
import net.teamfruit.skcraft.launcher.swing.WebpageScrollBarUI;

/**
 * The main launcher frame.
 */
@Log
public class LauncherFrame extends JFrame {

    private final Launcher launcher;

    @Getter
    private final InstanceTable instancesTable = new InstanceTable();
    private final InstanceTableModel instancesModel;
    @Getter
    private final JScrollPane instanceScroll = new JScrollPane(this.instancesTable);
    private WebpagePanel webView;
    private BoardPanel<InstanceTableCellPanel> selectedPane;
    private JPanel splitPane;
    private final JButton launchButton = new JButton(SharedLocale.tr("launcher.launch"));
    private final JButton refreshButton = new JButton(SharedLocale.tr("launcher.checkForUpdates"));
    private final JButton optionsButton = new JButton(SharedLocale.tr("launcher.options"));
    //private final JButton selfUpdateButton = new JButton(SharedLocale.tr("launcher.updateLauncher"));
    private final JCheckBox updateCheck = new JCheckBox(SharedLocale.tr("launcher.downloadUpdates"));

    /**
     * Create a new frame.
     *
     * @param launcher the launcher
     */
    public LauncherFrame(@NonNull final Launcher launcher) {
        super(tr("launcher.title", launcher.getVersion()));

        this.launcher = launcher;
        this.instancesModel = new InstanceTableModel(launcher.getInstances());

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(500, 500));
        initComponents();
        pack();
        setLocationRelativeTo(null);

        AppIcon.setFrameIconSet(this, AppIcon.getAppIconSet());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	final ObservableFuture<TipList> future = launcher.getInstanceTasks().reloadTips(LauncherFrame.this);
            	future.addListener(new Runnable() {
					@Override
					public void run() {
						try {
							TipsPanel.instance.updateTipList(future.get().getTipList());
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}, SwingExecutor.INSTANCE);

                loadInstances();
            }
        });
    }

    private void initComponents() {
    	setResizable(false);

        final JPanel container = createContainerPanel();
        container.setBackground(Color.WHITE);
        container.setLayout(new BorderLayout());

        this.webView = createNewsPanel();
        this.webView.setBrowserBorder(BorderFactory.createEmptyBorder());
        this.webView.setPreferredSize(new Dimension(250, 250));
        final JScrollPane webViewScroll = this.webView.getDocumentScroll();
        webViewScroll.getVerticalScrollBar().setUI(new WebpageScrollBarUI(webViewScroll));
        webViewScroll.getHorizontalScrollBar().setUI(new WebpageScrollBarUI(webViewScroll));
        // this.webView.getDocumentScroll().getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

        this.splitPane = new JPanel(new BorderLayout());

        this.selectedPane = new BoardPanel<InstanceTableCellPanel>();
        this.selectedPane.setOpaque(false);
        this.selectedPane.setPreferredSize(new Dimension(250, 60));
        this.selectedPane.setToolTipText(tr("launcher.launchButton"));

        //this.selfUpdateButton.setVisible(this.launcher.getUpdateManager().getPendingUpdate());

        this.launcher.getUpdateManager().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("readyUpdate"))
					launcher.getUpdateManager().performUpdate(LauncherFrame.this);
            }
        });
        /*
        this.launcher.getUpdateManager().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("pendingUpdate")) {
                	final boolean enabled = (Boolean) evt.getNewValue();
					LauncherFrame.this.selfUpdateButton.setVisible(enabled);
					if (enabled)
						LauncherFrame.this.selfUpdateButton.doClick();
                }
            }
        });
        */

        this.updateCheck.setSelected(true);
        this.instancesTable.setModel(this.instancesModel);

        this.instanceScroll.setPreferredSize(new Dimension(250, this.instanceScroll.getPreferredSize().height));
        this.instanceScroll.getVerticalScrollBar().setUI(new WebpageScrollBarUI(this.instanceScroll));
        this.instanceScroll.getHorizontalScrollBar().setUI(new WebpageScrollBarUI(this.instanceScroll));
        this.instanceScroll.setBorder(BorderFactory.createEmptyBorder());

        this.launchButton.setFont(this.launchButton.getFont().deriveFont(Font.BOLD));
        final JButton expandButton = new JButton(">");
        final JPanel buttons = new JPanel(new GridLayout(3, 1));
        this.refreshButton.setIcon(SwingHelper.createIcon(Launcher.class, "refresh_icon.png", 20, 20));
        this.refreshButton.setToolTipText(tr("launcher.refreshButton"));
        this.refreshButton.setText(null);
        buttons.add(this.refreshButton);
        // buttons.add(this.updateCheck);
        // buttons.add(this.selfUpdateButton);
        this.optionsButton.setIcon(SwingHelper.createIcon(Launcher.class, "settings_icon.png", 20, 20));
        this.optionsButton.setToolTipText(tr("launcher.optionButton"));
        this.optionsButton.setText(null);
        buttons.add(this.optionsButton);
        // buttons.add(this.launchButton);
        expandButton.setIcon(SwingHelper.createIcon(Launcher.class, "expand_icon.png", 20, 20));
        expandButton.setToolTipText(tr("launcher.expandButton"));
        expandButton.setText(null);
        buttons.add(expandButton);
        buttons.setOpaque(false);
        final JPanel leftBottomTopPanel = new JPanel(new GridBagLayout());
        final JLabel leftBottomTopText = new JLabel(tr("launcher.launchTitle"));
        leftBottomTopText.setFont(new Font(leftBottomTopText.getFont().getName(), Font.PLAIN, 16));
        leftBottomTopPanel.add(leftBottomTopText);
        leftBottomTopPanel.setUI(new BasicPanelUI());
        leftBottomTopPanel.setBackground(new Color(32, 30, 98));
        final JPanel leftBottomPane = new JPanel(new BorderLayout());
        leftBottomPane.add(leftBottomTopPanel, BorderLayout.NORTH);
        leftBottomPane.add(this.selectedPane, BorderLayout.CENTER);
        leftBottomPane.add(buttons, BorderLayout.EAST);
        leftBottomPane.setOpaque(false);
        final JPanel leftPane = new JPanel(new BorderLayout());
        leftPane.add(this.webView, BorderLayout.CENTER);
        leftPane.add(leftBottomPane, BorderLayout.SOUTH);
        leftPane.setOpaque(false);
        final JLabel instanceLabel = new JLabel(tr("launcher.instance"),SwingHelper.createIcon(Launcher.class, "package_icon.png", 20, 20),SwingConstants.LEFT);
        instanceLabel.setFont(new Font(instanceLabel.getFont().getName(), Font.PLAIN, 16));
        instanceLabel.setBorder(new EmptyBorder(3, 3, 3, 3));
        final JPanel rightPane = new JPanel(new BorderLayout()) {
        	@Override
        	protected void paintComponent(Graphics g) {
        		if(!isOpaque()) {
        			Graphics2D g2d = (Graphics2D) g.create();
        			g2d.setRenderingHint(
        		            RenderingHints.KEY_ANTIALIASING,
        		            RenderingHints.VALUE_ANTIALIAS_ON);
    		        g2d.setComposite(AlphaComposite.getInstance(
    		        		AlphaComposite.SRC_OVER, 0.3f));
	        		g2d.setColor(Color.BLACK);
	        		g2d.fillRect(0, 0, getWidth(), getHeight());
	        		g2d.dispose();
        		}
        		super.paintComponent(g);
        	}
        };
        rightPane.add(instanceLabel, BorderLayout.NORTH);
        rightPane.add(this.instanceScroll, BorderLayout.CENTER);
        rightPane.setVisible(false);
        rightPane.setOpaque(false);
        this.splitPane.add(leftPane, BorderLayout.CENTER);
        this.splitPane.add(rightPane, BorderLayout.EAST);
        this.splitPane.setOpaque(false);

        expandButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final boolean visible = !rightPane.isVisible();
				rightPane.setVisible(visible);

				final int totalWidth = getWidth();
				final int rightWidth = rightPane.getWidth();
				setMinimumSize(new Dimension(750-(visible?0:rightWidth), 500));
				setSize(visible?totalWidth+rightWidth:totalWidth-rightWidth, getHeight());
			}
		});
        // SwingHelper.flattenJSplitPane(this.splitPane);

        container.add(this.splitPane, BorderLayout.CENTER);
        add(container, BorderLayout.CENTER);

        this.instancesModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                if (LauncherFrame.this.instancesTable.getRowCount() > 0)
					LauncherFrame.this.instancesTable.setRowSelectionInterval(0, 0);
            }
        });

		final Cursor cursorhand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		final Cursor cursornormal = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		this.selectedPane.setCursor(cursorhand);
		this.selectedPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				LauncherFrame.this.launchButton.doClick();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				InstanceTableCellPanel panel = selectedPane.get();
				if (panel!=null) {
					panel.setShowSelected(true);
					repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				InstanceTableCellPanel panel = selectedPane.get();
				if (panel!=null) {
					panel.setShowSelected(false);
					repaint();
				}
			}
		});

        instancesTable.addMouseListener(new DoubleClickToButtonAdapter(launchButton));

		this.instancesTable.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(final MouseEvent e) {
				final int i = LauncherFrame.this.instancesTable.rowAtPoint(e.getPoint());
                if (i>=0)
					LauncherFrame.this.instancesTable.setCursor(cursorhand);
				else
					LauncherFrame.this.instancesTable.setCursor(cursornormal);
			}
		});
		this.instancesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			private final InstanceCellFactory factory = new InstanceCellFactory();

			@Override
			public void valueChanged(final ListSelectionEvent e) {
		        if (!e.getValueIsAdjusting()) {
		        	int index = LauncherFrame.this.instancesTable.getSelectionModel().getLeadSelectionIndex();
		        	if (index<0)
		        		index = 0;
		        	final Instance instance = LauncherFrame.this.instancesModel.getValueAt(index, 0);

		        	final InstanceTableCellPanel tablecell = this.factory.getCellComponent(null, instance, false);
		        	tablecell.setShowPlayIcon(true);
		    		LauncherFrame.this.selectedPane.set(tablecell);
		        }
			}
		});

        this.refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                loadInstances();
                LauncherFrame.this.launcher.getUpdateManager().checkForUpdate();
                LauncherFrame.this.webView.browse(LauncherFrame.this.launcher.getNewsURL(), false);
            }
        });

        /*
        this.selfUpdateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LauncherFrame.this.launcher.getUpdateManager().performUpdate(LauncherFrame.this);
            }
        });
        */

        this.optionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                showOptions();
            }
        });

        this.launchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                launch();
            }
        });

        this.instancesTable.addMouseListener(new PopupMouseAdapter() {
            @Override
            protected void showPopup(final MouseEvent e) {
                final int index = LauncherFrame.this.instancesTable.rowAtPoint(e.getPoint());
                Instance selected = null;
                if (index >= 0) {
                    LauncherFrame.this.instancesTable.setRowSelectionInterval(index, index);
                    selected = LauncherFrame.this.launcher.getInstances().get(index);
                }
                popupInstanceMenu(e.getComponent(), e.getX(), e.getY(), selected);
            }
        });
    }

    protected JPanel createContainerPanel() {
        return new JPanel();
    }

    /**
     * Return the news panel.
     *
     * @return the news panel
     */
    protected WebpagePanel createNewsPanel() {
        return WebpagePanel.forURL(this.launcher.getNewsURL(), false);
    }

    /**
     * Popup the menu for the instances.
     *
     * @param component the component
     * @param x mouse X
     * @param y mouse Y
     * @param selected the selected instance, possibly null
     */
    private void popupInstanceMenu(final Component component, final int x, final int y, final Instance selected) {
        final JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;

        if (selected != null) {
            menuItem = new JMenuItem(!selected.isLocal() ? tr("instance.install") : tr("instance.launch"));
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    launch();
                }
            });
            popup.add(menuItem);

            if (selected.isLocal()) {
                popup.addSeparator();

                menuItem = new JMenuItem(SharedLocale.tr("instance.openFolder"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, selected.getContentDir(), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openSaves"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, new File(selected.getContentDir(), "saves"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openResourcePacks"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, new File(selected.getContentDir(), "resourcepacks"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openScreenshots"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, new File(selected.getContentDir(), "screenshots"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.copyAsPath"));
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final File dir = selected.getContentDir();
                        dir.mkdirs();
                        SwingHelper.setClipboard(dir.getAbsolutePath());
                    }
                });
                popup.add(menuItem);

                popup.addSeparator();

                if (!selected.isUpdatePending()) {
                    menuItem = new JMenuItem(SharedLocale.tr("instance.forceUpdate"));
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            selected.setUpdatePending(true);
                            launch();
                            LauncherFrame.this.instancesModel.update();
                        }
                    });
                    popup.add(menuItem);
                }

                menuItem = new JMenuItem(SharedLocale.tr("instance.hardForceUpdate"));
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        confirmHardUpdate(selected);
                    }
                });
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.deleteFiles"));
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        confirmDelete(selected);
                    }
                });
                popup.add(menuItem);
            }

            popup.addSeparator();
        }

        menuItem = new JMenuItem(SharedLocale.tr("launcher.refreshList"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                loadInstances();
            }
        });
        popup.add(menuItem);

        popup.show(component, x, y);

    }

    private void confirmDelete(final Instance instance) {
        if (!SwingHelper.confirmDialog(this,
                tr("instance.confirmDelete", instance.getTitle()), SharedLocale.tr("confirmTitle")))
			return;

        final ObservableFuture<Instance> future = this.launcher.getInstanceTasks().delete(this, instance);

        // Update the list of instances after updating
        future.addListener(new Runnable() {
            @Override
            public void run() {
                loadInstances();
            }
        }, SwingExecutor.INSTANCE);
    }

    private void confirmHardUpdate(final Instance instance) {
        if (!SwingHelper.confirmDialog(this, SharedLocale.tr("instance.confirmHardUpdate"), SharedLocale.tr("confirmTitle")))
			return;

        final ObservableFuture<Instance> future = this.launcher.getInstanceTasks().hardUpdate(this, instance);

        // Update the list of instances after updating
        future.addListener(new Runnable() {
            @Override
            public void run() {
                launch();
                LauncherFrame.this.instancesModel.update();
            }
        }, SwingExecutor.INSTANCE);
    }

    private void loadInstances() {
        final ObservableFuture<InstanceList> future = this.launcher.getInstanceTasks().reloadInstances(this);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                instancesModel.update();
                if (instancesTable.getRowCount() > 0)
					instancesTable.setRowSelectionInterval(0, 0);
                requestFocus();
            }
        }, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(this, future, SharedLocale.tr("launcher.checkingTitle"), SharedLocale.tr("launcher.checkingStatus"));
        SwingHelper.addErrorDialogCallback(this, future);
    }

    private void showOptions() {
        final ConfigurationDialog configDialog = new ConfigurationDialog(this, this.launcher);
        configDialog.setVisible(true);
    }

    private void launch() {
        final boolean permitUpdate = this.updateCheck.isSelected();
        final Instance instance = this.launcher.getInstances().get(this.instancesTable.getSelectedRow());

        final LaunchOptions options = new LaunchOptions.Builder()
                .setInstance(instance)
                .setListener(new LaunchListenerImpl(this))
                .setUpdatePolicy(permitUpdate ? UpdatePolicy.UPDATE_IF_SESSION_ONLINE : UpdatePolicy.NO_UPDATE)
                .setWindow(this)
                .build();
        this.launcher.getLaunchSupervisor().launch(options);
    }

    private static class LaunchListenerImpl implements LaunchListener {
        private final WeakReference<LauncherFrame> frameRef;
        private final Launcher launcher;

        private LaunchListenerImpl(final LauncherFrame frame) {
            this.frameRef = new WeakReference<LauncherFrame>(frame);
            this.launcher = frame.launcher;
        }

        @Override
        public void instancesUpdated() {
            final LauncherFrame frame = this.frameRef.get();
            if (frame != null)
				frame.loadInstances();
        }

        @Override
        public void gameStarted() {
            final LauncherFrame frame = this.frameRef.get();
            if (frame != null)
				frame.dispose();
        }

        @Override
        public void gameClosed() {
            this.launcher.showLauncherWindow();
        }
    }

}
