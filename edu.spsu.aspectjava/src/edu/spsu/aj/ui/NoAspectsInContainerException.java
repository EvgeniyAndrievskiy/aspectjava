package edu.spsu.aj.ui;

import java.util.List;

import edu.spsu.aj.ui.AspectsModel.BadAspect;

class NoAspectsInContainerException extends Exception {
	private String containerPath;
	private List<BadAspect> badAspects = null;

	NoAspectsInContainerException(String containerPath, List<BadAspect> badAspects) {
		this.containerPath = containerPath;
		this.badAspects = badAspects;
	}
	
	String getContainerPath() {
		return containerPath;
	}
	
	List<BadAspect> getBadAspects() {
		return badAspects;
	}

}
