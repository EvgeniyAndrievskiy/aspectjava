package edu.spsu.aspectjava.eclipse.views;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;

import edu.spsu.aspectjava.eclipse.Activator;
import edu.spsu.aspectjava.eclipse.dialogs.ChooseProjectDialog;
import edu.spsu.aspectjava.weaver.models.example.Aspect;
import edu.spsu.aspectjava.weaver.models.example.AspectJar;
import edu.spsu.aspectjava.weaver.models.example.AspectModel;
import edu.spsu.aspectjava.weaver.models.example.AspectModelListener;
import edu.spsu.aspectjava.weaver.models.example.AspectWeavingRule;
import edu.spsu.aspectjava.weaver.models.example.NoAspectsInJarException;


public class AspectJavaView extends ViewPart {
	private TabFolder tabFolder;
	
	private TabItem aspectTab;
	private Label projectLabel1;
	private TreeViewer aspViewer;
	private AspectModel aspectModel;
	private AspectModelListener aspectModelListener;
	
	private TabItem joinpointTab;
	private Label projectLabel2;
	private TreeViewer joinpViewer;
	
	private Action setProject;
	private Action add;
	private Action remove;
	private Action reload;
	private Action down;
	private Action up;
	private Action options;
	private Action reset;
	
	private Image jarImage;
	private Image aspectImage;
	private Image ruleImage;
	private Image findImage;
	private Image weaveImage;
	private Image projectImage;
	
	private Button findButton;
	private Button weaveButton;
		
	private FileDialog multiOpenDialog;
	private ChooseProjectDialog projectDialog;
	
	private IProject targetProject = null;
	public static final String NO_SPECIFIED = "<no target project specified>";
	private IResourceChangeListener projectsChangeListener;
		
//	private Action doubleClickAction;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */

	class AspectsContentProvider implements ITreeContentProvider,
										   AspectModelListener{
		private Object[] emptyArray = new Object[0];
		

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			aspectModel = (AspectModel) newInput;
			if(oldInput != null){
				((AspectModel) oldInput).removeListener(this);
			}
			aspectModel.addListener(this);		
		}
		
		public void dispose() {
		}
		
		public Object[] getElements(Object parent) {
			return ((AspectModel) parent).getAspectJars().toArray();
		}
		
		public Object getParent(Object child) {
			if(child instanceof AspectJar){
				return null;
			}else if(child instanceof Aspect){
				return ((Aspect) child).getAspectJar();
			}else if(child instanceof AspectWeavingRule){
				return ((AspectWeavingRule) child).getAspect();
			}
			return null;		
		}
		
