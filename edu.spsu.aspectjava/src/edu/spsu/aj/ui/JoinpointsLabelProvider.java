package edu.spsu.aj.ui;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import edu.spsu.aj.weaver.Joinpoint;

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
			return AspectJavaView.packageImage;
		}else if(obj instanceof ClassNode){
			return AspectJavaView.classImage;
		}else if(obj instanceof MethodNode){
			return AspectJavaView.methodImage;
		}else if(obj instanceof AbstractInsnNode){
			return AspectJavaView.sourceImage;
		}else if(obj instanceof Joinpoint){
			return AspectJavaView.ruleImage;
		}
		return null;
	}
}
