package com.aijia.plugin.signedPackage;

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;

import org.jetbrains.android.facet.AndroidFacet;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Created by pc on 2016/4/7.
 */
public class CheckModulePanel extends JPanel {
	private boolean myHasError;
	private boolean myHasWarnings;

	public CheckModulePanel() {
		super(new VerticalFlowLayout(0));
	}

	public void updateMessages(AndroidFacet facet) {
		this.clearMessages();
		this.revalidate();
	}

	public boolean hasError() {
		return this.myHasError;
	}

	public boolean hasWarnings() {
		return this.myHasWarnings;
	}

	public void clearMessages() {
		this.removeAll();
		this.myHasError = false;
		this.myHasWarnings = false;
	}

	public void addError(String message) {
		JLabel label = new JLabel();
		label.setIcon(Messages.getErrorIcon());
		label.setText("<html><body><b>Error: " + message + "</b></body></html>");
		this.add(label);
		this.myHasError = true;
	}

	public void addWarning(String message) {
		JLabel label = new JLabel();
		label.setIcon(Messages.getWarningIcon());
		label.setText("<html><body><b>Warning: " + message + "</b></body></html>");
		this.add(label);
		this.myHasWarnings = true;
	}
}
