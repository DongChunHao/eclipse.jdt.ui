package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;

public class TemplateContentProvider implements IStructuredContentProvider {

	private StructuredViewer fViewer;
	private TemplateSet fTemplateSet;	

	public TemplateContentProvider(StructuredViewer viewer, TemplateSet templateSet) {
		fViewer= viewer;
		fTemplateSet= templateSet;
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */	
	public Object[] getElements(Object input) {
		return fTemplateSet.getTemplates();
	}

	/**
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/**
	 * @see IContentProvider#dispose()
	 */
	public void dispose() {
		fViewer= null;
		fTemplateSet= null;
	}
	
}

