package com.aijia.plugin.signedPackage;

import com.aijia.plugin.utils.AndroidBundle;
import com.aijia.plugin.utils.AndroidUiUtil;
import com.aijia.plugin.utils.PluginUtils;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Created by pc on 2016/4/7.
 */
class KeyStoreStep extends SignedPackageWizardStep implements ApkSigningSettingsForm {
	private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.exportSignedPackage.KeystoreStep");

	private static final String KEY_STORE_PASSWORD_KEY = "KEY_STORE_PASSWORD";
	private static final String KEY_PASSWORD_KEY = "KEY_PASSWORD";

	private JPanel myContentPanel;
	private JPasswordField myKeyStorePasswordField;
	private JPasswordField myKeyPasswordField;
	private TextFieldWithBrowseButton.NoPathCompletion myKeyAliasField;
	private JTextField myKeyStorePathField;
	private JButton myCreateKeyStoreButton;
	private JButton myLoadKeyStoreButton;
	private JBCheckBox myRememberPasswordCheckBox;

	private final SignedPackageWizard myWizard;

	private final boolean myUseGradleForSigning;

	public KeyStoreStep(SignedPackageWizard wizard,boolean useGradleForSigning) {
		myWizard = wizard;
		myUseGradleForSigning = useGradleForSigning;
		final Project project = wizard.getProject();

		final SignedApkSettings settings = SignedApkSettings.getInstance(project);
		//LOG.error("NewKeyStoreDialog------->settings: "+settings);
		myKeyStorePathField.setText(settings.KEY_STORE_PATH);
		myKeyAliasField.setText(settings.KEY_ALIAS);
		myRememberPasswordCheckBox.setSelected(settings.REMEMBER_PASSWORDS);

		if (settings.REMEMBER_PASSWORDS) {
			final PasswordSafe passwordSafe = PasswordSafe.getInstance();
			try {
				String password = passwordSafe.getPassword(project, KeyStoreStep.class, makePasswordKey(KEY_STORE_PASSWORD_KEY, settings.KEY_STORE_PATH, (String)null));
				if (password != null) {
					myKeyStorePasswordField.setText(password);
				}
				password = passwordSafe.getPassword(project, KeyStoreStep.class, makePasswordKey(KEY_PASSWORD_KEY, settings.KEY_STORE_PATH, settings.KEY_ALIAS));
				if (password != null) {
					myKeyPasswordField.setText(password);
				}
			}
			catch (PasswordSafeException e) {
				LOG.debug(e);
				myKeyStorePasswordField.setText("");
				myKeyPasswordField.setText("");
			}
		}
		AndroidUiUtil.initSigningSettingsForm(project, this);
	}

	@Override
	public JComponent getPreferredFocusedComponent() {
		if (myKeyStorePathField.getText().length() == 0) {
			return myKeyStorePathField;
		}
		else if (myKeyStorePasswordField.getPassword().length == 0) {
			return myKeyStorePasswordField;
		}
		else if (myKeyAliasField.getText().length() == 0) {
			return myKeyAliasField;
		}
		else if (myKeyPasswordField.getPassword().length == 0) {
			return myKeyPasswordField;
		}
		return null;
	}

	@Override
	public JComponent getComponent() {
		return myContentPanel;
	}

	@Override
	public String getHelpId() {
		return "reference.android.reference.extract.signed.package.specify.keystore";
	}

