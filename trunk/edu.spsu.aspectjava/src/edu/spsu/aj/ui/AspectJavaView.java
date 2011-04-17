package edu.spsu.aj.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;

import edu.spsu.aj.weaver.AbstractConditionClause.Context;
import edu.spsu.aj.weaver.Aspect;
import edu.spsu.aj.weaver.BadArgsInRuleExc;
import edu.spsu.aj.weaver.BadCondClauseExc;
import edu.spsu.aj.weaver.Joinpoint;
import edu.spsu.aj.weaver.LocalVariablesAdapter;
import edu.spsu.aj.weaver.Weaver;
import edu.spsu.aj.ui.AspectsModel.*;


public class AspectJavaView extends ViewPart {
	private TabFolder tabFolder;
	
	private TabItem aspectTab;
	private Label projectLabel1;
	private TreeViewer aspViewer;
	private AspectsModel aspectModel;
	
	private TabItem joinpointTab;
	private Label projectLabel2;
	private ContainerCheckedTreeViewer joinpViewer;
	
	private Action setProjectAction;
	private Action addFolderAction;
	private Action addJarsAction;
	private Action removeAction;
	private Action reloadAction;
	private Action moveDownAction;
	private Action moveUpAction;
//	private Action optionsAction;
	private Action resetAction;
	
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
	private List<File> targProjClassFiles;
	public static final String NO_SPECIFIED = "<no target project specified>";
	// An object to lock target project.
	private Object lock;
	
	private Weaver weaver;
	
//	private Action doubleClickAction;

