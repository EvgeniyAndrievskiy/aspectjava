package edu.spsu.aspectjava.eclipse;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	public static final String IMG_ADD = "icons/add.gif";
	public static final String IMG_CLASS = "icons/aspect.gif";
	public static final String IMG_ASPECT_JAVA_FRAMEWORK = "icons/aspect.java.gif";
	public static final String IMG_DELETE = "icons/delete.gif";
	public static final String IMG_ARROW_DOWN = "icons/down.gif";
	public static final String IMG_ARROW_UP = "icons/up.gif";
	public static final String IMG_FIND = "icons/find.gif";
	public static final String IMG_JAR = "icons/jar.gif";
	public static final String IMG_JAVA_PROJECT = "icons/javaproject.gif";
	public static final String IMG_OPTIONS = "icons/options.gif";
	public static final String IMG_REFRESH = "icons/refresh.gif";
	public static final String IMG_METHOD = "icons/rule.gif";
	public static final String IMG_SET_PROJECT = "icons/set_proj.gif";
	public static final String IMG_RESET = "icons/reset.gif";
	public static final String IMG_WEAVE = "icons/weave.gif";

	// The plug-in ID
	public static final String PLUGIN_ID = "edu.spsu.aspectjava";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
