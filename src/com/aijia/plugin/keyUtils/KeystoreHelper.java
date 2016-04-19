package com.aijia.plugin.keyUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

/**
 * Created by pc on 2016/4/7.
 */
public final class KeystoreHelper {
	public KeystoreHelper() {
	}

	public static boolean createNewStore(String osKeyStorePath, String storeType, String storePassword, String alias, String keyPassword, String description, int validityYears, DebugKeyProvider.IKeyGenOutput output) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException, IOException, DebugKeyProvider.KeytoolException {
		String os = System.getProperty("os.name");
		String keytoolCommand;
		if(os.startsWith("Windows")) {
			keytoolCommand = "keytool.exe";
		} else {
			keytoolCommand = "keytool";
		}

		String javaHome = System.getProperty("java.home");
		if(javaHome != null && javaHome.length() > 0) {
			keytoolCommand = javaHome + File.separator + "bin" + File.separator + keytoolCommand;
		}

		ArrayList commandList = new ArrayList();
		commandList.add(keytoolCommand);
		commandList.add("-genkey");
		commandList.add("-alias");
		commandList.add(alias);
		commandList.add("-keyalg");
		commandList.add("RSA");
		commandList.add("-dname");
		commandList.add(description);
		commandList.add("-validity");
		commandList.add(Integer.toString(validityYears * 365));
		commandList.add("-keypass");
		commandList.add(keyPassword);
		commandList.add("-keystore");
		commandList.add(osKeyStorePath);
		commandList.add("-storepass");
		commandList.add(storePassword);
		if(storeType != null) {
			commandList.add("-storetype");
			commandList.add(storeType);
		}

		String[] commandArray = (String[])commandList.toArray(new String[commandList.size()]);
		boolean result = false;

		int var23;
		try {
			var23 = grabProcessOutput(Runtime.getRuntime().exec(commandArray), output);
		} catch (Exception var22) {
			StringBuilder builder = new StringBuilder();
			boolean firstArg = true;
			String[] arr = commandArray;
			int len = commandArray.length;

			for(int i = 0; i < len; ++i) {
				String arg = arr[i];
				boolean hasSpace = arg.indexOf(32) != -1;
				if(firstArg) {
					firstArg = false;
				} else {
					builder.append(' ');
				}

				if(hasSpace) {
					builder.append('\"');
				}

				builder.append(arg);
				if(hasSpace) {
					builder.append('\"');
				}
			}

			throw new DebugKeyProvider.KeytoolException("Failed to create key: " + var22.getMessage(), javaHome, builder.toString());
		}

		return var23 == 0;
	}

	private static int grabProcessOutput(final Process process, final DebugKeyProvider.IKeyGenOutput output) {
		Thread t1 = new Thread("") {
			public void run() {
				InputStreamReader is = new InputStreamReader(process.getErrorStream());
				BufferedReader errReader = new BufferedReader(is);

				try {
					while(true) {
						String e = errReader.readLine();
						if(e == null) {
							break;
						}

						if(output != null) {
							output.err(e);
						} else {
							System.err.println(e);
						}
					}
				} catch (IOException var4) {
					;
				}

			}
		};
		Thread t2 = new Thread("") {
			public void run() {
				InputStreamReader is = new InputStreamReader(process.getInputStream());
				BufferedReader outReader = new BufferedReader(is);

				try {
					while(true) {
						String e = outReader.readLine();
						if(e == null) {
							break;
						}

						if(output != null) {
							output.out(e);
						} else {
							System.out.println(e);
						}
					}
				} catch (IOException var4) {
					;
				}

			}
		};
		t1.start();
		t2.start();

		try {
			t1.join();
		} catch (InterruptedException var7) {
			;
		}

		try {
			t2.join();
		} catch (InterruptedException var6) {
			;
		}

		try {
			return process.waitFor();
		} catch (InterruptedException var5) {
			return 0;
		}
	}
}
