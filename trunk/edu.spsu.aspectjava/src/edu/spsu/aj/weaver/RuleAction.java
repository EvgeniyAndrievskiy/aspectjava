package edu.spsu.aj.weaver;

public class RuleAction {
	private String name;  // method name
	private String desc; // method descriptor
	
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

}
