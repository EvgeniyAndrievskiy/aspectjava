package edu.spsu.aj.ui;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import edu.spsu.aj.ui.AspectsModel.AspectsPackage;
import edu.spsu.aj.weaver.Joinpoint;

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
//		if (child instanceof TreeObject) {
//			return ((TreeObject) child).getParent();
//		}
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

	public boolean hasChildren(Object parent) {
		if(parent instanceof Joinpoint) {
			return false;
		}else{
			return true;
		}
	}
	
	// Special utility-class.
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

}