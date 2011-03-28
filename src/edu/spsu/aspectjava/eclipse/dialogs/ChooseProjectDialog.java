package edu.spsu.aspectjava.eclipse.dialogs;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import edu.spsu.aspectjava.eclipse.Activator;

public class ChooseProjectDialog extends Dialog{
	private String message;
	private String title;
	private TableViewer projectsViewer;
	private Image projectImage = null;
	private IProject chosenProject = null;
	
	public ChooseProjectDialog(Shell shell, String title, String message){
		super(shell);
		this.message = message;
		this.title = title;
		
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
		newShell.setSize(200, 150);
		Rectangle monitor = newShell.getDisplay().getPrimaryMonitor().getBounds();
		newShell.setMinimumSize(monitor.width / 5, monitor.height / 3);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite  = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		composite.setLayout(gridLayout);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(message);
		GridData gridData1 = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gridData1.verticalIndent = 8;
		gridData1.horizontalIndent = 3;
		label.setLayoutData(gridData1);

		projectsViewer = new TableViewer(composite, SWT.BORDER | SWT.SINGLE);
		projectsViewer.setContentProvider(new ProjectsContentProvider());
		projectsViewer.setLabelProvider(new ProjectsLabelProvider());
		projectsViewer.setInput(ResourcesPlugin.getWorkspace());		
		GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData2.verticalIndent = 8;
		projectsViewer.getTable().setLayoutData(gridData2);
		
		return composite;
	}
	
	@Override
	public int open() {
		projectImage = Activator.getImageDescriptor(Activator.IMG_JAVA_PROJECT).createImage();
		int result = super.open();
		projectImage.dispose();
		return result;
	}
	
	@Override
	protected void okPressed() {
		chosenProject = (IProject) ((IStructuredSelection) projectsViewer.getSelection()).
			getFirstElement();
		super.okPressed();
	}
	
	public IProject getChosen(){
		return chosenProject;
	}
	
	class ProjectsContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			IProject[] allProjects = ((IWorkspace) inputElement).getRoot().getProjects();
			List<IProject> openProjects = new LinkedList<IProject>();
			for(int i = 0; i < allProjects.length; i++){
				if(allProjects[i].isOpen()){
					openProjects.add(allProjects[i]);
				}
			}
			return openProjects.toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}
	
	class ProjectsLabelProvider extends LabelProvider {
		
		@Override
		public Image getImage(Object element) {			
			return projectImage;
		}
		
		@Override
		public String getText(Object element) {
			return ((IProject) element).getName();
		}
	}
}
