/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BidiSegmentEvent;
import org.eclipse.swt.custom.BidiSegmentListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension3;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.AddTaskAction;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.ExtendedTextEditor;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
import org.eclipse.ui.texteditor.ResourceAction;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.tasklist.TaskList;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.OpenEditorActionGroup;
import org.eclipse.jdt.ui.actions.OpenViewActionGroup;
import org.eclipse.jdt.ui.actions.ShowActionGroup;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.GoToNextPreviousMemberAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectEnclosingAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectHistoryAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectNextAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectPreviousAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectionAction;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.JavaChangeHover;
import org.eclipse.jdt.internal.ui.text.JavaPairMatcher;
import org.eclipse.jdt.internal.ui.util.JavaUIHelp;
import org.eclipse.jdt.internal.ui.viewsupport.IViewPartInputProvider;

/**
 * Java specific text editor.
 */
public abstract class JavaEditor extends ExtendedTextEditor implements IViewPartInputProvider {
	
	/**
	 * Internal implementation class for a change listener.
	 * @since 3.0
	 */
	protected abstract class AbstractSelectionChangedListener implements ISelectionChangedListener  {

		/**
		 * Installs this selection changed listener with the given selection provider. If
		 * the selection provider is a post selection provider, post selection changed
		 * events are the preferred choice, otherwise normal selection changed events
		 * are requested.
		 * 
		 * @param selectionProvider
		 */
		public void install(ISelectionProvider selectionProvider) {
			if (selectionProvider == null)
				return;
				
			if (selectionProvider instanceof IPostSelectionProvider)  {
				IPostSelectionProvider provider= (IPostSelectionProvider) selectionProvider;
				provider.addPostSelectionChangedListener(this);
			} else  {
				selectionProvider.addSelectionChangedListener(this);
			}
		}

		/**
		 * Removes this selection changed listener from the given selection provider.
		 * 
		 * @param selectionProvider
		 */
		public void uninstall(ISelectionProvider selectionProvider) {
			if (selectionProvider == null)
				return;
			
			if (selectionProvider instanceof IPostSelectionProvider)  {
				IPostSelectionProvider provider= (IPostSelectionProvider) selectionProvider;
				provider.removePostSelectionChangedListener(this);
			} else  {
				selectionProvider.removeSelectionChangedListener(this);
			}			
		}
	}

	/**
	 * Updates the Java outline page selection and this editor's range indicator.
	 * 
	 * @since 3.0
	 */
	private class EditorSelectionChangedListener extends AbstractSelectionChangedListener {
		
		/*
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			selectionChanged();
		}

		public void selectionChanged() {
			ISourceReference element= computeHighlightRangeSourceReference();
			synchronizeOutlinePage(element);
			setSelection(element, false);
		}
	}
		
	/**
	 * Updates the selection in the editor's widget with the selection of the outline page. 
	 */
	class OutlineSelectionChangedListener  extends AbstractSelectionChangedListener {
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}
	}
	
