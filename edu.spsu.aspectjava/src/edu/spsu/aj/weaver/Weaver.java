package edu.spsu.aj.weaver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import static org.objectweb.asm.Opcodes.*;

import edu.spsu.aj.weaver.AbstractConditionClause.Context;

public class Weaver{
	
	// Instructions stack numbers (pushing_count - popping_count)
	private static byte[] instrSN = new byte[200];
	
	static{
		// Constants
		for(int i = ACONST_NULL; i <= LDC; i++) {
			instrSN[i] = 1;
		}
		// xLOAD
		for(int i = ILOAD; i <= ALOAD; i++) {
			instrSN[i] = 1;
		}
		// xALOAD
		for(int i = IALOAD; i <= SALOAD; i++) {
			instrSN[i] = -1;
		}
		// xSTORE
		for(int i = ISTORE; i <= ASTORE; i++) {
			instrSN[i] = -1;
		}
		// xASTORE
		for(int i = IASTORE; i <= SASTORE; i++) {
			instrSN[i] = -3;
		}
		// Stack
		// By default POP2 & DUP2x use +/-2 values (1word case).
		// Two-word cases are considered on-site in passValue().
		instrSN[POP] = -1;
		instrSN[POP2] = -2; 
		instrSN[DUP] = 1;
		instrSN[DUP_X1] = 1;
		instrSN[DUP_X2] = 1;
		instrSN[DUP2] = 2; 
		instrSN[DUP2_X1] = 2; 
		instrSN[DUP2_X2] = 2; 
		instrSN[SWAP] = 0;
		
		// Arithmetic and logic 1
		for(int i = IADD; i <= DREM; i++) {
			instrSN[i] = -1;
		}
		for(int i = INEG; i <= DNEG; i++) {
			instrSN[i] = 0;
		}
		for(int i = ISHL; i <= LXOR; i++) {
			instrSN[i] = -1;
		}
		
		instrSN[IINC] = 0;
		
		// Casts
		for(int i = I2L; i <= I2S; i++) {
			instrSN[i] = 0;
		}
		instrSN[CHECKCAST] = 0;
		
		// Arithmetic and logic 2
		for(int i = LCMP; i <= DCMPG; i++) {
			instrSN[i] = -1;
		}
		// Jumps
		for(int i = IFEQ; i <= IFLE; i++) {
			instrSN[i] = -1;
		}
		for(int i = IF_ICMPEQ; i <= IF_ACMPNE; i++) {
			instrSN[i] = -2;
		}
		instrSN[GOTO] = 0;
		instrSN[JSR] = 1;
		instrSN[RET] = 0;
		instrSN[TABLESWITCH] = -1;
		instrSN[LOOKUPSWITCH] = -1;
		instrSN[IFNULL] = -1;
		instrSN[IFNONNULL] = -1;
		
		// Return
		for(int i = IRETURN; i <= ARETURN; i++) {
			instrSN[i] = -1;
		}
		instrSN[RETURN] = 0;
		instrSN[ATHROW] = -1;
		
		// Fields, objects
		instrSN[GETSTATIC] = 1;
		instrSN[PUTSTATIC] = -1;
		instrSN[GETFIELD] = 0;
		instrSN[PUTFIELD] = -2;
		instrSN[NEW] = 1;
		instrSN[INSTANCEOF] = 0;
		instrSN[MONITORENTER] = -1;
		instrSN[MONITOREXIT] = -1;
		
		// Arrays
		instrSN[NEWARRAY] = 0;
		instrSN[ANEWARRAY] = 0;
		instrSN[ARRAYLENGTH] = 0;
		// Need sub array dimension
		instrSN[MULTIANEWARRAY] = 1;

		
		// Methods
		// Need sub arguments number
		// Need sub 1 in case of void ret value 
		instrSN[INVOKEVIRTUAL] = 0;
		instrSN[INVOKESPECIAL] = 0;
		instrSN[INVOKESTATIC] = 1;
		instrSN[INVOKEINTERFACE] = 0;

	}
	
