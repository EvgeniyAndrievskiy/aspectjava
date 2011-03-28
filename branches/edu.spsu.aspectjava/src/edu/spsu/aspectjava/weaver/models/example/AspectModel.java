package edu.spsu.aspectjava.weaver.models.example;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AspectModel{
	private ArrayList<AspectJar> aspectJars;	
	private List<AspectModelListener> listeners;
	
	public AspectModel(){
		aspectJars = new ArrayList<AspectJar>();
		listeners = new LinkedList<AspectModelListener>();
	}
	
	public List<AspectJar> getAspectJars() {
		return aspectJars;
	}
	
	public AspectJar getFirstJar(){
		if(isEmpty()){
			return null;
		}
		return aspectJars.get(0);
	}
	
	public AspectJar getLastJar(){
		if(isEmpty()){
			return null;
		}
		return aspectJars.get(getJarCount() - 1);
	}
	
	public int indexOf(AspectJar aspectsJar) {
		return aspectJars.indexOf(aspectsJar);
	}
	
	public int getJarCount(){
		return aspectJars.size();
	}
	
	public boolean isEmpty(){
		return getJarCount() == 0;
	}
	
	public void addListener(AspectModelListener l){
		listeners.add(l);
	}
	
	public void removeListener(AspectModelListener l){ 
		listeners.remove(l);
	}
	
	public boolean addAspectJar(JarFile jarFile) throws NoAspectsInJarException{
		if(containsJarFile(jarFile)){
			return false;
		}
		AspectJar aspectJar = parseAspectJar(jarFile);
		if(aspectJar == null){
			throw new NoAspectsInJarException(jarFile);
		}
		aspectJars.add(aspectJar);

		for(AspectModelListener listener:listeners){
			listener.addedAspectJar(aspectJar);
		}		
		return true;
	}
	
	public boolean removeAspectJar(AspectJar aspectsJar) {
		int index = indexOf(aspectsJar);
		boolean removed = aspectJars.remove(aspectsJar);
		if(!removed){
			return false;
		}
		
		for(AspectModelListener listener:listeners){
			listener.removedAspectJar(aspectsJar, index);
		}
		return true;
	}
	
	public boolean moveJarDown(AspectJar aspectsJar) {
		int index = indexOf(aspectsJar);
		if(index == -1){
			return false;
		}
		if(index == getJarCount() - 1){
			return false;
		}
		aspectJars.set(index, aspectJars.get(index + 1));
		aspectJars.set(index + 1, aspectsJar);
		
		for(AspectModelListener listener:listeners){
			listener.movedJarDown(aspectsJar);
		}
		
		return true;
	}
	
	public boolean moveJarUp(AspectJar aspectsJar) {
		int index = indexOf(aspectsJar);
		if(index == -1){
			return false;
		}
		if(index == 0){
			return false;
		}
		aspectJars.set(index, aspectJars.get(index - 1));
		aspectJars.set(index - 1, aspectsJar);
		
		for(AspectModelListener listener:listeners){
			listener.movedJarUp(aspectsJar);
		}
		
		return true;
	}
	
	private boolean containsJarFile(JarFile jarFile) {
		for(AspectJar aspectsJar:aspectJars){
			if(aspectsJar.getJarFile().getName().equals(jarFile.getName())){
				return true;
			}
		}
		return false;
	}

	private AspectJar parseAspectJar(JarFile jarFile) {
		AspectJar aspectsJar = new AspectJar(jarFile);
		URLClassLoader classLoader = null;
		try {
			// TODO: add annotations path to classpathes
			classLoader = new URLClassLoader(new URL[] { new URL(
					"jar:file:/" + jarFile.getName() + "!/")});
		} catch (MalformedURLException e) {
		}

		Enumeration<JarEntry> jarEntries = jarFile.entries();
		LinkedList<Aspect> list = new LinkedList<Aspect>();
		while (jarEntries.hasMoreElements()) {
			JarEntry entry = jarEntries.nextElement();
			if (entry.toString().endsWith("class")) {
				String str = entry.toString().replace('/', '.');
				String className = str.substring(0, str.length() - 6);
				Class class1 = null;
				try {
					class1 = classLoader.loadClass(className);
				} catch (ClassNotFoundException e) {
				}					
				Aspect aspect = parseAspectClass(class1);
				// TODO decide what is aspectsJar
//				if (aspect == null) {
//					return null;
//				}
				if(aspect != null){
					aspect.setAspectJar(aspectsJar);
					list.add(aspect);
				}
			}
		}
		
		if(list.isEmpty()){
			return null;
		}

		aspectsJar.setAspects(list);

		return aspectsJar;
	}
	
	private Aspect parseAspectClass(Class class1){
		// TODO verify that class1 extends Aspect
		Aspect aspect = new Aspect(class1);
		
		List<AspectWeavingRule> weavingRules = new LinkedList<AspectWeavingRule>();
		Method[] methods = class1.getDeclaredMethods();
		
//		Class class2 = null;
//		try {
//			class2 = class1.getClassLoader().loadClass("edu.spsu.aspectjava.weaver.models.example.AspectAction");
//		} catch (ClassNotFoundException e) {
//		}
		
		for(Method method:methods){			
//			Annotation annotation = method.getAnnotation(class2);
//			if(annotation == null){
//				continue;
//			}
			if(Modifier.isPublic(method.getModifiers()) && 
					Modifier.isStatic(method.getModifiers())){
				weavingRules.add(new AspectWeavingRule(aspect, method, null));
			}
		}
		
		if(weavingRules.isEmpty()){
			return null;
		}
		
		aspect.setWeavingRules(weavingRules);
		
		return aspect;
	}
	
}