	/*
	 * Link mode.  
	 */
	class MouseClickListener implements KeyListener, MouseListener, MouseMoveListener,
		FocusListener, PaintListener, IPropertyChangeListener, IDocumentListener, ITextInputListener {

		/** The session is active. */
		private boolean fActive;

		/** The currently active style range. */
		private IRegion fActiveRegion;
		/** The currently active style range as position. */
		private Position fRememberedPosition;
		/** The hand cursor. */
		private Cursor fCursor;
		
		/** The link color. */
		private Color fColor;
		/** The key modifier mask. */
		private int fKeyModifierMask;

		public void deactivate() {
			deactivate(false);
		}

		public void deactivate(boolean redrawAll) {
			if (!fActive)
				return;

			repairRepresentation(redrawAll);			
			fActive= false;
		}

		public void install() {

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;
				
			StyledText text= sourceViewer.getTextWidget();			
			if (text == null || text.isDisposed())
				return;
				
			updateColor(sourceViewer);

			sourceViewer.addTextInputListener(this);
			
			IDocument document= sourceViewer.getDocument();
			if (document != null)
				document.addDocumentListener(this);			

			text.addKeyListener(this);
			text.addMouseListener(this);
			text.addMouseMoveListener(this);
			text.addFocusListener(this);
			text.addPaintListener(this);
			
			updateKeyModifierMask();
			
			IPreferenceStore preferenceStore= getPreferenceStore();
			preferenceStore.addPropertyChangeListener(this);			
		}
		
		private void updateKeyModifierMask() {
			String modifiers= getPreferenceStore().getString(BROWSER_LIKE_LINKS_KEY_MODIFIER);
			fKeyModifierMask= computeStateMask(modifiers);
			if (fKeyModifierMask == -1) {
				// Fallback to stored state mask
				fKeyModifierMask= getPreferenceStore().getInt(BROWSER_LIKE_LINKS_KEY_MODIFIER_MASK);
			}
		}

		private int computeStateMask(String modifiers) {
			if (modifiers == null)
				return -1;
		
			if (modifiers.length() == 0)
				return SWT.NONE;

			int stateMask= 0;
			StringTokenizer modifierTokenizer= new StringTokenizer(modifiers, ",;.:+-* "); //$NON-NLS-1$
			while (modifierTokenizer.hasMoreTokens()) {
				int modifier= EditorUtility.findLocalizedModifier(modifierTokenizer.nextToken());
				if (modifier == 0 || (stateMask & modifier) == modifier)
					return -1;
				stateMask= stateMask | modifier;
			}
			return stateMask;
		}
		
		public void uninstall() {

			if (fColor != null) {
				fColor.dispose();
				fColor= null;
			}
			
			if (fCursor != null) {
				fCursor.dispose();
				fCursor= null;
			}
			
			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;
				
			sourceViewer.removeTextInputListener(this);

			IDocument document= sourceViewer.getDocument();
			if (document != null)
				document.removeDocumentListener(this);
				
			IPreferenceStore preferenceStore= getPreferenceStore();
			if (preferenceStore != null)
				preferenceStore.removePropertyChangeListener(this);
			
			StyledText text= sourceViewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;
				
			text.removeKeyListener(this);
			text.removeMouseListener(this);
			text.removeMouseMoveListener(this);
			text.removeFocusListener(this);
			text.removePaintListener(this);
			}
				
		/*
		 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(JavaEditor.LINK_COLOR)) {
				ISourceViewer viewer= getSourceViewer();
				if (viewer != null)	
					updateColor(viewer);
			} else if (event.getProperty().equals(BROWSER_LIKE_LINKS_KEY_MODIFIER)) {
				updateKeyModifierMask();
			}
		}

		private void updateColor(ISourceViewer viewer) {
			if (fColor != null)
				fColor.dispose();
	
			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;

			Display display= text.getDisplay();
			fColor= createColor(getPreferenceStore(), JavaEditor.LINK_COLOR, display);
		}

		/**
		 * Creates a color from the information stored in the given preference store.
		 * Returns <code>null</code> if there is no such information available.
		 */
		private Color createColor(IPreferenceStore store, String key, Display display) {
		
			RGB rgb= null;		
			
			if (store.contains(key)) {
				
				if (store.isDefault(key))
					rgb= PreferenceConverter.getDefaultColor(store, key);
				else
					rgb= PreferenceConverter.getColor(store, key);
			
				if (rgb != null)
					return new Color(display, rgb);
			}
			
			return null;
		}		
	
		private void repairRepresentation() {			
			repairRepresentation(false);
		}

		private void repairRepresentation(boolean redrawAll) {			

			if (fActiveRegion == null)
				return;
				
			ISourceViewer viewer= getSourceViewer();
			if (viewer != null) {
				resetCursor(viewer);

				int offset= fActiveRegion.getOffset();
				int length= fActiveRegion.getLength();

				// remove style
				if (!redrawAll && viewer instanceof ITextViewerExtension2)
					((ITextViewerExtension2) viewer).invalidateTextPresentation(offset, length);
				else
					viewer.invalidateTextPresentation();

				// remove underline				
				if (viewer instanceof ITextViewerExtension3) {
					ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
					offset= extension.modelOffset2WidgetOffset(offset);
				} else {
					offset -= viewer.getVisibleRegion().getOffset();
				}
				
				StyledText text= viewer.getTextWidget();
				try {
					text.redrawRange(offset, length, true);
				} catch (IllegalArgumentException x) {
					JavaPlugin.log(x);
				}
			}
			
			fActiveRegion= null;
		}

		// will eventually be replaced by a method provided by jdt.core		
		private IRegion selectWord(IDocument document, int anchor) {
		
			try {		
				int offset= anchor;
				char c;
	
				while (offset >= 0) {
					c= document.getChar(offset);
					if (!Character.isJavaIdentifierPart(c))
						break;
					--offset;
				}
	
				int start= offset;
	
				offset= anchor;
				int length= document.getLength();
	
				while (offset < length) {
					c= document.getChar(offset);
					if (!Character.isJavaIdentifierPart(c))
						break;
					++offset;
				}
				
				int end= offset;
				
				if (start == end)
					return new Region(start, 0);
				else
					return new Region(start + 1, end - start - 1);
				
			} catch (BadLocationException x) {
				return null;
			}
		}

		IRegion getCurrentTextRegion(ISourceViewer viewer) {

			int offset= getCurrentTextOffset(viewer);				
			if (offset == -1)
				return null;

			IJavaElement input= SelectionConverter.getInput(JavaEditor.this);
			if (input == null)
				return null;

			try {
				
				IJavaElement[] elements= null;
				synchronized (input) {
					elements= ((ICodeAssist) input).codeSelect(offset, 0);
				}
				
				if (elements == null || elements.length == 0)
					return null;
					
				return selectWord(viewer.getDocument(), offset);
					
			} catch (JavaModelException e) {
				return null;	
			}
		}

		private int getCurrentTextOffset(ISourceViewer viewer) {

			try {					
				StyledText text= viewer.getTextWidget();			
				if (text == null || text.isDisposed())
					return -1;

				Display display= text.getDisplay();				
				Point absolutePosition= display.getCursorLocation();
				Point relativePosition= text.toControl(absolutePosition);
				
				int widgetOffset= text.getOffsetAtLocation(relativePosition);
				if (viewer instanceof ITextViewerExtension3) {
					ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
					return extension.widgetOffset2ModelOffset(widgetOffset);
				} else {
					return widgetOffset + viewer.getVisibleRegion().getOffset();
				}

			} catch (IllegalArgumentException e) {
				return -1;
			}			
		}

		private void highlightRegion(ISourceViewer viewer, IRegion region) {

			if (region.equals(fActiveRegion))
				return;

			repairRepresentation();

			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;

			// highlight region
			int offset= 0;
			int length= 0;
			
			if (viewer instanceof ITextViewerExtension3) {
				ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
				IRegion widgetRange= extension.modelRange2WidgetRange(region);
				if (widgetRange == null)
					return;
					
				offset= widgetRange.getOffset();
				length= widgetRange.getLength();
				
			} else {
				offset= region.getOffset() - viewer.getVisibleRegion().getOffset();
				length= region.getLength();
			}
			
			StyleRange oldStyleRange= text.getStyleRangeAtOffset(offset);
			Color foregroundColor= fColor;
			Color backgroundColor= oldStyleRange == null ? text.getBackground() : oldStyleRange.background;
			StyleRange styleRange= new StyleRange(offset, length, foregroundColor, backgroundColor);
			text.setStyleRange(styleRange);

			// underline
			text.redrawRange(offset, length, true);

			fActiveRegion= region;
		}

		private void activateCursor(ISourceViewer viewer) {
			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;
			Display display= text.getDisplay();
			if (fCursor == null)
				fCursor= new Cursor(display, SWT.CURSOR_HAND);
			text.setCursor(fCursor);
		}
		
		private void resetCursor(ISourceViewer viewer) {
			StyledText text= viewer.getTextWidget();
			if (text != null && !text.isDisposed())
				text.setCursor(null);
						
			if (fCursor != null) {
				fCursor.dispose();
				fCursor= null;
			}
		}

		/*
		 * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
		 */
		public void keyPressed(KeyEvent event) {

			if (fActive) {
				deactivate();
				return;	
			}

			if (event.keyCode != fKeyModifierMask) {
				deactivate();
				return;
			}
			
			fActive= true;

//			removed for #25871			
//
//			ISourceViewer viewer= getSourceViewer();
//			if (viewer == null)
//				return;
//			
//			IRegion region= getCurrentTextRegion(viewer);
//			if (region == null)
//				return;
//			
//			highlightRegion(viewer, region);
//			activateCursor(viewer);												
		}

		/*
		 * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
		 */
		public void keyReleased(KeyEvent event) {
			
			if (!fActive)
				return;

			deactivate();				
		}

		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseDoubleClick(MouseEvent e) {}
		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseDown(MouseEvent event) {
			
			if (!fActive)
				return;
				
			if (event.stateMask != fKeyModifierMask) {
				deactivate();
				return;	
			}
			
			if (event.button != 1) {
				deactivate();
				return;	
			}			
		}

		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseUp(MouseEvent e) {

			if (!fActive)
				return;
				
			if (e.button != 1) {
				deactivate();
				return;
			}
			
			boolean wasActive= fCursor != null;
				
			deactivate();

			if (wasActive) {
				IAction action= getAction("OpenEditor");  //$NON-NLS-1$
				if (action != null)
					action.run();
			}
		}

		/*
		 * @see org.eclipse.swt.events.MouseMoveListener#mouseMove(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseMove(MouseEvent event) {
			
			if (event.widget instanceof Control && !((Control) event.widget).isFocusControl()) {
				deactivate();
				return;
			}
			
			if (!fActive) {
				if (event.stateMask != fKeyModifierMask)
					return;
				// modifier was already pressed
				fActive= true;
			}
	
			ISourceViewer viewer= getSourceViewer();
			if (viewer == null) {
				deactivate();
				return;
			}
				
			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed()) {
				deactivate();
				return;
			}
				
			if ((event.stateMask & SWT.BUTTON1) != 0 && text.getSelectionCount() != 0) {
				deactivate();
				return;
			}
		
			IRegion region= getCurrentTextRegion(viewer);
			if (region == null || region.getLength() == 0) {
				repairRepresentation();
				return;
			}
			
			highlightRegion(viewer, region);	
			activateCursor(viewer);												
		}

		/*
		 * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
		 */
		public void focusGained(FocusEvent e) {}

		/*
		 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
		 */
		public void focusLost(FocusEvent event) {
			deactivate();
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentAboutToBeChanged(DocumentEvent event) {
			if (fActive && fActiveRegion != null) {
				fRememberedPosition= new Position(fActiveRegion.getOffset(), fActiveRegion.getLength());
				try {
					event.getDocument().addPosition(fRememberedPosition);
				} catch (BadLocationException x) {
					fRememberedPosition= null;
		}
			}
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentChanged(DocumentEvent event) {
			if (fRememberedPosition != null && !fRememberedPosition.isDeleted()) {
				event.getDocument().removePosition(fRememberedPosition);
				fActiveRegion= new Region(fRememberedPosition.getOffset(), fRememberedPosition.getLength());
			}
			fRememberedPosition= null;

			ISourceViewer viewer= getSourceViewer();
			if (viewer != null) {
				StyledText widget= viewer.getTextWidget();
				if (widget != null && !widget.isDisposed()) {
					widget.getDisplay().asyncExec(new Runnable() {
						public void run() {
							deactivate();
						}
					});
				}
			}
		}

		/*
		 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
		 */
		public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
			if (oldInput == null)
				return;
			deactivate();
			oldInput.removeDocumentListener(this);
		}

		/*
		 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
		 */
		public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
			if (newInput == null)
				return;
			newInput.addDocumentListener(this);
		}

		/*
		 * @see PaintListener#paintControl(PaintEvent)
		 */
		public void paintControl(PaintEvent event) {	
			if (fActiveRegion == null)
				return;
	
			ISourceViewer viewer= getSourceViewer();
			if (viewer == null)
				return;
				
			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;
				
				
			int offset= 0;
			int length= 0;

			if (viewer instanceof ITextViewerExtension3) {
				
				ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
				IRegion widgetRange= extension.modelRange2WidgetRange(new Region(offset, length));
				if (widgetRange == null)
					return;
					
				offset= widgetRange.getOffset();
				length= widgetRange.getLength();
				
			} else {
				
			IRegion region= viewer.getVisibleRegion();			
			if (!includes(region, fActiveRegion))
			 	return;		    

				offset= fActiveRegion.getOffset() - region.getOffset();
				length= fActiveRegion.getLength();
			}
			
			// support for bidi
			Point minLocation= getMinimumLocation(text, offset, length);
			Point maxLocation= getMaximumLocation(text, offset, length);
	
			int x1= minLocation.x;
			int x2= minLocation.x + maxLocation.x - minLocation.x - 1;
			int y= minLocation.y + text.getLineHeight() - 1;
			
			GC gc= event.gc;
			if (fColor != null && !fColor.isDisposed())
			gc.setForeground(fColor);
			gc.drawLine(x1, y, x2, y);
		}

		private boolean includes(IRegion region, IRegion position) {
			return
				position.getOffset() >= region.getOffset() &&
				position.getOffset() + position.getLength() <= region.getOffset() + region.getLength();
		}

		private Point getMinimumLocation(StyledText text, int offset, int length) {
			Point minLocation= new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
	
			for (int i= 0; i <= length; i++) {
				Point location= text.getLocationAtOffset(offset + i);
				
				if (location.x < minLocation.x)
					minLocation.x= location.x;			
				if (location.y < minLocation.y)
					minLocation.y= location.y;			
			}	
			
			return minLocation;
		}
	
		private Point getMaximumLocation(StyledText text, int offset, int length) {
			Point maxLocation= new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
	
			for (int i= 0; i <= length; i++) {
				Point location= text.getLocationAtOffset(offset + i);
				
				if (location.x > maxLocation.x)
					maxLocation.x= location.x;			
				if (location.y > maxLocation.y)
					maxLocation.y= location.y;			
			}	
			
			return maxLocation;
		}
	}
	
