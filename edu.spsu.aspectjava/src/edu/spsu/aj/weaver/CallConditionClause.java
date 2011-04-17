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
	
	
	CallConditionClause(String condClause)  throws BadCondClauseExc{
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
		public static final String FORMAT = 
			"%args '(' arg'[' index ']' [ ',' arg'[' index ']' ]* ')'";
		private int[] info;
		private int argsCount;
		
		MethodArgsInfo(String str) throws BadCondClauseExc {
			super(str);	
		}

		@Override
		int getArgsCount() {
			return argsCount;
		}

		@Override
		Object getInfo() {
			return info;
		}

		@Override
		void setArgsCount(int argsCount) {
			this.argsCount = argsCount;		
		}

		@Override
		void setInfo(Object info) {
			this.info = (int[]) info;
		}

		@Override
		protected void parseArgsInfoString(String str) throws BadCondClauseExc {
			
			if(! toString.startsWith("%args")){
				throw new BadCondClauseExc(condClauseStr, "Bad format of" +
						" args info, it should start with '%args': "
						+ toString);
			}
			String str1 = toString.substring(5).trim();
			// str1 is supposed to be in the format of '( <arg-indices> )'
			if(! str1.startsWith("(")){
				throw new BadCondClauseExc(condClauseStr, "Bad format of" +
						" args info, '(' is missed: " + toString);
			}
			if(! str1.endsWith(")")){
				throw new BadCondClauseExc(condClauseStr, "Bad format of" +
						" args info, ')' is missed: " + toString);
			}
			String str2 = str1.substring(1, str1.length() - 1).trim();
			// str2 contains only arg-indices
			
			// Special case %args (..), that means all arguments from target method are passed into action.
			// This case is equivalent to argsInfo.getArgsCount() == -1 and/or argsInfo.getInfo() == null.
			// It's supposed to be filled by actual data on rule creating stage when action is known.
			if(str2.equals("..")){
				info = null;
				argsCount = -1;
				return;
			}
			String[] sa = str2.split(",");
			// sa contains strings in the format of ' arg[ <index> ] ' 
			info = new int[sa.length];
			for(int i = 0; i < sa.length; i++){
				String s = sa[i].trim();
				if(! s.startsWith("arg[") | ! s.endsWith("]")){
					throw new BadCondClauseExc(condClauseStr, "Bad arg-index format in" +
							" args info, 'arg[ <index> ]' expected: " + s);
				}
				String is = s.substring(4, s.length() - 1).trim();
				try {
					info[i] = Integer.parseInt(is);
				} catch (NumberFormatException e) {
					throw new BadCondClauseExc(condClauseStr, "Bad arg-index format in" +
							" args info, 'arg[ <index> ]' expected: " + s);
				}
			}
			argsCount = info.length;
			
		}
	
	}
	
	private class MethodPattern{
		private MethodNameFilter nameFilter;
		private MethodArgTypes argTypes;
		private boolean hasArgTypes;
		
		MethodPattern(String pattern) throws BadCondClauseExc {
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
					throw new BadCondClauseExc(condClauseStr, "Bad format of method pattern: "
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
			
			MethodNameFilter(String nf) throws BadCondClauseExc {
				nf = nf.trim();
				String[] tokens = nf.split("  *");		
				try {
					if(tokens.length == 4){
						initAccess(tokens[0]);
						hasStatic = true;
						if(!tokens[1].equals("static")){
							throw new BadCondClauseExc(condClauseStr, 
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
					throw new BadCondClauseExc(condClauseStr, "Bad format of method name filter: "
							+ nf);
				}
			}
			
			private void initAccess(String accStr) throws BadCondClauseExc{
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
					throw new BadCondClauseExc(condClauseStr, 
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
			
			MethodArgTypes(String at) throws BadCondClauseExc {
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
					throw new BadCondClauseExc(condClauseStr, "Bad format of method pattern arg types: "
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
