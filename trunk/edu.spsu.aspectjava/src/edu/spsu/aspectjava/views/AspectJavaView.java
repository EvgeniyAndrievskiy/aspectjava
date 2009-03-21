package edu.spsu.aspectjava.views;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import edu.spsu.aspectjava.AspectAction;
import edu.spsu.aspectjava.models.Aspect;
import edu.spsu.aspectjava.models.AspectJar;
import edu.spsu.aspectjava.models.AspectModel;
import edu.spsu.aspectjava.models.AspectModelListener;
import edu.spsu.aspectjava.models.AspectWeavingRule;
import edu.spsu.aspectjava.models.NoAspectsInJarException;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class AspectJavaView extends ViewPart {
	private TabFolder tabFolder;
	
	private TabItem aspectTab;
	private TreeViewer aspViewer;
	private AspectModel aspectModel;
	private AspectModelListener aspectModelListener;
	
	private TabItem joinpointTab;	
	private TreeViewer joinpViewer;
	
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
	
	private Button findButton;
	private Button weaveButton;
		
	private FileDialog multiOpenDialog;
		
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

	class AspViewContentProvider implements ITreeContentProvider,
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

		@Override
		public void addedAspectJar(AspectJar aspectsJar) {
			aspViewer.add(aspectModel, aspectsJar);	
		}

		@Override
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

		@Override
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

		@Override
		public void movedJarUp(AspectJar aspectsJar) {
			int oldIndex = aspectModel.indexOf(aspectsJar) + 1;
			aspViewer.remove(aspectModel, oldIndex);
			aspViewer.insert(aspectModel, aspectsJar, oldIndex - 1);		
			
			IStructuredSelection selection = new StructuredSelection(aspectsJar);
			aspViewer.setSelection(selection);
			
		}

	}
	
	class AspViewLabelProvider extends LabelProvider {

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
	
	class JoinpViewContentProvider implements ITreeContentProvider {
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
	
	class JoinpViewLabelProvider extends LabelProvider {

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
		
		createJoinpointTab();
		
		makeActions();
		
		contributeToActionBars();
		
		multiOpenDialog = new FileDialog(getViewSite().getShell(), SWT.OPEN | SWT.MULTI);
		multiOpenDialog.setText("Open");
		multiOpenDialog.setFilterPath("C:/");
		multiOpenDialog.setFileName(null);
		
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
							System.out.println(str);
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
		// add.setText("Action 1");
		add.setToolTipText("Add existing aspect JARs");
		add.setEnabled(true);
		URL url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/add.gif"), null);
		add.setImageDescriptor(ImageDescriptor.createFromURL(url));

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
		url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/delete.gif"), null);
		remove.setImageDescriptor(ImageDescriptor.createFromURL(url));

		reload = new Action() {
			public void run() {
				showMessage("Reload");
			}
		};
		reload.setToolTipText("Reload all JARs");
		reload.setEnabled(true);
		url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/refresh.gif"), null);
		reload.setImageDescriptor(ImageDescriptor.createFromURL(url));

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
		url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/down.gif"), null);
		down.setImageDescriptor(ImageDescriptor.createFromURL(url));

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
		url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/up.gif"), null);
		up.setImageDescriptor(ImageDescriptor.createFromURL(url));

		options = new Action() {
			public void run() {
				showMessage("options");
			}
		};
		options.setToolTipText("Options...");
		options.setEnabled(true);
		url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/options.gif"), null);
		options.setImageDescriptor(ImageDescriptor.createFromURL(url));

		reset = new Action() {
			public void run() {
				showMessage("reset");
			}
		};
		reset.setToolTipText("Reset Aspect.Java Framework and unlock"
				+ " all used sources");
		reset.setEnabled(false);
		url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/reset.gif"), null);
		reset.setImageDescriptor(ImageDescriptor.createFromURL(url));

		// doubleClickAction = new Action() {
		// public void run() {
		// ISelection selection = viewer.getSelection();
		// Object obj = ((IStructuredSelection)selection).getFirstElement();
		// showMessage("Double-click detected on "+obj.toString());
		// }
		// };
	}

	private void loadImages(){
		URL url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/jar.gif"), null);
		jarImage = ImageDescriptor.createFromURL(url).createImage();
		
		url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/aspect.gif"), null);
		aspectImage = ImageDescriptor.createFromURL(url).createImage();
		
		url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/rule.gif"), null);
		ruleImage = ImageDescriptor.createFromURL(url).createImage();
		
		url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/find.gif"), null);
		findImage = ImageDescriptor.createFromURL(url).createImage();
		
		url = FileLocator.find(Platform.getBundle(getViewSite().getPluginId()),
				new Path("icons/weave.gif"), null);
		weaveImage = ImageDescriptor.createFromURL(url).createImage();
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
		aspTabComp.setLayout(gridLayout);
		
		Composite aspectsViewerComp = new Composite(aspTabComp, SWT.BORDER);
		aspectsViewerComp.setLayout(new FillLayout());
		aspViewer = new TreeViewer(aspectsViewerComp,
				SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		aspViewer.setContentProvider(new AspViewContentProvider());
		aspViewer.setLabelProvider(new AspViewLabelProvider());
		aspViewer.setInput(aspectModel = new AspectModel());
		aspViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateToolBar();
			}
		});
		GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		aspectsViewerComp.setLayoutData(gridData1);
		
		findButton = new Button(aspTabComp, SWT.PUSH);
		findButton.setText("Find Joinpoints");
		findButton.setImage(findImage);
		GridData gridData2 = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		findButton.setLayoutData(gridData2);
		findButton.setEnabled(false);
		
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
		
		Composite joinpViewerComp = new Composite(joinTabComp, SWT.BORDER);
		joinpViewerComp.setLayout(new FillLayout());
		joinpViewer = new ContainerCheckedTreeViewer(joinpViewerComp,
				SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		joinpViewer.setContentProvider(new JoinpViewContentProvider());
		joinpViewer.setLabelProvider(new JoinpViewLabelProvider());
		joinpViewer.setInput(getViewSite());		
		GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		joinpViewerComp.setLayoutData(gridData1);
		
		weaveButton = new Button(joinTabComp, SWT.PUSH);
		weaveButton.setText("Weave Aspects");
		weaveButton.setImage(weaveImage);
		weaveButton.setEnabled(false);
		GridData gridData2 = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		weaveButton.setLayoutData(gridData2);
				
		joinpointTab.setControl(joinTabComp);
	}
	
	private void initAspectModelListener(){
		aspectModelListener = new AspectModelListener(){

			@Override
			public void addedAspectJar(AspectJar jarFile) {
				updateToolBar();
				findButton.setEnabled(true);
			}

			@Override
			public void removedAspectJar(AspectJar aspectsJar, int index) {
				if(aspectModel.isEmpty()){
					findButton.setEnabled(false);
				}
			}
			
			@Override
			public void movedJarDown(AspectJar aspectsJar) {
				
			}

			@Override
			public void movedJarUp(AspectJar aspectsJar) {
				
			}
			
		};
		aspectModel.addListener(aspectModelListener);
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
	
	private void showMessage(String message) {
		MessageDialog.openInformation(
			joinpViewer.getControl().getShell(),
			"Aspect.Java Framework",
			message);
	}
}