	/**
	 * The constructor.
	 */
	public AspectJavaView() {
		weaver = new Weaver();
		lock = new Object();
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
		
		createAspectsTab();
		
		createAspectsModelListener();
		
		createJoinpointsTab();
		
		createProjectChangeListener();
						
		// Contribute to action bars.
		IActionBars bars = getViewSite().getActionBars();
//		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
		
		// Create dialogs.
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
	
	private static void loadImages(){
		folderImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		jarImage = JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_JAR);
		aspectImage = Activator.getImageDescriptor(Activator.IMG_ASPECT).createImage();
		ruleImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		packageImage = JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PACKAGE);
		classImage = JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CLASS);
		methodImage = Activator.getImageDescriptor(Activator.IMG_METHOD).createImage();
		sourceImage = JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CUNIT);
		findImage = Activator.getImageDescriptor(Activator.IMG_FIND).createImage();
		weaveImage = Activator.getImageDescriptor(Activator.IMG_WEAVE).createImage();
		projectImage = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
	}
	
	private void createAspectsTab(){
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
				
				private void unlockTargetProject(){
					synchronized (lock) {
						lock.notify();
					}
					projectLabel1.setText(targetProject.getProject().getName());
					projectLabel2.setText(targetProject.getProject().getName());
				}
				
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
					// Fix potential bug
					if(targetProject == null ||
							aspectModel.isEmpty()){
						findButton.setEnabled(false);
						return;
					}
					if(joinpViewer.getInput() != null){
						joinpViewer.setInput(null);
						weaveButton.setEnabled(false);
						resetAction.setEnabled(false);
						unlockTargetProject();
					}
					// Collect all aspects
					List<Aspect> aspects = new LinkedList<Aspect>();
					for(AspectsContainer container : aspectModel.getAspectsContainers()){
						aspects.addAll(container.getAllAspects());
					}
					
					// Lock target project
					new Thread(){
						public void run() {
							try {
								ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
									
									@Override
									public void run(IProgressMonitor monitor) throws CoreException {
										try {
											synchronized (lock) {
												lock.wait();
											}
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}	
									}
								}, targetProject.getProject(), IWorkspace.AVOID_UPDATE, null);
							} catch (CoreException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						};
					}.start();
					projectLabel1.setText(targetProject.getProject().getName() + 
							" (locked)");
					projectLabel2.setText(targetProject.getProject().getName() + 
					" (locked)");
					
					// Collect all target project class files
					IPath outputPath = null;
					try {
						outputPath = targetProject.getOutputLocation();
					} catch (JavaModelException e1) {
					}				
					String outputStr = outputPath.toString();
					// 'outputStr' has format \project_name\path_to_output_dir
					String outputDir = outputStr.substring(outputStr.indexOf('/', 1));
					IPath projPath = targetProject.getProject().getLocation();
					String outputStrFull = projPath.toString() + outputDir;
					targProjClassFiles = AspectsModel.findClassFiles(new File(outputStrFull));
					
					if(targProjClassFiles.isEmpty()){
						MessageDialog.openError(getViewSite().getShell(), "Error", 
								"Target project is not built. Finding is aborted.");
						unlockTargetProject();
						return;
					}
					
					// Load target project
					List<ClassNode> targProjLoaded = new LinkedList<ClassNode>();
					for(File file : targProjClassFiles){
						ClassReader cr = null;
						try {
							cr = new ClassReader(new FileInputStream(file));
						} catch (FileNotFoundException e1) {
							MessageDialog.openError(getViewSite().getShell(), "Error",
									"Project loading is failed. Class file " + 
									file.getAbsolutePath() + " has been deleted outside" +
											" the workbench.");
							unlockTargetProject();
							return;
						} catch (IOException e1) {
							MessageDialog.openInformation(getViewSite().getShell(), "Information",
									"Sorry, there are some I/O errors " +
									"while loading " + file.getAbsolutePath() + ".");
							unlockTargetProject();
							return;
						}
						ClassNode cn = new ClassNode();
						// TODO think about performance, may be remove EXPAND_FRAMES
						cr.accept(cn, ClassReader.EXPAND_FRAMES);
						targProjLoaded.add(cn);
					}
					
					List<Joinpoint> joinpoints = weaver.findJoinpoints(targProjLoaded, aspects);
					if(joinpoints.isEmpty()){
						MessageDialog.openInformation(getViewSite().getShell(), "Information",
								"No joinpoints found.");
						unlockTargetProject();
						return;
					}else{
						joinpViewer.setInput(joinpoints);
						joinpViewer.expandAll();
						joinpViewer.setCheckedElements(joinpoints.toArray());
						tabFolder.setSelection(joinpointTab);
						updateToolBar();
						weaveButton.setEnabled(true);
						resetAction.setEnabled(true);
					}
				}
			});
			
			aspectTab.setControl(aspTabComp);
		}

	private void createAspectsModelListener(){
		aspectModel.addListener(new AspectsModelListener(){
	
			public void aspectsContainerAdded(AspectsContainer container) {
				aspViewer.add(aspectModel, container);
	
				updateToolBar();
				updateFindButton();
			}
			
			public void aspectsContainerAdded(int index, AspectsContainer container) {
				aspViewer.insert(aspectModel, container, index);
				aspViewer.setSelection(new StructuredSelection(container));
				
				updateToolBar();
				updateFindButton();
			}
	
			public void aspectsContainerRemoved(AspectsContainer container, int index) {
				aspViewer.remove(aspectModel, index);
				if (! aspectModel.isEmpty()) {
					if (index > (aspectModel.getContainersCount() + 1) / 2 - 1) {
						aspViewer.setSelection(new StructuredSelection(aspectModel
								.getLastContainer()));
					} else {
						aspViewer.setSelection(new StructuredSelection(aspectModel
								.getFirstContainer()));
					}
				}
				
				if(aspectModel.isEmpty()){
					findButton.setEnabled(false);
				}
			}
			
			public void containerMovedDown(AspectsContainer container) {
				int oldIndex = aspectModel.indexOf(container) - 1;
				TreePath[] treePaths = aspViewer.getExpandedTreePaths();
				aspViewer.remove(aspectModel, oldIndex);
				if (oldIndex == aspectModel.getContainersCount() - 1) {
					aspViewer.add(aspectModel, container);
					aspViewer.setExpandedTreePaths(treePaths);
				} else {
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
			
		});
	}

	private void createJoinpointsTab(){
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
				
				// Collect checked joinpoints
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
				
				// Get folder from user and weave joinpoints
				String targProjPath = targetProject.getProject().getLocation().toString();
				folderChooseDialog.setFilterPath(targProjPath);
				folderChooseDialog.setMessage("Choose folder for weaved project saving.");
				String str = folderChooseDialog.open();
				if(str != null){
					// Load target project
					List<ClassNode> targProjLoaded = new LinkedList<ClassNode>();
					for(File file : targProjClassFiles){
						ClassReader cr = null;
						try {
							cr = new ClassReader(new FileInputStream(file));
						} catch (FileNotFoundException e1) {
							MessageDialog.openError(getViewSite().getShell(), "Error",
									"Project loading is failed. Class file " + 
									file.getAbsolutePath() + " has been deleted outside" +
											" the workbench. Weaving is aborted.");
							return;
						} catch (IOException e1) {
							MessageDialog.openInformation(getViewSite().getShell(), "Information",
									"Sorry, there are some I/O errors " +
									"while loading " + file.getAbsolutePath() + ". Weaving is aborted.");
							return;
						}
						ClassNode cn = new ClassNode();
						// TODO think about performance, may be remove EXPAND_FRAMES
						cr.accept(cn, ClassReader.EXPAND_FRAMES);
						targProjLoaded.add(cn);
					}
					// weave joinpoints
					weaver.weaveJoinpoints(targProjLoaded, checkedJoinpoints);
					
					// Save weaved project
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
					
					// Write joinpoints.xml
					try {
						Writer w = new OutputStreamWriter(new FileOutputStream(str + "/" + "joinpoints.xml"), "UTF-8");
						w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
						w.write("<weaving destination=\"" + str + "\">\n");
						w.write("<target_project>" + targProjPath + "</target_project>\n");
						w.write("<aspects>\n");
						for(AspectsContainer container : aspectModel.getAspectsContainers()){
							w.write("<aspects_container isFolder=\"" + container.isFolder()
									+ "\">" + container.getPath() + "</aspects_container>\n");
						}
						w.write("</aspects>\n");
						w.write("<joinpoints>\n");
						for(Joinpoint jp : checkedJoinpoints){
							w.write("<joinpoint>\n");
							w.write("<target_class sourceFile=\"" + jp.getClazz().sourceFile + "\">"
									+ jp.getClazz().name.replace('/', '.') + "</target_class>\n");
							String methodName = jp.getMethod().name;
							methodName = methodName.replace("<", "&lt;");
							methodName = methodName.replace(">", "&gt;");
							w.write("<target_method>" + methodName + "</target_method>\n");
							String kind = null;
							String toString = null;
							if(jp.getInstr() instanceof MethodInsnNode){
								kind = "call";
								
								MethodInsnNode mInstr = (MethodInsnNode) jp.getInstr();
								StringBuilder sb = new StringBuilder();
								sb.append(Type.getReturnType(mInstr.desc).getClassName());
								sb.append(" ");
								sb.append(mInstr.owner.replace('/', '.'));
								sb.append('.');
								String methodName1 = mInstr.name;
								methodName1 = methodName1.replace("<", "&lt;");
								methodName1 = methodName1.replace(">", "&gt;");
								sb.append(methodName1);
								sb.append("(");			
								Type[] argTypes = Type.getArgumentTypes(mInstr.desc);
								if(argTypes.length > 0){
									sb.append(argTypes[0].getClassName());
								}
								for(int i = 1; i < argTypes.length; i++){
									sb.append(", " + argTypes[i].getClassName());
								}
								sb.append(")");
								toString = sb.toString();
							}else{
								//TODO USE & ASSIGN kinds 
							}
							AbstractInsnNode line = jp.getInstr().getPrevious();
							while(!(line instanceof LineNumberNode)){
								line = line.getPrevious();
							}
							int lineNumber = ((LineNumberNode)line).line;
							w.write("<target_point kind=\"" + kind + "\" line=\""
									+ lineNumber + "\">" + toString + "</target_point>\n");
							w.write("<action_weaved context=\"" + jp.getClause().getContext()
									+ "\">\n");
	
							// Build string for action
							StringBuilder sb = new StringBuilder();
							String actDesc = jp.getAspectRule().getAction().getDescriptor();
							String retType = Type.getReturnType(actDesc).getClassName();
							sb.append(retType);
							sb.append(" ");
							// This is the difference from action.toString()
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
							sb.append(")");
							w.write("<action aspect=\"" + jp.getAspect().getName()
									+ "\" name=\"" + jp.getAspectRule().getAction().getName()
									+ "\" desc=\"" + jp.getAspectRule().getAction().getDescriptor()
									+ "\">" + sb + "</action>\n");
							w.write("<args_info>" + jp.getClause().getArgsInfo() + "</args_info>\n");
							w.write("</action_weaved>\n");
							w.write("</joinpoint>\n");
						}
						w.write("</joinpoints>\n");
						w.write("</weaving>");
						w.close();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					
					// Write classpath.txt
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

	private void createProjectChangeListener(){
			IResourceChangeListener pCL = new IResourceChangeListener(){
	
				public void resourceChanged(IResourceChangeEvent event) {
					if(targetProject == null){
						return;
					}
					if(event.getType() == IResourceChangeEvent.PRE_CLOSE
							|| event.getType() == IResourceChangeEvent.PRE_DELETE){
						if(event.getResource() == targetProject.getProject()){
							getViewSite().getShell().getDisplay().syncExec(new Runnable(){
								public void run() {
									synchronized (lock) {
										lock.notify();
									}
									projectLabel1.setText(NO_SPECIFIED);
									projectLabel2.setText(NO_SPECIFIED);
									findButton.setEnabled(false);
									targetProject = null;
									joinpViewer.setInput(null);
									weaveButton.setEnabled(false);
									resetAction.setEnabled(false);
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
				addResourceChangeListener(pCL);
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
	
	private void openErrorDialogWithSave(String message, List<BadAspect> badAspects){
		MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK,
				"(click 'Details' button for detailed information)", null);
		for(BadAspect badAspect : badAspects){
			Exception e = badAspect.getException();
			String excDesc = null;
			if(e instanceof BadArgsInRuleExc){
				BadArgsInRuleExc bE = (BadArgsInRuleExc) e;
				excDesc = bE.getClause() + " -> " + bE.getRule().getAction()
					+ " (" + e.getMessage() + ")";
			}else if(e instanceof BadCondClauseExc){
				BadCondClauseExc bE = (BadCondClauseExc) e;
				excDesc = bE.getCondClause() + " ("
					+ bE.getMessage() + ")";
			}
			MultiStatus status2 = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK,
					badAspect.getClassFilePath() + ':', null);
			Status status3 = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					excDesc);
			status2.add(status3);
			status.add(status2);
		}
		ErrorDialogWithSave errorDialogWithSave = new ErrorDialogWithSave(getViewSite().getShell(), message, status);
		errorDialogWithSave.open();
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		// Make actions
		setProjectAction = new Action(){
			public void run(){
				int returnedCode = projectDialog.open();
				if(returnedCode == ChooseProjectDialog.OK){
					if(projectDialog.getChosen() != null){
						if(targetProject != null && 
								targetProject.getProject() == projectDialog.getChosen().getProject()){
							return;
						}
						targetProject = projectDialog.getChosen();
						synchronized (lock) {
							lock.notify();
						}
						projectLabel1.setText(targetProject.getProject().getName());
						projectLabel2.setText(targetProject.getProject().getName());
						updateFindButton();
						joinpViewer.setInput(null);
						weaveButton.setEnabled(false);
						resetAction.setEnabled(false);
						
					}
				}
			}
		};
		setProjectAction.setToolTipText("Choose target project for weaving");
		setProjectAction.setEnabled(true);
		setProjectAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT));
		
		addFolderAction = new Action() {
			
			public void run() {
				folderChooseDialog.setMessage("Choose aspects folder.");
				folderChooseDialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
				String str = folderChooseDialog.open();
				if (str != null) {
					try {
						List<BadAspect> badAspects =  aspectModel.addAspectsContainer(str);
						if(badAspects == null){
							MessageDialog.openInformation(getViewSite().getShell(),
							"Information", "Folder " + str
							+ " is already added.");
							return;
						}
						if(! badAspects.isEmpty()){
							openErrorDialogWithSave("Some aspects are" +
									" rejected due to bad format.", badAspects);
						}
					} catch (IOException e) {
						MessageDialog.openError(getViewSite()
								.getShell(), "Error",
								"There are some I/O errors while loading " + str + ".");
					} catch (NoAspectsInContainerException e) {
						if(e.getBadAspects().isEmpty()){
							MessageDialog.openInformation(getViewSite().getShell(),
							"Information", "Folder " + str
							+ " contains no aspects.");
						}else{
							openErrorDialogWithSave("Folder " + str
									+ " contains aspects, but they were" +
									" rejected due to bad format.", e.getBadAspects());
						}
					}
				}
			}
		};
		addFolderAction.setToolTipText("Add aspects folder");
		addFolderAction.setEnabled(true);
		ImageDescriptor add = Activator.getImageDescriptor(Activator.IMG_ADD_DEC);
		DecorationOverlayIcon d = new DecorationOverlayIcon(folderImage, add, IDecoration.BOTTOM_LEFT);
		addFolderAction.setImageDescriptor(d);
		
		addJarsAction = new Action() {
	
			public void run() {
				String str = jarsChooseDialog.open();
				String[] fileNames = jarsChooseDialog.getFileNames();
				if (str != null) {
					String parent = new File(str).getParent();
					for (int i = 0; i < fileNames.length; i++) {
						String path = parent + '\\' + fileNames[i];
						try {	
							List<BadAspect> badAspects =  aspectModel.addAspectsContainer(path);
							if(badAspects == null){
								MessageDialog.openInformation(getViewSite().getShell(),
								"Information", "JAR " + path
								+ " is already added.");
								return;
							}
							if(! badAspects.isEmpty()){
								openErrorDialogWithSave("Some aspects are" +
										" rejected due to bad format.", badAspects);
							}
						} catch (IOException e) {
							MessageDialog.openError(getViewSite().getShell(), "Error",
									"There are some I/O errors while loading " + path + ".");
						} catch (NoAspectsInContainerException e) {
							if(e.getBadAspects().isEmpty()){
								MessageDialog.openInformation(getViewSite().getShell(),
								"Information", "JAR " + str
								+ " contains no aspects.");
							}else{
								openErrorDialogWithSave("JAR " + str
										+ " contains aspects, but they were" +
										" rejected due to bad format.", e.getBadAspects());
							}
						}
					}
				}
			}
		};
		addJarsAction.setToolTipText("Add aspects JARs");
		addJarsAction.setEnabled(true);
		DecorationOverlayIcon d1 = new DecorationOverlayIcon(jarImage, add, IDecoration.BOTTOM_LEFT);
		addJarsAction.setImageDescriptor(d1);

		removeAction = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection)aspViewer.getSelection())
					.getFirstElement();
				if(selected instanceof AspectsContainer){
					aspectModel.removeAspectsContainer((AspectsContainer) selected);
				}else{
					removeAction.setEnabled(false);
					return;
				}
			}
		};
		removeAction.setToolTipText("Remove selected folder/JAR");
		removeAction.setEnabled(false);
		removeAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_ETOOL_DELETE));

		reloadAction = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection)aspViewer.getSelection())
					.getFirstElement();
				if(selected instanceof AspectsContainer){
					AspectsContainer container = (AspectsContainer) selected;
					String path = container.getPath();
					int index = aspectModel.removeAspectsContainer(container);
					try {
						List<BadAspect> badAspects = aspectModel.addAspectsContainer(index, path);
						if(! badAspects.isEmpty()){
							openErrorDialogWithSave("Some aspects are" +
									" rejected due to bad format.", badAspects);
						}
					} catch (NoAspectsInContainerException e) {
						String contStr = container.isFolder()? "Folder" : "JAR";
						if(e.getBadAspects().isEmpty()){
							MessageDialog.openInformation(getViewSite().getShell(),
									"Information", contStr + " " + path
									+ " contains no aspects.");
						}else{
							openErrorDialogWithSave(contStr + " " + path
									+ " contains aspects, but they were" +
									" rejected due to bad format.", e.getBadAspects());
						}	
					} catch (IOException e) {
						MessageDialog.openError(getViewSite().getShell(), "Error",
								"There are some I/O errors while loading " + path + ".");
					}
				}else{
					reloadAction.setEnabled(false);
					return;
				}
			}
		};
		reloadAction.setToolTipText("Reload selected folder/JAR");
		reloadAction.setEnabled(false);
		reloadAction.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_REFRESH));

		moveDownAction = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection) aspViewer
						.getSelection()).getFirstElement();
				if (selected instanceof AspectsContainer) {
					boolean moved = aspectModel.moveContainerDown((AspectsContainer) selected);
					if(!moved){
						moveDownAction.setEnabled(false);
					}
				} else {
					moveDownAction.setEnabled(false);
					return;
				}
			}
		};
		moveDownAction.setToolTipText("Move folder/JAR down the list");
		moveDownAction.setEnabled(false);
		moveDownAction.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_ARROW_DOWN));

		moveUpAction = new Action() {
			public void run() {
				Object selected = ((IStructuredSelection) aspViewer
						.getSelection()).getFirstElement();
				if (selected instanceof AspectsContainer) {
					boolean moved = aspectModel.moveContainerUp((AspectsContainer) selected);
					if(!moved){
						moveUpAction.setEnabled(false);
					}
				} else {
					moveUpAction.setEnabled(false);
					return;
				}
			}
		};
		moveUpAction.setToolTipText("Move folder/JAR up the list");
		moveUpAction.setEnabled(false);
		moveUpAction.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_ARROW_UP));

