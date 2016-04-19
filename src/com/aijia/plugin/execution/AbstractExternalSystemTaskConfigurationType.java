package com.aijia.plugin.execution;

import com.aijia.plugin.utils.ExternalSystemUtil;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskPojo;
import com.intellij.openapi.externalSystem.service.ui.DefaultExternalSystemUiAware;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;

/**
 * Created by aijia on 2016/4/15.
 */
public class AbstractExternalSystemTaskConfigurationType implements ConfigurationType {
	@NotNull
	private final ProjectSystemId myExternalSystemId;
	@NotNull
	private final ConfigurationFactory[] myFactories = new ConfigurationFactory[1];

	@NotNull private final NotNullLazyValue<Icon> myIcon = new NotNullLazyValue<Icon>() {
		@NotNull
		@Override
		protected Icon compute() {
			ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
			Icon result = null;
			if (manager instanceof ExternalSystemUiAware) {
				result = ((ExternalSystemUiAware)manager).getProjectIcon();
			}
			return result == null ? DefaultExternalSystemUiAware.INSTANCE.getTaskIcon() : result;
		}
	};

	protected AbstractExternalSystemTaskConfigurationType() {
		myExternalSystemId = GradleConstants.SYSTEM_ID;
		myFactories[0] = new ConfigurationFactory(this) {
			public RunConfiguration createTemplateConfiguration(Project project) {
				return doCreateConfiguration(myExternalSystemId, project, this, "");
			}
		};
	}

	public static AbstractExternalSystemTaskConfigurationType getInstance() {
		return ConfigurationTypeUtil.findConfigurationType(AbstractExternalSystemTaskConfigurationType.class);
	}

	@NotNull
	public ProjectSystemId getExternalSystemId() {
		ProjectSystemId projectSystemId = myExternalSystemId;
		if(myExternalSystemId == null) {
			throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null",
					new Object[]{"com/aijia/plugin/execution/AbstractExternalSystemTaskConfigurationType",
							"getExternalSystemId"}));
		} else {
			return projectSystemId;
		}
	}

	@NotNull
	public ConfigurationFactory getFactory() {
		ConfigurationFactory configurationFactory = myFactories[0];
		if(this.myFactories[0] == null) {
			throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null",
					new Object[]{"com/aijia/plugin/execution/AbstractExternalSystemTaskConfigurationType",
							"getFactory"}));
		} else {
			return configurationFactory;
		}
	}

	@NotNull
	protected ExternalSystemRunConfiguration doCreateConfiguration(
			@NotNull ProjectSystemId externalSystemId,
			@NotNull Project project,
			@NotNull ConfigurationFactory factory,
			@NotNull String name) {
		return new ExternalSystemRunConfiguration(externalSystemId, project, factory, name);
	}

	@Override
	public String getDisplayName() {
		return myExternalSystemId.getReadableName();
	}

	@Override
	public String getConfigurationTypeDescription() {
		return ExternalSystemBundle.message("run.configuration.description",
				myExternalSystemId.getReadableName());
	}

	@Override
	public Icon getIcon() {
		return myIcon.getValue();
	}

	@NotNull
	@Override
	public String getId() {
		return myExternalSystemId.getReadableName() + "RunConfiguration";
	}

	@Override
	public ConfigurationFactory[] getConfigurationFactories() {
		return myFactories;
	}

	@NotNull
	public static String generateName(@NotNull Project project, @NotNull ExternalSystemTaskExecutionSettings settings) {
		return generateName(project, settings.getExternalSystemId(), settings.getExternalProjectPath(), settings.getTaskNames(), settings.getExecutionName());
	}

	@NotNull
	public static String generateName(@NotNull Project project, @NotNull ExternalTaskPojo task, @NotNull ProjectSystemId externalSystemId) {
		return generateName(project, externalSystemId, task.getLinkedExternalProjectPath(), Collections.singletonList(task.getName()));
	}

	@NotNull
	public static String generateName(@NotNull Project project, @NotNull ProjectSystemId externalSystemId, @Nullable String externalProjectPath, @NotNull List<String> taskNames) {
		return generateName(project, externalSystemId, externalProjectPath, taskNames, (String)null);
	}

	@NotNull
	public static String generateName(@NotNull Project project, @NotNull ProjectSystemId externalSystemId, @Nullable String externalProjectPath, @NotNull List<String> taskNames, @Nullable String executionName) {
		String rootProjectPath = null;
		if(externalProjectPath != null) {
			ExternalProjectInfo buffer = ExternalSystemUtil.getExternalProjectInfo(project, externalSystemId, externalProjectPath);
			if(buffer != null) {
				rootProjectPath = buffer.getExternalProjectPath();
			}
		}

		StringBuilder buffer = new StringBuilder();
		String projectName;
		if(rootProjectPath == null) {
			projectName = null;
		} else {
			projectName = ExternalSystemApiUtil.getProjectRepresentationName(externalProjectPath, rootProjectPath);
		}

		if(!StringUtil.isEmptyOrSpaces(projectName)) {
			buffer.append(projectName);
			buffer.append(' ');
		} else {
			buffer.append(externalProjectPath);
			buffer.append(' ');
		}

		buffer.append('[');
		if(!StringUtil.isEmpty(executionName)) {
			buffer.append(executionName);
		} else if(!taskNames.isEmpty()) {
			Iterator var8 = taskNames.iterator();

			while(var8.hasNext()) {
				String taskName = (String)var8.next();
				buffer.append(taskName).append(' ');
			}

			buffer.setLength(buffer.length() - 1);
		}

		buffer.append(']');
		return buffer.toString();
	}
}
