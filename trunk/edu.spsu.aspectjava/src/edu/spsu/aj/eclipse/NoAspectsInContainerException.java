package edu.spsu.aj.eclipse;

class NoAspectsInContainerException extends Exception {
	private String containerPath;

	NoAspectsInContainerException(String containerPath) {
		this.containerPath = containerPath;
	}
	
	String getContainerPath() {
		return containerPath;
	}

}