	// Instructions that push on stack two-word values
	private static List<Integer> twoWordsOpcs = new LinkedList<Integer>();
	static{
		twoWordsOpcs.add(LLOAD);
		twoWordsOpcs.add(DLOAD);
		twoWordsOpcs.add(LCONST_0);
		twoWordsOpcs.add(LCONST_1);
		twoWordsOpcs.add(DCONST_0);
		twoWordsOpcs.add(DCONST_1);
		twoWordsOpcs.add(LADD);
		twoWordsOpcs.add(DADD);
		twoWordsOpcs.add(LSUB);
		twoWordsOpcs.add(DSUB);
		twoWordsOpcs.add(LMUL);
		twoWordsOpcs.add(DMUL);
		twoWordsOpcs.add(LDIV);
		twoWordsOpcs.add(DDIV);
		twoWordsOpcs.add(LREM);
		twoWordsOpcs.add(DREM);
		twoWordsOpcs.add(LNEG);
		twoWordsOpcs.add(DNEG);
		twoWordsOpcs.add(LSHL);
		twoWordsOpcs.add(LSHR);
		twoWordsOpcs.add(LUSHR);
		twoWordsOpcs.add(LAND);
		twoWordsOpcs.add(LOR);
		twoWordsOpcs.add(LXOR);
		twoWordsOpcs.add(I2L);
		twoWordsOpcs.add(F2L);
		twoWordsOpcs.add(D2L);
		twoWordsOpcs.add(I2D);
		twoWordsOpcs.add(L2D);
		twoWordsOpcs.add(F2D);
		twoWordsOpcs.add(LALOAD);
		twoWordsOpcs.add(DALOAD);
	}
	
	public List<Joinpoint> findJoinpoints(List<ClassNode> targetApp, List<Aspect> aspects) {
		List<Joinpoint> result = new ArrayList<Joinpoint>();
		for(ClassNode class_ : targetApp){
			for(MethodNode method : (List<MethodNode>) class_.methods){
				Iterator<AbstractInsnNode> iter = method.instructions.iterator();
				while(iter.hasNext()){
					AbstractInsnNode instr = iter.next();
					if(mayBeJP(instr)){
						for(Aspect aspect : aspects){
							Iterator<AspectRule> rules = aspect.getRules().iterator();
							while(rules.hasNext()){
								AspectRule rule = rules.next();
								AbstractConditionClause clause = 
									rule.getCondition().accept(instr, method, class_);
								if(clause != null){
									Joinpoint jp = new Joinpoint(instr, method,
											class_, aspect, rule, clause);
									if(checkTypes(jp)){
										result.add(new Joinpoint(instr, method,
											class_, aspect, rule, clause));
									}
								}
							}
						}
					}
				}
			}
		}
		return result;
	}
	
