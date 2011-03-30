package edu.spsu.aj.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import edu.spsu.aj.weaver.AbstractConditionClause;
import edu.spsu.aj.weaver.AbstractConditionClause.Context;
import edu.spsu.aj.weaver.Aspect;
import edu.spsu.aj.weaver.AspectRule;
import edu.spsu.aj.weaver.Joinpoint;
import edu.spsu.aj.weaver.LocalVariablesAdapter;
import edu.spsu.aj.weaver.Weaver;
import edu.spsu.aj.eclipse.AspectsModel.*;


public class AspectJavaView extends ViewPart {
	private TabFolder tabFolder;
	
	private TabItem aspectTab;
	private Label projectLabel1;
	private TreeViewer aspViewer;
	private AspectsModel aspectModel;
	private AspectsModelListener aspectModelListener;
	
	private TabItem joinpointTab;
	private Label projectLabel2;
	private ContainerCheckedTreeViewer joinpViewer;
	
	private Action setProject;
	private Action addFolder;
	private Action addJars;
	private Action remove;
	private Action reload;
	private Action down;
	private Action up;
	private Action options;
	private Action reset;
	
	static Image folderImage;
	static Image jarImage;
	static Image aspectImage;
	static Image ruleImage;
	static Image packageImage;
	static Image classImage;
	static Image methodImage;
	static Image sourceImage;
	static Image findImage;
	static Image weaveImage;
	static Image projectImage;
	
	private Button findButton;
	private Button weaveButton;
		
	private DirectoryDialog folderChooseDialog;
	private FileDialog jarsChooseDialog;
	private ChooseProjectDialog projectDialog;
	
	private IJavaProject targetProject = null;
	private List<ClassNode> targProjLoaded;
	public static final String NO_SPECIFIED = "<no target project specified>";
	private IResourceChangeListener projectsChangeListener;
	
	private Weaver weaver;
		
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
										   AspectsModelListener{
		private Object[] emptyArray = new Object[0];
		

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			if(newInput == null){
				return;
			}
			aspectModel = (AspectsModel) newInput;
			if(oldInput != null){
				((AspectsModel) oldInput).removeListener(this);
			}
			aspectModel.addListener(this);		
		}
		
		public void dispose() {
		}
		
		public Object[] getElements(Object parent) {
			return ((AspectsModel) parent).getAspectsContainers().toArray();
		}
		
		public Object getParent(Object child) {
			if(child instanceof AspectsContainer){
				return null;
			}else if(child instanceof Aspect){
//				return ((Aspect) child).getAspectJar();
			}else if(child instanceof AspectRule){
//				return ((AspectWeavingRule) child).getAspect();
			}
			return null;		
		}
		
