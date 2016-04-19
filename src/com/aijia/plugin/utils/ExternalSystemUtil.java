package com.aijia.plugin.utils;

import com.aijia.plugin.execution.AbstractExternalSystemTaskConfigurationType;
import com.aijia.plugin.execution.ExternalSystemRunConfiguration;
import com.intellij.execution.ExecutionAdapter;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.RunnerRegistry;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.task.TaskCallback;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Created by aijia on 2016/4/15.
 */
public class ExternalSystemUtil {
	private static final Logger LOG = Logger.getInstance("#" + ExternalSystemUtil.class.getName());
	@NotNull
	private static final Map<String, String> RUNNER_IDS = ContainerUtilRt.newHashMap();
	static {
		RUNNER_IDS.put(DefaultRunExecutor.EXECUTOR_ID, "ExternalSystemTaskRunner");
		RUNNER_IDS.put(DefaultDebugExecutor.EXECUTOR_ID, "ExternalSystemTaskDebugRunner");
	}

	@Nullable
	public static String getRunnerId(@NotNull String executorId) {
		return (String)RUNNER_IDS.get(executorId);
	}

	@Nullable
	public static ExternalProjectInfo getExternalProjectInfo(@NotNull Project project, @NotNull ProjectSystemId projectSystemId, @NotNull String externalProjectPath) {
		ExternalProjectSettings linkedProjectSettings = ExternalSystemApiUtil.getSettings(project, projectSystemId).getLinkedProjectSettings(externalProjectPath);
		return linkedProjectSettings == null?null: ProjectDataManager.getInstance().getExternalProjectData(project, projectSystemId, linkedProjectSettings.getExternalProjectPath());
	}

	@Nullable
	public static AbstractExternalSystemTaskConfigurationType findConfigurationType (@NotNull ProjectSystemId externalSystemId) {
		ConfigurationType[] configurationTypes = Extensions.getExtensions(ConfigurationType.CONFIGURATION_TYPE_EP);

		System.out.println("findConfigurationType-----1111---->"+configurationTypes.length);

		for(int i = 0; i < configurationTypes.length; ++i) {
			ConfigurationType type = configurationTypes[i];
			System.out.println("findConfigurationType-----2222---->"+type);
			if(type instanceof AbstractExternalSystemTaskConfigurationType) {
				System.out.println("findConfigurationType-----3333---->"+type);
				AbstractExternalSystemTaskConfigurationType candidate = (AbstractExternalSystemTaskConfigurationType)type;
				if(externalSystemId.equals(candidate.getExternalSystemId())) {
					System.out.println("findConfigurationType-----444444---->");
					return candidate;
				}
			}
		}

		System.out.println("findConfigurationType-----555555---->");

		return null;
	}

	@Nullable
	public static RunnerAndConfigurationSettings createExternalSystemRunnerAndConfigurationSettings(
			@NotNull ExternalSystemTaskExecutionSettings taskSettings, @NotNull Project project,
			@NotNull ProjectSystemId externalSystemId) {
		AbstractExternalSystemTaskConfigurationType configurationType = findConfigurationType(externalSystemId);

		System.out.println("create settings-----1111---->");
		if (configurationType == null) return null;

		System.out.println("create settings-----22222---->");
		String name = AbstractExternalSystemTaskConfigurationType.generateName(project, taskSettings);
		RunnerAndConfigurationSettings settings = RunManager.getInstance(project).createRunConfiguration(name, configurationType.getFactory());
		ExternalSystemRunConfiguration runConfiguration = (ExternalSystemRunConfiguration) settings.getConfiguration();
		runConfiguration.getSettings().setExternalProjectPath(taskSettings.getExternalProjectPath());
		runConfiguration.getSettings().setTaskNames(ContainerUtil.newArrayList(taskSettings.getTaskNames()));
		runConfiguration.getSettings().setTaskDescriptions(ContainerUtil.newArrayList(taskSettings.getTaskDescriptions()));
		runConfiguration.getSettings().setVmOptions(taskSettings.getVmOptions());
		runConfiguration.getSettings().setScriptParameters(taskSettings.getScriptParameters());
		runConfiguration.getSettings().setExecutionName(taskSettings.getExecutionName());
		return settings;
	}

	@Nullable
	public static Pair<ProgramRunner, ExecutionEnvironment> createRunner(
			@NotNull ExternalSystemTaskExecutionSettings taskSettings,
			@NotNull String executorId, @NotNull Project project,
			@NotNull ProjectSystemId externalSystemId) {
		System.out.println("createRunner-----11111----->");
		Executor executor = ExecutorRegistry.getInstance().getExecutorById(executorId);
		if (executor == null) {
			return null;
		}
		System.out.println("createRunner-----22222----->");
		String runnerId = getRunnerId(executorId);
		if (runnerId == null) {
			return null;
		}
		System.out.println("createRunner-----33333----->");
		ProgramRunner runner = RunnerRegistry.getInstance().findRunnerById(runnerId);
		if (runner == null) {
			return null;
		}
		System.out.println("createRunner-----44444----->");
		RunnerAndConfigurationSettings settings = createExternalSystemRunnerAndConfigurationSettings(taskSettings, project, externalSystemId);
		System.out.println("createRunner-----55555----->"+settings);
		return settings == null ? null : Pair.create(runner, new ExecutionEnvironment(executor, runner, settings, project));
	}

