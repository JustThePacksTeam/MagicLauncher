/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.creator.controller;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.skcraft.concurrency.Deferred;
import com.skcraft.concurrency.Deferreds;
import com.skcraft.concurrency.SettableProgress;
import com.skcraft.launcher.InstanceList;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.auth.OfflineSession;
import com.skcraft.launcher.auth.Session;
import com.skcraft.launcher.builder.BuilderConfig;
import com.skcraft.launcher.builder.FnPatternList;
import com.skcraft.launcher.creator.Creator;
import com.skcraft.launcher.creator.controller.task.DirectoryDeleter;
import com.skcraft.launcher.creator.controller.task.ManifestInfoEnumerator;
import com.skcraft.launcher.creator.controller.task.PackBuilder;
import com.skcraft.launcher.creator.controller.task.PackLoader;
import com.skcraft.launcher.creator.controller.task.ProblemChecker;
import com.skcraft.launcher.creator.controller.task.ServerDeploy;
import com.skcraft.launcher.creator.controller.task.TestLauncher;
import com.skcraft.launcher.creator.dialog.AboutDialog;
import com.skcraft.launcher.creator.dialog.BuildDialog;
import com.skcraft.launcher.creator.dialog.BuildDialog.BuildOptions;
import com.skcraft.launcher.creator.dialog.BuilderConfigDialog;
import com.skcraft.launcher.creator.dialog.DeployServerDialog;
import com.skcraft.launcher.creator.dialog.DeployServerDialog.DeployOptions;
import com.skcraft.launcher.creator.dialog.GenerateListingDialog;
import com.skcraft.launcher.creator.dialog.PackManagerFrame;
import com.skcraft.launcher.creator.dialog.ProblemViewer;
import com.skcraft.launcher.creator.dialog.VersionCheckDialog;
import com.skcraft.launcher.creator.model.creator.CreatorConfig;
import com.skcraft.launcher.creator.model.creator.ManifestEntry;
import com.skcraft.launcher.creator.model.creator.Pack;
import com.skcraft.launcher.creator.model.creator.Problem;
import com.skcraft.launcher.creator.model.creator.Workspace;
import com.skcraft.launcher.creator.model.swing.PackTableModel;
import com.skcraft.launcher.creator.server.TestServer;
import com.skcraft.launcher.creator.server.TestServerBuilder;
import com.skcraft.launcher.creator.swing.PackDirectoryFilter;
import com.skcraft.launcher.dialog.ConfigurationDialog;
import com.skcraft.launcher.dialog.ConsoleFrame;
import com.skcraft.launcher.dialog.LoginDialog;
import com.skcraft.launcher.dialog.ProgressDialog;
import com.skcraft.launcher.model.modpack.LaunchModifier;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.swing.PopupMouseAdapter;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.util.MorePaths;
import com.skcraft.launcher.util.SwingExecutor;

import lombok.Getter;

public class PackManagerController {

    private static final DateFormat VERSION_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final Pattern FILENAME_SANITIZE = Pattern.compile("[^a-z0-9_\\-\\.]+");

    @Getter private final File workspaceDir;
    @Getter private final Creator creator;
    @Getter private final File workspaceFile;
    @Getter private final File dataDir;
    @Getter private final File distDir;
    @Getter private final File launcherDir;
    @Getter private File webRoot;
    @Getter private Workspace workspace;
    @Getter private final Launcher launcher;
    @Getter private final ListeningExecutorService executor;
    @Getter private final TestServer testServer;

    private File lastServerDestDir;

    private final PackManagerFrame frame;
    private PackTableModel packTableModel;

