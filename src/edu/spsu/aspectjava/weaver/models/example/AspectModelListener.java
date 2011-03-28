package edu.spsu.aspectjava.weaver.models.example;

public interface AspectModelListener{	
	public void addedAspectJar(AspectJar aspectJar);

	public void removedAspectJar(AspectJar aspectJar, int index);
	
	public void movedJarDown(AspectJar aspectJar);
	
	public void movedJarUp(AspectJar aspectJar);
}
