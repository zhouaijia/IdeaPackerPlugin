package com.aijia.plugin.signedPackage;

import com.intellij.CommonBundle;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Created by pc on 2016/4/7.
 */
public class ChooseKeyDialog extends DialogWrapper {
	private JPanel myNewKeyPanel;
	private JBRadioButton myCreateNewKeyRadioButton;
	private JBRadioButton myUseExistingKeyRadioButton;
	private JComboBox myKeyCombo;
	private JPanel myPanel;
	private final NewKeyForm myNewKeyForm;
	private final Project myProject;
	private final String myKeyStorePath;
	private final char[] myKeyStorePassword;
	private final List<String> myExistingKeys;

	public ChooseKeyDialog(@NotNull Project project, @NotNull String keyStorePath, @NotNull char[] password, @NotNull List<String> existingKeys, @Nullable String keyToSelect) {
		super(project);
		this.myNewKeyForm = new ChooseKeyDialog.MyNewKeyForm();
		this.myProject = project;
		this.myKeyStorePath = keyStorePath;
		this.myKeyStorePassword = password;
		this.myExistingKeys = existingKeys;
		this.myKeyCombo.setModel(new CollectionComboBoxModel(existingKeys, existingKeys.get(0)));
		if(keyToSelect != null && existingKeys.contains(keyToSelect)) {
			this.myKeyCombo.setSelectedItem(keyToSelect);
		}

		this.myNewKeyPanel.add(this.myNewKeyForm.getContentPanel(), "Center");
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UIUtil.setEnabled(ChooseKeyDialog.this.myNewKeyPanel, ChooseKeyDialog.this.myCreateNewKeyRadioButton.isSelected(), true);
			}
		};
		this.myCreateNewKeyRadioButton.addActionListener(listener);
		this.myUseExistingKeyRadioButton.addActionListener(listener);
		boolean useExisting = existingKeys.size() > 0;
		this.myUseExistingKeyRadioButton.setSelected(useExisting);
		this.myCreateNewKeyRadioButton.setSelected(!useExisting);
		UIUtil.setEnabled(this.myNewKeyPanel, !useExisting, true);
		this.setTitle("Choose Key");
		this.init();
	}

	protected JComponent createCenterPanel() {
		return this.myPanel;
	}

	protected void doOKAction() {
		if(this.myCreateNewKeyRadioButton.isSelected()) {
			try {
				this.myNewKeyForm.createKey();
			} catch (CommitStepException var2) {
				Messages.showErrorDialog(this.myPanel, var2.getMessage(), CommonBundle.getErrorTitle());
				return;
			}
		}

		super.doOKAction();
	}

	@Nullable
	public String getChosenKey() {
		return this.myUseExistingKeyRadioButton.isSelected()?(String)this.myKeyCombo.getSelectedItem():this.myNewKeyForm.getKeyAlias();
	}

	@Nullable
	public char[] getChosenKeyPassword() {
		return this.myCreateNewKeyRadioButton.isSelected()?this.myNewKeyForm.getKeyPassword():null;
	}

	private class MyNewKeyForm extends NewKeyForm {
		private MyNewKeyForm() {
		}

		protected List<String> getExistingKeyAliasList() {
			return ChooseKeyDialog.this.myExistingKeys;
		}

		@NotNull
		protected Project getProject() {
			return ChooseKeyDialog.this.myProject;
		}

		@NotNull
		protected char[] getKeyStorePassword() {
			return ChooseKeyDialog.this.myKeyStorePassword;
		}

		@NotNull
		protected String getKeyStoreLocation() {
			return ChooseKeyDialog.this.myKeyStorePath;
		}
	}
}