//		optionsAction = new Action() {
//			public void run() {
//				
//			}
//		};
//		optionsAction.setToolTipText("Options...");
//		optionsAction.setEnabled(true);
//		optionsAction.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_OPTIONS));

		resetAction = new Action() {
			public void run() {
				// Fix potential bug
				if(joinpViewer.getInput() == null){
					setEnabled(false);
					return;
				}
				boolean ok = MessageDialog.openConfirm(getViewSite().getShell(), "Confirm", 
						"Do you really want to reset the joinpoints and " +
						"unlock target project?");
				if(ok){
					joinpViewer.setInput(null);
					weaveButton.setEnabled(false);
					resetAction.setEnabled(false);
					// Unlock target project
					synchronized (lock) {
						lock.notify();
					}
					projectLabel1.setText(targetProject.getProject().getName());
					projectLabel2.setText(targetProject.getProject().getName());
				}
			}
		};
		resetAction.setToolTipText("Reset joinpoints and unlock target project");
		resetAction.setEnabled(false);
		resetAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_ETOOL_CLEAR));

		// doubleClickAction = new Action() {
		// public void run() {
		// ISelection selection = viewer.getSelection();
		// Object obj = ((IStructuredSelection)selection).getFirstElement();
		// showMessage("Double-click detected on "+obj.toString());
		// }
		// };
		
		// Add actions
		Separator separator = new Separator();
		manager.add(setProjectAction);
		manager.add(separator);
		manager.add(addFolderAction);
		manager.add(addJarsAction);
		manager.add(separator);
		manager.add(moveDownAction);
		manager.add(moveUpAction);
		manager.add(removeAction);
		manager.add(reloadAction);
		manager.add(separator);
		manager.add(resetAction);