	/**
	 * This action dispatches into two behaviours: If there is no current text
	 * hover, the javadoc is displayed using information presenter. If there is
	 * a current text hover, it is converted into a information presenter in
	 * order to make it sticky.
	 */
	class InformationDispatchAction extends TextEditorAction {
		
		/** The wrapped text operation action. */
		private final TextOperationAction fTextOperationAction;
		
		/**
		 * Creates a dispatch action.
		 */
		public InformationDispatchAction(ResourceBundle resourceBundle, String prefix, final TextOperationAction textOperationAction) {
			super(resourceBundle, prefix, JavaEditor.this);
			if (textOperationAction == null)
				throw new IllegalArgumentException();
			fTextOperationAction= textOperationAction;
		}
		
		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {

			/**
			 * Information provider used to present the information.
			 * 
			 * @since 3.0
			 */
			class InformationProvider implements IInformationProvider, IInformationProviderExtension2 {

				private IRegion fHoverRegion;
				private String fHoverInfo;
				private IInformationControlCreator fControlCreator;
				
				InformationProvider(IRegion hoverRegion, String hoverInfo, IInformationControlCreator controlCreator) {
					fHoverRegion= hoverRegion;
					fHoverInfo= hoverInfo;
					fControlCreator= controlCreator;
				}
				/*
				 * @see org.eclipse.jface.text.information.IInformationProvider#getSubject(org.eclipse.jface.text.ITextViewer, int)
				 */
				public IRegion getSubject(ITextViewer textViewer, int invocationOffset) {					
					return fHoverRegion;
				}
				/*
				 * @see org.eclipse.jface.text.information.IInformationProvider#getInformation(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
				 */
				public String getInformation(ITextViewer textViewer, IRegion subject) {
					return fHoverInfo;
				}
				/*
				 * @see org.eclipse.jface.text.information.IInformationProviderExtension2#getInformationPresenterControlCreator()
				 * @since 3.0
				 */
				public IInformationControlCreator getInformationPresenterControlCreator() {
					return fControlCreator;
				}
			}

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null) {	
				fTextOperationAction.run();
				return;
			}
				
			if (sourceViewer instanceof ITextViewerExtension4)  {
				ITextViewerExtension4 extension4= (ITextViewerExtension4) sourceViewer;
				extension4.moveFocusToWidgetToken();
			}
			
			if (! (sourceViewer instanceof ITextViewerExtension2)) {
				fTextOperationAction.run();
				return;
			}
				
			ITextViewerExtension2 textViewerExtension2= (ITextViewerExtension2) sourceViewer;
			
			// does a text hover exist?
			ITextHover textHover= textViewerExtension2.getCurrentTextHover();
			if (textHover == null) {
				fTextOperationAction.run();
				return;				
			}

			Point hoverEventLocation= textViewerExtension2.getHoverEventLocation();
			int offset= computeOffsetAtLocation(sourceViewer, hoverEventLocation.x, hoverEventLocation.y);
			if (offset == -1) {
				fTextOperationAction.run();
				return;				
			}				

			try {
				// get the text hover content
				String contentType= TextUtilities.getContentType(sourceViewer.getDocument(), IJavaPartitions.JAVA_PARTITIONING, offset);

				IRegion hoverRegion= textHover.getHoverRegion(sourceViewer, offset);						
				if (hoverRegion == null)
					return;
				
				String hoverInfo= textHover.getHoverInfo(sourceViewer, hoverRegion);

				IInformationControlCreator controlCreator= null;				
				if (textHover instanceof IInformationProviderExtension2)
					controlCreator= ((IInformationProviderExtension2)textHover).getInformationPresenterControlCreator();
	
				IInformationProvider informationProvider= new InformationProvider(hoverRegion, hoverInfo, controlCreator);

				fInformationPresenter.setOffset(offset);	
				fInformationPresenter.setInformationProvider(informationProvider, contentType);
				fInformationPresenter.showInformation();

			} catch (BadLocationException e) {				
			}
		}

