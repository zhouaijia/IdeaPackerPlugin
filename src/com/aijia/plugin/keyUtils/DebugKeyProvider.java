package com.aijia.plugin.keyUtils;

import com.aijia.plugin.signedPackage.AndroidLocation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * Created by pc on 2016/4/7.
 */
public class DebugKeyProvider {
	private static final String PASSWORD_STRING = "android";
	private static final char[] PASSWORD_CHAR = "android".toCharArray();
	private static final String DEBUG_ALIAS = "AndroidDebugKey";
	private static final String CERTIFICATE_DESC = "CN=Android Debug,O=Android,C=US";
	private KeyStore.PrivateKeyEntry mEntry;

	public DebugKeyProvider(String osKeyStorePath, String storeType, DebugKeyProvider.IKeyGenOutput output) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException, IOException, DebugKeyProvider.KeytoolException, AndroidLocation.AndroidLocationException {
		if(osKeyStorePath == null) {
			osKeyStorePath = getDefaultKeyStoreOsPath();
		}

		if(!this.loadKeyEntry(osKeyStorePath, storeType)) {
			this.createNewStore(osKeyStorePath, storeType, output);
		}

	}

	public static String getDefaultKeyStoreOsPath() throws DebugKeyProvider.KeytoolException, AndroidLocation.AndroidLocationException {
		String folder = AndroidLocation.getFolder();
		if(folder == null) {
			throw new DebugKeyProvider.KeytoolException("Failed to get HOME directory!\n");
		} else {
			String osKeyStorePath = folder + "debug.keystore";
			return osKeyStorePath;
		}
	}

	public PrivateKey getDebugKey() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, UnrecoverableEntryException {
		return this.mEntry != null?this.mEntry.getPrivateKey():null;
	}

	public Certificate getCertificate() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, UnrecoverableEntryException {
		return this.mEntry != null?this.mEntry.getCertificate():null;
	}

	private boolean loadKeyEntry(String osKeyStorePath, String storeType) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableEntryException {
		try {
			KeyStore e = KeyStore.getInstance(storeType != null?storeType:KeyStore.getDefaultType());
			FileInputStream fis = new FileInputStream(osKeyStorePath);
			e.load(fis, PASSWORD_CHAR);
			fis.close();
			this.mEntry = (KeyStore.PrivateKeyEntry)e.getEntry("AndroidDebugKey", new KeyStore.PasswordProtection(PASSWORD_CHAR));
			return true;
		} catch (FileNotFoundException var5) {
			return false;
		}
	}

	private void createNewStore(String osKeyStorePath, String storeType, IKeyGenOutput output) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException, IOException, DebugKeyProvider.KeytoolException {
		if(KeystoreHelper.createNewStore(osKeyStorePath, storeType, "android", "AndroidDebugKey", "android", "CN=Android Debug,O=Android,C=US", 1, output)) {
			this.loadKeyEntry(osKeyStorePath, storeType);
		}

	}

	public static class KeytoolException extends Exception {
		private static final long serialVersionUID = 1L;
		private String mJavaHome = null;
		private String mCommandLine = null;

		KeytoolException(String message) {
			super(message);
		}

		KeytoolException(String message, String javaHome, String commandLine) {
			super(message);
			this.mJavaHome = javaHome;
			this.mCommandLine = commandLine;
		}

		public String getJavaHome() {
			return this.mJavaHome;
		}

		public String getCommandLine() {
			return this.mCommandLine;
		}
	}

	public interface IKeyGenOutput {
		void out(String var1);

		void err(String var1);
	}
}
