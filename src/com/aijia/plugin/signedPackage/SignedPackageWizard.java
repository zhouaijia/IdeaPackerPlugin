package com.aijia.plugin.signedPackage;

import com.aijia.plugin.gradle.GradleRunTaskDialog;
import com.aijia.plugin.utils.AndroidBundle;
import com.android.builder.model.Variant;
import com.google.common.base.Joiner;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilderImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.facet.Facet;
import com.intellij.ide.wizard.AbstractWizard;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.action.ExternalSystemActionUtil;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskExecutionInfo;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskPojo;
import com.intellij.openapi.externalSystem.model.project.ExternalConfigPathAware;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager;
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.externalSystem.service.notification.NotificationSource;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.net.NetUtils;
import com.intellij.util.text.DateFormatUtil;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.gradle.cli.CommandLineArgumentException;
import org.gradle.cli.CommandLineParser;
import org.gradle.cli.ParsedCommandLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.cmd.GradleCommandLineOptionsConverter;
import org.jetbrains.plugins.gradle.service.task.ExecuteGradleTaskHistoryService;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by aijia on 2016/4/7.
 */
public class SignedPackageWizard extends AbstractWizard<SignedPackageWizardStep> {
	public static Logger LOG;
	private static final String NOTIFICATION_TITLE = "Generate signed APK";

	private final Project myProject;
	private Facet myFacet;
	private GradleSigningInfo myGradleSigningInfo;
	private PrivateKey myPrivateKey;
	private X509Certificate myCertificate;
	private boolean mySigned;
	private AnActionEvent myAnActionEvent;

	public SignedPackageWizard(Project project, List<Facet> facets, boolean signed,AnActionEvent e) {
		super(AndroidBundle.message("android.export.package.wizard.title", new Object[0]), project);

		myProject = project;
		mySigned = signed;
		myAnActionEvent = e;

		//DOMConfigurator.configure("E:\\IdeaProjects\\IdeaPlugin\\ApkPackerPlugin\\src\\log4j.xml");
		PropertyConfigurator.configure("E:\\IdeaProjects\\IdeaPlugin\\ApkPackerPlugin\\log4j.properties");
		LOG = LogManager.getLogger(SignedPackageWizard.class.getName());
		/*assert facets.size() > 0;

		if(facets.size() <= 1 && !SystemInfo.isMac) {
			this.myFacet = (AndroidFacet)facets.get(0);
		} else {
			this.addStep(new ChooseModuleStep(this, facets));
		}

		boolean useGradleToSign = ((AndroidFacet)facets.get(0)).isGradleProject();
		if(signed) {
			this.addStep(new KeystoreStep(this, useGradleToSign));
		}

		if(useGradleToSign) {
			this.addStep(new GradleSignStep(this));
		} else {
			this.addStep(new ApkStep(this));
		}*/

		this.addStep(new KeyStoreStep(this, true));
		this.init();
	}

	@Override
	protected void doOKAction() {
		if (!commitCurrentStep()) return;
		super.doOKAction();

		/*assert this.myFacet != null;

		if(this.myFacet.isGradleProject()) {
			this.buildAndSignGradleProject();
		} else {
			this.buildAndSignIntellijProject();
		}*/

		openRunTaskDialog(myAnActionEvent);
	}

	private boolean commitCurrentStep() {
		try {
			mySteps.get(myCurrentStep).commitForNext();
		} catch (CommitStepException e) {
			Messages.showErrorDialog(getContentPane(), e.getMessage());
			return false;
		}
		return true;
	}

	private final String COMMAND_KEY_STORE_PATH = "android.injected.signing.store.file";
	private final String COMMAND_KEY_STORE_PASSWORD = "android.injected.signing.store.password";
	private final String COMMAND_KEY_STORE_ALIAS = "android.injected.signing.key.alias";
	private final String COMMAND_KEY_STORE_KEY_PASSWORD = "android.injected.signing.key.password";

	private String createProjectProperty(@NotNull String name, @NotNull String value) {
		return String.format("-P%1$s=%2$s", new Object[]{name, value});
	}

