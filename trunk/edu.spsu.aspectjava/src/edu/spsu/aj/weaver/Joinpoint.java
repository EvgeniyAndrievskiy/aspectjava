package edu.spsu.aj.weaver;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Joinpoint {
	private AbstractInsnNode instr;
	private MethodNode method;
	private ClassNode clazz;
	
	private Aspect aspect;
	private AspectRule rule;
	private AbstractConditionClause clause;

	Joinpoint(AbstractInsnNode instr, MethodNode method, ClassNode clazz,
			Aspect aspect, AspectRule rule, AbstractConditionClause clause) {
		this.instr = instr;
		this.method = method;
		this.aspect = aspect;
		this.clazz = clazz;
		this.rule = rule;
		this.clause = clause;
	}

	public AbstractInsnNode getInstr() {
		return instr;
	}

	public MethodNode getMethod() {
		return method;
	}

	public ClassNode getClazz() {
		return clazz;
	}

	public Aspect getAspect() {
		return aspect;
	}
	
	public AspectRule getAspectRule() {
		return rule;
	}

	public AbstractConditionClause getClause() {
		return clause;
	}

}
