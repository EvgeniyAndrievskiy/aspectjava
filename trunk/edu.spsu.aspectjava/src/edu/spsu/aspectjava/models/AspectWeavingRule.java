package edu.spsu.aspectjava.models;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AspectWeavingRule{
	private Aspect aspect;
	private Method action;
	private Annotation condition;
	
	private String toString;
	private boolean firstToString = true;
	
	public AspectWeavingRule(Aspect aspect, Method action) {
		this.aspect = aspect;
		this.action = action;
		this.condition = null;
	}

	public AspectWeavingRule(Aspect aspect, Method action, Annotation condition) {
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
	
	public Annotation getCondition(){
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
			stringBuffer.append(condition);
			
			toString = stringBuffer.toString();
			
			firstToString = false;
		}
		
		return toString;
	}
}