		// modified version from TextViewer
		private int computeOffsetAtLocation(ITextViewer textViewer, int x, int y) {
			
			StyledText styledText= textViewer.getTextWidget();
			IDocument document= textViewer.getDocument();
			
			if (document == null)
				return -1;		

			try {
				int widgetLocation= styledText.getOffsetAtLocation(new Point(x, y));
				if (textViewer instanceof ITextViewerExtension3) {
					ITextViewerExtension3 extension= (ITextViewerExtension3) textViewer;
					return extension.widgetOffset2ModelOffset(widgetLocation);
				} else {
					IRegion visibleRegion= textViewer.getVisibleRegion();
					return widgetLocation + visibleRegion.getOffset();
				}
			} catch (IllegalArgumentException e) {
				return -1;	
			}

		}
	}
	
	static protected class AnnotationAccess extends DefaultMarkerAnnotationAccess {
		
		public AnnotationAccess(MarkerAnnotationPreferences markerAnnotationPreferences) {
			super(markerAnnotationPreferences);
		}

		/*
		 * @see org.eclipse.jface.text.source.IAnnotationAccess#getType(org.eclipse.jface.text.source.Annotation)
		 */
		public Object getType(Annotation annotation) {
			if (annotation instanceof IJavaAnnotation) {
				IJavaAnnotation javaAnnotation= (IJavaAnnotation) annotation;
				if (javaAnnotation.isRelevant())
					return javaAnnotation.getAnnotationType();
			}
			return null;
		}

		/*
		 * @see org.eclipse.jface.text.source.IAnnotationAccess#isMultiLine(org.eclipse.jface.text.source.Annotation)
		 */
		public boolean isMultiLine(Annotation annotation) {
			return true;
		}

		/*
		 * @see org.eclipse.jface.text.source.IAnnotationAccess#isTemporary(org.eclipse.jface.text.source.Annotation)
		 */
		public boolean isTemporary(Annotation annotation) {
			if (annotation instanceof IJavaAnnotation) {
				IJavaAnnotation javaAnnotation= (IJavaAnnotation) annotation;
				if (javaAnnotation.isRelevant())
					return javaAnnotation.isTemporary();
			}
			return false;
		}
	}

	private class PropertyChangeListener implements org.eclipse.core.runtime.Preferences.IPropertyChangeListener {		
		/*
		 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
		 */
		public void propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent event) {
			handlePreferencePropertyChanged(event);
		}
	}
	
	
	/** Preference key for the link color */
	protected final static String LINK_COLOR= PreferenceConstants.EDITOR_LINK_COLOR;
	/** Preference key for matching brackets */
	protected final static String MATCHING_BRACKETS=  PreferenceConstants.EDITOR_MATCHING_BRACKETS;
	/** Preference key for matching brackets color */
	protected final static String MATCHING_BRACKETS_COLOR=  PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR;
	/** Preference key for compiler task tags */
	private final static String COMPILER_TASK_TAGS= JavaCore.COMPILER_TASK_TAGS;
	/** Preference key for browser like links */
	private final static String BROWSER_LIKE_LINKS= PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS;
	/** Preference key for key modifier of browser like links */
	private final static String BROWSER_LIKE_LINKS_KEY_MODIFIER= PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER;
	/**
	 * Preference key for key modifier mask of browser like links.
	 * The value is only used if the value of <code>EDITOR_BROWSER_LIKE_LINKS</code>
	 * cannot be resolved to valid SWT modifier bits.
	 * 
	 * @since 2.1.1
	 */
	private final static String BROWSER_LIKE_LINKS_KEY_MODIFIER_MASK= PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER_MASK;
	
	protected final static char[] BRACKETS= { '{', '}', '(', ')', '[', ']' };

	/** The outline page */
	protected JavaOutlinePage fOutlinePage;
	/** Outliner context menu Id */
	protected String fOutlinerContextMenuId;
	/**
	 * The editor selection changed listener.
	 * 
	 * @since 3.0
	 */
	private EditorSelectionChangedListener fEditorSelectionChangedListener;
	/** The selection changed listener */
	protected AbstractSelectionChangedListener fOutlineSelectionChangedListener= new OutlineSelectionChangedListener();
	/** The editor's bracket matcher */
	protected JavaPairMatcher fBracketMatcher= new JavaPairMatcher(BRACKETS);
	/** The mouse listener */
	private MouseClickListener fMouseListener;
	/** The information presenter. */
	private InformationPresenter fInformationPresenter;
	/** History for structure select action */
	private SelectionHistory fSelectionHistory;
	/** The preference property change listener for java core. */
	private org.eclipse.core.runtime.Preferences.IPropertyChangeListener fPropertyChangeListener= new PropertyChangeListener();
	
	protected CompositeActionGroup fActionGroups;
	private CompositeActionGroup fContextMenuGroup;

		
	/**
	 * Returns the most narrow java element including the given offset.
	 * 
	 * @param offset the offset inside of the requested element
	 * @return the most narrow java element
	 */
	abstract protected IJavaElement getElementAt(int offset);
	
	/**
	 * Returns the java element of this editor's input corresponding to the given IJavaElement
	 */
	abstract protected IJavaElement getCorrespondingElement(IJavaElement element);
	
	/**
	 * Sets the input of the editor's outline page.
	 */
	abstract protected void setOutlinePageInput(JavaOutlinePage page, IEditorInput input);
	
	
	/**
	 * Default constructor.
	 */
	public JavaEditor() {
		super();
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		setSourceViewerConfiguration(new JavaSourceViewerConfiguration(textTools, this, IJavaPartitions.JAVA_PARTITIONING));
		setRangeIndicator(new DefaultRangeIndicator());
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setKeyBindingScopes(new String[] { "org.eclipse.jdt.ui.javaEditorScope" });  //$NON-NLS-1$
	}
	
	/*
	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected final ISourceViewer createSourceViewer(Composite parent, IVerticalRuler verticalRuler, int styles) {
		
		ISourceViewer viewer= createJavaSourceViewer(parent, verticalRuler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		
		StyledText text= viewer.getTextWidget();
		text.addBidiSegmentListener(new  BidiSegmentListener() {
			public void lineGetSegments(BidiSegmentEvent event) {
				event.segments= getBidiLineSegments(event.lineOffset, event.lineText);
			}
		});
		
		JavaUIHelp.setHelp(this, text, IJavaHelpContextIds.JAVA_EDITOR);

		// ensure source viewer decoration support has been created and configured
		getSourceViewerDecorationSupport(viewer);				
		
		return viewer;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.ExtendedTextEditor#createAnnotationAccess()
	 */
	protected IAnnotationAccess createAnnotationAccess() {
		return new AnnotationAccess(new MarkerAnnotationPreferences());
	}
	
	public final ISourceViewer getViewer() {
		return getSourceViewer();
	}
	
	/*
	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected ISourceViewer createJavaSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean isOverviewRulerVisible, int styles) {
		return new JavaSourceViewer(parent, verticalRuler, getOverviewRuler(), isOverviewRulerVisible(), styles);
	}
	
	/*
	 * @see AbstractTextEditor#affectsTextPresentation(PropertyChangeEvent)
	 */
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		return textTools.affectsBehavior(event);
	}
		
	/**
	 * Sets the outliner's context menu ID.
	 */
	protected void setOutlinerContextMenuId(String menuId) {
		fOutlinerContextMenuId= menuId;
	}
			
	/**
	 *  Returns the standard action group of this editor.
	 */
	protected ActionGroup getActionGroup() {
		return fActionGroups;
	} 
	
	/*
	 * @see AbstractTextEditor#editorContextMenuAboutToShow
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
			
		super.editorContextMenuAboutToShow(menu);
		menu.appendToGroup(ITextEditorActionConstants.GROUP_UNDO, new Separator(IContextMenuConstants.GROUP_OPEN));	
		menu.insertAfter(IContextMenuConstants.GROUP_OPEN, new GroupMarker(IContextMenuConstants.GROUP_SHOW));	
		
		ActionContext context= new ActionContext(getSelectionProvider().getSelection());
		fContextMenuGroup.setContext(context);
		fContextMenuGroup.fillContextMenu(menu);
		fContextMenuGroup.setContext(null);
	}			
	
	/**
	 * Creates the outline page used with this editor.
	 */
	protected JavaOutlinePage createOutlinePage() {
		JavaOutlinePage page= new JavaOutlinePage(fOutlinerContextMenuId, this);
		fOutlineSelectionChangedListener.install(page);
		setOutlinePageInput(page, getEditorInput());
		return page;
	}
	
	/**
	 * Informs the editor that its outliner has been closed.
	 */
	public void outlinePageClosed() {
		if (fOutlinePage != null) {
			fOutlineSelectionChangedListener.uninstall(fOutlinePage);
			fOutlinePage= null;
			resetHighlightRange();
		}
	}

	/**
	 * Synchronizes the outliner selection with the given element
	 * position in the editor.
	 * 
	 * @param element the java element to select
	 */
	protected void synchronizeOutlinePage(ISourceReference element) {
		synchronizeOutlinePage(element, true);
	}
	
	/**
	 * Synchronizes the outliner selection with the given element
	 * position in the editor.
	 * 
	 * @param element the java element to select
	 * @param checkIfOutlinePageActive <code>true</code> if check for active outline page needs to be done
	 */
	protected void synchronizeOutlinePage(ISourceReference element, boolean checkIfOutlinePageActive) {
		if (fOutlinePage != null && element != null && !(checkIfOutlinePageActive && isJavaOutlinePageActive())) {
			fOutlineSelectionChangedListener.uninstall(fOutlinePage);
			fOutlinePage.select(element);
			fOutlineSelectionChangedListener.install(fOutlinePage);
		}
	}
		
	/**
	 * Synchronizes the outliner selection with the actual cursor
	 * position in the editor.
	 */
	public void synchronizeOutlinePageSelection() {
		synchronizeOutlinePage(computeHighlightRangeSourceReference());
	}
	
	
	/*
	 * Get the desktop's StatusLineManager
	 */
	protected IStatusLineManager getStatusLineManager() {
		IEditorActionBarContributor contributor= getEditorSite().getActionBarContributor();
		if (contributor instanceof EditorActionBarContributor) {
			return ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
		}
		return null;
	}	
	
	/*
	 * @see AbstractTextEditor#getAdapter(Class)
	 */
	public Object getAdapter(Class required) {
		
		if (IContentOutlinePage.class.equals(required)) {
			if (fOutlinePage == null)
				fOutlinePage= createOutlinePage();
			return fOutlinePage;
		}
		
		if (required == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					return new String[] { JavaUI.ID_PACKAGES, IPageLayout.ID_OUTLINE, IPageLayout.ID_RES_NAV };
				}

			};
		}
			
		return super.getAdapter(required);
	}
	
	protected void setSelection(ISourceReference reference, boolean moveCursor) {
		
		ISelection selection= getSelectionProvider().getSelection();
		if (selection instanceof TextSelection) {
			TextSelection textSelection= (TextSelection) selection;
			if (textSelection.getOffset() != 0 || textSelection.getLength() != 0)
				markInNavigationHistory();
		}
		
		if (reference != null) {
			
			StyledText  textWidget= null;
			
			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer != null)
				textWidget= sourceViewer.getTextWidget();
			
			if (textWidget == null)
				return;
				
			try {
				
				ISourceRange range= reference.getSourceRange();
				if (range == null)
					return;
				
				int offset= range.getOffset();
				int length= range.getLength();
				
				if (offset < 0 || length < 0)
					return;
									
				setHighlightRange(offset, length, moveCursor);

				if (!moveCursor)
					return;
											
				offset= -1;
				length= -1;
				
				if (reference instanceof IMember) {
					range= ((IMember) reference).getNameRange();
					if (range != null) {
						offset= range.getOffset();
						length= range.getLength();
					}
				} else if (reference instanceof IImportDeclaration) {
					String name= ((IImportDeclaration) reference).getElementName();
					if (name != null && name.length() > 0) {
						String content= reference.getSource();
						if (content != null) {
							offset= range.getOffset() + content.indexOf(name);
							length= name.length();
						}
					}
				} else if (reference instanceof IPackageDeclaration) {
					String name= ((IPackageDeclaration) reference).getElementName();
					if (name != null && name.length() > 0) {
						String content= reference.getSource();
						if (content != null) {
							offset= range.getOffset() + content.indexOf(name);
							length= name.length();
						}
					}
				}
				
				if (offset > -1 && length > 0) {
					
					try  {
						textWidget.setRedraw(false);
						sourceViewer.revealRange(offset, length);
						sourceViewer.setSelectedRange(offset, length);
					} finally {
						textWidget.setRedraw(true);
					}
					
					markInNavigationHistory();
				}
				
			} catch (JavaModelException x) {
			} catch (IllegalArgumentException x) {
			}
						
		} else if (moveCursor) {
			resetHighlightRange();
			markInNavigationHistory();
		}
	}
		
	public void setSelection(IJavaElement element) {
		
		if (element == null || element instanceof ICompilationUnit || element instanceof IClassFile) {
			/*
			 * If the element is an ICompilationUnit this unit is either the input
			 * of this editor or not being displayed. In both cases, nothing should
			 * happened. (http://dev.eclipse.org/bugs/show_bug.cgi?id=5128)
			 */
			return;
		}
		
		IJavaElement corresponding= getCorrespondingElement(element);
		if (corresponding instanceof ISourceReference) {
			ISourceReference reference= (ISourceReference) corresponding;
			// set hightlight range
			setSelection(reference, true);
			// set outliner selection
			if (fOutlinePage != null) {
				fOutlineSelectionChangedListener.uninstall(fOutlinePage);
				fOutlinePage.select(reference);
				fOutlineSelectionChangedListener.install(fOutlinePage);
			}
		}
	}
	
	protected void doSelectionChanged(SelectionChangedEvent event) {
				
		ISourceReference reference= null;
		
		ISelection selection= event.getSelection();
		Iterator iter= ((IStructuredSelection) selection).iterator();
		while (iter.hasNext()) {
			Object o= iter.next();
			if (o instanceof ISourceReference) {
				reference= (ISourceReference) o;
				break;
			}
		}
		if (!isActivePart() && JavaPlugin.getActivePage() != null)
			JavaPlugin.getActivePage().bringToTop(this);
			
		setSelection(reference, !isActivePart());
	}
	
	/*
	 * @see AbstractTextEditor#adjustHighlightRange(int, int)
	 */
	protected void adjustHighlightRange(int offset, int length) {
		
		try {
			
			IJavaElement element= getElementAt(offset);
			while (element instanceof ISourceReference) {
				ISourceRange range= ((ISourceReference) element).getSourceRange();
				if (offset < range.getOffset() + range.getLength() && range.getOffset() < offset + length) {
					setHighlightRange(range.getOffset(), range.getLength(), true);
					if (fOutlinePage != null) {
						fOutlineSelectionChangedListener.uninstall(fOutlinePage);
						fOutlinePage.select((ISourceReference) element);
						fOutlineSelectionChangedListener.install(fOutlinePage);
					}
					return;
				}
				element= element.getParent();
			}
			
		} catch (JavaModelException x) {
			JavaPlugin.log(x.getStatus());
		}
		
		resetHighlightRange();
	}
			
	protected boolean isActivePart() {
		IWorkbenchPart part= getActivePart();
		return part != null && part.equals(this);
	}

	private boolean isJavaOutlinePageActive() {
		IWorkbenchPart part= getActivePart();
		return part instanceof ContentOutline && ((ContentOutline)part).getCurrentPage() == fOutlinePage;
	}

	private IWorkbenchPart getActivePart() {
		IWorkbenchWindow window= getSite().getWorkbenchWindow();
		IPartService service= window.getPartService();
		IWorkbenchPart part= service.getActivePart();
		return part;
	}
	
	/*
	 * @see AbstractTextEditor#doSetInput
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		setOutlinePageInput(fOutlinePage, input);
	}
	
	/*
	 * @see IWorkbenchPart#dispose()
	 */
	public void dispose() {

		if (isBrowserLikeLinks())
			disableBrowserLikeLinks();
			
		if (fPropertyChangeListener != null) {
			Preferences preferences= JavaCore.getPlugin().getPluginPreferences();
			preferences.removePropertyChangeListener(fPropertyChangeListener);
			fPropertyChangeListener= null;
		}
		
		if (fBracketMatcher != null) {
			fBracketMatcher.dispose();
			fBracketMatcher= null;
		}
		
		if (fSelectionHistory != null) {
			fSelectionHistory.dispose();
			fSelectionHistory= null;
		}
		
		if (fEditorSelectionChangedListener != null)  {
			fEditorSelectionChangedListener.uninstall(getSelectionProvider());
			fEditorSelectionChangedListener= null;
		}
				
		super.dispose();
	}
	
	protected void createActions() {
		super.createActions();
		
		ResourceAction resAction= new AddTaskAction(JavaEditorMessages.getResourceBundle(), "AddTask.", this); //$NON-NLS-1$
		resAction.setHelpContextId(IAbstractTextEditorHelpContextIds.ADD_TASK_ACTION);
		resAction.setActionDefinitionId(ITextEditorActionDefinitionIds.ADD_TASK);
		setAction(ITextEditorActionConstants.ADD_TASK, resAction);

		ActionGroup oeg, ovg, jsg, sg;
		fActionGroups= new CompositeActionGroup(new ActionGroup[] {
			oeg= new OpenEditorActionGroup(this),
			sg= new ShowActionGroup(this),
			ovg= new OpenViewActionGroup(this),
			jsg= new JavaSearchActionGroup(this)
		});
		fContextMenuGroup= new CompositeActionGroup(new ActionGroup[] {oeg, ovg, sg, jsg});
		
		resAction= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ShowJavaDoc.", this, ISourceViewer.INFORMATION, true); //$NON-NLS-1$
		resAction= new InformationDispatchAction(JavaEditorMessages.getResourceBundle(), "ShowJavaDoc.", (TextOperationAction) resAction); //$NON-NLS-1$
		resAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		setAction("ShowJavaDoc", resAction); //$NON-NLS-1$
		WorkbenchHelp.setHelp(resAction, IJavaHelpContextIds.SHOW_JAVADOC_ACTION);
		
		Action action= new GotoMatchingBracketAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_MATCHING_BRACKET);				
		setAction(GotoMatchingBracketAction.GOTO_MATCHING_BRACKET, action);
			
		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(),"ShowOutline.", this, JavaSourceViewer.SHOW_OUTLINE, true); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
		setAction(IJavaEditorActionDefinitionIds.SHOW_OUTLINE, action);
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.SHOW_OUTLINE_ACTION);

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(),"OpenStructure.", this, JavaSourceViewer.OPEN_STRUCTURE, true); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE);
		setAction(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE, action);
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.OPEN_STRUCTURE_ACTION);
		
		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(),"OpenHierarchy.", this, JavaSourceViewer.SHOW_HIERARCHY, true); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_HIERARCHY);
		setAction(IJavaEditorActionDefinitionIds.OPEN_HIERARCHY, action);
		WorkbenchHelp.setHelp(action, IJavaHelpContextIds.OPEN_HIERARCHY_ACTION);
		
		fSelectionHistory= new SelectionHistory(this);

		action= new StructureSelectEnclosingAction(this, fSelectionHistory);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_ENCLOSING);				
		setAction(StructureSelectionAction.ENCLOSING, action);

		action= new StructureSelectNextAction(this, fSelectionHistory);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_NEXT);
		setAction(StructureSelectionAction.NEXT, action);

		action= new StructureSelectPreviousAction(this, fSelectionHistory);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_PREVIOUS);
		setAction(StructureSelectionAction.PREVIOUS, action);

		StructureSelectHistoryAction historyAction= new StructureSelectHistoryAction(this, fSelectionHistory);
		historyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST);		
		setAction(StructureSelectionAction.HISTORY, historyAction);
		fSelectionHistory.setHistoryAction(historyAction);
				
		action= GoToNextPreviousMemberAction.newGoToNextMemberAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_NEXT_MEMBER);				
		setAction(GoToNextPreviousMemberAction.NEXT_MEMBER, action);

		action= GoToNextPreviousMemberAction.newGoToPreviousMemberAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_PREVIOUS_MEMBER);				
		setAction(GoToNextPreviousMemberAction.PREVIOUS_MEMBER, action);
	}
	
	public void updatedTitleImage(Image image) {
		setTitleImage(image);
	}
	
	/*
	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		
		try {			

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;
				
			String property= event.getProperty();	
			
			if (PreferenceConstants.EDITOR_TAB_WIDTH.equals(property)) {
				Object value= event.getNewValue();
				if (value instanceof Integer) {
					sourceViewer.getTextWidget().setTabs(((Integer) value).intValue());
				} else if (value instanceof String) {
					sourceViewer.getTextWidget().setTabs(Integer.parseInt((String) value));
				}
				return;
			}
			
			if (isJavaEditorHoverProperty(property))
				updateHoverBehavior();
			
			if (BROWSER_LIKE_LINKS.equals(property)) {
				if (isBrowserLikeLinks())
					enableBrowserLikeLinks();
				else
					disableBrowserLikeLinks();
				return;
			}
			
			if (PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE.equals(property)) {
				if ((event.getNewValue() instanceof Boolean) && ((Boolean)event.getNewValue()).booleanValue()) {
					fEditorSelectionChangedListener= new EditorSelectionChangedListener();
					fEditorSelectionChangedListener.install(getSelectionProvider());
					fEditorSelectionChangedListener.selectionChanged();
				} else {
					fEditorSelectionChangedListener.uninstall(getSelectionProvider());
					fEditorSelectionChangedListener= null;
				}
				return;
			}
			
			if (PreferenceConstants.EDITOR_DISABLE_OVERWRITE_MODE.equals(property)) {
				if (event.getNewValue() instanceof Boolean) {
					Boolean disable= (Boolean) event.getNewValue();
					configureInsertMode(OVERWRITE, !disable.booleanValue());
				}
			}
			
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}
	
	private boolean isJavaEditorHoverProperty(String property) {
		return	PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIERS.equals(property);
	}
	
	/**
	 * Return whether the browser like links should be enabled
	 * according to the preference store settings.
	 * @return <code>true</code> if the browser like links should be enabled
	 */
	private boolean isBrowserLikeLinks() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(BROWSER_LIKE_LINKS);
	}
	
	/**
	 * Enables browser like links.
	 */
	private void enableBrowserLikeLinks() {
		if (fMouseListener == null) {
			fMouseListener= new MouseClickListener();
			fMouseListener.install();
		}
	}
	
	/**
	 * Disables browser like links.
	 */
	private void disableBrowserLikeLinks() {
		if (fMouseListener != null) {
			fMouseListener.uninstall();
			fMouseListener= null;
		}
	}
	
	/**
	 * Handles a property change event describing a change
	 * of the java core's preferences and updates the preference
	 * related editor properties.
	 * 
	 * @param event the property change event
	 */
	protected void handlePreferencePropertyChanged(org.eclipse.core.runtime.Preferences.PropertyChangeEvent event) {
		if (COMPILER_TASK_TAGS.equals(event.getProperty())) {
			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer != null && affectsTextPresentation(new PropertyChangeEvent(event.getSource(), event.getProperty(), event.getOldValue(), event.getNewValue())))
				sourceViewer.invalidateTextPresentation();
		}
	}

	/**
	 * Returns a segmentation of the line of the given viewer's input document appropriate for
	 * bidi rendering. The default implementation returns only the string literals of a java code
	 * line as segments.
	 * 
	 * @param viewer the text viewer
	 * @param lineOffset the offset of the line
	 * @return the line's bidi segmentation
	 * @throws BadLocationException in case lineOffset is not valid in document
	 */
	public static int[] getBidiLineSegments(ITextViewer viewer, int lineOffset) throws BadLocationException {
			
		IDocument document= viewer.getDocument();
		if (document == null)
			return null;
			
		IRegion line= document.getLineInformationOfOffset(lineOffset);
		ITypedRegion[] linePartitioning= TextUtilities.computePartitioning(document, IJavaPartitions.JAVA_PARTITIONING, lineOffset, line.getLength());
		
		List segmentation= new ArrayList();
		for (int i= 0; i < linePartitioning.length; i++) {
			if (IJavaPartitions.JAVA_STRING.equals(linePartitioning[i].getType()))
				segmentation.add(linePartitioning[i]);
		}
		
		
		if (segmentation.size() == 0) 
			return null;
			
		int size= segmentation.size();
		int[] segments= new int[size * 2 + 1];
		
		int j= 0;
		for (int i= 0; i < size; i++) {
			ITypedRegion segment= (ITypedRegion) segmentation.get(i);
			
			if (i == 0)
				segments[j++]= 0;
				
			int offset= segment.getOffset() - lineOffset;
			if (offset > segments[j - 1])
				segments[j++]= offset;
				
			if (offset + segment.getLength() >= line.getLength())
				break;
				
			segments[j++]= offset + segment.getLength();
		}
		
		if (j < segments.length) {
			int[] result= new int[j];
			System.arraycopy(segments, 0, result, 0, j);
			segments= result;
		}
		
		return segments;
	}
		
	/**
	 * Returns a segmentation of the given line appropriate for bidi rendering. The default
	 * implementation returns only the string literals of a java code line as segments.
	 * 
	 * @param lineOffset the offset of the line
	 * @param line the content of the line
	 * @return the line's bidi segmentation
	 */
	protected int[] getBidiLineSegments(int widgetLineOffset, String line) {
		if (line != null && line.length() > 0) {
			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer != null) {
				int lineOffset;
				if (sourceViewer instanceof ITextViewerExtension3) {
					ITextViewerExtension3 extension= (ITextViewerExtension3) sourceViewer;
					lineOffset= extension.widgetOffset2ModelOffset(widgetLineOffset);
				} else {
					IRegion visible= sourceViewer.getVisibleRegion();
					lineOffset= visible.getOffset() + widgetLineOffset;
				}
				try {
					return getBidiLineSegments(sourceViewer, lineOffset);
				} catch (BadLocationException x) {
					// don't segment line in this case
				}
			}
		}
		return null;
	}
	
	/*
	 * Update the hovering behavior depending on the preferences.
	 */
	private void updateHoverBehavior() {
		SourceViewerConfiguration configuration= getSourceViewerConfiguration();
		String[] types= configuration.getConfiguredContentTypes(getSourceViewer());

		for (int i= 0; i < types.length; i++) {
			
			String t= types[i];

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer instanceof ITextViewerExtension2) {
				// Remove existing hovers			
				((ITextViewerExtension2)sourceViewer).removeTextHovers(t);
				
				int[] stateMasks= configuration.getConfiguredTextHoverStateMasks(getSourceViewer(), t);

				if (stateMasks != null) {
					for (int j= 0; j < stateMasks.length; j++)	{
						int stateMask= stateMasks[j];
						ITextHover textHover= configuration.getTextHover(sourceViewer, t, stateMask);
						((ITextViewerExtension2)sourceViewer).setTextHover(textHover, t, stateMask);
					}
				} else {
					ITextHover textHover= configuration.getTextHover(sourceViewer, t);
					((ITextViewerExtension2)sourceViewer).setTextHover(textHover, t, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
				}
			} else
				sourceViewer.setTextHover(configuration.getTextHover(sourceViewer, t), t);
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.viewsupport.IViewPartInputProvider#getViewPartInput()
	 */
	public Object getViewPartInput() {
		return getEditorInput().getAdapter(IJavaElement.class);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#doSetSelection(ISelection)
	 */
	protected void doSetSelection(ISelection selection) {
		super.doSetSelection(selection);
		synchronizeOutlinePageSelection();
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		Preferences preferences= JavaCore.getPlugin().getPluginPreferences();
		preferences.addPropertyChangeListener(fPropertyChangeListener);			
		
		IInformationControlCreator informationControlCreator= new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell shell) {
				boolean cutDown= false;
				int style= cutDown ? SWT.NONE : (SWT.V_SCROLL | SWT.H_SCROLL);
				return new DefaultInformationControl(shell, SWT.RESIZE, style, new HTMLTextPresenter(cutDown));
			}
		};

		fInformationPresenter= new InformationPresenter(informationControlCreator);
		fInformationPresenter.setSizeConstraints(60, 10, true, true);		
		fInformationPresenter.install(getSourceViewer());
		
		if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE)) {
			fEditorSelectionChangedListener= new EditorSelectionChangedListener();
			fEditorSelectionChangedListener.install(getSelectionProvider());
		}
		
		if (isBrowserLikeLinks())
			enableBrowserLikeLinks();

		if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_DISABLE_OVERWRITE_MODE))
			configureInsertMode(OVERWRITE, false);
	}
	
	protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
		
		support.setCharacterPairMatcher(fBracketMatcher);
		support.setMatchingCharacterPainterPreferenceKeys(MATCHING_BRACKETS, MATCHING_BRACKETS_COLOR);
		
		super.configureSourceViewerDecorationSupport(support);
	}
	
	/**
	 * Jumps to the next enabled annotation according to the given direction.
	 * An annotation type is enabled if it is configured to be in the
	 * Next/Previous tool bar drop down menu and if it is checked.
	 */
	public void gotoAnnotation(boolean forward) {
		
		ISelectionProvider provider= getSelectionProvider();
		
		ITextSelection s= (ITextSelection) provider.getSelection();
		Position annotationPosition= new Position(0, 0);
		IJavaAnnotation nextAnnotation= getNextAnnotation(s.getOffset(), s.getLength(),forward, annotationPosition);
		
		setStatusLineErrorMessage(null);

		if (nextAnnotation != null) {
			
			IMarker marker= null;
			if (nextAnnotation instanceof MarkerAnnotation)
				marker= ((MarkerAnnotation) nextAnnotation).getMarker();
			else {
				Iterator e= nextAnnotation.getOverlaidIterator();
				if (e != null) {
					while (e.hasNext()) {
						Object o= e.next();
						if (o instanceof MarkerAnnotation) {
							marker= ((MarkerAnnotation) o).getMarker();
							break;
						}
					}
				}
			}
			
			if (marker != null) {
				IWorkbenchPage page= getSite().getPage();
				IViewPart view= view= page.findView("org.eclipse.ui.views.TaskList"); //$NON-NLS-1$
				if (view instanceof TaskList) {
					StructuredSelection ss= new StructuredSelection(marker);
					((TaskList) view).setSelection(ss, true);
				}
			}
			
			selectAndReveal(annotationPosition.getOffset(), annotationPosition.getLength());
			if (nextAnnotation.isProblem())
				setStatusLineErrorMessage(nextAnnotation.getMessage());
		}
	}
	
	/**
	 * Jumps to the matching bracket.
	 */
	public void gotoMatchingBracket() {
		
		ISourceViewer sourceViewer= getSourceViewer();
		IDocument document= sourceViewer.getDocument();
		if (document == null)
			return;
		
		IRegion selection= getSignedSelection(sourceViewer);

		int selectionLength= Math.abs(selection.getLength());
		if (selectionLength > 1) {
			setStatusLineErrorMessage(JavaEditorMessages.getString("GotoMatchingBracket.error.invalidSelection"));	//$NON-NLS-1$		
			sourceViewer.getTextWidget().getDisplay().beep();
			return;
		}

		// #26314
		int sourceCaretOffset= selection.getOffset() + selection.getLength();
		if (isSurroundedByBrackets(document, sourceCaretOffset))
			sourceCaretOffset -= selection.getLength();

		IRegion region= fBracketMatcher.match(document, sourceCaretOffset);
		if (region == null) {
			setStatusLineErrorMessage(JavaEditorMessages.getString("GotoMatchingBracket.error.noMatchingBracket"));	//$NON-NLS-1$		
			sourceViewer.getTextWidget().getDisplay().beep();
			return;		
		}
		
		int offset= region.getOffset();
		int length= region.getLength();
		
		if (length < 1)
			return;
			
		int anchor= fBracketMatcher.getAnchor();
		// http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
		int targetOffset= (JavaPairMatcher.RIGHT == anchor) ? offset + 1: offset + length;
		
		boolean visible= false;
		if (sourceViewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3) sourceViewer;
			visible= (extension.modelOffset2WidgetOffset(targetOffset) > -1);
		} else {
			IRegion visibleRegion= sourceViewer.getVisibleRegion();
			// http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
			visible= (targetOffset >= visibleRegion.getOffset() && targetOffset <= visibleRegion.getOffset() + visibleRegion.getLength());
		}
		
		if (!visible) {
			setStatusLineErrorMessage(JavaEditorMessages.getString("GotoMatchingBracket.error.bracketOutsideSelectedElement"));	//$NON-NLS-1$		
			sourceViewer.getTextWidget().getDisplay().beep();
			return;
		}
		
		if (selection.getLength() < 0)
			targetOffset -= selection.getLength();
			
		sourceViewer.setSelectedRange(targetOffset, selection.getLength());
		sourceViewer.revealRange(targetOffset, selection.getLength());
	}

	/**
	 * Ses the given message as error message to this editor's status line.
	 * @param msg message to be set
	 */
	protected void setStatusLineErrorMessage(String msg) {
		IEditorStatusLine statusLine= (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
		if (statusLine != null)
			statusLine.setMessage(true, msg, null);	
	}
	
	private static IRegion getSignedSelection(ITextViewer viewer) {

		StyledText text= viewer.getTextWidget();
		int caretOffset= text.getCaretOffset();
		Point selection= text.getSelection();
		
		// caret left
		int offset, length;
		if (caretOffset == selection.x) {
			offset= selection.y;
			length= selection.x - selection.y;			
			
		// caret right
		} else {
			offset= selection.x;
			length= selection.y - selection.x;			
		}
		
		return new Region(offset, length);
	}
	
	private static boolean isBracket(char character) {
		for (int i= 0; i != BRACKETS.length; ++i)
			if (character == BRACKETS[i])
				return true;
		return false;
	}

	private static boolean isSurroundedByBrackets(IDocument document, int offset) {
		if (offset == 0 || offset == document.getLength())
			return false;

		try {
			return
				isBracket(document.getChar(offset - 1)) &&
				isBracket(document.getChar(offset));
			
		} catch (BadLocationException e) {
			return false;	
		}
	}



	private IJavaAnnotation getNextAnnotation(int offset, int length, boolean forward, Position annotationPosition) {
		IJavaAnnotation nextAnnotation= null;
		Position nextAnnotationPosition= null;
		IJavaAnnotation containingAnnotation= null;
		Position containingAnnotationPosition= null;
		boolean currentAnnotation= false;
		
		IDocument document= getDocumentProvider().getDocument(getEditorInput());
		int endOfDocument= document.getLength(); 
		int distance= 0;
		
		IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());
		Iterator e= new JavaAnnotationIterator(model, true);
		while (e.hasNext()) {
			IJavaAnnotation a= (IJavaAnnotation) e.next();
			Preferences workbenchTextEditorPrefStore= Platform.getPlugin("org.eclipse.ui.workbench.texteditor").getPluginPreferences(); //$NON-NLS-1$
			Iterator iter= getAnnotationPreferences().getAnnotationPreferences().iterator();
			boolean isNavigationTarget= false;
			while (iter.hasNext()) {
				AnnotationPreference annotationPref= (AnnotationPreference)iter.next();
				if (annotationPref.getAnnotationType().equals(a.getAnnotationType())) {
					String key;
					if (forward)
						key= annotationPref.getIsGoToNextNavigationTargetKey();
					else
						key= annotationPref.getIsGoToPreviousNavigationTargetKey();
					if (key != null)
						isNavigationTarget= workbenchTextEditorPrefStore.getBoolean(key);
					break;
				}
				annotationPref= null;
			}
			
			if (a.hasOverlay() || !isNavigationTarget)
				continue;
				
			Position p= model.getPosition((Annotation) a);
			if (!(p.includes(offset) || (p.getLength() == 0 && offset == p.offset))) {
				
				int currentDistance= 0;
				
				if (forward) {
					currentDistance= p.getOffset() - offset;
					if (currentDistance < 0)
						currentDistance= endOfDocument - offset + p.getOffset();
				} else {
					currentDistance= offset - p.getOffset();
					if (currentDistance < 0)
						currentDistance= offset + endOfDocument - p.getOffset();
				}						
										
				if (nextAnnotation == null || currentDistance < distance) {
					distance= currentDistance;
					nextAnnotation= a;
					nextAnnotationPosition= p;
				}
			} else {
				if (containingAnnotationPosition == null || containingAnnotationPosition.length > p.length) {
					containingAnnotation= a;
					containingAnnotationPosition= p;
					if (length == p.length)
						currentAnnotation= true;
			}
		}
		}
		if (containingAnnotationPosition != null && (!currentAnnotation || nextAnnotation == null)) {
			annotationPosition.setOffset(containingAnnotationPosition.getOffset());
			annotationPosition.setLength(containingAnnotationPosition.getLength());
			return containingAnnotation;
		}
		if (nextAnnotationPosition != null) {
			annotationPosition.setOffset(nextAnnotationPosition.getOffset());
			annotationPosition.setLength(nextAnnotationPosition.getLength());
		}
		
		return nextAnnotation;
	}
	
	/**
	 * Computes and returns the source reference that includes the caret and
	 * serves as provider for the outline page selection and the editor range
	 * indication.
	 * 
	 * @return the computed source reference
	 * @since 3.0
	 */
	protected ISourceReference computeHighlightRangeSourceReference() {
		ISourceViewer sourceViewer= getSourceViewer();
		if (sourceViewer == null)
			return null;
			
		StyledText styledText= sourceViewer.getTextWidget();
		if (styledText == null)
			return null;
		
		int caret= 0;
		if (sourceViewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3)sourceViewer;
			caret= extension.widgetOffset2ModelOffset(styledText.getCaretOffset());
		} else {
			int offset= sourceViewer.getVisibleRegion().getOffset();
			caret= offset + styledText.getCaretOffset();
		}

		IJavaElement element= getElementAt(caret, false);
		
		if ( !(element instanceof ISourceReference))
			return null;
		
		if (element.getElementType() == IJavaElement.IMPORT_DECLARATION) {
			
			IImportDeclaration declaration= (IImportDeclaration) element;
			IImportContainer container= (IImportContainer) declaration.getParent();
			ISourceRange srcRange= null;
			
			try {
				srcRange= container.getSourceRange();
			} catch (JavaModelException e) {
			}
			
			if (srcRange != null && srcRange.getOffset() == caret)
				return container;
		}
		
		return (ISourceReference) element;
	}

	/**
	 * Returns the most narrow java element including the given offset.
	 * 
	 * @param offset the offset inside of the requested element
	 * @param reconcile <code>true</code> if editor input should be reconciled in advance
	 * @return the most narrow java element
	 * @since 3.0
	 */
	protected IJavaElement getElementAt(int offset, boolean reconcile) {
		return getElementAt(offset);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.ExtendedTextEditor#createChangeHover()
	 */
	protected LineChangeHover createChangeHover() {
		return new JavaChangeHover(IJavaPartitions.JAVA_PARTITIONING);
	}

	protected boolean isPrefQuickDiffAlwaysOn() {
		return false; // never show change ruler for the non-editable java editor. Overridden in subclasses like CompilationUnitEditor
	}

}