		public Object [] getChildren(Object parent) {
			if(parent instanceof AspectJar){
				return ((AspectJar) parent).getAspects().toArray();
			}else if(parent instanceof Aspect){
				return ((Aspect) parent).getWeavingRules().toArray();
			}else if(parent instanceof AspectWeavingRule){
				return emptyArray;
			}
			return null;
		}
		
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length != 0;
		}

		public void addedAspectJar(AspectJar aspectsJar) {
			aspViewer.add(aspectModel, aspectsJar);	
		}

		public void removedAspectJar(AspectJar aspectsJar, int index) {
			aspViewer.remove(aspectModel, index);
			if(aspectModel.isEmpty()){
				return;
			}
			if(index > (aspectModel.getJarCount() + 1) / 2 - 1){
				aspViewer.setSelection(new StructuredSelection(aspectModel.getLastJar()));
			}else{
				aspViewer.setSelection(new StructuredSelection(aspectModel.getFirstJar()));
			}		
		}

		public void movedJarDown(AspectJar aspectsJar) {
			int oldIndex = aspectModel.indexOf(aspectsJar) - 1;
			aspViewer.remove(aspectModel, oldIndex);
			if(oldIndex == aspectModel.getJarCount() - 1){
				aspViewer.add(aspectModel, aspectsJar);	
			}else{
				aspViewer.insert(aspectModel, aspectsJar, oldIndex + 1);
			}
			
			IStructuredSelection selection = new StructuredSelection(aspectsJar);
			aspViewer.setSelection(selection);
		}

		public void movedJarUp(AspectJar aspectsJar) {
			int oldIndex = aspectModel.indexOf(aspectsJar) + 1;
			aspViewer.remove(aspectModel, oldIndex);
			aspViewer.insert(aspectModel, aspectsJar, oldIndex - 1);		
			
			IStructuredSelection selection = new StructuredSelection(aspectsJar);
			aspViewer.setSelection(selection);
			
		}

	}
	
	class AspectsLabelProvider extends LabelProvider {

		public String getText(Object obj) {
			if(obj instanceof AspectJar){
				return new File(((AspectJar) obj).getJarFile().getName()).getName();
			}else if(obj instanceof Aspect){
				return ((Aspect) obj).getAspectClass().getName();
			}else if(obj instanceof AspectWeavingRule){	
				return obj.toString();
			}
			return null;
		}
				
		public Image getImage(Object obj) {
			if(obj instanceof AspectJar){
				return jarImage;
			}else if(obj instanceof Aspect){
				return aspectImage;
			}else if(obj instanceof AspectWeavingRule){
				return ruleImage;
			}
			return null;
		}
	}
	
	class JoinpointsContentProvider implements ITreeContentProvider {
		private Object[] emptyArray = new Object[0];

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
//			if (parent.equals(getViewSite())) {
//				if (invisibleRoot == null)
//					initialize();
//				return getChildren(invisibleRoot);
//			}
//			return getChildren(parent);
			return emptyArray;
		}

		public Object getParent(Object child) {
//			if (child instanceof TreeObject) {
//				return ((TreeObject) child).getParent();
//			}
			return null;
		}

		public Object[] getChildren(Object parent) {
//			if (parent instanceof TreeParent) {
//				return ((TreeParent) parent).getChildren();
//			}
//			return new Object[0];
			return emptyArray;
		}

		public boolean hasChildren(Object parent) {
//			if (parent instanceof TreeParent)
//				return ((TreeParent) parent).hasChildren();
			return false;
		}
	}
	
	class JoinpointsLabelProvider extends LabelProvider {

		public String getText(Object obj) {
			return obj.toString();
		}
		public Image getImage(Object obj) {
//			String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
//			if (obj instanceof TreeParent)
//			   imageKey = ISharedImages.IMG_OBJ_FOLDER;
//			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
			return null;
		}
	}

	/**
	 * The constructor.
	 */
	public AspectJavaView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		loadImages();
		
		tabFolder = new TabFolder(parent, SWT.NONE);
		
		tabFolder.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateToolBar();
			}		
		});
		
		createAspectTab();
		
		initAspectModelListener();
		
		initProjectsChangeListener();
		
		createJoinpointTab();
		
		makeActions();
		
		contributeToActionBars();
		
		multiOpenDialog = new FileDialog(getViewSite().getShell(), SWT.OPEN | SWT.MULTI);
		multiOpenDialog.setText("Open");
		multiOpenDialog.setFilterPath("C:/");
		multiOpenDialog.setFileName(null);
		
		projectDialog = new ChooseProjectDialog(getViewSite().getShell(), 
				"Choose Project", "Choose target project for weaving.");
				
//		hookContextMenu();
//		hookDoubleClickAction();
		
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		joinpViewer.getControl().setFocus();
	}
	
	public void dispose(){
		jarImage.dispose();
		aspectImage.dispose();
		ruleImage.dispose();		
		findImage.dispose();
		weaveImage.dispose();		
		projectImage.dispose();
	}

//	private void hookContextMenu() {
//		MenuManager menuMgr = new MenuManager("#PopupMenu");
//		menuMgr.setRemoveAllWhenShown(true);
//		menuMgr.addMenuListener(new IMenuListener() {
//			public void menuAboutToShow(IMenuManager manager) {
//				AspectView.this.fillContextMenu(manager);
//			}
//		});
//		Menu menu = menuMgr.createContextMenu(joinpViewer.getControl());
//		joinpViewer.getControl().setMenu(menu);
//		getSite().registerContextMenu(menuMgr, joinpViewer);
//	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
//		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

//	private void fillLocalPullDown(IMenuManager manager) {
//		manager.add(add);
//		manager.add(new Separator());
//		manager.add(remove);
//	}