	public static void runTask(@NotNull ExternalSystemTaskExecutionSettings taskSettings,
	                           @NotNull String executorId, @NotNull Project project,
	                           @NotNull ProjectSystemId externalSystemId) {
		runTask(taskSettings, executorId, project, externalSystemId, null,
				ProgressExecutionMode.IN_BACKGROUND_ASYNC);
	}

	public static void runTask(@NotNull final ExternalSystemTaskExecutionSettings taskSettings,
	                           @NotNull final String executorId, @NotNull final Project project,
	                           @NotNull ProjectSystemId externalSystemId,
	                           @Nullable final TaskCallback callback,
	                           @NotNull final ProgressExecutionMode progressExecutionMode) {
		System.out.println("runTask----111111----->");

		Pair pair = createRunner(taskSettings, executorId, project, externalSystemId);
		if(pair != null) {
			System.out.println("runTask----22222----->");
			final ProgramRunner runner = (ProgramRunner)pair.first;
			final ExecutionEnvironment environment = (ExecutionEnvironment)pair.second;
			final ExternalSystemUtil.TaskUnderProgress task = new ExternalSystemUtil.TaskUnderProgress() {
				public void execute(@NotNull ProgressIndicator indicator) {
					System.out.println("runTask----33333----->");
					final Semaphore targetDone = new Semaphore();
					final Ref result = new Ref(Boolean.valueOf(false));
					Disposable disposable = Disposer.newDisposable();
					project.getMessageBus().connect(disposable).subscribe(
							ExecutionManager.EXECUTION_TOPIC,
							new ExecutionAdapter() {
						public void processStartScheduled(String executorIdLocal, ExecutionEnvironment environmentLocal) {
							System.out.println("runTask----4444----->");
							if(executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
								targetDone.down();
							}
						}

						public void processNotStarted(String executorIdLocal, @NotNull ExecutionEnvironment environmentLocal) {
							System.out.println("runTask----55555----->");
							if(executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
								targetDone.up();
							}
						}

						public void processStarted(String executorIdLocal,
						                           @NotNull ExecutionEnvironment environmentLocal,
						                           @NotNull ProcessHandler handler) {
							System.out.println("runTask----666666----->");
							if(executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
								handler.addProcessListener(new ProcessAdapter() {
									public void processTerminated(ProcessEvent event) {
										System.out.println("runTask----7777777----->");
										result.set(Boolean.valueOf(event.getExitCode() == 0));
										targetDone.up();
									}
								});
							}
						}
					});

					try {
						System.out.println("runTask----88888----->");
						ApplicationManager.getApplication().invokeAndWait(new Runnable() {
							public void run() {
								System.out.println("runTask----99999----->");
								try {
									runner.execute(environment);
								} catch (ExecutionException var2) {
									targetDone.up();
									ExternalSystemUtil.LOG.error(var2);
								}

							}
						}, ModalityState.NON_MODAL);
					} catch (Exception var6) {
						ExternalSystemUtil.LOG.error(var6);
						Disposer.dispose(disposable);
						return;
					}

					targetDone.waitFor();
					Disposer.dispose(disposable);
					if(callback != null) {
						System.out.println("runTask----1010101----->");
						if(((Boolean)result.get()).booleanValue()) {
							callback.onSuccess();
						} else {
							callback.onFailure();
						}
					}
				}
			};
			UIUtil.invokeAndWaitIfNeeded(new Runnable() {
				public void run() {
					System.out.println("runTask----1212121121----->"+progressExecutionMode.ordinal());
					final String title = AbstractExternalSystemTaskConfigurationType.generateName(project, taskSettings);

					switch(progressExecutionMode.ordinal()) {
						case 1:
							(new Task.Modal(project, title, true) {
								public void run(@NotNull ProgressIndicator indicator) {
									task.execute(indicator);
								}
							}).queue();
							break;
						case 2:
							(new Task.Backgroundable(project, title) {
								public void run(@NotNull ProgressIndicator indicator) {
									task.execute(indicator);
								}
							}).queue();
							break;
						case 3:
							(new Task.Backgroundable(project, title, true, PerformInBackgroundOption.DEAF) {
								public void run(@NotNull ProgressIndicator indicator) {
									task.execute(indicator);
								}
							}).queue();
					}
				}
			});
		}
	}

	private interface TaskUnderProgress {
		void execute(@NotNull ProgressIndicator var1);
	}

}
