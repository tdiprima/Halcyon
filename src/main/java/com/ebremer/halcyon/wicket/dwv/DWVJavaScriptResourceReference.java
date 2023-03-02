package com.ebremer.halcyon.wicket.dwv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Application;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/**
 *
 * @author erich
 */
public class DWVJavaScriptResourceReference extends JavaScriptResourceReference {
    
   	private static final long serialVersionUID = 82802530894457750L;
	private static final DWVJavaScriptResourceReference INSTANCE = new DWVJavaScriptResourceReference();
        
	public static DWVJavaScriptResourceReference get() {
            return INSTANCE;
	}

	private DWVJavaScriptResourceReference() {
            super(DWVJavaScriptResourceReference.class, "js/dwv.min.js");
	}

	@Override
	public List<HeaderItem> getDependencies() {
            List<HeaderItem> dependencies = new ArrayList<>();
            for (Iterator<? extends HeaderItem> iterator = super.getDependencies().iterator(); iterator.hasNext();) {
			HeaderItem headerItem = iterator.next();
			dependencies.add(headerItem);
            }
            dependencies.add(JavaScriptHeaderItem.forReference(Application.get()
		.getJavaScriptLibrarySettings()
		.getJQueryReference()));
            return dependencies;
	} 
}
