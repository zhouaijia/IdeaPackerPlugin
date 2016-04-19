package com.aijia.plugin.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.filters.TextConsoleBuilderImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.NotNull;

/**
 * Created by pc on 2016/4/15.
 */
public class DefaultExternalSystemExecutionConsoleManager
		implements ExternalSystemExecutionConsoleManager<ExternalSystemRunConfiguration> {
	private ProcessHandler myProcessHandler;

	public DefaultExternalSystemExecutionConsoleManager() {
	}

	@NotNull
	public ProjectSystemId getExternalSystemId() {
		ProjectSystemId projectSystemId = ProjectSystemId.IDE;
		if(ProjectSystemId.IDE == null) {
			throw new IllegalStateException(String.format(
					"@NotNull method %s.%s must not return null",
					new Object[]{"com/aijia/plugin/execution/DefaultExternalSystemExecutionConsoleManager",
							"getExternalSystemId"})
			);
		} else {
			return projectSystemId;
		}
	}

	@NotNull
	@Override
	public ExecutionConsole attachExecutionConsole(
			@NotNull ExternalSystemTask task, @NotNull Project project,
			 @NotNull ExternalSystemRunConfiguration configuration, @NotNull Executor executor,
			@NotNull ExecutionEnvironment env,
			@NotNull ProcessHandler processHandler) throws ExecutionException {
		this.myProcessHandler = processHandler;
		ConsoleView executionConsole = (new TextConsoleBuilderImpl(project)).getConsole();
		executionConsole.attachToProcess(processHandler);
		return executionConsole;
	}

	@Override
	public void onOutput(@NotNull String text, @NotNull Key processOutputType) {
		assert this.myProcessHandler != null;
		this.myProcessHandler.notifyTextAvailable(text, processOutputType);
	}

	public boolean isApplicableFor(@NotNull ExternalSystemTask task) {
		return true;
	}

	public AnAction[] getRestartActions() {
		return new AnAction[0];
	}
}
