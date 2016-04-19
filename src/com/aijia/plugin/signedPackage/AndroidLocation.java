package com.aijia.plugin.signedPackage;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Created by pc on 2016/4/7.
 */
public final class AndroidLocation {
	public static final String FOLDER_DOT_ANDROID = ".android";
	public static final String FOLDER_AVD = "avd";
	private static String sPrefsLocation = null;

	public AndroidLocation() {
	}

	@NotNull
	public static final String getFolder() throws AndroidLocation.AndroidLocationException {
		if(sPrefsLocation == null) {
			String f = findValidPath(new AndroidLocation.EnvVar[]{AndroidLocation.EnvVar.ANDROID_SDK_HOME, AndroidLocation.EnvVar.USER_HOME, AndroidLocation.EnvVar.HOME});
			if(f == null) {
				throw new AndroidLocation.AndroidLocationException("Unable to get the Android SDK home directory.\nMake sure the environment variable ANDROID_SDK_HOME is set up.");
			}

			sPrefsLocation = f;
			if(!sPrefsLocation.endsWith(File.separator)) {
				sPrefsLocation = sPrefsLocation + File.separator;
			}

			sPrefsLocation = sPrefsLocation + ".android" + File.separator;
		}

		File f1 = new File(sPrefsLocation);
		if(!f1.exists()) {
			try {
				f1.mkdir();
			} catch (SecurityException var3) {
				AndroidLocation.AndroidLocationException e2 = new AndroidLocation.AndroidLocationException(String.format("Unable to create folder \'%1$s\'. This is the path of preference folder expected by the Android tools.", new Object[]{sPrefsLocation}));
				e2.initCause(var3);
				throw e2;
			}
		} else if(f1.isFile()) {
			throw new AndroidLocation.AndroidLocationException(sPrefsLocation + " is not a directory! " + "This is the path of preference folder expected by the Android tools.");
		}

		return sPrefsLocation;
	}

	public static final void resetFolder() {
		sPrefsLocation = null;
	}

	private static String findValidPath(AndroidLocation.EnvVar... vars) {
		AndroidLocation.EnvVar[] var1 = vars;
		int var2 = vars.length;

		for(int var3 = 0; var3 < var2; ++var3) {
			AndroidLocation.EnvVar var = var1[var3];
			String path;
			if(var.mIsSysProp) {
				path = checkPath(System.getProperty(var.mName));
				if(path != null) {
					return path;
				}
			}

			if(var.mIsEnvVar) {
				path = checkPath(System.getenv(var.mName));
				if(path != null) {
					return path;
				}
			}
		}

		return null;
	}

	private static String checkPath(String path) {
		if(path != null) {
			File f = new File(path);
			if(f.isDirectory()) {
				return path;
			}
		}

		return null;
	}

	public static enum EnvVar {
		ANDROID_SDK_HOME("ANDROID_SDK_HOME", true, true),
		USER_HOME("user.home", true, false),
		HOME("HOME", false, true);

		final String mName;
		final boolean mIsSysProp;
		final boolean mIsEnvVar;

		private EnvVar(String name, boolean isSysProp, boolean isEnvVar) {
			this.mName = name;
			this.mIsSysProp = isSysProp;
			this.mIsEnvVar = isEnvVar;
		}

		public String getName() {
			return this.mName;
		}
	}

	public static final class AndroidLocationException extends Exception {
		private static final long serialVersionUID = 1L;

		public AndroidLocationException(String string) {
			super(string);
		}
	}
}
