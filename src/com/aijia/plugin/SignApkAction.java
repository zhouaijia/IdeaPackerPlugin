package com.aijia.plugin;

import com.aijia.plugin.signedPackage.CheckModulePanel;
import com.aijia.plugin.signedPackage.SignedPackageWizard;
import com.aijia.plugin.utils.PluginUtils;
import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;

/**
 * Created by pc on 2016/4/6.
 */
public class SignApkAction extends AnAction {
	public SignApkAction() {
		super("Signed apk");
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		Project project = (Project)e.getData(CommonDataKeys.PROJECT);

		assert project != null;

		List facets = PluginUtils.getApplicationFacets(project);

		//assert facets.size() > 0;

		/*if(facets.size() != 1 || checkFacet((AndroidFacet)facets.get(0))) {
			ExportSignedPackageWizard wizard = new ExportSignedPackageWizard(project, facets, true);
			wizard.show();
		}*/

		SignedPackageWizard wizard = new SignedPackageWizard(project, facets, true,e);
		wizard.show();
	}

	private static boolean checkFacet(AndroidFacet facet) {
		final CheckModulePanel panel = new CheckModulePanel();
		panel.updateMessages(facet);
		final boolean hasError = panel.hasError();
		if(!hasError && !panel.hasWarnings()) {
			return true;
		} else {
			DialogWrapper dialog = new DialogWrapper(facet.getModule().getProject()) {
				{
					if(!hasError) {
						this.setOKButtonText("Continue");
					}

					this.init();
				}

				@NotNull
				protected Action[] createActions() {
					return hasError?new Action[]{this.getOKAction()}:super.createActions();
				}

				protected JComponent createCenterPanel() {
					return panel;
				}
			};
			dialog.setTitle(hasError?CommonBundle.getErrorTitle():CommonBundle.getWarningTitle());
			dialog.show();
			return !hasError && dialog.isOK();
		}
	}

	/*@Override
	public void update(AnActionEvent e) {
		Project project = (Project)e.getData(CommonDataKeys.PROJECT);
		e.getPresentation().setEnabled(project != null && PluginUtils.getApplicationFacets(project).size() > 0);
	}*/
}