    public PackManagerController(final PackManagerFrame frame, final File workspaceDir, final Creator creator) throws IOException {
        this.workspaceDir = workspaceDir;
        this.creator = creator;
        this.dataDir = Workspace.getDataDir(workspaceDir);
        workspaceFile = Workspace.getWorkspaceFile(workspaceDir);

        this.distDir = new File(workspaceDir, "_upload");
        launcherDir = new File(dataDir, "staging/launcher");
        final File launcherConfigDir = new File(creator.getDataDir(), "launcher");
        this.webRoot = new File(dataDir, "staging/www");

        launcherDir.mkdirs();
        launcherConfigDir.mkdirs();
        webRoot.mkdirs();

        this.launcher = new Launcher(Creator.class, new String[0], launcherDir, launcherConfigDir);
        this.executor = launcher.getExecutor();
        this.frame = frame;

        final TestServerBuilder builder = new TestServerBuilder();
        builder.setBaseDir(webRoot);
        builder.setPort(0);
        testServer = builder.build();
    }

    public void show() {
        frame.setVisible(true);
        frame.setTitle("Modpack Creator - [" + workspaceDir.getAbsolutePath() + "]");

        initListeners();
        loadWorkspace();

        Deferreds.makeDeferred(executor.submit(() -> {
            startServer();
            return null;
        }))
                .handle(
                        result -> {
                        },
                        (ex) -> SwingHelper.showErrorDialog(frame, "Failed to start a local web server. You will be unable to test modpacks.", "Error", ex)
                );
    }

    private void startServer() throws Exception {
        testServer.start();

        launcher.getProperties().setProperty("newsUrl", "http://localhost:" + testServer.getLocalPort() + "/news.html");
        launcher.getProperties().setProperty("packageListUrl", "http://localhost:" + testServer.getLocalPort() + "/packages.json");
        launcher.getProperties().setProperty("selfUpdateUrl", "http://localhost:" + testServer.getLocalPort() + "/latest.json");
    }

    private void loadWorkspace() {
        final PackLoader loader = new PackLoader();

        final SettableProgress progress = new SettableProgress("Loading workspace...", -1);

        final Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(() -> {
            final Workspace workspace = Persistence.load(workspaceFile, Workspace.class);
            workspace.setDirectory(workspaceDir);
            workspace.load();
            if (!workspaceFile.exists())
				Persistence.commitAndForget(workspace);
            this.workspace = workspace;
            return workspace;
        }))
                .thenTap(() -> progress.observe(loader))
                .thenApply(loader)
                .thenApplyAsync(packs -> {
                    final JTable table = frame.getPackTable();
                    packTableModel = new PackTableModel(packs);
                    table.setModel(packTableModel);
                    packTableModel.fireTableDataChanged();
                    table.getRowSorter().toggleSortOrder(1);
                    if (packTableModel.getRowCount() > 0)
						table.addRowSelectionInterval(0, 0);

                    return packs;
                }, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(frame, deferred, progress, "Loading workspace...", "Loading workspace...");
        SwingHelper.addErrorDialogCallback(frame, deferred);
    }

    private boolean checkPackLoaded(final Pack pack) {
        if (pack.isLoaded())
			return true;
		else {
            SwingHelper.showErrorDialog(frame, "The selected pack could not be loaded. You will have to remove it from the workspace or change its location.", "Error");
            return false;
        }
    }

    public Optional<Pack> getPackFromIndex(final int selectedIndex, final boolean requireLoaded) {
        if (selectedIndex >= 0) {
            final Pack pack = workspace.getPacks().get(selectedIndex);
            if (pack != null && (!requireLoaded || checkPackLoaded(pack)))
				return Optional.fromNullable(pack);
        }
        return Optional.absent();
    }

    public Optional<Pack> getSelectedPack(final boolean requireLoaded) {
        final JTable table = frame.getPackTable();
        int selectedIndex = table.getSelectedRow();
        if (selectedIndex >= 0) {
            selectedIndex = table.convertRowIndexToModel(selectedIndex);
            final Pack pack = workspace.getPacks().get(selectedIndex);
            if (pack != null && (!requireLoaded || checkPackLoaded(pack)))
				return Optional.fromNullable(pack);
        }

        SwingHelper.showErrorDialog(frame, "Please select a modpack from the list.", "Error");
        return Optional.absent();
    }

    public boolean writeWorkspace() {
        try {
            Persistence.commit(workspace);
            return true;
        } catch (final IOException e) {
            SwingHelper.showErrorDialog(frame, "Failed to write the current state of the workspace to disk. " +
                    "Any recent changes to the list of modpacks may not appear in the workspace on the next load.", "Error", e);
            return false;
        }
    }

