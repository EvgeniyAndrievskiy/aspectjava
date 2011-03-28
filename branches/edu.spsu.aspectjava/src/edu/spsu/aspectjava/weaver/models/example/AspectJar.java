package edu.spsu.aspectjava.weaver.models.example;

import java.util.List;
import java.util.jar.JarFile;

public class AspectJar{
	private JarFile jarFile;
	private List<Aspect> aspects;

	public AspectJar(JarFile jarFile){
		this.jarFile = jarFile;
	}
	
	public AspectJar(JarFile jarFile, List<Aspect> aspects){
		this.jarFile = jarFile;
		this.aspects = aspects;
	}
	
	public JarFile getJarFile() {
		return jarFile;
	}

	public void setJarFile(JarFile jarFile) {
		this.jarFile = jarFile;
	}

	public List<Aspect> getAspects() {
		return aspects;
	}

	public void setAspects(List<Aspect> aspects) {
		this.aspects = aspects;
	}	
}
