package com.aijia.plugin.utils;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPasswordField;

/**
 * Created by pc on 2016/4/7.
 */
public class PluginUtils {
	@NotNull
	public static List<Facet> getApplicationFacets(@NotNull Project project) {
		ArrayList result = new ArrayList();
		Iterator var2 = ProjectFacetManager.getInstance(project).getFacets(new FacetTypeId("android")).iterator();

		while(var2.hasNext()) {
			Facet facet = (Facet)var2.next();
			//if(!facet.isLibraryProject()) {
				result.add(facet);
			//}
		}

		return result;
	}

	public static void checkNewPassword(JPasswordField passwordField, JPasswordField confirmedPasswordField) throws CommitStepException {
		char[] password = passwordField.getPassword();
		char[] confirmedPassword = confirmedPasswordField.getPassword();

		try {
			checkPassword(password);
			if(password.length < 6) {
				throw new CommitStepException(AndroidBundle.message("android.export.package.incorrect.password.length", new Object[0]));
			}

			if(!Arrays.equals(password, confirmedPassword)) {
				throw new CommitStepException(AndroidBundle.message("android.export.package.passwords.not.match.error", new Object[0]));
			}
		} finally {
			Arrays.fill(password, '\u0000');
			Arrays.fill(confirmedPassword, '\u0000');
		}

	}

	public static void checkPassword(char[] password) throws CommitStepException {
		if(password.length == 0) {
			throw new CommitStepException(AndroidBundle.message("android.export.package.specify.password.error", new Object[0]));
		}
	}

	public static void checkPassword(JPasswordField passwordField) throws CommitStepException {
		char[] password = passwordField.getPassword();

		try {
			checkPassword(password);
		} finally {
			Arrays.fill(password, '\u0000');
		}
	}

	@NotNull
	public static <T> List<T> toList(@NotNull Enumeration<T> enumeration) {
		return ContainerUtil.toList(enumeration);
	}
}
