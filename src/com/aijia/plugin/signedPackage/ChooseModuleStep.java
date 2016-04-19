package com.aijia.plugin.signedPackage;

import com.intellij.facet.Facet;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ListCellRendererWrapper;

import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidBundle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;

/**
 * Created by pc on 2016/4/7.
 */
class ChooseModuleStep extends SignedPackageWizardStep {
	public static final String MODULE_PROPERTY = "ExportedModule";
	private JComboBox myModuleCombo;
	private JPanel myContentPanel;
	private org.jetbrains.android.exportSignedPackage.CheckModulePanel myCheckModulePanel;
	private final SignedPackageWizard myWizard;

	protected ChooseModuleStep(SignedPackageWizard wizard, List<Facet> facets) {
		this.myWizard = wizard;

		assert (facets.size() > 0);

		Facet selection = (Facet) facets.get(0);
		String module = PropertiesComponent.getInstance(wizard.getProject()).getValue("ExportedModule");
		if (module != null) {
			Iterator var5 = facets.iterator();

			while (var5.hasNext()) {
				Facet facet = (Facet) var5.next();
				if (module.equals(facet.getModule().getName())) {
					selection = facet;
					break;
				}
			}
		}

		this.myModuleCombo.setModel(new CollectionComboBoxModel(facets, selection));
		this.myModuleCombo.setRenderer(new ListCellRendererWrapper() {
			@Override
			public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
				Module module = ((Facet)value).getModule();
				this.setText(module.getName());
				this.setIcon(ModuleType.get(module).getIcon());
			}
		});
		this.myModuleCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ChooseModuleStep.this.myCheckModulePanel.updateMessages(ChooseModuleStep.this.getSelectedFacet());
			}
		});
		this.myCheckModulePanel.updateMessages(this.getSelectedFacet());
	}

	private AndroidFacet getSelectedFacet() {
		return (AndroidFacet)this.myModuleCombo.getSelectedItem();
	}

	@Override
	public String getHelpId() {
		return "reference.android.reference.extract.signed.package.choose.module";
	}

	protected void commitForNext() throws CommitStepException {
		if(this.myCheckModulePanel.hasError()) {
			throw new CommitStepException(AndroidBundle.message("android.project.contains.errors.error", new Object[0]));
		} else {
			Facet selectedFacet = this.getSelectedFacet();

			assert selectedFacet != null;

			this.myWizard.setFacet(selectedFacet);
		}
	}

	@Override
	public JComponent getComponent() {
		return this.myContentPanel;
	}
}
