package edu.spsu.aspectjava.models;

import java.util.List;

public class Aspect{
	private AspectJar aspectJar;
	private Class aspectClass;
	private List<AspectWeavingRule> weavingRules;
	
	public Aspect(Class aspectClass){
		this.aspectClass = aspectClass;
	}
	
	public Aspect(AspectJar aspectsJar, Class aspectClass){
		this.aspectJar = aspectsJar;
		this.aspectClass = aspectClass;
	}
	
	public Aspect(AspectJar aspectsJar, Class aspectClass,
			List<AspectWeavingRule> weavingRules){
		this.aspectJar = aspectsJar;
		this.aspectClass = aspectClass;
		this.weavingRules = weavingRules;
	}

	public AspectJar getAspectJar() {
		return aspectJar;
	}
	
	public void setAspectJar(AspectJar aspectsJar) {
		this.aspectJar = aspectsJar;	
	}

	public Class getAspectClass() {
		return aspectClass;
	}
	
	public List<AspectWeavingRule> getWeavingRules() {
		return weavingRules;
	}

	public void setWeavingRules(List<AspectWeavingRule> weavingRules) {
		this.weavingRules = weavingRules;
	}
}
