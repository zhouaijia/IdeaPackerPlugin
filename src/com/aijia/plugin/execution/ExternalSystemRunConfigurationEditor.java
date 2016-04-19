package com.aijia.plugin.execution;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.execution.*;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.awt.GridBagLayout;

import javax.swing.JComponent;

/**
 * Created by pc on 2016/4/18.
 */
public class ExternalSystemRunConfigurationEditor extends SettingsEditor<ExternalSystemRunConfiguration> {
	@NotNull
	private final ExternalSystemTaskSettingsControl myControl;

	public ExternalSystemRunConfigurationEditor(@NotNull Project project, @NotNull ProjectSystemId externalSystemId) {
		this.myControl = new ExternalSystemTaskSettingsControl(project, externalSystemId);
	}

	protected void resetEditorFrom(ExternalSystemRunConfiguration s) {
		this.myControl.setOriginalSettings(s.getSettings());
		this.myControl.reset();
	}

	protected void applyEditorTo(ExternalSystemRunConfiguration s) throws ConfigurationException {
		this.myControl.apply(s.getSettings());
	}

	@NotNull
	protected JComponent createEditor() {
		PaintAwarePanel result = new PaintAwarePanel(new GridBagLayout());
		this.myControl.fillUi(result, 0);
		return result;
	}

	protected void disposeEditor() {
		this.myControl.disposeUIResources();
	}
}
