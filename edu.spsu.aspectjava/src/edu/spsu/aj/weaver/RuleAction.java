package edu.spsu.aj.weaver;

import org.objectweb.asm.Type;

public class RuleAction {
	private String name;  // method name
	private String desc; // method descriptor	
	private String toString = null;
	
	public RuleAction(String name, String desc) {
		this.name = name;
		this.desc = desc;
	}

	public String getDescriptor() {
		return desc;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		if(toString == null){
			StringBuilder stringBuilder = new StringBuilder("");
			stringBuilder.append(Type.getReturnType(desc).getClassName());
			stringBuilder.append(" ");
			stringBuilder.append(name);
			stringBuilder.append("(");
			
			Type[] argTypes = Type.getArgumentTypes(desc);
			
			if(argTypes.length > 0){
				stringBuilder.append(argTypes[0].getClassName());
			}
			for(int i = 1; i < argTypes.length; i++){
				stringBuilder.append(", " + argTypes[i].getClassName());
			}
			stringBuilder.append(")");
			
			toString = stringBuilder.toString();		
		}
		
		return toString;
	}

}