    public boolean writeBuilderConfig(final Pack pack, final BuilderConfig config) {
        try {
            Persistence.write(pack.getConfigFile(), config, Persistence.L2F_LIST_PRETTY_PRINTER);
            return true;
        } catch (final IOException e) {
            SwingHelper.showErrorDialog(frame, "Failed to write modpack.json to disk. Aborting.", "Error", e);
            return false;
        }
    }

    private boolean isOfflineEnabled() {
        final CreatorConfig config = creator.getConfig();

        if (config.isOfflineEnabled())
			return true;
		else {
            final Session session = LoginDialog.showLoginRequest(frame, launcher);
            if (session != null) {
                config.setOfflineEnabled(true);
                Persistence.commitAndForget(config);
                return true;
            } else
				return false;
        }
    }

    public boolean canAddPackDir(final File dir) {
        try {
            if (dir.exists() && !dir.isDirectory()) {
                SwingHelper.showErrorDialog(frame, "The selected path is a file that already exists. It must be a directory.", "Error");
                return false;
            } else if (dir.getCanonicalPath().equals(workspaceDir.getCanonicalPath())) {
                SwingHelper.showErrorDialog(frame, "You cannot choose the workspace directory.", "Error");
                return false;
            } else if (workspace.hasPack(dir)) {
                SwingHelper.showErrorDialog(frame, "There is already a modpack in this workspace that uses that directory.", "Error");
                return false;
            } else
				return true;
        } catch (final IOException e) {
            SwingHelper.showErrorDialog(frame, "An unexpected error occurred while checking if the modpack being added can be added.", "Error", e);
            return false;
        }
    }

    public boolean addPackToWorkspace(final Pack pack) {
        pack.load();

        try {
            final File base = workspaceDir;
            final File child = pack.getDirectory();
            if (MorePaths.isSubDirectory(base, child))
				pack.setLocation(MorePaths.relativize(base, child));
        } catch (final IOException e) {
            SwingHelper.showErrorDialog(frame, "An unexpected error occurred that could have been caused by the removal " +
                    "of the workspace or modpack directory.", "Error", e);
            return false;
        }

        final List<Pack> packs = workspace.getPacks();
        pack.setWorkspace(workspace);
        packs.add(pack);
        packTableModel.fireTableRowsInserted(packs.size() - 1, packs.size() - 1);
        writeWorkspace();

        return true;
    }

    public void updatePackInWorkspace(final Pack pack) {
        final List<Pack> packs = workspace.getPacks();
        pack.load();
        final int index = packs.indexOf(pack);
        if (index >= 0)
			packTableModel.fireTableRowsUpdated(index, index);
        writeWorkspace();
    }

    public boolean removePackFromWorkspace(final Pack pack) {
        if (workspace.getPacks().remove(pack)) {
            packTableModel.fireTableDataChanged();
            writeWorkspace();
            return true;
        } else
			return false;
    }

    private void addDefaultConfig(final BuilderConfig config) {
        final LaunchModifier launchModifier = new LaunchModifier();
        launchModifier.setFlags(ImmutableList.of("-Dfml.ignoreInvalidMinecraftCertificates=true"));
        config.setLaunchModifier(launchModifier);

        final FnPatternList userFiles = new FnPatternList();
        userFiles.setInclude(Lists.newArrayList("options.txt", "optionsshaders.txt"));
        userFiles.setExclude(Lists.<String>newArrayList());
        config.setUserFiles(userFiles);
    }