//	private void fillContextMenu(IMenuManager manager) {
//		manager.add(add);
//		manager.add(remove);
//		manager.add(new Separator());
//		drillDownAdapter.addNavigationActions(manager);
//		// Other plug-ins can contribute there actions here
//		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
//	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		Separator separator = new Separator();
		manager.add(setProject);
		manager.add(separator);
		manager.add(add);
		manager.add(remove);
		manager.add(reload);
		manager.add(separator);
		manager.add(down);
		manager.add(up);
		manager.add(separator);
		manager.add(reset);
		manager.add(separator);
		manager.add(options);
	}

	private void makeActions() {
		setProject = new Action(){
			public void run(){
				int returnedCode = projectDialog.open();
				if(returnedCode == ChooseProjectDialog.OK){
					if(projectDialog.getChosen() != null){
						targetProject = projectDialog.getChosen();
						projectLabel1.setText(targetProject.getName());
						projectLabel2.setText(targetProject.getName());
						updateFindButton();
					}
				}
			}
		};
		setProject.setToolTipText("Choose target project for weaving");
		setProject.setEnabled(true);
		setProject.setImageDescriptor(Activator.
				getImageDescriptor(Activator.IMG_SET_PROJECT));
		
		add = new Action() {
			private String[] jar = new String[]{"*.jar"};
			
			public void run() {
				multiOpenDialog.setFilterExtensions(jar);
				String str = multiOpenDialog.open();
				String[] fileNames = multiOpenDialog.getFileNames();
				if (str != null) {
					String path = new File(str).getParent();
					for (int i = 0; i < fileNames.length; i++) {
						try {
							str = path + '\\' + fileNames[i];
							boolean added =  aspectModel.addAspectJar(new JarFile(str));
							if(!added){
								MessageDialog.openError(getViewSite().getShell(),
								"Error", "JAR " + str
								+ " is already added.");
							}
						} catch (IOException e) {
							MessageDialog.openError(getViewSite()
									.getShell(), "Error",
									"There's some error while loading " + str);
						} catch (NoAspectsInJarException e) {
							MessageDialog.openError(getViewSite().getShell(),
							"Error", "JAR " + e.getJarFile().getName()
							+ " failed to load.\n" +
							"Probably it is not a valid Aspect.Java aspect unit"
							+ " or it has no actions.");
						}
					}
				}
			}
		};
		add.setToolTipText("Add existing aspect JARs");
		add.setEnabled(true);
		add.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_ADD));

		remove = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection)aspViewer.getSelection())
					.getFirstElement();
				if(selected instanceof AspectJar){
					aspectModel.removeAspectJar((AspectJar) selected);
				}else{
					remove.setEnabled(false);
					return;
				}
			}
		};
		// remove.setText("Action 2");
		remove.setToolTipText("Remove selected JAR");
		remove.setEnabled(false);
		remove.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_DELETE));

		reload = new Action() {
			public void run() {
				showMessage("Reload");
			}
		};
		reload.setToolTipText("Reload all JARs");
		reload.setEnabled(true);
		reload.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_REFRESH));

		down = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection) aspViewer
						.getSelection()).getFirstElement();
				if (selected instanceof AspectJar) {
					boolean moved = aspectModel.moveJarDown((AspectJar) selected);
					if(!moved){
						down.setEnabled(false);
					}
				} else {
					down.setEnabled(false);
					return;
				}
			}
		};
		down.setToolTipText("Move aspect JAR down the queue");
		down.setEnabled(false);
		down.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_ARROW_DOWN));

		up = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection) aspViewer
						.getSelection()).getFirstElement();
				if (selected instanceof AspectJar) {
					boolean moved = aspectModel.moveJarUp((AspectJar) selected);
					if(!moved){
						up.setEnabled(false);
					}
				} else {
					up.setEnabled(false);
					return;
				}
			}
		};
		up.setToolTipText("Move aspect JAR up the queue");
		up.setEnabled(false);
		up.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_ARROW_UP));

		options = new Action() {
			public void run() {
				showMessage("options");
			}
		};
		options.setToolTipText("Options...");
		options.setEnabled(true);
		options.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_OPTIONS));

		reset = new Action() {
			public void run() {
				showMessage("reset");
			}
		};
		reset.setToolTipText("Reset Aspect.Java Framework and unlock"
				+ " all used sources");
		reset.setEnabled(false);
		reset.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_RESET));

		// doubleClickAction = new Action() {
		// public void run() {
		// ISelection selection = viewer.getSelection();
		// Object obj = ((IStructuredSelection)selection).getFirstElement();
		// showMessage("Double-click detected on "+obj.toString());
		// }
		// };
	}

	private void loadImages(){
		jarImage = Activator.getImageDescriptor(Activator.IMG_JAR).createImage();
		aspectImage = Activator.getImageDescriptor(Activator.IMG_CLASS).createImage();
		ruleImage = Activator.getImageDescriptor(Activator.IMG_METHOD).createImage();
		findImage = Activator.getImageDescriptor(Activator.IMG_FIND).createImage();
		weaveImage = Activator.getImageDescriptor(Activator.IMG_WEAVE).createImage();
		projectImage = Activator.getImageDescriptor(Activator.IMG_JAVA_PROJECT).createImage();
	}
	
	private void createAspectTab(){
		aspectTab = new TabItem(tabFolder, SWT.NONE);
		aspectTab.setText("Aspects");
		Composite aspTabComp = new Composite(tabFolder, SWT.NONE);
		aspTabComp.setBackground(getViewSite().getShell().getDisplay().
				getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		GridLayout gridLayout= new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.verticalSpacing = 5;
		gridLayout.marginWidth = 5;
//		gridLayout.makeColumnsEqualWidth = true;
		aspTabComp.setLayout(gridLayout);
		
		Composite projectComp = new Composite(aspTabComp, SWT.NONE);
		RowLayout rowLayout = new RowLayout();
		rowLayout.spacing = 7;
		rowLayout.type = SWT.HORIZONTAL;
		rowLayout.wrap = false;
		rowLayout.center = true;
		projectComp.setLayout(rowLayout);
		Label imageLabel = new Label(projectComp, SWT.NONE);
		imageLabel.setImage(projectImage);
		projectLabel1 = new Label(projectComp, SWT.NONE);
		projectLabel1.setText(NO_SPECIFIED);
		projectLabel1.setToolTipText("Target project");
		GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData2.verticalIndent = 4;
		projectComp.setLayoutData(gridData2);
		
		Composite aspectsViewerComp = new Composite(aspTabComp, SWT.BORDER);
		aspectsViewerComp.setLayout(new FillLayout());
		aspViewer = new TreeViewer(aspectsViewerComp,
				SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		aspViewer.setContentProvider(new AspectsContentProvider());
		aspViewer.setLabelProvider(new AspectsLabelProvider());
		aspViewer.setInput(aspectModel = new AspectModel());
		aspViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event) {
				updateToolBar();
			}
		});
		GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData1.verticalIndent = 4;
		gridData1.verticalSpan = gridLayout.numColumns;
		aspectsViewerComp.setLayoutData(gridData1);
		
		findButton = new Button(aspTabComp, SWT.PUSH);
		findButton.setText("Find Joinpoints");
		findButton.setImage(findImage);
		findButton.setEnabled(false);
		GridData gridData3 = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gridData3.verticalSpan = gridLayout.numColumns;
		findButton.setLayoutData(gridData3);
		
		aspectTab.setControl(aspTabComp);
	}
	
	private void createJoinpointTab(){
		joinpointTab = new TabItem(tabFolder, SWT.NONE);
		joinpointTab.setText("Joinpoints");
		Composite joinTabComp = new Composite(tabFolder, SWT.NONE);
		joinTabComp.setBackground(getViewSite().getShell().getDisplay().
				getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		GridLayout gridLayout= new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.verticalSpacing = 5;
		gridLayout.marginWidth = 5;
		joinTabComp.setLayout(gridLayout);	
		
		Composite projectComp = new Composite(joinTabComp, SWT.NONE);
		RowLayout rowLayout = new RowLayout();
		rowLayout.spacing = 7;
		rowLayout.type = SWT.HORIZONTAL;
		rowLayout.wrap = false;
		rowLayout.center = true;
		projectComp.setLayout(rowLayout);
		Label imageLabel = new Label(projectComp, SWT.NONE);
		imageLabel.setImage(projectImage);
		projectLabel2 = new Label(projectComp, SWT.NONE);
		projectLabel2.setText(NO_SPECIFIED);
		projectLabel2.setToolTipText("Target project");
		GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData2.verticalIndent = 4;
		projectComp.setLayoutData(gridData2);
		
		Composite joinpViewerComp = new Composite(joinTabComp, SWT.BORDER);
		joinpViewerComp.setLayout(new FillLayout());
		joinpViewer = new ContainerCheckedTreeViewer(joinpViewerComp,
				SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		joinpViewer.setContentProvider(new JoinpointsContentProvider());
		joinpViewer.setLabelProvider(new JoinpointsLabelProvider());
		joinpViewer.setInput(getViewSite());		
		GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData1.verticalIndent = 4;
		gridData1.verticalSpan = gridLayout.numColumns;
		joinpViewerComp.setLayoutData(gridData1);
		
		weaveButton = new Button(joinTabComp, SWT.PUSH);
		weaveButton.setText("Weave Aspects");
		weaveButton.setImage(weaveImage);
		weaveButton.setEnabled(false);
		GridData gridData3 = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gridData3.verticalSpan = gridLayout.numColumns;
		weaveButton.setLayoutData(gridData3);
				
		joinpointTab.setControl(joinTabComp);
	}
	
	private void initAspectModelListener(){
		aspectModelListener = new AspectModelListener(){

			public void addedAspectJar(AspectJar jarFile) {
				updateToolBar();
				updateFindButton();
			}

			public void removedAspectJar(AspectJar aspectsJar, int index) {
				if(aspectModel.isEmpty()){
					findButton.setEnabled(false);
				}
			}
			
			public void movedJarDown(AspectJar aspectsJar) {
				
			}

			public void movedJarUp(AspectJar aspectsJar) {
				
			}
			
		};
		aspectModel.addListener(aspectModelListener);
	}
	
	private void initProjectsChangeListener(){
		projectsChangeListener = new IResourceChangeListener(){

			public void resourceChanged(IResourceChangeEvent event) {
				if(event.getType() == IResourceChangeEvent.PRE_CLOSE
						|| event.getType() == IResourceChangeEvent.PRE_DELETE){
					if(event.getResource() == targetProject){
						getViewSite().getShell().getDisplay().syncExec(new Runnable(){
							public void run() {
								projectLabel1.setText(NO_SPECIFIED);
								projectLabel2.setText(NO_SPECIFIED);	
								findButton.setEnabled(false);
								targetProject = null;
							}
						});
					}
//				}else if(event.getType() == IResourceChangeEvent.POST_CHANGE){
//					IResourceDelta delta = event.getDelta();
//					if(delta.getResource() == targetProject){
//						
//					}
				}
				
			}
			
		};
		ResourcesPlugin.getWorkspace().
			addResourceChangeListener(projectsChangeListener);
	}
	
	//	private void hookDoubleClickAction() {
//		joinpViewer.addDoubleClickListener(new IDoubleClickListener() {
//			public void doubleClick(DoubleClickEvent event) {
//				doubleClickAction.run();
//			}
//		});
//	}
	
	private void updateToolBar(){
		int currentTabIndex = tabFolder.getSelectionIndex();
		TabItem currentTab = tabFolder.getItem(currentTabIndex);
		if(currentTab == aspectTab){
			add.setEnabled(true);
			reload.setEnabled(true);
			options.setEnabled(true);
			IStructuredSelection selection = (IStructuredSelection) aspViewer.getSelection();
			if(selection.isEmpty()){
				remove.setEnabled(false);
				up.setEnabled(false);
				down.setEnabled(false);
				return;
			}
			Object selected = selection.getFirstElement();
			if(selected instanceof AspectJar){
				remove.setEnabled(true);
				int index = aspectModel.indexOf((AspectJar) selected);
				if(index > 0){
					up.setEnabled(true);
				}else{
					up.setEnabled(false);
				}
				if(index < aspectModel.getJarCount() - 1){
					down.setEnabled(true);
				}else{
					down.setEnabled(false);
					return;
				}
			}else{
				remove.setEnabled(false);
				up.setEnabled(false);
				down.setEnabled(false);
			}
		}else if(currentTab == joinpointTab){
			add.setEnabled(false);
			remove.setEnabled(false);
			reload.setEnabled(false);
			up.setEnabled(false);
			down.setEnabled(false);
			options.setEnabled(false);
		}
	}
	
	private void updateFindButton(){
		if(targetProject != null && !aspectModel.isEmpty()){
			findButton.setEnabled(true);
		}
	}
	
	public static void showMessage(String message) {
		MessageDialog.openInformation(
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
			"Aspect.Java Framework",
			message);
	}
}