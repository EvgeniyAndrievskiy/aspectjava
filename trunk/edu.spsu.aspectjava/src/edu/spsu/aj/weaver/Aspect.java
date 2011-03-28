package edu.spsu.aj.weaver;

import java.util.Iterator;
import java.util.List;

public class Aspect {
	private String name; // package.class format
	private String description;  // from AspectDescription annotation
	private List<AspectRule> rules;
	
	public Aspect(String name, List<AspectRule> rules){
		if(name.contains("/")){
			name = name.replace('/', '.');
		}
		this.name = name;
		description = null;
		this.rules = rules;
	}
	
	public Aspect(String name, String desc, List<AspectRule> rules){
		if(name.contains("/")){
			name = name.replace('/', '.');
		}
		this.name = name;
		this.description = desc;
		this.rules = rules;
	}
	
	public List<AspectRule> getRules(){
		return rules;
	}
	
	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}
}
