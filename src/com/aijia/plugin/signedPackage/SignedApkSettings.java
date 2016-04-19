package com.aijia.plugin.signedPackage;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;

/**
 * Created by pc on 2016/4/7.
 */
@State(
		name = "SignedApkSettings",
		storages = {
				@Storage(file = StoragePathMacros.WORKSPACE_FILE)
		}
)
public class SignedApkSettings implements PersistentStateComponent<SignedApkSettings> {
	public String KEY_STORE_PATH = "";
	public String KEY_ALIAS = "";
	public boolean REMEMBER_PASSWORDS = false;

	public SignedApkSettings() {
	}

	public SignedApkSettings getState() {
		return this;
	}

	public void loadState(SignedApkSettings state) {
		XmlSerializerUtil.copyBean(state, this);
	}

	public static SignedApkSettings getInstance(Project project) {
		return ServiceManager.getService(project, SignedApkSettings.class);
	}
}
