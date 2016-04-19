package com.aijia.plugin.signedPackage;

import com.aijia.plugin.keyUtils.DebugKeyProvider;
import com.aijia.plugin.keyUtils.KeystoreHelper;
import com.aijia.plugin.utils.AndroidBundle;
import com.aijia.plugin.utils.PluginUtils;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Created by pc on 2016/4/7.
 */
public abstract class NewKeyForm {
	private static final Logger LOG = Logger.getInstance("#com.aijia.plugin.signedPackage.NewKeyForm");
	private JPanel myContentPanel;
	private JTextField myAliasField;
	private JPasswordField myKeyPasswordField;
	private JPasswordField myConfirmKeyPasswordField;
	private JSpinner myValiditySpinner;
	private JTextField myFirstAndLastNameField;
	private JTextField myOrganizationUnitField;
	private JTextField myCityField;
	private JTextField myStateOrProvinceField;
	private JTextField myCountryCodeField;
	private JPanel myCertificatePanel;
	private JTextField myOrganizationField;
	private KeyStore myKeyStore;
	private PrivateKey myPrivateKey;
	private X509Certificate myCertificate;

	public NewKeyForm() {
		this.myValiditySpinner.setModel(new SpinnerNumberModel(25, 1, 1000, 1));
	}

	private int getValidity() {
		SpinnerNumberModel model = (SpinnerNumberModel)this.myValiditySpinner.getModel();
		return model.getNumber().intValue();
	}

	public void init() {
		this.myAliasField.setText(this.generateAlias());
	}

	public JPanel getContentPanel() {
		return this.myContentPanel;
	}

	private boolean findNonEmptyCertificateField() {
		Component[] var1 = this.myCertificatePanel.getComponents();
		int var2 = var1.length;

		for(int var3 = 0; var3 < var2; ++var3) {
			Component component = var1[var3];
			if(component instanceof JTextField && ((JTextField)component).getText().trim().length() > 0) {
				return true;
			}
		}

		return false;
	}

	public void createKey() throws CommitStepException {
		if(this.getKeyAlias().length() == 0) {
			throw new CommitStepException(AndroidBundle.message("android.export.package.specify.key.alias.error", new Object[0]));
		} else {
			PluginUtils.checkNewPassword(this.myKeyPasswordField, this.myConfirmKeyPasswordField);
			if(!this.findNonEmptyCertificateField()) {
				throw new CommitStepException(AndroidBundle.message("android.export.package.specify.certificate.field.error", new Object[0]));
			} else {
				this.doCreateKey();
			}
		}
	}

	@NotNull
	private String generateAlias() {
		List aliasList = this.getExistingKeyAliasList();
		String prefix = "key";
		if(aliasList == null) {
			return prefix + '0';
		} else {
			HashSet aliasSet = new HashSet();
			Iterator i = aliasList.iterator();

			String alias;
			while(i.hasNext()) {
				alias = (String)i.next();
				aliasSet.add(alias.toLowerCase());
			}

			int var6 = 0;

			while(true) {
				alias = prefix + var6;
				if(!aliasSet.contains(alias)) {
					return alias;
				}

				++var6;
			}
		}
	}

	@Nullable
	protected abstract List<String> getExistingKeyAliasList();

	private static void buildDName(StringBuilder builder, String prefix, JTextField textField) {
		if(textField != null) {
			String value = textField.getText().trim();
			if(value.length() > 0) {
				if(builder.length() > 0) {
					builder.append(",");
				}

				builder.append(prefix);
				builder.append('=');
				builder.append(value);
			}
		}

	}

	private String getDName() {
		StringBuilder builder = new StringBuilder();
		buildDName(builder, "CN", this.myFirstAndLastNameField);
		buildDName(builder, "OU", this.myOrganizationUnitField);
		buildDName(builder, "O", this.myOrganizationField);
		buildDName(builder, "L", this.myCityField);
		buildDName(builder, "ST", this.myStateOrProvinceField);
		buildDName(builder, "C", this.myCountryCodeField);
		return builder.toString();
	}

