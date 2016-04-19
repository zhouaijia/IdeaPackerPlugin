package com.aijia.plugin.utils;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.util.SystemProperties;

import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JPanel;

/**
 * Created by pc on 2016/4/7.
 */
public abstract class SaveFileListener implements ActionListener {
	private final JPanel myContentPanel;
	private final TextFieldWithBrowseButton myTextField;
	private final String myDialogTitle;
	private final String myExtension;

	public SaveFileListener(JPanel contentPanel, TextFieldWithBrowseButton textField, String dialogTitle, String extension) {
		this.myContentPanel = contentPanel;
		this.myTextField = textField;
		this.myDialogTitle = dialogTitle;
		this.myExtension = extension;
	}

	@Nullable
	protected abstract String getDefaultLocation();

	public void actionPerformed(ActionEvent e) {
		String path = this.myTextField.getText().trim();
		if(path.length() == 0) {
			String file = this.getDefaultLocation();
			path = file != null && file.length() > 0?file: SystemProperties.getUserHome();
		}

		File file1 = new File(path);
		if(!file1.exists()) {
			path = SystemProperties.getUserHome();
		}

		FileSaverDescriptor descriptor = new FileSaverDescriptor(this.myDialogTitle, "Save as *." + this.myExtension, new String[]{this.myExtension});
		FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, this.myContentPanel);
		VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(file1.exists()?file1:new File(path));
		if(vf == null) {
			vf = VfsUtil.getUserHomeDir();
		}

		VirtualFileWrapper result = saveFileDialog.save(vf, (String)null);
		if(result != null && result.getFile() != null) {
			this.myTextField.setText(result.getFile().getPath());
		}
	}
}
