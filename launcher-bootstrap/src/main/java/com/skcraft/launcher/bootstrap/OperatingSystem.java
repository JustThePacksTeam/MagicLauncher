package com.skcraft.launcher.bootstrap;

import java.io.File;

public enum OperatingSystem {
	LINUX("linux", new String[] { "linux", "unix" }),
	WINDOWS(
			"windows",
			new String[] { "win" }),
	OSX("osx", new String[] { "mac" }),
	UNKNOWN(
			"unknown",
			new String[0]);

	private final String name;
	private final String[] aliases;

	private OperatingSystem(final String name, final String... aliases) {
		this.name = name;
		this.aliases = (aliases==null ? new String[0] : aliases);
	}

	public String getName() {
		return this.name;
	}

	public String[] getAliases() {
		return this.aliases;
	}

	public boolean isSupported() {
		return this!=UNKNOWN;
	}

	public String getJavaDir() {
		final String separator = System.getProperty("file.separator");
		final String path = System.getProperty("java.home")+separator+"bin"
				+separator;
		if ((getCurrentPlatform()==WINDOWS)
				&&(new File(path+"javaw.exe").isFile())) {
			return path+"javaw.exe";
		}
		return path+"java";
	}

	public static OperatingSystem getCurrentPlatform() {
		final String osName = System.getProperty("os.name").toLowerCase();
		for (final OperatingSystem os : values()) {
			for (final String alias : os.getAliases()) {
				if (osName.contains(alias)) {
					return os;
				}
			}
		}
		return UNKNOWN;
	}

	public File getWorkingDirectory(final String dirname) {
		final String userHome = System.getProperty("user.home", ".");
		File workingDirectory;
		switch (this) {
			case LINUX:
				workingDirectory = new File(userHome, dirname+"/");
				break;
			case WINDOWS:
				final String applicationData = System.getenv("APPDATA");
				final String folder = applicationData!=null ? applicationData : userHome;

				workingDirectory = new File(folder, dirname+"/");
				break;
			case OSX:
				workingDirectory = new File(userHome, "Library/Application Support/"+dirname);
				break;
			default:
				workingDirectory = new File(userHome, dirname+"/");
		}
		return workingDirectory;
	}
}