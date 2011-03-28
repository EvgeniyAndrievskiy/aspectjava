package edu.spsu.aspectjava.weaver.models.example;

import java.lang.reflect.Method;


public class AspectWeavingRule{
	private Aspect aspect;
	private Method action;
	private AspectAction condition;
	
	private String toString;
	private boolean firstToString = true;
	
	public AspectWeavingRule(Aspect aspect, Method action) {
		this.aspect = aspect;
		this.action = action;
		this.condition = null;
	}

	public AspectWeavingRule(Aspect aspect, Method action, AspectAction condition) {
		this.action = action;
		this.aspect = aspect;
		this.condition = condition;
	}



	public Aspect getAspect() {
		return aspect;
	}

	public Method getAction() {
		return action;
	}
	
	public AspectAction getCondition(){
		return condition;
	}
	
	public String toString(){
		if(firstToString){
			StringBuffer stringBuffer = new StringBuffer("");
			stringBuffer.append(action.getReturnType().getName());
			stringBuffer.append(" ");
			stringBuffer.append(action.getName());
			stringBuffer.append("(");
			
			Class[] paramTypes = action.getParameterTypes();
			
			if(paramTypes.length > 0){
				stringBuffer.append(paramTypes[0].getSimpleName());
			}
			for(int i = 1; i < paramTypes.length; i++){
				stringBuffer.append(", " + paramTypes[i].getSimpleName());
			}
			stringBuffer.append(") -> ");
			// TODO: use condition.value() instead of condition
			stringBuffer.append(condition);
			
			toString = stringBuffer.toString();
			
			firstToString = false;
		}
		
		return toString;
	}
}
