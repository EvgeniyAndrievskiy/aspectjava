package edu.spsu.aspectjava.models;

import java.util.jar.JarFile;

public class NoAspectsInJarException extends Exception {
	private JarFile jarFile;

	public NoAspectsInJarException(JarFile jarFile) {
		this.jarFile = jarFile;
	}
	
	public JarFile getJarFile() {
		return jarFile;
	}

}
