package com.aijia.plugin.signedPackage;

import com.intellij.ide.wizard.CommitStepException;
import com.intellij.ide.wizard.StepAdapter;

import javax.swing.Icon;

/**
 * Created by pc on 2016/4/7.
 */
public abstract class SignedPackageWizardStep extends StepAdapter {
	private int previousStepIndex = -1;

	public SignedPackageWizardStep() {
	}

	public void setPreviousStepIndex(int previousStepIndex) {
		this.previousStepIndex = previousStepIndex;
	}

	public int getPreviousStepIndex() {
		return this.previousStepIndex;
	}

	protected boolean canFinish() {
		return false;
	}

	public abstract String getHelpId();

	protected abstract void commitForNext() throws CommitStepException;

	public Icon getIcon() {
		return null;
	}
}