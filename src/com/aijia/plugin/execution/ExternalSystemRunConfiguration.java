package com.aijia.plugin.execution;

import com.aijia.plugin.utils.AndroidBundle;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskPojo;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.net.NetUtils;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.xmlb.XmlSerializer;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by aijia on 2016/4/15.
 */
public class ExternalSystemRunConfiguration extends LocatableConfigurationBase {
	private static final Logger LOG = Logger.getInstance("#" + ExternalSystemRunConfiguration.class.getName());
	private ExternalSystemTaskExecutionSettings mySettings = new ExternalSystemTaskExecutionSettings();

	public ExternalSystemRunConfiguration(@NotNull ProjectSystemId externalSystemId,
	                                      Project project,
	                                      ConfigurationFactory factory,
	                                      String name) {
		super(project, factory, name);
		mySettings.setExternalSystemIdString(externalSystemId.getId());
	}

	public String suggestedName() {
		return AbstractExternalSystemTaskConfigurationType.generateName(getProject(), mySettings);
	}

	public RunConfiguration clone() {
		ExternalSystemRunConfiguration result = (ExternalSystemRunConfiguration)super.clone();
		result.mySettings = mySettings.clone();
		return result;
	}

	public void readExternal(Element element) throws InvalidDataException {
		super.readExternal(element);
		Element e = element.getChild("ExternalSystemSettings");
		if(e != null) {
			mySettings = XmlSerializer.deserialize(e, ExternalSystemTaskExecutionSettings.class);
		}

	}

	public void writeExternal(Element element) throws WriteExternalException {
		super.writeExternal(element);
		element.addContent(XmlSerializer.serialize(this.mySettings));
	}

	@NotNull
	public ExternalSystemTaskExecutionSettings getSettings() {
		ExternalSystemTaskExecutionSettings var10000 = this.mySettings;
		if(this.mySettings == null) {
			throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", new Object[]{"com/intellij/openapi/externalSystem/service/execution/ExternalSystemRunConfiguration", "getSettings"}));
		} else {
			return var10000;
		}
	}

