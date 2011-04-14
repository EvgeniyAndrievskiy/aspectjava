package edu.spsu.aj.ui;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import edu.spsu.aj.ui.AspectsModel.AspectsContainer;
import edu.spsu.aj.ui.AspectsModel.AspectsPackage;
import edu.spsu.aj.weaver.Aspect;
import edu.spsu.aj.weaver.AspectRule;

class AspectsContentProvider implements ITreeContentProvider {
	private Object[] emptyArray = new Object[0];

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}

	public void dispose() {
	}

	public Object[] getElements(Object parent) {
		return ((AspectsModel) parent).getAspectsContainers().toArray();
	}

	public Object getParent(Object child) {
//		if (child instanceof AspectsContainer) {
//			return null;
//		} else if (child instanceof Aspect) {
//			// return ((Aspect) child).getAspectJar();
//		} else if (child instanceof AspectRule) {
//			// return ((AspectWeavingRule) child).getAspect();
//		}
		return null;
	}

	public Object[] getChildren(Object parent) {
		if (parent instanceof AspectsContainer) {
			return ((AspectsContainer) parent).getPackages().toArray();
		} else if (parent instanceof AspectsPackage) {
			return ((AspectsPackage) parent).getAspects().toArray();
		} else if (parent instanceof Aspect) {
			return ((Aspect) parent).getRules().toArray();
		} else if (parent instanceof AspectRule) {
			return emptyArray;
		}
		return null;
	}

	public boolean hasChildren(Object parent) {
		if (parent instanceof AspectsContainer) {
			return ! ((AspectsContainer) parent).isEmpty();
		} else if (parent instanceof AspectsPackage) {
			return ! ((AspectsPackage) parent).getAspects().isEmpty();
		} else if (parent instanceof Aspect) {
			return ! ((Aspect) parent).getRules().isEmpty();
		} else if (parent instanceof AspectRule) {
			return false;
		}
		return false;
	}

}
