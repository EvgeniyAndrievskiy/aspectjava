package edu.spsu.aj.ui;

class NoAspectsInContainerException extends Exception {
	private String containerPath;

	NoAspectsInContainerException(String containerPath) {
		this.containerPath = containerPath;
	}
	
	String getContainerPath() {
		return containerPath;
	}

}
