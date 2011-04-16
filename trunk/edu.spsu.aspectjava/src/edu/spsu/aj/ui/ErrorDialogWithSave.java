package edu.spsu.aj.ui;

import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

class ErrorDialogWithSave extends ErrorDialog{
	public static final int SAVE_ID = 70;
	private Button saveLogButton;
	private FileDialog saveDialog;
	private MultiStatus status;

	ErrorDialogWithSave(Shell parentShell, String message, MultiStatus status) {
		super(parentShell, "Error", message, status, IStatus.ERROR);
		saveDialog = new FileDialog(parentShell, SWT.SAVE);
		saveDialog.setOverwrite(true);
		saveDialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString());
		this.status = status;
	}
	
//	void setStatus(MultiStatus status) {
//		super.setStatus(status);
//	}
//	
//	void setMessage(String message){
//		this.message = message;
//	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		saveLogButton = createButton(parent, SAVE_ID, "Save Log", true);
	}
	
	@Override
	protected void buttonPressed(int id) {
		super.buttonPressed(id);
		if(id == SAVE_ID){
			String str = saveDialog.open();
			if(str != null){
				try {
					FileWriter w = new FileWriter(str);
					for(IStatus s : status.getChildren()){
						w.write(s.getMessage() + '\n');
					}
					w.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