	private void doCreateKey() throws CommitStepException {
		String keystoreLocation = this.getKeyStoreLocation();
		String keystorePassword = new String(this.getKeyStorePassword());
		String keyPassword = new String(this.getKeyPassword());
		String keyAlias = this.getKeyAlias();
		String dname = this.getDName();

		assert dname != null;

		if(keystorePassword.indexOf(34) < 0 && keyPassword.indexOf(34) < 0) {
			boolean createdStore = false;
			final StringBuilder errorBuilder = new StringBuilder();
			final StringBuilder outBuilder = new StringBuilder();

			try {
				createdStore = KeystoreHelper.createNewStore(keystoreLocation, (String)null,
						keystorePassword, keyAlias, keyPassword, dname, this.getValidity(),
						new DebugKeyProvider.IKeyGenOutput() {
					public void err(String message) {
						errorBuilder.append(message).append('\n');
						NewKeyForm.LOG.info("Error: " + message);
					}

					public void out(String message) {
						outBuilder.append(message).append('\n');
						NewKeyForm.LOG.info(message);
					}
				});
			} catch (Exception var10) {
				LOG.info(var10);
				errorBuilder.append(var10.getMessage()).append('\n');
			}

			normalizeBuilder(errorBuilder);
			normalizeBuilder(outBuilder);
			if(createdStore) {
				if(errorBuilder.length() > 0) {
					String prefix = AndroidBundle.message("android.create.new.key.error.prefix", new Object[0]);
					Messages.showErrorDialog(this.myContentPanel, prefix + '\n' + errorBuilder.toString());
				}

				this.loadKeystoreAndKey(keystoreLocation, keystorePassword, keyAlias, keyPassword);
			} else if(errorBuilder.length() > 0) {
				throw new CommitStepException(errorBuilder.toString());
			} else if(outBuilder.length() > 0) {
				throw new CommitStepException(outBuilder.toString());
			} else {
				throw new CommitStepException(AndroidBundle.message("android.cannot.create.new.key.error", new Object[0]));
			}
		} else {
			throw new CommitStepException("Passwords cannot contain quote character");
		}
	}

	@NotNull
	public char[] getKeyPassword() {
		return this.myKeyPasswordField.getPassword();
	}

	@NotNull
	public String getKeyAlias() {
		return this.myAliasField.getText().trim();
	}

	@NotNull
	protected abstract Project getProject();

	@NotNull
	protected abstract char[] getKeyStorePassword();

	@NotNull
	protected abstract String getKeyStoreLocation();

	private void loadKeystoreAndKey(String keystoreLocation, String keystorePassword, String keyAlias, String keyPassword) throws CommitStepException {
		FileInputStream fis = null;

		try {
			KeyStore e = KeyStore.getInstance(KeyStore.getDefaultType());
			fis = new FileInputStream(new File(keystoreLocation));
			e.load(fis, keystorePassword.toCharArray());
			this.myKeyStore = e;
			KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)e.getEntry(keyAlias, new KeyStore.PasswordProtection(keyPassword.toCharArray()));
			if(entry == null) {
				throw new CommitStepException(AndroidBundle.message("android.extract.package.cannot.find.key.error", new Object[]{keyAlias}));
			}

			PrivateKey privateKey = entry.getPrivateKey();
			Certificate certificate = entry.getCertificate();
			if(privateKey == null || certificate == null) {
				throw new CommitStepException(AndroidBundle.message("android.extract.package.cannot.find.key.error", new Object[]{keyAlias}));
			}

			this.myPrivateKey = privateKey;
			this.myCertificate = (X509Certificate)certificate;
		} catch (Exception var17) {
			throw new CommitStepException("Error: " + var17.getMessage());
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException var16) {
					;
				}
			}

		}

	}

	private static void normalizeBuilder(StringBuilder builder) {
		if(builder.length() > 0) {
			builder.deleteCharAt(builder.length() - 1);
		}
	}

	@Nullable
	public KeyStore getKeyStore() {
		return this.myKeyStore;
	}

	@Nullable
	public PrivateKey getPrivateKey() {
		return this.myPrivateKey;
	}

	@Nullable
	public X509Certificate getCertificate() {
		return this.myCertificate;
	}
}
