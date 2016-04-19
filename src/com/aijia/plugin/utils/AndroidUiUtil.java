package com.aijia.plugin.utils;

import com.aijia.plugin.signedPackage.ApkSigningSettingsForm;
import com.aijia.plugin.signedPackage.ChooseKeyDialog;
import com.aijia.plugin.signedPackage.NewKeyStoreDialog;
import com.intellij.CommonBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;

/**
 * Created by pc on 2016/4/7.
 */
public class AndroidUiUtil {
	private static final Logger LOG = Logger.getInstance("org.jetbrains.android.util.AndroidUiUtil");

	private AndroidUiUtil() {
	}

	@Nullable
	private static List<String> loadExistingKeys(@NotNull ApkSigningSettingsForm form) {
		String errorPrefix = "Cannot load key store: ";
		FileInputStream is = null;

		List var4;
		try {
			is = new FileInputStream(new File(form.getKeyStorePathField().getText().trim()));
			KeyStore e = KeyStore.getInstance(KeyStore.getDefaultType());
			e.load(is, form.getKeyStorePasswordField().getPassword());
			var4 = PluginUtils.toList(e.aliases());
			return var4;
		} catch (KeyStoreException var22) {
			Messages.showErrorDialog(form.getPanel(), "Cannot load key store: " + var22.getMessage(), CommonBundle.getErrorTitle());
			var4 = null;
			return var4;
		} catch (FileNotFoundException var23) {
			Messages.showErrorDialog(form.getPanel(), "Cannot load key store: " + var23.getMessage(), CommonBundle.getErrorTitle());
			var4 = null;
		} catch (CertificateException var24) {
			Messages.showErrorDialog(form.getPanel(), "Cannot load key store: " + var24.getMessage(), CommonBundle.getErrorTitle());
			var4 = null;
			return var4;
		} catch (NoSuchAlgorithmException var25) {
			Messages.showErrorDialog(form.getPanel(), "Cannot load key store: " + var25.getMessage(), CommonBundle.getErrorTitle());
			var4 = null;
			return var4;
		} catch (IOException var26) {
			Messages.showErrorDialog(form.getPanel(), "Cannot load key store: " + var26.getMessage(), CommonBundle.getErrorTitle());
			var4 = null;
			return var4;
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException var21) {
					LOG.info(var21);
				}
			}

		}

		return var4;
	}

	public static void initSigningSettingsForm(@NotNull final Project project, @NotNull final ApkSigningSettingsForm form) {
		form.getLoadKeyStoreButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String defaultPath = form.getKeyStorePathField().getText().trim();
				VirtualFile defaultFile = LocalFileSystem.getInstance().findFileByPath(defaultPath);
				FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
				VirtualFile file = FileChooser.chooseFile(descriptor, form.getPanel(), project, defaultFile);
				if(file != null) {
					form.getKeyStorePathField().setText(FileUtil.toSystemDependentName(file.getPath()));
				}

			}
		});
		form.getCreateKeyStoreButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NewKeyStoreDialog dialog = new NewKeyStoreDialog(project, form.getKeyStorePathField().getText());
				dialog.show();
				if(dialog.getExitCode() == 0) {
					form.getKeyStorePathField().setText(dialog.getKeyStorePath());
					form.getKeyStorePasswordField().setText(String.valueOf(dialog.getKeyStorePassword()));
					form.getKeyAliasField().setText(dialog.getKeyAlias());
					form.getKeyPasswordField().setText(String.valueOf(dialog.getKeyPassword()));
				}

			}
		});
		form.getKeyAliasField().getButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List keys = AndroidUiUtil.loadExistingKeys(form);
				if(keys != null) {
					ChooseKeyDialog dialog = new ChooseKeyDialog(project, form.getKeyStorePathField().getText().trim(), form.getKeyStorePasswordField().getPassword(), keys, form.getKeyAliasField().getText().trim());
					dialog.show();
					if(dialog.getExitCode() == 0) {
						String chosenKey = dialog.getChosenKey();
						if(chosenKey != null) {
							form.getKeyAliasField().setText(chosenKey);
						}

						char[] password = dialog.getChosenKeyPassword();
						if(password != null) {
							form.getKeyPasswordField().setText(String.valueOf(password));
						}
					}

				}
			}
		});
	}
}