//		manager.add(separator);
//		manager.add(optionsAction);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		tabFolder.setFocus();
	}
	
	public void dispose(){
		aspectImage.dispose();
		methodImage.dispose();
		findImage.dispose();
		weaveImage.dispose();
		synchronized (lock) {
			lock.notify();
		}
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

	private void updateToolBar(){
		int currentTabIndex = tabFolder.getSelectionIndex();
		TabItem currentTab = tabFolder.getItem(currentTabIndex);
		if(currentTab == aspectTab){
			addFolderAction.setEnabled(true);
			addJarsAction.setEnabled(true);
//			optionsAction.setEnabled(true);
			IStructuredSelection selection = (IStructuredSelection) aspViewer.getSelection();
			if(selection.isEmpty()){
				reloadAction.setEnabled(false);
				removeAction.setEnabled(false);
				moveUpAction.setEnabled(false);
				moveDownAction.setEnabled(false);
				return;
			}
			Object selected = selection.getFirstElement();
			if(selected instanceof AspectsContainer){
				reloadAction.setEnabled(true);
				removeAction.setEnabled(true);
				int index = aspectModel.indexOf((AspectsContainer) selected);
				if(index > 0){
					moveUpAction.setEnabled(true);
				}else{
					moveUpAction.setEnabled(false);
				}
				if(index < aspectModel.getContainersCount() - 1){
					moveDownAction.setEnabled(true);
				}else{
					moveDownAction.setEnabled(false);
					return;
				}
			}else{
				reloadAction.setEnabled(false);
				removeAction.setEnabled(false);
				moveUpAction.setEnabled(false);
				moveDownAction.setEnabled(false);
			}
		}else if(currentTab == joinpointTab){
			addFolderAction.setEnabled(false);
			addJarsAction.setEnabled(false);
			removeAction.setEnabled(false);
			reloadAction.setEnabled(false);
			moveUpAction.setEnabled(false);
			moveDownAction.setEnabled(false);
//			optionsAction.setEnabled(false);
		}
	}
	
	private void updateFindButton(){
		if(targetProject != null && !aspectModel.isEmpty()){
			findButton.setEnabled(true);
		}
	}
	
}