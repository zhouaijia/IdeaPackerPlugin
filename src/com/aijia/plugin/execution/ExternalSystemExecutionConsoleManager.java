package com.aijia.plugin.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.NotNull;

/**
 * Created by aijia on 2016/4/15.
 */
public interface ExternalSystemExecutionConsoleManager<ExternalSystemRunConfiguration extends LocatableConfigurationBase> {
	ExtensionPointName<ExternalSystemExecutionConsoleManager> EP_NAME = ExtensionPointName.create(
			"com.aijia.externalSystemExecutionConsoleManager");

	@NotNull
	ProjectSystemId getExternalSystemId();

	@NotNull
	ExecutionConsole attachExecutionConsole(
			@NotNull ExternalSystemTask task, @NotNull Project project,
			@NotNull ExternalSystemRunConfiguration configuration, @NotNull Executor executor,
			@NotNull ExecutionEnvironment executionEnvironment,
			@NotNull ProcessHandler processHandler) throws ExecutionException;

	void onOutput(@NotNull String text, @NotNull Key processOutputType);

	boolean isApplicableFor(@NotNull ExternalSystemTask externalSystemTask);

	AnAction[] getRestartActions();
}