	@NotNull
	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
		return new ExternalSystemRunConfigurationEditor(this.getProject(), this.mySettings.getExternalSystemId());
	}

	@Nullable
	public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
		return new ExternalSystemRunConfiguration.MyRunnableState(this.mySettings, this.getProject(), DefaultDebugExecutor.EXECUTOR_ID.equals(executor.getId()), this, env);
	}

	@NotNull
	private static ExternalSystemExecutionConsoleManager<ExternalSystemRunConfiguration> getConsoleManagerFor(@NotNull ExternalSystemTask task) {
		ExternalSystemExecutionConsoleManager[] var1 = ExternalSystemExecutionConsoleManager.EP_NAME.getExtensions();
		int var2 = var1.length;

		for(int var3 = 0; var3 < var2; ++var3) {
			ExternalSystemExecutionConsoleManager executionConsoleManager = var1[var3];
			if(executionConsoleManager.isApplicableFor(task)) {
				return executionConsoleManager;
			}
		}

		return new DefaultExternalSystemExecutionConsoleManager();
	}

	private static class MyProcessHandler extends ProcessHandler {
		private final ExternalSystemExecuteTaskTask myTask;

		public MyProcessHandler(ExternalSystemExecuteTaskTask task) {
			this.myTask = task;
		}

		protected void destroyProcessImpl() {
		}

		protected void detachProcessImpl() {
			this.myTask.cancel(new ExternalSystemTaskNotificationListener[0]);
			this.notifyProcessDetached();
		}

		public boolean detachIsDefault() {
			return true;
		}

		@Nullable
		public OutputStream getProcessInput() {
			return null;
		}

		public void notifyProcessTerminated(int exitCode) {
			super.notifyProcessTerminated(exitCode);
		}
	}

	public static class MyRunnableState implements RunProfileState {
		@NotNull
		private final ExternalSystemTaskExecutionSettings mySettings;
		@NotNull
		private final Project myProject;
		@NotNull
		private final ExternalSystemRunConfiguration myConfiguration;
		@NotNull
		private final ExecutionEnvironment myEnv;
		private final int myDebugPort;

		public MyRunnableState(@NotNull ExternalSystemTaskExecutionSettings settings, @NotNull Project project, boolean debug, @NotNull ExternalSystemRunConfiguration configuration, @NotNull ExecutionEnvironment env) {
			this.mySettings = settings;
			this.myProject = project;
			this.myConfiguration = configuration;
			this.myEnv = env;
			int port;
			if(debug) {
				try {
					port = NetUtils.findAvailableSocketPort();
				} catch (IOException var8) {
					ExternalSystemRunConfiguration.LOG.warn("Unexpected I/O exception occurred on attempt to find a free port to use for external system task debugging", var8);
					port = 0;
				}
			} else {
				port = 0;
			}

			this.myDebugPort = port;
		}

		public int getDebugPort() {
			return this.myDebugPort;
		}

		@Nullable
		public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
			System.out.println("ExternalSystemRunConfiguration--->execute-->1111111");
			if (this.myProject.isDisposed()) return null;

			final List<ExternalTaskPojo> tasks = ContainerUtilRt.newArrayList();
			for (String taskName : mySettings.getTaskNames()) {
				tasks.add(new ExternalTaskPojo(taskName, mySettings.getExternalProjectPath(), null));
			}

			System.out.println("ExternalSystemRunConfiguration--->execute-->222222");

			if (tasks.isEmpty()) {
				throw new ExecutionException(AndroidBundle.message("run.error.undefined.task"));
			}

			String debuggerSetup = null;
			if (myDebugPort > 0) {
				debuggerSetup = "-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=" + myDebugPort;
			}

			ApplicationManager.getApplication().assertIsDispatchThread();
			FileDocumentManager.getInstance().saveAllDocuments();

			final ExternalSystemExecuteTaskTask task = new ExternalSystemExecuteTaskTask(
					mySettings.getExternalSystemId(),
					myProject,
					tasks,
					mySettings.getVmOptions(),
					mySettings.getScriptParameters(),
					debuggerSetup);

			final MyProcessHandler processHandler = new MyProcessHandler(task);
			final ExternalSystemExecutionConsoleManager consoleManager =
					ExternalSystemRunConfiguration.getConsoleManagerFor(task);
			final ExecutionConsole consoleView =
					consoleManager.attachExecutionConsole(task, myProject, myConfiguration, executor, myEnv, processHandler);
			Disposer.register(myProject, consoleView);

			System.out.println("ExternalSystemRunConfiguration--->execute-->3333333");

			ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
				@Override
				public void run() {
					System.out.println("ExternalSystemRunConfiguration--->execute-->444444");
					final String startDateTime = DateFormatUtil.formatTimeWithSeconds(System.currentTimeMillis());
					final String greeting;
					if (mySettings.getTaskNames().size() > 1) {
						greeting = AndroidBundle.message("run.text.starting.multiple.task", startDateTime, mySettings.toString());
					} else {
						greeting = AndroidBundle.message("run.text.starting.single.task", startDateTime, mySettings.toString());
					}
					processHandler.notifyTextAvailable(greeting, ProcessOutputTypes.SYSTEM);
					task.execute(new ExternalSystemTaskNotificationListenerAdapter() {
						private boolean myResetGreeting = true;

						@Override
						public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
							if (myResetGreeting) {
								processHandler.notifyTextAvailable("\r", ProcessOutputTypes.SYSTEM);
								myResetGreeting = false;
							}

							consoleManager.onOutput(text, stdOut ? ProcessOutputTypes.STDOUT : ProcessOutputTypes.STDERR);
						}

						@Override
						public void onFailure(@NotNull ExternalSystemTaskId id, @NotNull Exception e) {
							String exceptionMessage = ExceptionUtil.getMessage(e);
							String text = exceptionMessage == null ? e.toString() : exceptionMessage;
							processHandler.notifyTextAvailable(text + '\n', ProcessOutputTypes.STDERR);
							processHandler.notifyProcessTerminated(1);
						}

						@Override
						public void onEnd(@NotNull ExternalSystemTaskId id) {
							final String endDateTime = DateFormatUtil.formatTimeWithSeconds(System.currentTimeMillis());
							final String farewell;
							if (mySettings.getTaskNames().size() > 1) {
								farewell = AndroidBundle.message("run.text.ended.multiple.task", endDateTime, mySettings.toString());
							} else {
								farewell = AndroidBundle.message("run.text.ended.single.task", endDateTime, mySettings.toString());
							}
							processHandler.notifyTextAvailable(farewell, ProcessOutputTypes.SYSTEM);
							processHandler.notifyProcessTerminated(0);
						}
					});
				}
			});

			DefaultExecutionResult result = new DefaultExecutionResult(consoleView, processHandler);
			result.setRestartActions(consoleManager.getRestartActions());
			return result;
		}
	}
}