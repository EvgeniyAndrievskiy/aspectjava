package edu.spsu.aj.weaver;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class LocalVariablesAdapter extends ClassAdapter {
	
	public LocalVariablesAdapter(ClassVisitor cv) {
		super(cv);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		mv = new LocalVariablesSorter(access, desc, mv);
		return mv;
	}
	
}