	private boolean checkTypes(Joinpoint jp){
		AbstractInsnNode instr = jp.getInstr();
		AbstractConditionClause clause = jp.getClause();
		AspectRule rule = jp.getAspectRule();
		RuleAction action = rule.getAction();
		String actDesc = action.getDescriptor();
		Type[] actArgTypes = Type.getArgumentTypes(actDesc);
		
		// For INSTEAD context case check equality of return type & corresponding target instr type.
		if(clause.getContext() == Context.INSTEAD){
			// CALL joinpoint case.
			if(clause instanceof CallConditionClause){
				MethodInsnNode mInstr = (MethodInsnNode) instr;
				if(! Type.getReturnType(mInstr.desc).getClassName().
						equals(Type.getReturnType(actDesc).getClassName())){
					return false;
				}
			}// USE & ASSIGN joinpoint cases.
			else{
				// TODO: in INSTEAD case check types equality for USE & ASSIGN. 
			}
		}
		
		// If action has args then check equality of argument types 
		// taken from action & target instr (according to args info).
		if(actArgTypes.length != 0){
			ArgsInfo argsInfo = clause.getArgsInfo();
			// CALL joinpoint case.
			if(clause instanceof CallConditionClause){
				MethodInsnNode mInstr = (MethodInsnNode) instr;
				Type[] targArgTypes = Type.getArgumentTypes(mInstr.desc);
				int[] info = (int[]) argsInfo.getInfo();
				
				try {
					for(int i = 0; i < actArgTypes.length; i++) {
						if(! actArgTypes[i].getClassName().
								equals(targArgTypes[info[i]].getClassName())) {
							return false;
						}
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					return false;
				}
			}// USE & ASSIGN joinpoint cases.
			else{
				// TODO: check equality of argument types taken from action & target instr for USE & ASSIGN.
			}
		}
		return true;
	}

	// TODO: implement for USE & ASSIGN jp kinds
	private boolean mayBeJP(AbstractInsnNode instr) {		
		return instr.getType() == AbstractInsnNode.METHOD_INSN;
	}
	
//	private AbstractInsnNode passValue(AbstractInsnNode arg) {
//		int opcode = arg.getOpcode();
//		/**  SET OF SPECIAL CASES 'method1(ENTITY = method2());'  **/
//		// Case such as 'method1(local = method2());'
//		if(ISTORE <= opcode && opcode <= ASTORE) {
//			// Pass xSTORE, DUP and go recurrence
//			return passValue(arg.getPrevious().getPrevious());
//		}
//		// Case such as 'method1(a[i] = method2());'
//		if(IASTORE <= opcode && opcode <= SASTORE) {
//			// Pass xASTORE, DUP and go recurrence
//			AbstractInsnNode node =  passValue(arg.getPrevious().getPrevious());
//			return passValue(passValue(node));
//		}
//		// Case such as 'method1(o.f = method2());'
//		if(opcode == PUTFIELD) {
//			// Pass PUTFIELD, DUP and go recurrence
//			AbstractInsnNode node =  passValue(arg.getPrevious().getPrevious());
//			return passValue(node);
//		}
//		// Case such as 'method1(Class1.sf = method2());'
//		if(opcode == PUTSTATIC) {
//			// Pass PUTSTATIC, DUP and go recurrence
//			return passValue(arg.getPrevious().getPrevious());
//		}
//		/**   **/
//		
//		/** CASTS & IINC instr **/
//		if(I2L <= opcode && opcode <= I2S || opcode == CHECKCAST) { 
//			return passValue(arg.getPrevious());
//		}
//		if(opcode == IINC) {
//			return passValue((arg.getPrevious()));
//		}
//		if(ILOAD <= opcode && opcode <= ALOAD) {
//			return arg.getPrevious();
//		}
//		if(IALOAD <= opcode && opcode <= SALOAD) {
//			return passValue(passValue(arg.getPrevious()));
//		}
//		if(opcode == GETSTATIC) {
//			return arg.getPrevious();
//		}
//		if(opcode == GETFIELD) {
//			return passValue(arg.getPrevious());
//		}
//		// Constants
//		if(ACONST_NULL <= opcode && opcode <= DCONST_1
//				|| opcode == BIPUSH || opcode == SIPUSH
//				|| opcode == LDC) {
//			return arg.getPrevious();
//		}
//		
//		/** Methods **/
//		
//		if(opcode == INVOKESTATIC) {
//			int argCount = Type.getArgumentTypes(((MethodInsnNode) arg).desc).length;
//			AbstractInsnNode instr = arg;
//			for(int i = 0; i < argCount - 1; i++) {
//				instr = passValue(instr);
//			}
//			return passValue(instr);
//		}
//		
//		if(arg.getType() == AbstractInsnNode.FRAME ||
//				arg.getType() == AbstractInsnNode.LINE ||
//				arg.getType() == AbstractInsnNode.LABEL){
//			return passValue((arg.getPrevious()));
//		}
//		int[] a = new int[]{0, 1, 2, 3};
//		Integer j = 0;
//		j++;
//		int i = 0;
//		return null;
//	}
	
	private AbstractInsnNode passValue(AbstractInsnNode arg) {
		int i = 1;
		AbstractInsnNode current = arg;
		while(i != 0) {
			if(current.getType() == AbstractInsnNode.FRAME ||
					current.getType() == AbstractInsnNode.LINE ||
					current.getType() == AbstractInsnNode.LABEL){
				current = current.getPrevious();
				continue;
			}
			int opc = current.getOpcode();
			i -= instrSN[opc];
			
			// Consider pop2, dup2x cases with 2word value (instrSN = +/-1)
			if(opc == POP2 || opc == DUP2 || opc == DUP2_X1 || opc == DUP2_X2){
				// *ABSOLUTE CASES*
				AbstractInsnNode prev = current.getPrevious();
				int op = prev.getOpcode();
				if(twoWordsOpcs.contains(op)){
					if(opc == POP2){
						i--;
					}else{
						i++;
					}
				}else if(prev.getType() == AbstractInsnNode.METHOD_INSN){
					Type retType = Type.getReturnType(((MethodInsnNode) prev).desc);
					if(retType.getSort() == Type.LONG || retType.getSort() == Type.DOUBLE){
						if(opc == POP2){
							i--;
						}else{
							i++;
						}
					}
				}else if(op == GETFIELD | op == GETSTATIC){
					Type type = Type.getType(((FieldInsnNode) prev).desc);
					if(type.getSort() == Type.LONG || type.getSort() == Type.DOUBLE){
						if(opc == POP2){
							i--;
						}else{
							i++;
						}
					}
				}else if(op == LDC){
					LdcInsnNode ldci = (LdcInsnNode) prev;
					if(ldci.cst instanceof Long || ldci.cst instanceof Double){
						if(opc == POP2){
							i--;
						}else{
							i++;
						}
					}else if(ldci.cst instanceof Type){
						Type t = (Type) ldci.cst;
						if(t.getSort() == Type.LONG || t.getSort() == Type.DOUBLE){
							if(opc == POP2){
								i--;
							}else{
								i++;
							}
						}
					}
				}
				// *Statistical particular cases*
				else if(op == PUTSTATIC || op == PUTFIELD){
					Type type = Type.getType(((FieldInsnNode) prev).desc);
					if(type.getSort() == Type.LONG || type.getSort() == Type.DOUBLE){
						// Suppose multi assign case for long/double
						if(opc == DUP2 || opc == DUP2_X1){
							i++;
						}
					}
				}else if(op == LSTORE || op == DSTORE){
					// Suppose --/++ on long/double in multi assign case 
					if(opc == DUP2 || opc == DUP2_X1){
						i++;
					}
				}
				
				// TODO: IINC case ? - practice shows that iinc isn't used for
				// long & double
			}
			if(current.getType() == AbstractInsnNode.METHOD_INSN) {
				MethodInsnNode mInstr = (MethodInsnNode) current;
				i += Type.getArgumentTypes(mInstr.desc).length;
				if(Type.getReturnType(mInstr.desc).getSort() == Type.VOID) {
					i++;
				}
			}else if(current.getType() == AbstractInsnNode.MULTIANEWARRAY_INSN) {
				i += ((MultiANewArrayInsnNode) current).dims;
			}
			current = current.getPrevious();
		}
		return current;
	}
	
	
	private void disposeValue(AbstractInsnNode instr, MethodNode method) {
		int opcode = instr.getOpcode();
		int type = instr.getType();
		
		if(type == AbstractInsnNode.FRAME ||
				type == AbstractInsnNode.LINE ||
				type == AbstractInsnNode.LABEL){
			disposeValue(instr.getPrevious(), method);
		}else if(instr.getType() == AbstractInsnNode.METHOD_INSN) {
			int retType = Type.getReturnType(((MethodInsnNode) instr).desc).getSort();
			InsnNode pop;
			if(retType == Type.LONG || retType == Type.DOUBLE){
				pop = new InsnNode(POP2);
			}else{
				pop = new InsnNode(POP);
			}
			method.instructions.insert(instr, pop);
		}else if(ILOAD <= opcode && opcode <= ALOAD) {
			method.instructions.remove(instr);
		}else if(opcode == IINC) {
			disposeValue(instr.getPrevious(), method);
		}else if(I2L <= opcode && opcode <= I2S || opcode == CHECKCAST) { 
			AbstractInsnNode prev = instr.getPrevious();
			method.instructions.remove(instr);
			disposeValue(prev, method);
		}else if(ACONST_NULL <= opcode && opcode <= DCONST_1
				|| opcode == BIPUSH || opcode == SIPUSH
				|| opcode == LDC) {
			method.instructions.remove(instr);
		}else if(IALOAD <= opcode && opcode <= SALOAD) {
			// We need remove xALOAD instr & dispose two values: for array index & for array object
			AbstractInsnNode indexValue = instr.getPrevious();
			method.instructions.remove(instr);
			AbstractInsnNode objectValue = passValue(indexValue);
			disposeValue(indexValue, method);
			disposeValue(objectValue, method);
		}else if(opcode == GETSTATIC) {
			method.instructions.remove(instr);
		}else if(opcode == GETFIELD) {
			AbstractInsnNode objectValue = instr.getPrevious();
			method.instructions.remove(instr);
			disposeValue(objectValue, method);
		}else if(IADD <= opcode && opcode <= DREM ||
				ISHL<= opcode && opcode <= LXOR) {
			AbstractInsnNode argValue1 = instr.getPrevious();
			method.instructions.remove(instr);
			AbstractInsnNode argValue2 = passValue(argValue1);
			disposeValue(argValue1, method);
			disposeValue(argValue2, method);
		}else if(INEG <= opcode && opcode <= DNEG) {
			AbstractInsnNode argValue = instr.getPrevious();
			method.instructions.remove(instr);
			disposeValue(argValue, method);
		}else{
			throw new UnsupportedOperationException("disposeValue(): not implemented yet - "
					+ opcode);
		}
	}
	
	public void weaveJoinpoints(List<ClassNode> targetApp, List<Joinpoint> jPoints) {
		HashMap<String, ClassNode> classNodes = new HashMap<String, ClassNode>();
		for(ClassNode classNode : targetApp){
			classNodes.put(classNode.name, classNode);
		}
		List<Joinpoint> workingJPs = new LinkedList<Joinpoint>();
		for(Joinpoint jp : jPoints){
			ClassNode cn = classNodes.get(jp.getClazz().name);
			int  methodI = jp.getClazz().methods.indexOf(jp.getMethod());
			MethodNode method = (MethodNode) cn.methods.get(methodI);
			int instrI = jp.getMethod().instructions.indexOf(jp.getInstr());
			AbstractInsnNode instr = method.instructions.get(instrI);
			Joinpoint wjp = new Joinpoint(instr, method, cn, jp.getAspect(), jp.getAspectRule(), jp.getClause());
			workingJPs.add(wjp);
		}
		for(Joinpoint jPoint : workingJPs){
			MethodNode method = jPoint.getMethod();
			AbstractInsnNode instr = jPoint.getInstr();
			AspectRule rule = jPoint.getAspectRule();
			
			String owner = jPoint.getAspect().getName().replace('.', '/');
			String name = rule.getAction().getName();
			String desc = rule.getAction().getDescriptor();
			MethodInsnNode actionCall = new MethodInsnNode(INVOKESTATIC, owner, name, desc);
			
			Context context = jPoint.getClause().getContext();	
			
			// If action has no args.
			if (Type.getArgumentTypes(desc).length == 0) {
				switch (context) {
				case BEFORE:
					method.instructions.insertBefore(instr, actionCall);
					if(Type.getReturnType(desc).getSort() != Type.VOID){
						InsnNode pop;
						if(Type.getReturnType(desc).getSort() == Type.LONG ||
								Type.getReturnType(desc).getSort() == Type.DOUBLE){
							pop = new InsnNode(POP2);
						}else{
							pop = new InsnNode(POP);
						}
						method.instructions.insert(actionCall, pop);
					}
					break;
				case AFTER:
					method.instructions.insert(instr, actionCall);
					if(Type.getReturnType(desc).getSort() != Type.VOID){
						InsnNode pop;
						if(Type.getReturnType(desc).getSort() == Type.LONG ||
								Type.getReturnType(desc).getSort() == Type.DOUBLE){
							pop = new InsnNode(POP2);
						}else{
							pop = new InsnNode(POP);
						}
						method.instructions.insert(actionCall, pop);
					}
					break;
				case INSTEAD:
					// CALL joinpoint kind case
					if(jPoint.getClause() instanceof CallConditionClause){         
						MethodInsnNode mInstr = (MethodInsnNode) instr; 
						
//						InsnList instrList = new InsnList();
//						for(int i = 0; i < Type.getArgumentTypes(mInstr.desc).length; i++){
//							InsnNode pop = new InsnNode(POP);
//							instrList.add(pop);
//						}
//						instrList.add(aspectCall);
//						method.instructions.insert(instr, instrList);
						
						// Remove all target method arguments.
						AbstractInsnNode current = instr.getPrevious();
						for(int i = 0; i < Type.getArgumentTypes(mInstr.desc).length; i++){
							AbstractInsnNode next = passValue(current);
							disposeValue(current, method);
							current = next;
						}
						
						// For non-static target methods we need to remove target object
						if(mInstr.getOpcode() != INVOKESTATIC) {
							disposeValue(current, method);
						}
						method.instructions.insert(instr, actionCall);
						method.instructions.remove(instr);
					}else{
						// TODO: instead of USE & ASSIGN ?
					}
					break;
				}
			// If action has args.
			}else{
				// CALL joinpoint case
				if(jPoint.getClause() instanceof CallConditionClause) {
					MethodInsnNode mInstr = (MethodInsnNode) instr;
					
					Type[] actArgTypes = Type.getArgumentTypes(desc);
					int actArgCount = actArgTypes.length;
					Type[] targArgTypes = Type.getArgumentTypes(mInstr.desc);
					int[] argsInfo  = (int[]) jPoint.getClause().getArgsInfo().getInfo();
					
					// Build array with begin-end pairs of instructions corresponding to each target method argument.
					BeginEnd[] targetArgsInstrs = new BeginEnd[targArgTypes.length];
					BeginEnd last = new BeginEnd();
					last.setEnd(mInstr.getPrevious());
					targetArgsInstrs[targArgTypes.length - 1] = last;
					for(int i = targArgTypes.length - 2; i >= 0; i --){
						AbstractInsnNode node = passValue(targetArgsInstrs[i + 1].getEnd());
						if(node == null){
							targetArgsInstrs[i + 1].setBegin(method.instructions.getFirst());
						}else{
							targetArgsInstrs[i + 1].setBegin(node.getNext());
						}
						BeginEnd prev = new BeginEnd();
						prev.setEnd(node);
						targetArgsInstrs[i] = prev;
					}
					AbstractInsnNode node = passValue(targetArgsInstrs[0].getEnd());
					if(node == null){
						targetArgsInstrs[0].setBegin(method.instructions.getFirst());
					}else{
						targetArgsInstrs[0].setBegin(node.getNext());
					}
					
					// Case for BEFORE & AFTER
					if(context == Context.BEFORE || context == Context.AFTER){
						// Build array with locals vars (a-la 'ASTORE var') in order to store in them 
						// duplicates of necessary target method arguments. 
						int[] argsToLocals = new int[actArgCount];
						// Store locals begin from 'method.maxLocals' index; it's supposed to use LocalVariablesSorter afterwards
						argsToLocals[0] = method.maxLocals;
						for(int i = 1; i < actArgCount; i++) {
							if(actArgTypes[i - 1].getSort() == Type.LONG 
									|| actArgTypes[i - 1].getSort() == Type.DOUBLE) {
								argsToLocals[i] = argsToLocals[i - 1] + 2;
							}else{
								argsToLocals[i] = argsToLocals[i - 1] + 1;
							}
						}
						
						// After each necessary end arg instruction in targetArgsInstrs insert DUP/DUP2 & corresponding xSTORE.
						for(int i = 0; i < actArgCount; i++){
							AbstractInsnNode node1 = targetArgsInstrs[argsInfo[i]].getEnd();
							AbstractInsnNode dupNode;
							if(targArgTypes[argsInfo[i]].getSort() == Type.LONG
									|| targArgTypes[argsInfo[i]].getSort() == Type.DOUBLE) {
								dupNode = new InsnNode(DUP2);
							}else{
								dupNode = new InsnNode(DUP);
							}
							method.instructions.insert(node1, dupNode);
							AbstractInsnNode storeNode;
							if(targArgTypes[argsInfo[i]].getSort() == Type.INT
									|| targArgTypes[argsInfo[i]].getSort() == Type.BOOLEAN
									|| targArgTypes[argsInfo[i]].getSort() == Type.CHAR
									|| targArgTypes[argsInfo[i]].getSort() == Type.BYTE
									|| targArgTypes[argsInfo[i]].getSort() == Type.SHORT) {
								storeNode = new VarInsnNode(ISTORE, argsToLocals[i]);
							}else if(targArgTypes[argsInfo[i]].getSort() == Type.LONG){
								storeNode = new VarInsnNode(LSTORE, argsToLocals[i]);
							}else if(targArgTypes[argsInfo[i]].getSort() == Type.FLOAT){
								storeNode = new VarInsnNode(FSTORE, argsToLocals[i]);
							}else if(targArgTypes[argsInfo[i]].getSort() == Type.DOUBLE){
								storeNode = new VarInsnNode(DSTORE, argsToLocals[i]);
							}else if(targArgTypes[argsInfo[i]].getSort() == Type.OBJECT){
								storeNode = new VarInsnNode(ASTORE, argsToLocals[i]);
							}else{
								throw new UnsupportedOperationException("Unsupported argument data type: " 
										+ targArgTypes[argsInfo[i]]);
							}
							method.instructions.insert(dupNode, storeNode);
						}
						
						// Insert action call
						if(context == Context.BEFORE){
							method.instructions.insertBefore(mInstr, actionCall);
						}else if(context == Context.AFTER){
							method.instructions.insert(mInstr, actionCall);
						}
						
						// Insert xLOADs with arguments before action call
						AbstractInsnNode current = actionCall;
						for(int i = actArgCount - 1; i >= 0; i--){
							AbstractInsnNode loadNode;
							if(actArgTypes[i].getSort() == Type.INT
									|| targArgTypes[argsInfo[i]].getSort() == Type.BOOLEAN
									|| targArgTypes[argsInfo[i]].getSort() == Type.CHAR
									|| targArgTypes[argsInfo[i]].getSort() == Type.BYTE
									|| targArgTypes[argsInfo[i]].getSort() == Type.SHORT) {
								loadNode = new VarInsnNode(ILOAD, argsToLocals[i]);
							}else if(targArgTypes[argsInfo[i]].getSort() == Type.LONG){
								loadNode = new VarInsnNode(LLOAD, argsToLocals[i]);
							}else if(targArgTypes[argsInfo[i]].getSort() == Type.FLOAT){
								loadNode = new VarInsnNode(FLOAD, argsToLocals[i]);
							}else if(targArgTypes[argsInfo[i]].getSort() == Type.DOUBLE){
								loadNode = new VarInsnNode(DLOAD, argsToLocals[i]);
							}else if(targArgTypes[argsInfo[i]].getSort() == Type.OBJECT){
								loadNode = new VarInsnNode(ALOAD, argsToLocals[i]);
							}else{
								throw new UnsupportedOperationException("Unsupported argument data type: " 
										+ targArgTypes[argsInfo[i]]);
							}
							method.instructions.insertBefore(current, loadNode);
							current = loadNode;
						}
						
						// If necessary insert POP/POP2 in order to remove from stack action's returned value
						if(Type.getReturnType(desc).getSort() != Type.VOID){
							InsnNode pop;
							if(Type.getReturnType(desc).getSort() == Type.LONG ||
									Type.getReturnType(desc).getSort() == Type.DOUBLE){
								pop = new InsnNode(POP2);
							}else{
								pop = new InsnNode(POP);
							}
							method.instructions.insert(actionCall, pop);
						}
					// Case for INSTEAD
					}else if(context == Context.INSTEAD) {

						// Reorder necessary arguments according to args info.
						for(int i = 0; i < actArgCount; i++) {
							AbstractInsnNode current = targetArgsInstrs[argsInfo[i]].getEnd();
							AbstractInsnNode currentBegin = targetArgsInstrs[argsInfo[i]].getBegin();
							InsnList list = new InsnList();
							while(current != currentBegin){
								AbstractInsnNode prev = current.getPrevious();
								method.instructions.remove(current);
								if(list.size() == 0){
									list.add(current);
								}else{
									list.insertBefore(list.getFirst(), current);
								}
								current = prev;
							}
							method.instructions.remove(currentBegin);
							if(list.size() == 0){
								list.add(currentBegin);
							}else{
								list.insertBefore(list.getFirst(), currentBegin);
							}
							method.instructions.insertBefore(mInstr, list);
						}
						
						// Remove unnecessary arguments.
						AbstractInsnNode current = targetArgsInstrs[argsInfo[0]].getBegin().getPrevious();
						for(int i = 0; i < targArgTypes.length - actArgCount; i++){
							AbstractInsnNode next = passValue(current);
							disposeValue(current, method);
							current = next;
						}
						
						// For non-static target methods remove target object.
						if(mInstr.getOpcode() != INVOKESTATIC) {
							disposeValue(current, method);
						}
						method.instructions.insert(instr, actionCall);
						method.instructions.remove(instr);
					}
				}else{
					// TODO: args case for USE & ASSIGN
				}
			}
		}
	}
	
	private static class BeginEnd{
		private AbstractInsnNode begin;
		private AbstractInsnNode end;
		
		BeginEnd(AbstractInsnNode begin, AbstractInsnNode end) {
			this.begin = begin;
			this.end = end;
		}
		
		BeginEnd() {
			this.begin = null;
			this.end = null;
		}

		AbstractInsnNode getBegin() {
			return begin;
		}

		AbstractInsnNode getEnd() {
			return end;
		}

		void setBegin(AbstractInsnNode begin) {
			this.begin = begin;
		}

		void setEnd(AbstractInsnNode end) {
			this.end = end;
		}
		
	}

}
