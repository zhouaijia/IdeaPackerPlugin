package com.aijia.plugin.signedPackage;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Created by pc on 2016/4/7.
 */
public interface ApkSigningSettingsForm {
	JButton getLoadKeyStoreButton();

	JTextField getKeyStorePathField();

	JPanel getPanel();

	JButton getCreateKeyStoreButton();

	JPasswordField getKeyStorePasswordField();

	TextFieldWithBrowseButton getKeyAliasField();

	JPasswordField getKeyPasswordField();
}