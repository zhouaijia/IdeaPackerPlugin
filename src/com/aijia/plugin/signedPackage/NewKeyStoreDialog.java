package com.aijia.plugin.signedPackage;

import com.aijia.plugin.utils.AndroidBundle;
import com.aijia.plugin.utils.PluginUtils;
import com.aijia.plugin.utils.SaveFileListener;
import com.intellij.CommonBundle;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 * Created by pc on 2016/4/7.
 */
public class NewKeyStoreDialog extends DialogWrapper {
	private static final Logger LOG = Logger.getInstance("#com.aijia.plugin.signedPackage.NewKeyStoreDialog");
	private JPanel myNewKeyPanel;
	private JPanel myPanel;
	private TextFieldWithBrowseButton myKeyStorePathField;
	private JPasswordField myPasswordField;
	private JPasswordField myConfirmedPassword;
	private NewKeyForm myNewKeyForm;
	private Project myProject;

	public NewKeyStoreDialog(@NotNull Project project, @NotNull String defaultKeyStorePath) {
		super(project);
		this.myProject = project;
		this.myKeyStorePathField.setText(defaultKeyStorePath);
		this.myNewKeyForm = new MyNewKeyForm();

		//LOG.error("NewKeyStoreDialog------->myNewKeyPanel: "+myNewKeyPanel);

		this.myNewKeyPanel.add(myNewKeyForm.getContentPanel(), "Center");
		this.myKeyStorePathField.addActionListener(new SaveFileListener(this.myPanel, this.myKeyStorePathField, AndroidBundle.message("android.extract.package.choose.keystore.title", new Object[0]), "jks") {
			protected String getDefaultLocation() {
				return NewKeyStoreDialog.this.getKeyStorePath();
			}
		});
		this.setTitle("New Key Store");
		this.init();
	}

	protected JComponent createCenterPanel() {
		return this.myPanel;
	}

	public JComponent getPreferredFocusedComponent() {
		return this.myKeyStorePathField;
	}

	protected void doOKAction() {
		if(this.getKeyStorePath().length() == 0) {
			Messages.showErrorDialog(this.myPanel, "Specify key store path", CommonBundle.getErrorTitle());
		} else {
			try {
				PluginUtils.checkNewPassword(this.myPasswordField, this.myConfirmedPassword);
				this.myNewKeyForm.createKey();
			} catch (CommitStepException var2) {
				Messages.showErrorDialog(this.myPanel, var2.getMessage(), CommonBundle.getErrorTitle());
				return;
			}

			super.doOKAction();
		}
	}

	@NotNull
	public String getKeyStorePath() {
		return this.myKeyStorePathField.getText().trim();
	}

	@NotNull
	public char[] getKeyStorePassword() {
		return this.myPasswordField.getPassword();
	}

	@NotNull
	public String getKeyAlias() {
		return this.myNewKeyForm.getKeyAlias();
	}

	@NotNull
	public char[] getKeyPassword() {
		return this.myNewKeyForm.getKeyPassword();
	}

	private class MyNewKeyForm extends NewKeyForm {
		private MyNewKeyForm() {
		}

		protected List<String> getExistingKeyAliasList() {
			return Collections.emptyList();
		}

		@NotNull
		protected Project getProject() {
			return NewKeyStoreDialog.this.myProject;
		}

		@NotNull
		protected char[] getKeyStorePassword() {
			return NewKeyStoreDialog.this.getKeyStorePassword();
		}

		@NotNull
		protected String getKeyStoreLocation() {
			return NewKeyStoreDialog.this.getKeyStorePath();
		}
	}
}
