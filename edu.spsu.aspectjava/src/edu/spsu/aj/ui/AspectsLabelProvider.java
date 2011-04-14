package edu.spsu.aj.ui;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import edu.spsu.aj.ui.AspectsModel.AspectsContainer;
import edu.spsu.aj.ui.AspectsModel.AspectsPackage;
import edu.spsu.aj.weaver.Aspect;
import edu.spsu.aj.weaver.AspectRule;

class AspectsLabelProvider extends ColumnLabelProvider {

	public String getText(Object obj) {
		if(obj instanceof AspectsContainer){
			return ((AspectsContainer) obj).getPath();
		}else if(obj instanceof AspectsPackage){
			return ((AspectsPackage) obj).getName();
		}else if(obj instanceof Aspect){
			// package.class format
			String name =  ((Aspect) obj).getName();
			// if lastIndexOf == -1 all works good
			return name.substring(name.lastIndexOf('.') + 1);
		}else if(obj instanceof AspectRule){	
			return obj.toString();
		}
		return null;
	}
	
	@Override
	public String getToolTipText(Object element) {
		if(element instanceof AspectsContainer){
			return null;
		}else if(element instanceof AspectsPackage){
			return ((AspectsPackage) element).getPath();
		}else if(element instanceof Aspect){
			return ((Aspect) element).getDescription();
		}else if(element instanceof AspectRule){	
			return ((AspectRule) element).getDescription();
		}
		return null;
	}
	
	@Override
	public int getToolTipTimeDisplayed(Object object) {
		return 5000;
	}
	
	@Override
	public int getToolTipDisplayDelayTime(Object object) {
		return 0;
	}
			
	public Image getImage(Object obj) {
		if(obj instanceof AspectsContainer){
			if(((AspectsContainer) obj).isFolder()){
				return AspectJavaView.folderImage;
			}else{
				return AspectJavaView.jarImage;
			}
		}else if(obj instanceof AspectsPackage){
			return AspectJavaView.packageImage;
		}else if(obj instanceof Aspect){
			return AspectJavaView.aspectImage;
		}else if(obj instanceof AspectRule){
			return AspectJavaView.ruleImage;
		}
		return null;
	}
}