		public Object [] getChildren(Object parent) {
			if(parent instanceof AspectsContainer){
				return ((AspectsContainer) parent).getPackages().toArray();
			}else if(parent instanceof AspectsPackage){
				return ((AspectsPackage) parent).getAspects().toArray();
			}else if(parent instanceof Aspect){
				return ((Aspect) parent).getRules().toArray();
			}else if(parent instanceof AspectRule){
				return emptyArray;
			}
			return null;
		}
		
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length != 0;
		}

		public void aspectsContainerAdded(AspectsContainer container) {
			aspViewer.add(aspectModel, container);	
		}
		
		@Override
		public void aspectsContainerAdded(int index, AspectsContainer container) {
			aspViewer.insert(aspectModel, container, index);
			aspViewer.setSelection(new StructuredSelection(container));
		}

		public void aspectsContainerRemoved(AspectsContainer container, int index) {
			aspViewer.remove(aspectModel, index);
			if(aspectModel.isEmpty()){
				return;
			}
			if(index > (aspectModel.getContainersCount() + 1) / 2 - 1){
				aspViewer.setSelection(new StructuredSelection(aspectModel.getLastContainer()));
			}else{
				aspViewer.setSelection(new StructuredSelection(aspectModel.getFirstContainer()));
			}		
		}

		public void containerMovedDown(AspectsContainer container) {
			int oldIndex = aspectModel.indexOf(container) - 1;
			TreePath[] treePaths = aspViewer.getExpandedTreePaths();
			aspViewer.remove(aspectModel, oldIndex);
			if(oldIndex == aspectModel.getContainersCount() - 1){
				aspViewer.add(aspectModel, container);	
				aspViewer.setExpandedTreePaths(treePaths);
			}else{
				aspViewer.insert(aspectModel, container, oldIndex + 1);
				aspViewer.setExpandedTreePaths(treePaths);
			}
			
			IStructuredSelection selection = new StructuredSelection(container);
			aspViewer.setSelection(selection);
		}

		public void containerMovedUp(AspectsContainer container) {
			int oldIndex = aspectModel.indexOf(container) + 1;
			TreePath[] treePaths = aspViewer.getExpandedTreePaths();
			aspViewer.remove(aspectModel, oldIndex);
			aspViewer.insert(aspectModel, container, oldIndex - 1);	
			aspViewer.setExpandedTreePaths(treePaths);
			
			IStructuredSelection selection = new StructuredSelection(container);
			aspViewer.setSelection(selection);
			
		}

	}
	
	class AspectsLabelProvider extends ColumnLabelProvider {

		public String getText(Object obj) {
			if(obj instanceof AspectsContainer){
				return ((AspectsContainer) obj).getPath();
			}else if(obj instanceof AspectsPackage){
				return ((AspectsPackage) obj).getName();
			}else if(obj instanceof Aspect){
				// package.class format
				String name =  ((Aspect) obj).getName();
				return name.substring(name.lastIndexOf('.') + 1);
			}else if(obj instanceof AspectRule){	
				return obj.toString();
			}
			return null;
		}
		
		@Override
		public String getToolTipText(Object element) {
			if(element instanceof AspectsContainer){
				return null;
			}else if(element instanceof AspectsPackage){
				return ((AspectsPackage) element).getPath();
			}else if(element instanceof Aspect){
				return ((Aspect) element).getDescription();
			}else if(element instanceof AspectRule){	
				return ((AspectRule) element).getDescription();
			}
			return null;
		}
		
		@Override
		public int getToolTipTimeDisplayed(Object object) {
			return 5000;
		}
		
		@Override
		public int getToolTipDisplayDelayTime(Object object) {
			return 0;
		}
		
//		@Override
//		public boolean useNativeToolTip(Object object) {
//			return true;
//		}
				
		public Image getImage(Object obj) {
			if(obj instanceof AspectsContainer){
				if(((AspectsContainer) obj).isFolder()){
					return folderImage;
				}else{
					return jarImage;
				}
			}else if(obj instanceof AspectsPackage){
				return packageImage;
			}else if(obj instanceof Aspect){
				return aspectImage;
			}else if(obj instanceof AspectRule){
				return ruleImage;
			}
			return null;
		}
	}
	
	class JoinpointsContentProvider implements ITreeContentProvider {
		private Object[] emptyArray = new Object[0];
		private List<Joinpoint> input;

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			input = (List<Joinpoint>) newInput;
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			List<Joinpoint> joinpoints = (List<Joinpoint>) parent;
			Set<String> packages = new HashSet<String>();
			for(Joinpoint jp : joinpoints){
				ClassNode cn = jp.getClazz();
				int l = cn.name.lastIndexOf('/');
				if(l < 0){
					packages.add(AspectsPackage.DEFAULT_PACKAGE);
				}else{
					packages.add(cn.name.substring(0, l));
				}
			}
			return packages.toArray();
		}

		public Object getParent(Object child) {
//			if (child instanceof TreeObject) {
//				return ((TreeObject) child).getParent();
//			}
			return null;
		}

		public Object[] getChildren(Object parent) {
			if(parent instanceof String){  // case for packages
				List<ClassNode> classNodes = new IdentityLinkedList<ClassNode>();
				for(Joinpoint jp : input){
					String package1 = null;
					int l = jp.getClazz().name.lastIndexOf('/');
					if(l < 0){
						package1 = AspectsPackage.DEFAULT_PACKAGE;
					}else{
						package1 = jp.getClazz().name.substring(0, l);
					}
					if(parent.hashCode() != package1.hashCode()){
						continue;
					}else{
						if(parent.equals(package1)){
							classNodes.add(jp.getClazz());
						}
					}
				}
				return classNodes.toArray();
			}else if(parent instanceof ClassNode){
				List<MethodNode> methods = new IdentityLinkedList<MethodNode>();
				for(Joinpoint joinpoint : input){
					if(parent == joinpoint.getClazz()){
						methods.add(joinpoint.getMethod());
					}
				}
				return methods.toArray();
			}else if(parent instanceof MethodNode){
				List<AbstractInsnNode> instr = new IdentityLinkedList<AbstractInsnNode>();
				for(Joinpoint joinpoint : input){
					if(parent == joinpoint.getMethod()){
						instr.add(joinpoint.getInstr());
					}
				}
				return instr.toArray();
			}else if(parent instanceof AbstractInsnNode){
				List<Joinpoint> jps = new LinkedList<Joinpoint>();
				for(Joinpoint jp : input){
					if(parent == jp.getInstr()){
						jps.add(jp);
					}
				}
				return jps.toArray();
			}else{
				return emptyArray;
			}
		}

		// TODO think about performance ?
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length != 0;
		}

	}
	
	class JoinpointsLabelProvider extends LabelProvider {

		public String getText(Object obj) {
			if(obj instanceof String){  // case for packages
				return ((String) obj).replace('/', '.');
			}else if(obj instanceof ClassNode){
				ClassNode cn = (ClassNode) obj;
				int l = cn.name.lastIndexOf('/');
				if(l < 0){
					return cn.name;
				}else{
					return cn.name.substring(l + 1);
				}
			}else if(obj instanceof MethodNode){
				return ((MethodNode) obj).name;
			}else if(obj instanceof AbstractInsnNode){
				AbstractInsnNode instr = (AbstractInsnNode) obj;
				// CALL joinpoint case
				if(instr instanceof MethodInsnNode){
					MethodInsnNode mInstr = (MethodInsnNode) instr;
					StringBuilder sb = new StringBuilder("%call ");
					sb.append(Type.getReturnType(mInstr.desc).getClassName());
					sb.append(" ");
					sb.append(mInstr.owner.replace('/', '.'));
					sb.append('.');
					sb.append(mInstr.name);
					sb.append("(");
					
					Type[] argTypes = Type.getArgumentTypes(mInstr.desc);
					
					if(argTypes.length > 0){
						sb.append(argTypes[0].getClassName());
					}
					for(int i = 1; i < argTypes.length; i++){
						sb.append(", " + argTypes[i].getClassName());
					}
					sb.append(")");
					AbstractInsnNode line = instr.getPrevious();
					while(!(line instanceof LineNumberNode)){
						line = line.getPrevious();
					}
					sb.append(" @ line ");
					sb.append(((LineNumberNode)line).line);
					return sb.toString();
				}// USE & ASSIGN joinpoint cases
				else{
					return "";
				}
			}else if(obj instanceof Joinpoint){
				Joinpoint jp = (Joinpoint) obj;
				StringBuilder sb = new StringBuilder();
				String actDesc = jp.getAspectRule().getAction().getDescriptor();
				String retType = Type.getReturnType(actDesc).getClassName();
				sb.append(retType);
				sb.append(" ");
				sb.append(jp.getAspect().getName());
				sb.append('.');
				sb.append(jp.getAspectRule().getAction().getName());
				sb.append("(");
				
				Type[] argTypes = Type.getArgumentTypes(actDesc);
				
				if(argTypes.length > 0){
					sb.append(argTypes[0].getClassName());
				}
				for(int i = 1; i < argTypes.length; i++){
					sb.append(", " + argTypes[i].getClassName());
				}
				sb.append(") -> ");
				sb.append(jp.getClause());
				return sb.toString();
			}
			return null;
		}
		public Image getImage(Object obj) {
			if(obj instanceof String){  // case for packages
				return packageImage;
			}else if(obj instanceof ClassNode){
				return classImage;
			}else if(obj instanceof MethodNode){
				return methodImage;
			}else if(obj instanceof AbstractInsnNode){
				return sourceImage;
			}else if(obj instanceof Joinpoint){
				return ruleImage;
			}
			return null;
		}
	}

	/**
	 * The constructor.
	 */
	public AspectJavaView() {
		weaver = new Weaver();
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
		
		folderChooseDialog = new DirectoryDialog(getViewSite().getShell());
		folderChooseDialog.setText("Choose folder");
		
		jarsChooseDialog = new FileDialog(getViewSite().getShell(), SWT.OPEN | SWT.MULTI);
		jarsChooseDialog.setText("Choose JARs");
		jarsChooseDialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
		jarsChooseDialog.setFileName(null);
		jarsChooseDialog.setFilterExtensions(new String[]{"*.jar"});
		
		projectDialog = new ChooseProjectDialog(getViewSite().getShell(), 
				"Choose project", "Choose target project for weaving.\n" +
				"NOTE: you can choose only java project.");
				
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
		folderImage.dispose();
		jarImage.dispose();
		aspectImage.dispose();
		ruleImage.dispose();
		packageImage.dispose();
		classImage.dispose();
		methodImage.dispose();
		sourceImage.dispose();
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
		manager.add(addFolder);
		manager.add(addJars);
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
						if(targetProject != projectDialog.getChosen()){
							targetProject = projectDialog.getChosen();
							projectLabel1.setText(targetProject.getProject().getName());
							projectLabel2.setText(targetProject.getProject().getName());
							updateFindButton();
							joinpViewer.setInput(null);
							weaveButton.setEnabled(false);
						}
					}
				}
			}
		};
		setProject.setToolTipText("Choose target project for weaving");
		setProject.setEnabled(true);
		setProject.setImageDescriptor(Activator.
				getImageDescriptor(Activator.IMG_SET_PROJECT));
		
		addFolder = new Action() {
			
			public void run() {
				folderChooseDialog.setMessage("Choose aspects folder.");
				folderChooseDialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
				String str = folderChooseDialog.open();
				if (str != null) {
						try {
							boolean added =  aspectModel.addAspectsContainer(str);
							if(!added){
								MessageDialog.openError(getViewSite().getShell(),
								"Error", "Folder " + str
								+ " is already added.");
							}
						} catch (IOException e) {
							// This exception type is not expected.
//							MessageDialog.openError(getViewSite()
//									.getShell(), "Error",
//									"There are some I/O errors while loading " + str + ".");
						} catch (NoAspectsInContainerException e) {
							MessageDialog.openError(getViewSite().getShell(),
							"Error", "Folder " + str
							+ " contains no aspects.");
						}
				}
			}
		};
		addFolder.setToolTipText("Add aspects folder");
		addFolder.setEnabled(true);
		addFolder.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_ADD_FOLDER));
		
		addJars = new Action() {
	
			public void run() {
				String str = jarsChooseDialog.open();
				String[] fileNames = jarsChooseDialog.getFileNames();
				if (str != null) {
					String parent = new File(str).getParent();
					for (int i = 0; i < fileNames.length; i++) {
						String path = parent + '\\' + fileNames[i];
						try {	
							boolean added =  aspectModel.addAspectsContainer(path);
							if(!added){
								MessageDialog.openError(getViewSite().getShell(),
								"Error", "JAR " + path
								+ " is already added.");
							}
						} catch (IOException e) {
							MessageDialog.openError(getViewSite().getShell(), "Error",
									"There are some I/O errors while loading " + path + ".");
						} catch (NoAspectsInContainerException e) {
							MessageDialog.openError(getViewSite().getShell(),
							"Error", "JAR " + path
							+ " contains no aspects.");
						}
					}
				}
			}
		};
		addJars.setToolTipText("Add aspects JARs");
		addJars.setEnabled(true);
		addJars.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_ADD_JARS));

		remove = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection)aspViewer.getSelection())
					.getFirstElement();
				if(selected instanceof AspectsContainer){
					aspectModel.removeAspectsContainer((AspectsContainer) selected);
				}else{
					remove.setEnabled(false);
					return;
				}
			}
		};
		remove.setToolTipText("Remove selected folder/JAR");
		remove.setEnabled(false);
		remove.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_DELETE));

		reload = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection)aspViewer.getSelection())
					.getFirstElement();
				if(selected instanceof AspectsContainer){
					AspectsContainer container = (AspectsContainer) selected;
					String path = container.getPath();
					String contStr = container.isFolder()? "Folder" : "JAR";
					int index = aspectModel.removeAspectsContainer(container);
					try {
						aspectModel.addAspectsContainer(index, path);
					} catch (NoAspectsInContainerException e) {
						MessageDialog.openError(getViewSite().getShell(),
								"Error", contStr + " " + path
								+ " contains no aspects.");
					} catch (IOException e) {
						MessageDialog.openError(getViewSite().getShell(), "Error",
								"There are some I/O errors while loading " + path + ".");
					}
				}else{
					reload.setEnabled(false);
					return;
				}
			}
		};
		reload.setToolTipText("Reload selected folder/JAR");
		reload.setEnabled(false);
		reload.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_REFRESH));

		down = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection) aspViewer
						.getSelection()).getFirstElement();
				if (selected instanceof AspectsContainer) {
					boolean moved = aspectModel.moveContainerDown((AspectsContainer) selected);
					if(!moved){
						down.setEnabled(false);
					}
				} else {
					down.setEnabled(false);
					return;
				}
			}
		};
		down.setToolTipText("Move folder/JAR down the list");
		down.setEnabled(false);
		down.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_ARROW_DOWN));

		up = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection) aspViewer
						.getSelection()).getFirstElement();
				if (selected instanceof AspectsContainer) {
					boolean moved = aspectModel.moveContainerUp((AspectsContainer) selected);
					if(!moved){
						up.setEnabled(false);
					}
				} else {
					up.setEnabled(false);
					return;
				}
			}
		};
		up.setToolTipText("Move folder/JAR up the list");
		up.setEnabled(false);
		up.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_ARROW_UP));

		options = new Action() {
			public void run() {
				
			}
		};
		options.setToolTipText("Options...");
		options.setEnabled(true);
		options.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_OPTIONS));

		reset = new Action() {
			public void run() {
				
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

	private static void loadImages(){
		folderImage = Activator.getImageDescriptor(Activator.IMG_FOLDER).createImage();
		jarImage = Activator.getImageDescriptor(Activator.IMG_JAR).createImage();
		aspectImage = Activator.getImageDescriptor(Activator.IMG_ASPECT).createImage();
		ruleImage = Activator.getImageDescriptor(Activator.IMG_RULE).createImage();
		packageImage = Activator.getImageDescriptor(Activator.IMG_PACKAGE).createImage();
		classImage = Activator.getImageDescriptor(Activator.IMG_CLASS).createImage();
		methodImage = Activator.getImageDescriptor(Activator.IMG_METHOD).createImage();
		sourceImage = Activator.getImageDescriptor(Activator.IMG_SOURCE).createImage();
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
		ColumnViewerToolTipSupport.enableFor(aspViewer);
		aspViewer.setInput(aspectModel = new AspectsModel());
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
		findButton.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
//				new Job("Finding joinpoints") {
//					{
//						setUser(true);
//					}
//					
//					@Override
//					protected IStatus run(IProgressMonitor monitor) {
//						// TODO Auto-generated method stub
//						return null;
//					}
//				};
				if(targetProject == null ||
						aspectModel.isEmpty()){
					findButton.setEnabled(false);
					return;
				}
				List<Aspect> aspects = new LinkedList<Aspect>();
				for(AspectsContainer container : aspectModel.getAspectsContainers()){
					aspects.addAll(container.getAllAspects());
				}
				IPath outputPath = null;
				try {
					outputPath = targetProject.getOutputLocation();
				} catch (JavaModelException e1) {
				}
				IPath projPath = targetProject.getProject().getLocation();
				
				//Format \project_name\path_to_output_dir
				String outputStr = outputPath.toString();
				String outputDir = outputStr.substring(outputStr.indexOf('/', 1));
				String outputStrFull = projPath.toString() + outputDir;
				// TODO lock target project
				List<File> classFiles = AspectsModel.findClassFiles(new File(outputStrFull));
				targProjLoaded = new LinkedList<ClassNode>();
				for(File file : classFiles){
					ClassReader cr = null;
					try {
						cr = new ClassReader(new FileInputStream(file));
					} catch (FileNotFoundException e1) {
						MessageDialog.openError(getViewSite().getShell(), "Error",
								"Project loading has failed. Class file " + 
								file.getAbsolutePath() + " has not been found.");
						return;
					} catch (IOException e1) {
						MessageDialog.openError(getViewSite().getShell(), "Error",
								"Sorry, project loading has failed. There are some errors " +
								"while loading " + file.getAbsolutePath() + ".");
						return;
					}
					ClassNode cn = new ClassNode();
					// TODO think about performance, remove EXPAND_FRAMES
					cr.accept(cn, ClassReader.EXPAND_FRAMES);
					targProjLoaded.add(cn);
				}
				List<Joinpoint> joinpoints = weaver.findJoinpoints(targProjLoaded, aspects);
				if(joinpoints.isEmpty()){
					MessageDialog.openInformation(getViewSite().getShell(), "Information",
							"No joinpoints found.");
				}else{
					joinpViewer.setInput(joinpoints);
					joinpViewer.expandAll();
					joinpViewer.setCheckedElements(joinpoints.toArray());
					tabFolder.setSelection(joinpointTab);
					updateToolBar();
					weaveButton.setEnabled(true);
				}
			}
		});
		
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
		joinpViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object obj = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if(obj instanceof AbstractInsnNode){
					AbstractInsnNode line = ((AbstractInsnNode) obj).getPrevious();
					while(!(line instanceof LineNumberNode)){
						line = line.getPrevious();
					}
					int lineNumber = ((LineNumberNode) line).line;
					ClassNode cn = null;
					for(Joinpoint jp : (List<Joinpoint>) joinpViewer.getInput()){
						if(jp.getInstr() == obj){
							cn = jp.getClazz();
							break;
						}
					}
					String sourceFile = cn.sourceFile;
					String packageStr = null;
					int l = cn.name.lastIndexOf('/');
					if(l < 0){
						packageStr = "";
					}else{
						packageStr = cn.name.substring(0, l + 1);
					}
					IJavaElement el = null;
					try {
						el = targetProject.findElement(new Path(packageStr
								+ sourceFile));
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						ITextEditor editorPart = (ITextEditor) JavaUI.openInEditor(el);
						IDocument doc = editorPart.getDocumentProvider().getDocument(editorPart.getEditorInput());
						editorPart.getSelectionProvider().setSelection(new TextSelection(
								doc.getLineOffset(lineNumber - 1), doc.getLineLength(lineNumber - 1)));					
					} catch (PartInitException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
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
		weaveButton.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<Joinpoint> input = (List<Joinpoint>) joinpViewer.getInput();
				List<Joinpoint> checkedJoinpoints = new LinkedList<Joinpoint>();
				for(Joinpoint joinpoint : input){
					if(joinpViewer.getChecked(joinpoint)){
						checkedJoinpoints.add(joinpoint);
					}
				}
				if(checkedJoinpoints.isEmpty()){
					MessageDialog.openWarning(getViewSite().getShell(), "Warning", 
							"There are no joinpoints selected.");
					return;
				}
				// If there are 2 checked INSTEAD joinpoints with 
				// the same target instruction, than show warning dialog.
				List<Joinpoint> checkedInsteadJps = new ArrayList<Joinpoint>();
				for(Joinpoint jp : checkedJoinpoints){
					if(jp.getClause().getContext() == Context.INSTEAD){
						checkedInsteadJps.add(jp);
					}
				}
				for(int i = 0; i < checkedInsteadJps.size(); i++){
					Joinpoint jp = checkedInsteadJps.get(i);
					for(int j = i + 1; j < checkedInsteadJps.size(); j++){
						if(jp.getInstr() == checkedInsteadJps.get(j).getInstr()){
							MessageDialog.openWarning(getViewSite().getShell(), "Warning", 
									"There are 2 checked INSTEAD joinpoints with " +
									"the same target instruction.");
							joinpViewer.setSelection(new StructuredSelection(jp.getInstr()));
							return;
						}
					}
				}
				String targProjPath = targetProject.getProject().getLocation().toString();
				folderChooseDialog.setFilterPath(targProjPath);
				folderChooseDialog.setMessage("Choose folder for weaved project saving.");
				String str = folderChooseDialog.open();
				if(str != null){
					weaver.weaveJoinpoints(checkedJoinpoints);
					weaveButton.setEnabled(false);
					for(ClassNode cn : targProjLoaded){
						ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
						LocalVariablesAdapter lva = new LocalVariablesAdapter(cw);
						cn.accept(lva);
						byte[] b = cw.toByteArray();
						File classFile = new File(str + "/" + cn.name + ".class");
						classFile.getParentFile().mkdirs();
						try {
							FileOutputStream out = new FileOutputStream(classFile);
							out.write(b);
							out.close();
						} catch (IOException e1) {
							MessageDialog.openError(getViewSite().getShell(), "Error",
									"There are some I/O errors while saving the project.");
							return;
						}
					}
					// TODO write joinpoints.xml
					try {
						FileWriter fw = new FileWriter(str + "/" + "classpath.txt");
						StringBuilder classpath = new StringBuilder();
						classpath.append(str + ';');
						for(AspectsContainer container : aspectModel.getAspectsContainers()){
							if(container.isFolder()){
								Set<String> cps = new HashSet<String>();
								for(AspectsPackage package1 : container.getPackages()){
									String s = package1.getPath().substring(1)
										.replace(package1.getName().replace('.', '\\'), "");
									String cp = container.getPath() + s;
									cps.add(cp);
								}
								for(String s : cps){
									classpath.append(s + ';');
								}
							}else{
								classpath.append(container.getPath() + ';');
							}
						}
						fw.write(classpath.toString());
						fw.close();
					} catch (IOException e1) {
					}
					MessageDialog.openInformation(getViewSite().getShell(), "Information", 
							"The aspects are weaved successfully.\n" +
							"The weaved project is saved in " + str + ".");
				}		
			}
		});
				
		joinpointTab.setControl(joinTabComp);
	}
	
	private void initAspectModelListener(){
		aspectModelListener = new AspectsModelListener(){

			public void aspectsContainerAdded(AspectsContainer container) {
				updateToolBar();
				updateFindButton();
			}
			
			public void aspectsContainerAdded(int index, AspectsContainer container) {
				updateToolBar();
				updateFindButton();
			}

			public void aspectsContainerRemoved(AspectsContainer container, int index) {
				if(aspectModel.isEmpty()){
					findButton.setEnabled(false);
				}
			}
			
			public void containerMovedDown(AspectsContainer container) {
				
			}

			public void containerMovedUp(AspectsContainer container) {
				
			}
			
		};
		aspectModel.addListener(aspectModelListener);
	}
	
	private void initProjectsChangeListener(){
		projectsChangeListener = new IResourceChangeListener(){

			public void resourceChanged(IResourceChangeEvent event) {
				if(event.getType() == IResourceChangeEvent.PRE_CLOSE
						|| event.getType() == IResourceChangeEvent.PRE_DELETE){
					if(event.getResource() == targetProject.getProject()){
						getViewSite().getShell().getDisplay().syncExec(new Runnable(){
							public void run() {
								projectLabel1.setText(NO_SPECIFIED);
								projectLabel2.setText(NO_SPECIFIED);
								findButton.setEnabled(false);
								targetProject = null;
								joinpViewer.setInput(null);
								weaveButton.setEnabled(false);
//								tabFolder.setSelection(aspectTab);
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
			addFolder.setEnabled(true);
			addJars.setEnabled(true);
			options.setEnabled(true);
			IStructuredSelection selection = (IStructuredSelection) aspViewer.getSelection();
			if(selection.isEmpty()){
				reload.setEnabled(false);
				remove.setEnabled(false);
				up.setEnabled(false);
				down.setEnabled(false);
				return;
			}
			Object selected = selection.getFirstElement();
			if(selected instanceof AspectsContainer){
				reload.setEnabled(true);
				remove.setEnabled(true);
				int index = aspectModel.indexOf((AspectsContainer) selected);
				if(index > 0){
					up.setEnabled(true);
				}else{
					up.setEnabled(false);
				}
				if(index < aspectModel.getContainersCount() - 1){
					down.setEnabled(true);
				}else{
					down.setEnabled(false);
					return;
				}
			}else{
				reload.setEnabled(false);
				remove.setEnabled(false);
				up.setEnabled(false);
				down.setEnabled(false);
			}
		}else if(currentTab == joinpointTab){
			addFolder.setEnabled(false);
			addJars.setEnabled(false);
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
	
	// Special utility-class used in JoinpointsContentProvider. 
	private static class IdentityLinkedList<E> extends LinkedList<E>{
		@Override
		public boolean contains(Object arg0) {
			for(E element : this){
				if(element == arg0){
					return true;
				}
			}
			return false;
		}
		
		public boolean add(E arg0) {
			if(contains(arg0)){
				return false;
			}else{
				super.add(arg0);
				return true;
			}
		}
	}
	
	private static class RuleCondClausePair{
		private AspectRule rule;
		private AbstractConditionClause condClause;
		
		RuleCondClausePair(AspectRule rule, AbstractConditionClause condClause) {
			this.rule = rule;
			this.condClause = condClause;
		}

		AspectRule getRule() {
			return rule;
		}

		AbstractConditionClause getCondClause() {
			return condClause;
		}
		
	}
}