	@Override
	protected void commitForNext() throws CommitStepException {
		final String keyStoreLocation = myKeyStorePathField.getText().trim();
		//LOG.error("commitForNext------->keyStoreLocation: "+keyStoreLocation);
		if (keyStoreLocation.length() == 0) {
			throw new CommitStepException(AndroidBundle.message("android.export.package.specify.keystore.location.error"));
		}

		final char[] keyStorePassword = myKeyStorePasswordField.getPassword();
		if (keyStorePassword.length == 0) {
			throw new CommitStepException(AndroidBundle.message("android.export.package.specify.key.store.password.error"));
		}

		final String keyAlias = myKeyAliasField.getText().trim();
		if (keyAlias.length() == 0) {
			throw new CommitStepException(AndroidBundle.message("android.export.package.specify.key.alias.error"));
		}

		final char[] keyPassword = myKeyPasswordField.getPassword();
		if (keyPassword.length == 0) {
			throw new CommitStepException(AndroidBundle.message("android.export.package.specify.key.password.error"));
		}

		if(this.myUseGradleForSigning) {
			this.myWizard.setGradleSigningInfo(new GradleSigningInfo(keyStoreLocation, keyStorePassword, keyAlias, keyPassword));
		} else {
			final KeyStore keyStore = loadKeyStore(new File(keyStoreLocation));
			if (keyStore == null) {
				throw new CommitStepException(AndroidBundle.message("android.export.package.keystore.error.title"));
			}
			loadKeyAndSaveToWizard(keyStore, keyAlias, keyPassword);
		}

		final Project project = myWizard.getProject();
		final SignedApkSettings settings = SignedApkSettings.getInstance(project);

		settings.KEY_STORE_PATH = keyStoreLocation;
		settings.KEY_ALIAS = keyAlias;

		final boolean rememberPasswords = myRememberPasswordCheckBox.isSelected();
		settings.REMEMBER_PASSWORDS = rememberPasswords;
		final PasswordSafe passwordSafe = PasswordSafe.getInstance();
		String keyStorePasswordKey = makePasswordKey(KEY_STORE_PASSWORD_KEY, keyStoreLocation, (String)null);
		String keyPasswordKey = makePasswordKey(KEY_PASSWORD_KEY, keyStoreLocation, keyAlias);

		try {
			if (rememberPasswords) {
				passwordSafe.storePassword(project, KeyStoreStep.class, keyStorePasswordKey, new String(keyStorePassword));
				passwordSafe.storePassword(project, KeyStoreStep.class, keyPasswordKey, new String(keyPassword));
			}
			else {
				passwordSafe.removePassword(project, KeyStoreStep.class, keyStorePasswordKey);
				passwordSafe.removePassword(project, KeyStoreStep.class, keyPasswordKey);
			}
		}
		catch (PasswordSafeException e) {
			LOG.debug(e);
			throw new CommitStepException("Cannot store passwords: " + e.getMessage());
		}
	}

	private static String makePasswordKey(@NotNull String prefix, @NotNull String keyStorePath, @Nullable String keyAlias) {
		return prefix + "__" + keyStorePath + (keyAlias != null?"__" + keyAlias:"");
	}

	private KeyStore loadKeyStore(File keystoreFile) throws CommitStepException {
		final char[] password = myKeyStorePasswordField.getPassword();
		FileInputStream fis = null;
		PluginUtils.checkPassword(password);
		if (!keystoreFile.isFile()) {
			throw new CommitStepException(AndroidBundle.message("android.cannot.find.file.error", keystoreFile.getPath()));
		}
		final KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			//noinspection IOResourceOpenedButNotSafelyClosed
			fis = new FileInputStream(keystoreFile);
			keyStore.load(fis, password);
		}
		catch (Exception e) {
			throw new CommitStepException(e.getMessage());
		}
		finally {
			if (fis != null) {
				try {
					fis.close();
				}
				catch (IOException ignored) {
				}
			}
			Arrays.fill(password, '\0');
		}
		return keyStore;
	}

	private void loadKeyAndSaveToWizard(KeyStore keyStore, String alias, char[] keyPassword) throws CommitStepException {
		KeyStore.PrivateKeyEntry entry;
		try {
			assert keyStore != null;
			entry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, new KeyStore.PasswordProtection(keyPassword));
		}
		catch (Exception e) {
			throw new CommitStepException("Error: " + e.getMessage());
		}
		if (entry == null) {
			throw new CommitStepException(AndroidBundle.message("android.extract.package.cannot.find.key.error", alias));
		}
		PrivateKey privateKey = entry.getPrivateKey();
		Certificate certificate = entry.getCertificate();
		if (privateKey == null || certificate == null) {
			throw new CommitStepException(AndroidBundle.message("android.extract.package.cannot.find.key.error", alias));
		}
		myWizard.setPrivateKey(privateKey);
		myWizard.setCertificate((X509Certificate)certificate);
	}

	@Override
	public JButton getLoadKeyStoreButton() {
		return myLoadKeyStoreButton;
	}

	@Override
	public JTextField getKeyStorePathField() {
		return myKeyStorePathField;
	}

	@Override
	public JPanel getPanel() {
		return myContentPanel;
	}

	@Override
	public JButton getCreateKeyStoreButton() {
		return myCreateKeyStoreButton;
	}

	@Override
	public JPasswordField getKeyStorePasswordField() {
		return myKeyStorePasswordField;
	}

	@Override
	public TextFieldWithBrowseButton getKeyAliasField() {
		return myKeyAliasField;
	}

	@Override
	public JPasswordField getKeyPasswordField() {
		return myKeyPasswordField;
	}
}