    private void initListeners() {
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(final WindowEvent e) {
                try {
                    testServer.stop();
                } catch (final Exception ignored) {
                }
                System.exit(0); // TODO: Proper shutdown
            }
        });

        frame.getPackTable().addMouseListener(new MouseAdapter() {
            @Override
			public void mousePressed(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final JTable table = (JTable) e.getSource();
                    final Point point = e.getPoint();
                    int selectedIndex = table.rowAtPoint(point);
                    if (selectedIndex >= 0) {
                        selectedIndex = table.convertRowIndexToModel(selectedIndex);
                        final Optional<Pack> optional = getPackFromIndex(selectedIndex, true);
                        if (optional.isPresent())
							if (e.isControlDown())
								SwingHelper.browseDir(optional.get().getDirectory(), frame);
							else
								startTest(optional.get(), false);
                    }
                }
            }
        });

        frame.getPackTable().addMouseListener(new PopupMouseAdapter() {
            @Override
            protected void showPopup(final MouseEvent e) {
                final JTable table = (JTable) e.getSource();
                final Point point = e.getPoint();
                final int selectedIndex = table.rowAtPoint(point);
                if (selectedIndex >= 0) {
                    table.setRowSelectionInterval(selectedIndex, selectedIndex);
                    final Optional<Pack> optional = getSelectedPack(false);
                    if (optional.isPresent())
						popupPackMenu(e.getComponent(), e.getX(), e.getY(), optional.get());
                }
            }
        });

        frame.getNewPackMenuItem().addActionListener(event -> tryAddPackViaDialog());

        frame.getNewPackAtLocationMenuItem().addActionListener(e -> tryAddPackViaDirectory(true));

        frame.getImportPackMenuItem().addActionListener(event -> tryAddPackViaDirectory(false));

        frame.getRemovePackItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(false);

            if (optional.isPresent()) {
                final Pack pack = optional.get();

                if (!pack.isLoaded() || SwingHelper.confirmDialog(frame, "Are you sure that you want to remove this modpack? No files will be deleted " +
                        "and you can later re-import the modpack using 'Add Existing'.", "Confirm"))
					removePackFromWorkspace(pack);
            }
        });

        frame.getDeletePackItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(false);

            if (optional.isPresent()) {
                final Pack pack = optional.get();

                final String input = JOptionPane.showInputDialog(
                        frame,
                        "Are you sure that you want to delete '" + pack.getDirectory().getAbsolutePath() + "'? " +
                                "If yes, type 'delete' below.",
                        "Confirm",
                        JOptionPane.WARNING_MESSAGE);

                if (input != null && input.replaceAll("'", "").equalsIgnoreCase("delete")) {
                    removePackFromWorkspace(pack);

                    final DirectoryDeleter deleter = new DirectoryDeleter(pack.getDirectory());
                    final Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(deleter), executor);
                    ProgressDialog.showProgress(frame, deferred, deleter, "Deleting modpack...", "Deleting modpack...");
                    SwingHelper.addErrorDialogCallback(frame, deferred);
                } else if (input != null)
					SwingHelper.showMessageDialog(frame, "You did not enter the correct word. Nothing was deleted.", "Failure", null, JOptionPane.INFORMATION_MESSAGE);
            }
        });

        frame.getChangePackLocationMenuItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(false);

            if (optional.isPresent()) {
                final Pack pack = optional.get();

                final JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Choose New Folder for Pack");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setFileFilter(new PackDirectoryFilter());

                File dir = workspaceDir;

                do {
                    chooser.setCurrentDirectory(dir);
                    final int returnVal = chooser.showOpenDialog(frame);

                    if (returnVal == JFileChooser.APPROVE_OPTION)
						dir = chooser.getSelectedFile();
					else
						return;
                } while (!canAddPackDir(dir));

                pack.setLocation(dir.getAbsolutePath());
                updatePackInWorkspace(pack);
            }
        });

        frame.getRefreshMenuItem().addActionListener(e -> loadWorkspace());

        frame.getQuitMenuItem().addActionListener(e -> {
            frame.dispose();
        });

        frame.getEditConfigMenuItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                final Pack pack = optional.get();
                final File file = pack.getConfigFile();
                final BuilderConfig config = Persistence.read(file, BuilderConfig.class);

                if (BuilderConfigDialog.showEditor(frame, config)) {
                    writeBuilderConfig(pack, config);
                    updatePackInWorkspace(pack);
                }
            }
        });

        frame.getOpenFolderMenuItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent())
				SwingHelper.browseDir(optional.get().getDirectory(), frame);
        });

        frame.getCheckProblemsMenuItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                final ProblemChecker checker = new ProblemChecker(optional.get());
                final Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(checker), executor)
                        .handleAsync(this::showProblems, (ex) -> {
                        }, SwingExecutor.INSTANCE);
                SwingHelper.addErrorDialogCallback(frame, deferred);
            }
        });

        frame.getTestMenuItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                final Pack pack = optional.get();
                startTest(pack, false);
            }
        });

        frame.getTestOnlineMenuItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                final Pack pack = optional.get();
                startTest(pack, true);
            }
        });

        frame.getOptionsMenuItem().addActionListener(e -> {
            final ConfigurationDialog configDialog = new ConfigurationDialog(frame, launcher);
            configDialog.setVisible(true);
        });

        frame.getClearInstanceMenuItem().addActionListener(e -> {
            final DirectoryDeleter deleter = new DirectoryDeleter(launcher.getInstancesDir());
            final Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(deleter), executor);
            ProgressDialog.showProgress(frame, deferred, deleter, "Deleting test instances...", "Deleting test instances...");
            SwingHelper.addErrorDialogCallback(frame, deferred);
        });

        frame.getClearWebRootMenuItem().addActionListener(e -> {
            final DirectoryDeleter deleter = new DirectoryDeleter(webRoot);
            final Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(deleter), executor);
            ProgressDialog.showProgress(frame, deferred, deleter, "Deleting web server files...", "Deleting web server files...");
            SwingHelper.addErrorDialogCallback(frame, deferred);
        });

        frame.getBuildMenuItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                final Pack pack = optional.get();
                buildPack(pack);
            }
        });

        frame.getDeployServerMenuItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                final Pack pack = optional.get();
                final DeployOptions options = DeployServerDialog.showDeployDialog(frame, lastServerDestDir);

                if (options != null) {
                    ConsoleFrame.showMessages();

                    final File destDir = options.getDestDir();
                    destDir.mkdirs();
                    lastServerDestDir = destDir;

                    final ServerDeploy deploy = new ServerDeploy(pack.getSourceDir(), options);
                    final Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(deploy), executor)
                            .handleAsync(r -> SwingHelper.showMessageDialog(frame, "Server deployment complete!", "Success", null, JOptionPane.INFORMATION_MESSAGE),
                                    ex -> {
                                    },
                                    SwingExecutor.INSTANCE);
                    ProgressDialog.showProgress(frame, deferred, deploy, "Deploying files...", "Deploying server files...");
                    SwingHelper.addErrorDialogCallback(frame, deferred);
                }
            }
        });

        frame.getGeneratePackagesMenuItem().addActionListener(e -> {
            final List<ManifestEntry> entries = workspace.getPackageListingEntries();
            final ManifestInfoEnumerator enumerator = new ManifestInfoEnumerator(distDir);
            final Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(() -> enumerator.apply(entries)))
                    .handleAsync(loaded -> {
                        final GenerateListingDialog dialog = new GenerateListingDialog(frame);
                        final GenerateListingController controller = new GenerateListingController(dialog, workspace, loaded, executor);
                        controller.setOutputDir(distDir);
                        controller.show();
                    }, ex -> {
                    }, SwingExecutor.INSTANCE);
            ProgressDialog.showProgress(frame, deferred, new SettableProgress("Searching...", -1), "Searching for manifests...", "Searching for manifests...");
            SwingHelper.addErrorDialogCallback(frame, deferred);
        });

        frame.getVersionCheckMenuItem().addActionListener(e -> {
            final Optional<Pack> optional = getSelectedPack(true);

            if (optional.isPresent()) {
                final Pack pack = optional.get();

                final VersionCheckDialog dialog = new VersionCheckDialog(frame);
                final VersionCheckController controller = new VersionCheckController(dialog, executor);
                    controller.showUpdates(pack.getModsDir(), pack.getCachedConfig().getGameVersion(), frame);
            }
        });

        frame.getOpenOutputFolderMenuItem().addActionListener(e -> SwingHelper.browseDir(distDir, frame));

        frame.getOpenWorkspaceFolderMenuItem().addActionListener(e1 -> SwingHelper.browseDir(workspaceDir, frame));

        frame.getOpenLauncherFolderMenuItem().addActionListener(e1 -> SwingHelper.browseDir(launcherDir, frame));

        frame.getOpenWebRootMenuItem().addActionListener(e1 -> SwingHelper.browseDir(webRoot, frame));

        frame.getOpenConsoleMenuItem().addActionListener(e -> {
            ConsoleFrame.showMessages();
        });

        frame.getDocsMenuItem().addActionListener(e -> {
            SwingHelper.openURL("https://github.com/SKCraft/Launcher/wiki", frame);
        });

        frame.getAboutMenuItem().addActionListener(e -> {
            AboutDialog.showAboutDialog(frame);
        });

        SwingHelper.addActionListeners(frame.getNewPackButton(), frame.getNewPackMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getImportButton(), frame.getImportPackMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getEditConfigButton(), frame.getEditConfigMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getOpenFolderButton(), frame.getOpenFolderMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getCheckProblemsButton(), frame.getCheckProblemsMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getTestButton(), frame.getTestMenuItem().getActionListeners());
        SwingHelper.addActionListeners(frame.getBuildButton(), frame.getBuildMenuItem().getActionListeners());
    }

    private void popupPackMenu(final Component component, final int x, final int y, final Pack pack) {
        final JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;

        menuItem = new JMenuItem("Edit modpack.json...");
        menuItem.addActionListener(e -> frame.getEditConfigMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("Open Directory");
        menuItem.addActionListener(e -> frame.getOpenFolderMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("Check for Problems");
        menuItem.addActionListener(e -> frame.getCheckProblemsMenuItem().doClick());
        popup.add(menuItem);

        popup.addSeparator();

        menuItem = new JMenuItem("Test");
        menuItem.addActionListener(e -> frame.getTestMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("Test Online");
        menuItem.addActionListener(e -> frame.getTestOnlineMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("Build...");
        menuItem.addActionListener(e -> frame.getBuildMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("Deploy Server...");
        menuItem.addActionListener(e -> frame.getDeployServerMenuItem().doClick());
        popup.add(menuItem);

        popup.addSeparator();

        menuItem = new JMenuItem("Change Location...");
        menuItem.addActionListener(e -> frame.getChangePackLocationMenuItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("Remove...");
        menuItem.addActionListener(e -> frame.getRemovePackItem().doClick());
        popup.add(menuItem);

        menuItem = new JMenuItem("Delete Forever...");
        menuItem.addActionListener(e -> frame.getDeletePackItem().doClick());
        popup.add(menuItem);

        popup.show(component, x, y);
    }

    private void tryAddPackViaDialog() {
        final BuilderConfig config = new BuilderConfig();
        addDefaultConfig(config);
        final Pack pack = new Pack();
        File dir;

        do
			if (BuilderConfigDialog.showEditor(frame, config))
				dir = new File(workspaceDir, config.getName());
			else
				return;
		while (!canAddPackDir(dir));

        pack.setLocation(dir.getAbsolutePath());

        if (pack.getConfigFile().exists()) {
            if (SwingHelper.confirmDialog(frame, "There's already an existing modpack with that name, though " +
                    "it is not imported into this workspace. Would you like to import it and ignore " +
                    "the new modpack that you just entered?", "Conflict"))
				addPackToWorkspace(pack);
        } else if (writeBuilderConfig(pack, config)) {
		    pack.createGuideFolders();
		    addPackToWorkspace(pack);
		}
    }

    private void tryAddPackViaDirectory(final boolean createNew) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Folder for Pack");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setFileFilter(new PackDirectoryFilter());

        File dir = workspaceDir;

        do {
            chooser.setCurrentDirectory(dir);
            final int returnVal = chooser.showOpenDialog(frame);

            if (returnVal == JFileChooser.APPROVE_OPTION)
				dir = chooser.getSelectedFile();
			else
				return;
        } while (!canAddPackDir(dir));

        final Pack pack = new Pack();
        pack.setLocation(dir.getAbsolutePath());

        if (pack.getConfigFile().exists()) {
            if (createNew) {
                if (SwingHelper.confirmDialog(frame, "There's already a modpack in that directory. Do you want to import it instead?", "Exists Already"))
					addPackToWorkspace(pack);
            } else
				addPackToWorkspace(pack);
        } else if (createNew || SwingHelper.confirmDialog(frame, "You've selected a directory that doesn't seem to be a modpack " +
                "(at least with the directory structure this program expects). Would you like to create the files necessary to turn " +
                "that folder into a modpack?", "Import Error")) {

            final BuilderConfig config = new BuilderConfig();
            addDefaultConfig(config);

            if (BuilderConfigDialog.showEditor(frame, config))
				if (writeBuilderConfig(pack, config)) {
                    pack.createGuideFolders();
                    addPackToWorkspace(pack);
                }
        }
    }

    private void startTest(final Pack pack, final boolean online) {
        Session session;

        if (online) {
            session = LoginDialog.showLoginRequest(frame, launcher);
            if (session == null)
				return;
        } else {
            if (!isOfflineEnabled())
				return;

            session = new OfflineSession("Player");
        }

        final String version = generateVersionFromDate();

        final PackBuilder builder = new PackBuilder(pack, webRoot, version, "staging.json", false, false);
        final InstanceList.Enumerator enumerator = launcher.getInstances().createEnumerator();
        final TestLauncher instanceLauncher = new TestLauncher(launcher, frame, pack.getCachedConfig().getName(), session);

        final SettableProgress progress = new SettableProgress(builder);

        ConsoleFrame.showMessages();

        final Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(builder), executor)
                .thenTap(() -> progress.set("Loading instance in test launcher...", -1))
                .thenRun(enumerator)
                .thenTap(() -> progress.set("Launching", -1))
                .thenApply(instanceLauncher)
                .handleAsync(result -> ConsoleFrame.hideMessages(), ex -> {}, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(frame, deferred, progress, "Setting up test instance...", "Preparing files for launch...");
        SwingHelper.addErrorDialogCallback(frame, deferred);
    }

    private void buildPack(final Pack pack) {
        final String initialVersion = generateVersionFromDate();
        final BuildOptions options = BuildDialog.showBuildDialog(frame, initialVersion, generateManifestName(pack), distDir);

        if (options != null) {
            ConsoleFrame.showMessages();
            final PackBuilder builder = new PackBuilder(pack, options.getDestDir(), options.getVersion(), options.getManifestFilename(), false, true);
            final Deferred<?> deferred = Deferreds.makeDeferred(executor.submit(builder), executor)
                    .handleAsync(result -> {
                        ConsoleFrame.hideMessages();
                        SwingHelper.showMessageDialog(frame, "Successfully generated the package files.", "Success", null, JOptionPane.INFORMATION_MESSAGE);
                    }, ex -> {}, SwingExecutor.INSTANCE);
            ProgressDialog.showProgress(frame, deferred, builder, "Building modpack...", "Building modpack...");
            SwingHelper.addErrorDialogCallback(frame, deferred);
        }
    }

    private void showProblems(final List<Problem> problems) {
        if (problems.isEmpty())
			SwingHelper.showMessageDialog(frame, "No potential problems found!", "Success", null, JOptionPane.INFORMATION_MESSAGE);
		else {
            final ProblemViewer viewer = new ProblemViewer(frame, problems);
            viewer.setVisible(true);
        }
    }

    public String generateManifestName(final Pack pack) {
        final File file = pack.getConfigFile();
        if (file.exists()) {
            final BuilderConfig config = Persistence.read(file, BuilderConfig.class, true);
            if (config != null) {
                String name = Strings.nullToEmpty(config.getName());
                name = name.toLowerCase();
                name = FILENAME_SANITIZE.matcher(name).replaceAll("-");
                name = name.trim();
                if (!name.isEmpty())
					return name + ".json";
            }
        }

        return "my_modpack.json";
    }

    public static String generateVersionFromDate() {
        final Date today = Calendar.getInstance().getTime();
        return VERSION_DATE_FORMAT.format(today);
    }

}
