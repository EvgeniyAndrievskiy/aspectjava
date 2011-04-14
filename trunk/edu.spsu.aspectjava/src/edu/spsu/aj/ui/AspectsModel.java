package edu.spsu.aj.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
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
import edu.spsu.aj.weaver.BadArgsInRule;
import edu.spsu.aj.weaver.BadCondClauseFormat;
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
	
	boolean addAspectsContainer(String containerPath) throws NoAspectsInContainerException, IOException{
		if(containsAspectsContainer(containerPath)){
			return false;
		}
		AspectsContainer container = null;
		if(containerPath.endsWith(".jar")){
			container = parseAspectsJar(containerPath);
			aspectsContainers.add(container);
		}else{
			final AspectsContainer cont1 = new AspectsContainer(containerPath, true);
			container = cont1;
			List<File> classFiles = findClassFiles(new File(containerPath));
			for(final File file : classFiles){
				ClassReader cr = null;
				try{
					cr = new ClassReader(new FileInputStream(file));	
				}catch (IOException e) {
					continue;
				}
				cr.accept(new ClassAdapter(){
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
							Aspect aspect = new Aspect(name, description, rules);
							String parent = file.getParent();
							String packagePath = '.' + parent.replace(cont1.getPath(), "");
							// package.class format
							String aspectName = aspect.getName();
							String aspectPackage = null;
							int l = aspectName.lastIndexOf('.');
							if(l < 0){
								aspectPackage = AspectsPackage.DEFAULT_PACKAGE;
							}else{
								aspectPackage = aspectName.substring(0, l);
							}
							AspectsPackage package1 = cont1.addPackage(aspectPackage, packagePath);
							package1.addAspect(aspect);
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
										try {
											AspectRule rule = new AspectRule(description, condition, action);
											rules.add(rule);
										} catch (BadArgsInRule e) {
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
												} catch (BadCondClauseFormat e) {
													condition = null;
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
				
			}
			if(!cont1.isEmpty()){
				aspectsContainers.add(cont1);
			}else{
				throw new NoAspectsInContainerException(containerPath);
			}
		}

		for(AspectsModelListener listener:listeners){
			listener.aspectsContainerAdded(container);
		}		
		return true;
	}
	
	
	 boolean addAspectsContainer(int index, String containerPath) throws NoAspectsInContainerException, IOException {
		if(containsAspectsContainer(containerPath)){
			return false;
		}
		AspectsContainer cont = null;
		if(containerPath.endsWith(".jar")){
			cont = parseAspectsJar(containerPath);
			aspectsContainers.add(index, cont);
		}else{
			List<File> classFiles = findClassFiles(new File(containerPath));
			final AspectsContainer container = new AspectsContainer(containerPath, true);
			cont = container;
			for(final File file : classFiles){
				ClassReader cr = null;
				try{
					cr = new ClassReader(new FileInputStream(file));	
				}catch (IOException e) {
					continue;
				}
				cr.accept(new ClassAdapter(){
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
							Aspect aspect = new Aspect(name, description, rules);
							String parent = file.getParent();
							String packagePath = '.' + parent.replace(container.getPath(), "");
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
										try {
											AspectRule rule = new AspectRule(description, condition, action);
											rules.add(rule);
										} catch (BadArgsInRule e) {
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
												} catch (BadCondClauseFormat e) {
													condition = null;
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
				
			}
			if(!container.isEmpty()){
				aspectsContainers.add(index, container);
			}else{
				throw new NoAspectsInContainerException(containerPath);
			}
		}

		for(AspectsModelListener listener:listeners){
			listener.aspectsContainerAdded(index, cont);
		}		
		return true;
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
	
	private AspectsContainer parseAspectsJar(String jarFilePath) throws IOException, NoAspectsInContainerException {
		JarFile jarFile = new JarFile(jarFilePath);

		Enumeration<JarEntry> jarEntries = jarFile.entries();
		final AspectsContainer container = new AspectsContainer(jarFilePath, false);
		while (jarEntries.hasMoreElements()) {
			final JarEntry entry = jarEntries.nextElement();
			if (entry.toString().endsWith(".class")) {
				ClassReader cr = null;
				try{
					cr = new ClassReader(jarFile.getInputStream(entry));
				}catch (IOException e) {
					continue;
				}
				cr.accept(new ClassAdapter(){
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
							Aspect aspect = new Aspect(name, description, rules);
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
										try {
											AspectRule rule = new AspectRule(description, condition, action);
											rules.add(rule);
										} catch (BadArgsInRule e) {
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
												} catch (BadCondClauseFormat e) {
													condition = null;
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
			}
		}
		
		if(container.isEmpty()){
			throw new NoAspectsInContainerException(jarFilePath);
		}else{
			return container;
		}
	}
	
//	private Aspect parseAspectClass(Class class1){
//		// TODO (to the future) verify that class1 extends Aspect
//		
//		List<AspectRule> weavingRules = new LinkedList<AspectRule>();
//		Method[] methods = class1.getDeclaredMethods();
//		
//		for(Method method:methods){			
//			if(Modifier.isPublic(method.getModifiers()) && 
//					Modifier.isStatic(method.getModifiers())){
//				AspectAction actionAnnot = method.getAnnotation(aspectAction);
//				if(actionAnnot == null){
//					continue;
//				}
//				String cond = actionAnnot.value();
//				AspectDescription descAnnot = method.getAnnotation(aspectDesc);
//				if(descAnnot == null){
//					try {
//						weavingRules.add(new AspectRule(new RuleCondition(cond), new RuleAction(method.getName(),
//								Type.getMethodDescriptor(method))));
//					} catch (BadArgsInRule e) {
//						return null;
//					} catch (BadCondClauseFormat e) {
//						return null;
//					}
//				}else{
//					String desc = descAnnot.value();
//					try {
//						weavingRules.add(new AspectRule(desc, new RuleCondition(cond), new RuleAction(method.getName(),
//								Type.getMethodDescriptor(method))));
//					} catch (BadArgsInRule e) {
//						return null;
//					} catch (BadCondClauseFormat e) {
//						return null;
//					}
//				}
//			}
//		}
//		
//		if(weavingRules.isEmpty()){
//			return null;
//		}
//		
//		Aspect aspect;
//		AspectDescription descAnnot2 = (AspectDescription) class1.getAnnotation(aspectDesc);
//		if(descAnnot2 != null){
//			String desc = descAnnot2.value();
//			aspect = new Aspect(class1.getName(), desc, weavingRules);
//		}else{
//			aspect = new Aspect(class1.getName(), weavingRules);
//		}
//		
//		return aspect;
//	}
	
	// Class-adapter with all default implementations (returning null or doing nothing).
	private static class ClassAdapter implements ClassVisitor{

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
	private static class MethodAdapter implements MethodVisitor{

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
	private static class AnnotationAdapter implements AnnotationVisitor{

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
}