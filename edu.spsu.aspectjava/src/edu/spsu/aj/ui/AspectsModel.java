package edu.spsu.aj.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.spsu.aj.weaver.Aspect;
import edu.spsu.aj.weaver.AspectRule;
import edu.spsu.aj.weaver.BadArgsInRuleExc;
import edu.spsu.aj.weaver.BadCondClauseExc;
import edu.spsu.aj.weaver.RuleAction;
import edu.spsu.aj.weaver.RuleCondition;

class AspectsModel{
	private ArrayList<AspectsContainer> aspectsContainers;	
	private List<AspectsModelListener> listeners;
		
	AspectsModel(){
		aspectsContainers = new ArrayList<AspectsContainer>();
		listeners = new LinkedList<AspectsModelListener>();
	}
	
	List<AspectsContainer> getAspectsContainers() {
		return aspectsContainers;
	}
	
	AspectsContainer getFirstContainer(){
		if(isEmpty()){
			return null;
		}
		return aspectsContainers.get(0);
	}
	
	AspectsContainer getLastContainer(){
		if(isEmpty()){
			return null;
		}
		return aspectsContainers.get(getContainersCount() - 1);
	}
	
	int indexOf(AspectsContainer container) {
		return aspectsContainers.indexOf(container);
	}
	
	int getContainersCount(){
		return aspectsContainers.size();
	}
	
	boolean isEmpty(){
		return getContainersCount() == 0;
	}
	
	void addListener(AspectsModelListener l){
		listeners.add(l);
	}
	
	void removeListener(AspectsModelListener l){ 
		listeners.remove(l);
	}
	
	// Return list of bad-formed aspects (also empty), null - if model already contains
	// the container.
	List<BadAspect> addAspectsContainer(String containerPath) throws NoAspectsInContainerException, IOException{
		if(containsAspectsContainer(containerPath)){
			return null;
		}
		List<BadAspect> badAspects = new LinkedList<BadAspect>();
		AspectsContainer container = parseAspectsContainer(containerPath, badAspects);
		if(container != null){
			aspectsContainers.add(container);
		}else{
			throw new NoAspectsInContainerException(containerPath, badAspects);
		}

		for(AspectsModelListener listener:listeners){
			listener.aspectsContainerAdded(container);
		}
		
		return badAspects;
	}
	
	
	// Return list of bad-formed aspects (also empty), null - if model already contains
	// the container. 
	List<BadAspect> addAspectsContainer(int index, String containerPath) throws NoAspectsInContainerException, IOException {
		if(containsAspectsContainer(containerPath)){
			return null;
		}
		List<BadAspect> badAspects = new LinkedList<BadAspect>();
		AspectsContainer container = parseAspectsContainer(containerPath, badAspects);
		if(container != null){
			aspectsContainers.add(index, container);
		}else{
			throw new NoAspectsInContainerException(containerPath, badAspects);
		}
		for(AspectsModelListener listener:listeners){
			listener.aspectsContainerAdded(index, container);
		}		
		return badAspects;
	}
	
	// Return index of removed container.
	int removeAspectsContainer(AspectsContainer container) {
		int index = indexOf(container);
		boolean removed = aspectsContainers.remove(container);
		
		if(removed){
			for(AspectsModelListener listener:listeners){
				listener.aspectsContainerRemoved(container, index);
			}
		}
				
		return index;
	}
	
	boolean moveContainerDown(AspectsContainer container) {
		int index = indexOf(container);
		if(index == -1){
			return false;
		}
		if(index == getContainersCount() - 1){
			return false;
		}
		aspectsContainers.set(index, aspectsContainers.get(index + 1));
		aspectsContainers.set(index + 1, container);
		
		for(AspectsModelListener listener:listeners){
			listener.containerMovedDown(container);
		}
		
		return true;
	}
	
