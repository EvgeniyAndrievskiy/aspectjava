package edu.spsu.aj.weaver;

import java.util.regex.PatternSyntaxException;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class CallConditionClause extends AbstractConditionClause {
	private MethodPattern methPattern;
	private ArgsInfo argsInfo;
	
	
	CallConditionClause(String condClause)  throws BadCondClauseFormat{
		super(condClause);
		methPattern = new MethodPattern(patternStr);	
		if(argsInfoStr == null){
			argsInfo = null;
		}else{
			argsInfo = new MethodArgsInfo(argsInfoStr);			
		}
	}

	@Override
	boolean accepts(AbstractInsnNode instr, MethodNode method, ClassNode class1) {
		if(instr.getType() != AbstractInsnNode.METHOD_INSN){
			return false;
		}
		if(checkRestrictions(instr, method, class1) == false){
			return false;
		}
		return methPattern.accepts(instr, method, class1);
	}

	
	@Override
	public ArgsInfo getArgsInfo() {
		return argsInfo;
	}

	private class MethodArgsInfo extends ArgsInfo{
		private int[] argsInfo;
		private int argsCount;
		
		MethodArgsInfo(String argsInfoStr) {
			super(argsInfoStr);
			
			// Special case %args(..), that means all arguments from target method are passed into action.
			// This case is equivalent to argsInfo.getArgsCount() == -1 and/or argsInfo.getInfo() == null.
			// It's supposed to be filled by actual data on rule creating stage when action is known.
			if(argsInfoStr.contains("..")){
				argsInfo = null;
				argsCount = -1;
			}else{
				// TODO: modify code below to parse argsInfoStr (with syntax error checking).
				String[] sa = argsInfoStr.split("\\D+");
				argsInfo = new int[sa.length - 1];
				for(int i = 1; i < sa.length; i++){
					argsInfo[i - 1] = Integer.parseInt(sa[i]);
				}
				argsCount = argsInfo.length;
			}
		}

		@Override
		int getArgsCount() {
			return argsCount;
		}

		@Override
		Object getInfo() {
			return argsInfo;
		}

		@Override
		void setArgsCount(int argsCount) {
			this.argsCount = argsCount;		
		}

		@Override
		void setInfo(Object info) {
			this.argsInfo = (int[]) info;
		}
	
	}
	
	private class MethodPattern{
		private MethodNameFilter nameFilter;
		private MethodArgTypes argTypes;
		private boolean hasArgTypes;
		
		MethodPattern(String pattern) throws BadCondClauseFormat {
			pattern = pattern.trim();
			int br = pattern.indexOf('(');
			if(br < 0){
				hasArgTypes = false;
				nameFilter = new MethodNameFilter(pattern);
				argTypes = null;
			}else{
				nameFilter = new MethodNameFilter(pattern.substring(0, br));
				hasArgTypes = true;
				// cut surrounding brackets
				if(pattern.charAt(pattern.length() - 1) != ')'){
					throw new BadCondClauseFormat(condClauseStr, "Bad format of method pattern: "
							+ pattern);
				}else{
					argTypes = new MethodArgTypes(pattern.substring(br + 1,
							pattern.length() - 1));
				}
			}
		}
		
		boolean accepts(AbstractInsnNode instr, MethodNode method, ClassNode class1){
			if(hasArgTypes){
				return argTypes.accepts(instr, method, class1) && 
					nameFilter.accepts(instr, method, class1);
			}else{
				return nameFilter.accepts(instr, method, class1);
			}
		}
		
		
		private class MethodNameFilter{
			public static final int PUBLIC = 0;
			public static final int PRIVATE = 1;
			public static final int PROTECTED = 2;
			public static final int DEFAULT = 3;
			
			private int access;
			private boolean hasAccess = false;
			private boolean hasStatic = false;
			private EntityWildCard retType;
			private boolean hasRetType = false;
			private EntityWildCard methNameWC;
			
			MethodNameFilter(String nf) throws BadCondClauseFormat {
				nf = nf.trim();
				String[] tokens = nf.split("  *");		
				try {
					if(tokens.length == 4){
						initAccess(tokens[0]);
						hasStatic = true;
						if(!tokens[1].equals("static")){
							throw new BadCondClauseFormat(condClauseStr, 
									"Unexpected token instead of \"static\": "
									 + tokens[1]);
						}
						hasRetType = true;
						retType = new EntityWildCard(tokens[2].equals("*")?"void"
								:tokens[2], EntityWildCard.TYPE_WC);
						methNameWC = new EntityWildCard(tokens[3], 
								EntityWildCard.METHOD_WC);
					}else if(tokens.length == 3){
						methNameWC = new EntityWildCard(tokens[2], 
								EntityWildCard.METHOD_WC);
						if(tokens[0].equals("static")){
							hasAccess = false;
							hasStatic = true;
							hasRetType = true;
							retType = new EntityWildCard(tokens[1].equals("*")?"void"
									:tokens[1], EntityWildCard.TYPE_WC);
						}else if(tokens[1].equals("static")){
							hasStatic = true;
							initAccess(tokens[0]);
							hasRetType = false;
						}
						// if there is no "static"...
						else{
							hasStatic = false;
							initAccess(tokens[0]);
							hasRetType = true;
							retType = new EntityWildCard(tokens[1].equals("*")?"void"
									:tokens[1], EntityWildCard.TYPE_WC);
						}
					}else if(tokens.length == 2){
						methNameWC = new EntityWildCard(tokens[1], 
								EntityWildCard.METHOD_WC);
						if(tokens[0].equals("static")){
							hasAccess = false;
							hasStatic = true;
							hasRetType = false;	
						}else{
							hasStatic = false;
							if(tokens[0].equals("public")){
								access = PUBLIC;
								hasAccess = true;
								hasRetType = false;
							}else if(tokens[0].equals("private")){
								access = PRIVATE;
								hasAccess = true;
								hasRetType = false;
							}else if(tokens[0].equals("protected")){
								access = PROTECTED;
								hasAccess = true;
								hasRetType = false;
							// fix ambiguous case a-la "* somemethod": here "*" is void
							}else if(tokens[0].equals("default")){
								access = DEFAULT;
								hasAccess = true;
								hasRetType = false;
							}else{
								hasAccess = false;
								hasRetType = true;
								retType = new EntityWildCard(tokens[0].equals("*")?"void"
										:tokens[0], EntityWildCard.TYPE_WC);
							}
						}
					}else{
						methNameWC = new EntityWildCard(tokens[0], 
								EntityWildCard.METHOD_WC);
						hasAccess = false;
						hasStatic = false;
						hasRetType = false;
					}
				} catch (PatternSyntaxException	 e) {
					throw new BadCondClauseFormat(condClauseStr, "Bad format of method name filter: "
							+ nf);
				}
			}
			
			private void initAccess(String accStr) throws BadCondClauseFormat{
				hasAccess = true;
				if(accStr.equals("public")){
					access = PUBLIC;
				}else if(accStr.equals("private")){
					access = PRIVATE;
				}else if(accStr.equals("protected")){
					access = PROTECTED;				
				}else if(accStr.equals("*")){
					access = DEFAULT;
				}else{
					throw new BadCondClauseFormat(condClauseStr, 
					"Unexpected access string in method pattern: "
							+ accStr);
				}
			}
			
			boolean accepts(AbstractInsnNode instr, MethodNode method, ClassNode class1){
				MethodInsnNode methInstr = (MethodInsnNode) instr;
				if(hasStatic){
					if(methInstr.getOpcode() != Opcodes.INVOKESTATIC){
						return false;
					}
				}
				if(hasRetType){
					String retTypeString = Type.getReturnType(methInstr.desc).getClassName();
					if(!retType.hasPackage()){
						int dot = retTypeString.lastIndexOf('.');
						retTypeString = retTypeString.substring(dot + 1);
					}
					if(!retType.matches(retTypeString)){
						return false;
					}
				}
				if(methNameWC.hasClass()){
					String methOwnerStr = methInstr.owner.replace('/', '.');
					if(methNameWC.hasPackage()){
						return methNameWC.matches(methOwnerStr + '.' + methInstr.name);
					}else{
						int dot = methOwnerStr.lastIndexOf('.');
						return methNameWC.matches(methOwnerStr.substring(dot + 1)
								+ '.' + methInstr.name);
					}
				}else{
					return methNameWC.matches(methInstr.name);
				}
			}	
		}
		
		private class MethodArgTypes{
			private EntityWildCard[] aTypes;
			
			MethodArgTypes(String at) throws BadCondClauseFormat {
				at = at.trim();
				String[] argTStrings = at.split(",");
				
				int length = argTStrings.length;
				aTypes = new EntityWildCard[length];
				
				String str = argTStrings[0].trim();
				try {
					if(str.equals("..")){
						aTypes[0] = null;
					}else{
						aTypes[0] = new EntityWildCard(str, EntityWildCard.TYPE_WC);
					}
					str = argTStrings[length - 1].trim();
					if(str.equals("..")){
						aTypes[length - 1] = null;
					}else{
						aTypes[length - 1] = new EntityWildCard(str, EntityWildCard.TYPE_WC);
					}
					for(int i = 1; i < length - 1; i++){
						aTypes[i] = new EntityWildCard(argTStrings[i].trim(), 
								EntityWildCard.TYPE_WC);
					}
				} catch (PatternSyntaxException e) {
					throw new BadCondClauseFormat(condClauseStr, "Bad format of method pattern arg types: "
							+ at);
				}
			}
			
			boolean accepts(AbstractInsnNode instr, MethodNode method, ClassNode class1){
				MethodInsnNode methInstr = (MethodInsnNode) instr;
				Type[] types = Type.getArgumentTypes(methInstr.desc);
				
				if(aTypes[0] == null){
					// fix (..) case
					if(aTypes.length == 1){
						return true;
					}
					if(aTypes[aTypes.length - 1] == null){
						// fix (.., ..) case
						if(aTypes.length == 2){
							return true;
						}
						int endI = types.length - aTypes.length + 2;
						for(int i = 0; i < endI + 1; i++){
							for(int j = 1; j < aTypes.length - 1; j++){
								String typeName = types[i + j - 1].getClassName();
								if(!aTypes[j].hasPackage()){
									int dot = typeName.lastIndexOf('.');
									typeName = typeName.substring(dot + 1);
								}
								if(aTypes[j].matches(typeName)){
									if(j == aTypes.length - 2){
										return true;
									}
									continue;
								}else{
									break;
								}
							}
						}
						return false;
					}else{
						if(types.length < aTypes.length - 1){
							return false;
						}
						// go from right to left
						for(int j = 0; j < aTypes.length - 1; j++){
							String typeName = types[types.length - 1 - j].getClassName();
							if(!aTypes[aTypes.length - 1 - j].hasPackage()){
								int dot = typeName.lastIndexOf('.');
								typeName = typeName.substring(dot + 1);
							}
							if(!aTypes[aTypes.length - 1 - j].matches(typeName)){
								return false;
							}
						}
						return true;
					}
				}else{
					int aLength; // actual length of aTypes, without any null
					if(aTypes[aTypes.length - 1] != null){
						aLength = aTypes.length;
						if(types.length != aTypes.length){
							return false;
						}
					}else{
						aLength = aTypes.length - 1;
					}
					if(types.length < aLength){
						return false;
					}
					for(int j = 0; j < aLength; j++){
						String typeName = types[j].getClassName();
						if(!aTypes[j].hasPackage()){
							int dot = typeName.lastIndexOf('.');
							typeName = typeName.substring(dot + 1);
						}
						if(!aTypes[j].matches(typeName)){
							return false;
						}
					}
					return true;
				}			
			}
			
		}
	}
	
}