	public void openRunTaskDialog(AnActionEvent e) {
		final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
		ExecuteGradleTaskHistoryService historyService = ExecuteGradleTaskHistoryService.getInstance(project);
		GradleRunTaskDialog dialog = new GradleRunTaskDialog(project, historyService.getHistory());
		String lastWorkingDirectory = historyService.getWorkDirectory();
		if (lastWorkingDirectory.length() == 0) {
			lastWorkingDirectory = obtainAppropriateWorkingDirectory(e);
		}

		dialog.setWorkDirectory(lastWorkingDirectory);

		if (StringUtil.isEmptyOrSpaces(historyService.getCanceledCommand())) {
			if (historyService.getHistory().size() > 0) {
				dialog.setCommandLine(historyService.getHistory().get(0));
			}
		} else {
			dialog.setCommandLine(historyService.getCanceledCommand());
		}

		if (!dialog.showAndGet()) {
			historyService.setCanceledCommand(dialog.getCommandLine());
			return;
		}

		historyService.setCanceledCommand(null);

		String workDirectory = dialog.getWorkDirectory();
		String fullCommandLine = dialog.getCommandLine();
		fullCommandLine = fullCommandLine.trim();
		historyService.addCommand(fullCommandLine, workDirectory);

		try {
			if(fullCommandLine != null && fullCommandLine.contains("batchBuildApk")) {
				String password = new String(myGradleSigningInfo.keyStorePassword);
				String keyPassword = new String(myGradleSigningInfo.keyPassword);
				if (password != null && password.length() > 0
						&& keyPassword != null && keyPassword.length() > 0) {
					String moduleName = fullCommandLine.split("-")[1];
					if(moduleName == null || moduleName.length() <= 0){
						LOG.error("openRunTaskDialog------>module name is null");
						return;
					}
					fullCommandLine = "-Pchannel="+moduleName+"/channels.txt "
							+ createProjectProperty(COMMAND_KEY_STORE_PATH, myGradleSigningInfo.keyStoreFilePath) + " "
							+ createProjectProperty(COMMAND_KEY_STORE_PASSWORD, password) + " "
							+ createProjectProperty(COMMAND_KEY_STORE_ALIAS, myGradleSigningInfo.keyAlias) + " "
							+ createProjectProperty(COMMAND_KEY_STORE_KEY_PASSWORD, keyPassword) + " "
							+ "clean buildApkRelease";
				}
			}
		} catch (Exception ee) {
			ee.printStackTrace();
		}

		final ExternalTaskExecutionInfo taskExecutionInfo;
		try {
			taskExecutionInfo = buildTaskInfo(workDirectory, fullCommandLine);
		} catch (CommandLineArgumentException ex) {
			final NotificationData notificationData = new NotificationData(
					"<b>Command-line arguments cannot be parsed</b>",
					"<i>" + fullCommandLine + "</i> \n" + ex.getMessage(),
					NotificationCategory.WARNING, NotificationSource.TASK_EXECUTION
			);
			notificationData.setBalloonNotification(true);
			ExternalSystemNotificationManager.getInstance(project).showNotification(GradleConstants.SYSTEM_ID, notificationData);
			return;
		}
		/*RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
		ExternalSystemUtil.runTask(taskExecutionInfo.getSettings(), taskExecutionInfo.getExecutorId(), project, GradleConstants.SYSTEM_ID);
		RunnerAndConfigurationSettings configuration =
				ExternalSystemUtil.createExternalSystemRunnerAndConfigurationSettings(taskExecutionInfo.getSettings(),
						project, GradleConstants.SYSTEM_ID);
		if (configuration == null) return;

		final RunnerAndConfigurationSettings existingConfiguration = runManager.findConfigurationByName(configuration.getName());
		if(existingConfiguration == null) {
			runManager.setTemporaryConfiguration(configuration);
		} else {
			runManager.setSelectedConfiguration(existingConfiguration);
		}*/

		try {
			getState(taskExecutionInfo.getSettings()).execute();
		} catch (Exception ee) {
		}
	}

