/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.launch;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import com.google.common.base.Function;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.dialog.ProcessConsoleFrame;
import com.skcraft.launcher.swing.MessageLog;

import lombok.NonNull;
import lombok.extern.java.Log;
import net.teamfruit.skcraft.launcher.launch.ExitHandler;

/**
 * Handles post-process creation during launch.
 */
@Log
public class LaunchProcessHandler implements Function<Process, ProcessConsoleFrame> {

    private static final int CONSOLE_NUM_LINES = 10000;

	private final Launcher launcher;
    private ProcessConsoleFrame consoleFrame;

    public LaunchProcessHandler(@NonNull Launcher launcher) {
        this.launcher = launcher;
    }

    @Override
    public ProcessConsoleFrame apply(final Process process) {
        log.info("Watching process " + process);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    consoleFrame = new ProcessConsoleFrame(CONSOLE_NUM_LINES, true);
                    consoleFrame.setProcess(process);
                    if (launcher.getConfig().isShowConsole())
                    	consoleFrame.setVisible(true);
                    MessageLog messageLog = consoleFrame.getMessageLog();
                    messageLog.consume(process.getInputStream());
                    messageLog.consume(process.getErrorStream(), Color.RED);
                }
            });

            // Wait for the process to end
            process.waitFor();
        } catch (InterruptedException e) {
            // Orphan process
        } catch (InvocationTargetException e) {
            log.log(Level.WARNING, "Unexpected failure", e);
        }

        log.info("Process ended, re-showing launcher...");

        // Restore the launcher
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (consoleFrame != null) {
                    consoleFrame.setProcess(null);
			        consoleFrame.getClearLogButton().setEnabled(false);
                    consoleFrame.requestFocus();
                }
            }
        });

        return consoleFrame;
    }

}
