package edu.spsu.aj.weaver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;



public class TestWeaving {

	/**
	 * @param args
	 * @throws BadCondClauseFormat 
	 * @throws IOException 
	 * @throws BadArgsInRule 
	 */
	public static void main(String[] args) throws BadCondClauseFormat, IOException, BadArgsInRule {
		String cond = " %after %call *target* && %args(2, 1)";
		RuleCondition condition = new RuleCondition(cond);
		RuleAction action = new RuleAction("action1", "(Ljava/lang/String;Ljava/lang/String;)J");
		AspectRule rule = new AspectRule(condition, action);
		List<AspectRule> rules = new LinkedList<AspectRule>();
		rules.add(rule);
		Aspect aspect = new Aspect("edu.spsu.aj.weaver.test.Aspect1", rules);
		List<Aspect> aList = new LinkedList<Aspect>();
		aList.add(aspect);
		
		ClassNode cn = new ClassNode();
		ClassReader cr = new ClassReader("edu.spsu.aj.weaver.test.Target");
		cr.accept(cn, ClassReader.EXPAND_FRAMES);
		List<ClassNode> tList = new LinkedList<ClassNode>();
		tList.add(cn);
		
		Weaver weaver = new Weaver();
		
		List<Joinpoint> jps = weaver.findJoinpoints(tList, aList);
//		weaver.weaveJoinpoints(jps);
		
		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
		LocalVariablesAdapter lva = new LocalVariablesAdapter(cw);
		cn.accept(lva);
		byte[] b = cw.toByteArray();
		FileOutputStream out = 
			new FileOutputStream("C:/Users/Евгений/workspace/edu.spsu.aspectjava/bin/edu/spsu/aj/weaver/test/Target.class");
		out.write(b);
		out.close();
		
		System.out.println();

	}

}