	private static ExternalTaskExecutionInfo buildTaskInfo(@NotNull String projectPath, @NotNull String fullCommandLine)
			throws CommandLineArgumentException {
		CommandLineParser gradleCmdParser = new CommandLineParser();

		GradleCommandLineOptionsConverter commandLineConverter = new GradleCommandLineOptionsConverter();
		commandLineConverter.configure(gradleCmdParser);
		ParsedCommandLine parsedCommandLine = gradleCmdParser.parse(ParametersListUtil.parse(fullCommandLine, true));

		final Map<String, List<String>> optionsMap =
				commandLineConverter.convert(parsedCommandLine, new HashMap<String, List<String>>());

		final List<String> systemProperties = optionsMap.remove("system-prop");
		final String vmOptions = systemProperties == null ? "" : StringUtil.join(systemProperties, new Function<String, String>() {
			@Override
			public String fun(String entry) {
				return "-D" + entry;
			}
		}, " ");

		final String scriptParameters = StringUtil.join(optionsMap.entrySet(), new Function<Map.Entry<String, List<String>>, String>() {
			@Override
			public String fun(Map.Entry<String, List<String>> entry) {
				final List<String> values = entry.getValue();
				final String longOptionName = entry.getKey();
				if (values != null && !values.isEmpty()) {
					return StringUtil.join(values, new Function<String, String>() {
						@Override
						public String fun(String entry) {
							return "--" + longOptionName + ' ' + entry;
						}
					}, " ");
				} else {
					return "--" + longOptionName;
				}
			}
		}, " ");

		final List<String> tasks = parsedCommandLine.getExtraArguments();

		ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
		settings.setExternalProjectPath(projectPath);
		settings.setTaskNames(tasks);
		settings.setScriptParameters(scriptParameters);
		settings.setVmOptions(vmOptions);
		settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.toString());
		return new ExternalTaskExecutionInfo(settings, DefaultRunExecutor.EXECUTOR_ID);
	}

	private static String obtainAppropriateWorkingDirectory(AnActionEvent e) {
		final List<ExternalSystemNode> selectedNodes = ExternalSystemDataKeys.SELECTED_NODES.getData(e.getDataContext());
		if (selectedNodes == null || selectedNodes.size() != 1) {
			final Module module = ExternalSystemActionUtil.getModule(e.getDataContext());
			String projectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
			return projectPath == null ? "" : projectPath;
		}

		final ExternalSystemNode<?> node = selectedNodes.get(0);
		final Object externalData = node.getData();
		if (externalData instanceof ExternalConfigPathAware) {
			return ((ExternalConfigPathAware)externalData).getLinkedExternalProjectPath();
		} else {
			final ExternalConfigPathAware parentExternalConfigPathAware = node.findParentData(ExternalConfigPathAware.class);
			return parentExternalConfigPathAware != null ? parentExternalConfigPathAware.getLinkedExternalProjectPath() : "";
		}
	}

	public boolean isSigned() {
		return this.mySigned;
	}

	public static String getMergedFlavorName(Variant variant) {
		return Joiner.on('-').join(variant.getProductFlavors());
	}

	@Nullable
	@Override
	protected String getHelpID() {
		return null;
	}

	public Project getProject() {
		return this.myProject;
	}

	public void setFacet(@NotNull Facet facet) {
		this.myFacet = facet;
	}

	public void setGradleSigningInfo(GradleSigningInfo gradleSigningInfo) {
		this.myGradleSigningInfo = gradleSigningInfo;
	}

	public void setPrivateKey(@NotNull PrivateKey privateKey) {
		this.myPrivateKey = privateKey;
	}

	public void setCertificate(@NotNull X509Certificate certificate) {
		this.myCertificate = certificate;
	}

	public PrivateKey getPrivateKey() {
		return this.myPrivateKey;
	}

	public X509Certificate getCertificate() {
		return this.myCertificate;
	}

	@Nullable
	public MyRunnableState getState(@NotNull ExternalSystemTaskExecutionSettings settings) throws ExecutionException {
		return new MyRunnableState(settings, myProject);
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

	public static class MyRunnableState {
		@NotNull
		private final ExternalSystemTaskExecutionSettings mySettings;
		@NotNull
		private final Project myProject;
		private final int myDebugPort;

		public MyRunnableState(@NotNull ExternalSystemTaskExecutionSettings settings,@NotNull Project project) {
			this.mySettings = settings;
			this.myProject = project;
			int port;
			try {
				port = NetUtils.findAvailableSocketPort();
			} catch (IOException var8) {
				LOG.warn("Unexpected I/O exception occurred on attempt to find a free port to use for external system task debugging", var8);
				port = 0;
			}

			this.myDebugPort = port;
		}

		public ExecutionConsole attachExecutionConsole(
				@NotNull Project project,
				@NotNull ProcessHandler processHandler) throws ExecutionException {
			ConsoleView executionConsole = (new TextConsoleBuilderImpl(project)).getConsole();
			executionConsole.attachToProcess(processHandler);
			return executionConsole;
		}

		@Nullable
		public ExecutionResult execute() throws ExecutionException {
			if (this.myProject.isDisposed()) return null;

			final List<ExternalTaskPojo> tasks = ContainerUtilRt.newArrayList();
			for (String taskName : mySettings.getTaskNames()) {
				tasks.add(new ExternalTaskPojo(taskName, mySettings.getExternalProjectPath(), null));
			}

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
			final ExecutionConsole consoleView = attachExecutionConsole(myProject, processHandler);
			Disposer.register(myProject, consoleView);

			ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
				@Override
				public void run() {
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

							processHandler.notifyTextAvailable(text, stdOut ? ProcessOutputTypes.STDOUT : ProcessOutputTypes.STDERR);
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
			result.setRestartActions(new AnAction[0]);
			return result;
		}
	}
}
