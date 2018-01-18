package net.teamfruit.skcraft.launcher.relaunch;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.launch.JavaProcessBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

@Log
@RequiredArgsConstructor
public class LauncherRelauncher {
	private final Launcher launcher;
	private final File result;

	public void launch() throws Throwable {
        File[] files = result.getParentFile().listFiles(new LauncherBinary.Filter());
        List<LauncherBinary> binaries = new ArrayList<LauncherBinary>();

        if (files != null)
			for (File file : files) {
                log.info("Found " + file.getAbsolutePath() + "...");
                binaries.add(new LauncherBinary(file));
            }

        if (!binaries.isEmpty())
			launchExisting(binaries, true);
    }

    public void launchExisting(List<LauncherBinary> binaries, boolean redownload) throws Exception {
        Collections.sort(binaries);
        LauncherBinary working = null;
        Class<?> clazz = null;

        for (LauncherBinary binary : binaries) {
            File testFile = binary.getPath();
            try {
                testFile = binary.getExecutableJar();
                log.info("Trying " + testFile.getAbsolutePath() + "...");
                clazz = load(testFile);
                log.info("Launcher loaded successfully.");
                working = binary;
                break;
            } catch (Throwable t) {
                log.log(Level.WARNING, "Failed to load " + testFile.getAbsoluteFile(), t);
            }
        }

        if (working != null) {
            for (LauncherBinary binary : binaries)
				if (working != binary) {
                    log.info("Removing " + binary.getPath() + "...");
                    binary.remove();
                }

            execute(working, clazz);
        } else
        	throw new IOException("Failed to find launchable .jar");
    }

    public void execute(LauncherBinary working, Class<?> clazz) {
		try {
			JavaProcessBuilder builder = new JavaProcessBuilder();
			builder.classPath(working.getExecutableJar().getCanonicalFile());
			builder.setMainClass(clazz.getName());
			List<String> args = builder.getArgs();
			args.addAll(Arrays.asList(launcher.getArgs()));

			File workingdir = new File(System.getProperty("user.dir")).getCanonicalFile();
	        ProcessBuilder processBuilder = new ProcessBuilder(builder.buildCommand());
	        processBuilder.directory(workingdir);
	        log.info("Relaunching: " + builder);
	        processBuilder.start();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public Class<?> load(File jarFile) throws MalformedURLException, ClassNotFoundException {
        URL[] urls = new URL[] { jarFile.toURI().toURL() };
        URLClassLoader child = new URLClassLoader(urls, this.getClass().getClassLoader());
        Class<?> clazz = Class.forName(launcher.getMainClass().getName(), true, child);
        return clazz;
    }

}