	boolean moveContainerUp(AspectsContainer container) {
		int index = indexOf(container);
		if(index == -1){
			return false;
		}
		if(index == 0){
			return false;
		}
		aspectsContainers.set(index, aspectsContainers.get(index - 1));
		aspectsContainers.set(index - 1, container);
		
		for(AspectsModelListener listener:listeners){
			listener.containerMovedUp(container);
		}
		
		return true;
	}
	
	private boolean containsAspectsContainer(String containerPath) {
		for(AspectsContainer container:aspectsContainers){
			if(container.getPath().equals(containerPath)){
				return true;
			}
		}
		return false;
	}

	private static FileFilter fileFilter1 = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			if(pathname.isDirectory()){
				return true;
			}
			if(pathname.getName().endsWith(".class")){
				return true;
			}
			return false;
		}
	};
	// Finds all class files in specified directory and all its subdirectories.
	public static List<File> findClassFiles(File directory){
		List<File> classFiles = new LinkedList<File>();
		File[] inFiles = directory.listFiles(fileFilter1);
		for(File file : inFiles) {
			if(file.isFile()){
				classFiles.add(file);
			}else{
				classFiles.addAll(findClassFiles(file));
			}
		}
		return classFiles;
	}

	private AspectsContainer parseAspectsContainer(String containerPath, 
			List<BadAspect> badAspects) throws IOException{
		AspectsContainer container = null;
		if(containerPath.endsWith(".jar")){
			JarFile jarFile = new JarFile(containerPath);
			Enumeration<JarEntry> jarEntries = jarFile.entries();
			container = new AspectsContainer(containerPath, false);
			while (jarEntries.hasMoreElements()) {
				JarEntry entry = jarEntries.nextElement();
				if (entry.toString().endsWith(".class")) {
					try {
						Aspect aspect = parseAspectClass(jarFile.getInputStream(entry));
						if(aspect != null){
							String name = entry.getName();
							String packagePath = null;
							int l1 = name.lastIndexOf('/');
							if(l1 < 0){
								packagePath = "/";
							}else{
								packagePath = '/' + name.substring(0, name.lastIndexOf('/'));
							}
							// package.class format
							String aspectName = aspect.getName();
							String aspectPackage = null;
							int l = aspectName.lastIndexOf('.');
							if(l < 0){
								aspectPackage = AspectsPackage.DEFAULT_PACKAGE;
							}else{
								aspectPackage = aspectName.substring(0, l);
							}							
							AspectsPackage package1 = container.addPackage(aspectPackage, packagePath);
							package1.addAspect(aspect);
						}
					} catch (BadArgsInRuleExc e) {
						badAspects.add(new BadAspect(containerPath
								 + "/" + entry.getName(), e));
					} catch (BadCondClauseExc e) {
						badAspects.add(new BadAspect(containerPath
								 + "/" + entry.getName(), e));
					}
				}
			}
		}else{
			container = new AspectsContainer(containerPath, true);
			List<File> classFiles = findClassFiles(new File(containerPath));
			for(File file : classFiles){
				try {
					Aspect aspect = parseAspectClass(new FileInputStream(file));
					if(aspect != null){
						String parent = file.getParent();
						String packagePath = '.' + parent.replace(container
								.getPath(), "");
						// package.class format
						String aspectName = aspect.getName();
						String aspectPackage = null;
						int l = aspectName.lastIndexOf('.');
						if(l < 0){
							aspectPackage = AspectsPackage.DEFAULT_PACKAGE;
						}else{
							aspectPackage = aspectName.substring(0, l);
						}
						AspectsPackage package1 = container.addPackage(aspectPackage, packagePath);
						package1.addAspect(aspect);
					}
				} catch (BadArgsInRuleExc e) {
					badAspects.add(new BadAspect(file.toString(), e));
				} catch (BadCondClauseExc e) {
					badAspects.add(new BadAspect(file.toString(), e));
				}		
			}
		}
		if(container.isEmpty()){
			return null;
		}else{
			return container;
		}
	}
	
	private Aspect parseAspectClass(InputStream inputStream) throws 
			IOException, BadArgsInRuleExc, BadCondClauseExc{
		// Class-adapter with all default implementations (returning null or doing nothing).
		// For convenience aspect & exception fields are added.
		class ClassAdapter implements ClassVisitor{
			protected Aspect aspect = null;
			protected Exception exception = null;
			
			Aspect getAspect(){
				return aspect;
			}
			
			Exception getException(){
				return exception;
			}

			@Override
			public void visit(int version, int access, String name,
					String signature, String superName, String[] interfaces) {			
			}

			@Override
			public void visitSource(String source, String debug) {			
			}

			@Override
			public void visitOuterClass(String owner, String name, String desc) {			
			}

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				return null;
			}

			@Override
			public void visitAttribute(Attribute attr) {			
			}

			@Override
			public void visitInnerClass(String name, String outerName,
					String innerName, int access) {			
			}

			@Override
			public FieldVisitor visitField(int access, String name, String desc,
					String signature, Object value) {
				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String desc,
					String signature, String[] exceptions) {
				return null;
			}

			@Override
			public void visitEnd() {			
			}
			
		}
	
		// Class-adapter with all default implementations (returning null or doing nothing).
		class MethodAdapter implements MethodVisitor{

			@Override
			public AnnotationVisitor visitAnnotationDefault() {
				return null;
			}

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				return null;
			}

			@Override
			public AnnotationVisitor visitParameterAnnotation(int parameter,
					String desc, boolean visible) {
				return null;
			}

			@Override
			public void visitAttribute(Attribute attr) {			
			}

			@Override
			public void visitCode() {			
			}

			@Override
			public void visitFrame(int type, int nLocal, Object[] local,
					int nStack, Object[] stack) {			
			}

			@Override
			public void visitInsn(int opcode) {			
			}

			@Override
			public void visitIntInsn(int opcode, int operand) {			
			}

			@Override
			public void visitVarInsn(int opcode, int var) {			
			}

			@Override
			public void visitTypeInsn(int opcode, String type) {			
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name,
					String desc) {			
			}

			@Override
			public void visitMethodInsn(int opcode, String owner, String name,
					String desc) {			
			}

			@Override
			public void visitJumpInsn(int opcode, Label label) {			
			}

			@Override
			public void visitLabel(Label label) {			
			}

			@Override
			public void visitLdcInsn(Object cst) {			
			}

			@Override
			public void visitIincInsn(int var, int increment) {			
			}

			@Override
			public void visitTableSwitchInsn(int min, int max, Label dflt,
					Label[] labels) {			
			}

			@Override
			public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {			
			}

			@Override
			public void visitMultiANewArrayInsn(String desc, int dims) {			
			}

			@Override
			public void visitTryCatchBlock(Label start, Label end, Label handler,
					String type) {			
			}

			@Override
			public void visitLocalVariable(String name, String desc,
					String signature, Label start, Label end, int index) {			
			}

			@Override
			public void visitLineNumber(int line, Label start) {			
			}

			@Override
			public void visitMaxs(int maxStack, int maxLocals) {			
			}

			@Override
			public void visitEnd() {			
			}	
		}
	
		// Class-adapter with all default implementations (returning null or doing nothing).
		class AnnotationAdapter implements AnnotationVisitor{

			@Override
			public void visit(String name, Object value) {			
			}

			@Override
			public void visitEnum(String name, String desc, String value) {			
			}

			@Override
			public AnnotationVisitor visitAnnotation(String name, String desc) {
				return null;
			}

			@Override
			public AnnotationVisitor visitArray(String name) {
				return null;
			}

			@Override
			public void visitEnd() {			
			}	
		}
		
		ClassReader cr = null;	
		cr = new ClassReader(inputStream);
		ClassAdapter ca;
		cr.accept(ca = new ClassAdapter(){
			private String name; 
			private String description = null; 
			private List<AspectRule> rules = new LinkedList<AspectRule>();

			@Override
			public void visit(int version, int access, String name,
					String signature, String superName, String[] interfaces) {
				this.name = name;
				
			}
			
			@Override
			public void visitEnd() {
				if(!rules.isEmpty()){
					aspect = new Aspect(name, description, rules);
				}
			}

			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				if(!visible){
					return null;
				}
				if(desc.equals("Ledu/spsu/aj/AspectDescription;")){
					return new AnnotationAdapter() {
						
						@Override
						public void visit(String name, Object value) {
							description = (String) value;				
						}
					};
				}else{
					return null;
				}
			}

			@Override
			public MethodVisitor visitMethod(int access, final String name, final String desc,
					String signature, String[] exceptions) {
				if(access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)){
					return new MethodAdapter() {
						private String description = null;  
						private RuleCondition condition = null;
						private RuleAction action = new RuleAction(name, desc);
						
						@Override
						public void visitEnd() {
							if(condition != null){					
								AspectRule rule;
								try {
									rule = new AspectRule(description, condition, action);
									rules.add(rule);
								} catch (BadArgsInRuleExc e) {
									exception = e;
								}											
							}
						}
						
						@Override
						public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
							if(!visible){
								return null;
							}
							if(desc.equals("Ledu/spsu/aj/AspectAction;")){
								return new AnnotationAdapter() {
									
									@Override
									public void visit(String name, Object value) {
										try {
											condition = new RuleCondition((String)value);
										} catch (BadCondClauseExc e) {
											exception = e;
										}
									}
								};
							}else if(desc.equals("Ledu/spsu/aj/AspectDescription;")){
								return new AnnotationAdapter() {

									@Override
									public void visit(String name, Object value) {
										description = (String) value;				
									}
								};
							}else{
								return null;
							}
						}
					};
				}else{
					return null;
				}
			}
			
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		if(ca.getException() != null){
			Exception exception = ca.getException();
			if(exception instanceof BadArgsInRuleExc){
				throw (BadArgsInRuleExc) exception;
			}else if(exception instanceof BadCondClauseExc){
				throw (BadCondClauseExc) exception;
			}
		}
		return ca.getAspect();
	}
	

	static class AspectsContainer{
		private String path;
		private boolean isFolder;
		private HashMap<String, AspectsPackage> aspectsPackages;
		
		AspectsContainer(String path, boolean isFolder) {
			this.path = path;
			this.isFolder = isFolder;
			this.aspectsPackages = new HashMap<String, AspectsModel.AspectsPackage>();
		}

		boolean isEmpty() {
			return aspectsPackages.isEmpty();
		}

		List<Aspect> getAllAspects() {
			List<Aspect> aspects = new LinkedList<Aspect>();
			for(AspectsPackage package1 : getPackages()){
				aspects.addAll(package1.getAspects());
			}
			return aspects;
		}
		
		Collection<AspectsPackage> getPackages(){
			return aspectsPackages.values();
		}
		
		AspectsPackage addPackage(String name, String packagePath){
			AspectsPackage ap = aspectsPackages.get(packagePath);
			if(ap == null){
				ap = new AspectsPackage(name, packagePath);
				aspectsPackages.put(packagePath, ap);
			}
			return ap;
		}
		
		String getPath(){
			return path;
		}

		boolean isFolder() {
			return isFolder;
		}
		
	}
	static class AspectsPackage{
		private String name;
		static final String DEFAULT_PACKAGE = "(default package)";
		private String path; // relative path to package folder
		private List<Aspect> aspects;
		
		AspectsPackage(String package1, String packagePath) {
			this.name = package1;
			this.path = packagePath;
			aspects = new LinkedList<Aspect>();
		}

		String getName() {
			return name;
		}
		
		String getPath() {
			return path;
		}

		List<Aspect> getAspects() {
			return aspects;
		}
		
		void addAspect(Aspect aspect){
			aspects.add(aspect);
		}
		
	}
	
	// Class-wrapper with info about bad-formed aspect
	static class BadAspect {
		private String  classFile;
		private Exception exception;
		
		BadAspect(String classFile, Exception e){
			this.classFile = classFile;
			this.exception = e;
		}
		
		String getClassFilePath(){
			return classFile;
		}
		
		Exception getException(){
			return exception;
		}
	}
}