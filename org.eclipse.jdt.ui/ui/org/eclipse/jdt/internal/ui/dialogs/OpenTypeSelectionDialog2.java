/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.jdt.internal.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoHistory;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class OpenTypeSelectionDialog2 extends SelectionStatusDialog {

	private boolean fMultipleSelection;
	private IRunnableContext fRunnableContext;
	private IJavaSearchScope fScope;
	private int fElementKind;
	
	private TypeSelectionComponent fContent;
	
	private IDialogSettings fSettings;
	private Point fLocation;
	private Point fSize;

	private static final String DIALOG_SETTINGS= "org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog"; //$NON-NLS-1$
	private static final String WIDTH= "width"; //$NON-NLS-1$
	private static final String HEIGHT= "height"; //$NON-NLS-1$
	
	public OpenTypeSelectionDialog2(Shell parent, boolean multi, IRunnableContext context, IJavaSearchScope scope, int elementKinds) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fMultipleSelection= multi;
		fRunnableContext= context;
		fScope= (scope != null) ? scope : SearchEngine.createWorkspaceScope();
		fElementKind= elementKinds;
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		fSettings= settings.getSection(DIALOG_SETTINGS);
		if (fSettings == null) {
			fSettings= new DialogSettings(DIALOG_SETTINGS);
			settings.addSection(fSettings);
			fSettings.put(WIDTH, 480);
			fSettings.put(HEIGHT, 320);
		}
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.OPEN_TYPE_DIALOG);
	}

	protected Point getInitialSize() {
		Point result= super.getInitialSize();
		if (fSize != null) {
			result.x= Math.max(result.x, fSize.x);
			result.y= Math.max(result.y, fSize.y);
			Rectangle display= getShell().getMonitor().getClientArea();
			result.x= Math.min(result.x, display.width);
			result.y= Math.min(result.y, display.height);
		}
		return result;
	}
	
	protected Point getInitialLocation(Point initialSize) {
		Point result= super.getInitialLocation(initialSize);
		if (fLocation != null) {
			result.x= fLocation.x;
			result.y= fLocation.y;
			Rectangle display= getShell().getMonitor().getClientArea();
			int xe= result.x + initialSize.x;
			if (xe > display.width) {
				result.x-= xe - display.width; 
			}
			int ye= result.y + initialSize.y;
			if (ye > display.height) {
				result.y-= ye - display.height; 
			}
		}
		return result;
	}
	
	public void create() {
		super.create();
		fContent.populate();
		getOkButton().setEnabled(fContent.getSelection().length > 0);
	}

	protected Control createDialogArea(Composite parent) {
		Composite area= (Composite)super.createDialogArea(parent);
		readSettings();
		fContent= new TypeSelectionComponent(area, SWT.NONE, getMessage(), 
			fMultipleSelection, fScope, fElementKind);
		GridData gd= new GridData(GridData.FILL_BOTH);
		fContent.setLayoutData(gd);
		fContent.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				okPressed();
			}
			public void widgetSelected(SelectionEvent e) {
				// don't access the selection event directly since we 
				// are sending fake event
				TypeInfo[] infos= fContent.getSelection();
				getOkButton().setEnabled(infos.length > 0);
			}
		});
		return area;
	}
	
	public int open() {
		try {
			ensureConsistency();
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, JavaUIMessages.TypeSelectionDialog_error3Title, JavaUIMessages.TypeSelectionDialog_error3Message); 
			return CANCEL;
		} catch (InterruptedException e) {
			// cancelled by user
			return CANCEL;
		}
		return super.open();
	}
	
	public boolean close() {
		fContent.close();
		TypeInfoHistory.getInstance().save();
		writeSettings();
		return super.close();
	}
	
	protected void okPressed() {
		fContent.close();
		super.okPressed();
	}
	
	protected void cancelPressed() {
		fContent.close();
		super.cancelPressed();
	}

	protected void computeResult() {
		TypeInfo[] selected= fContent.getSelection();
		if (selected == null || selected.length == 0)
			return;
		
		TypeInfoHistory history= TypeInfoHistory.getInstance();
		List result= new ArrayList(selected.length);
		if (result != null) {
			for (int i= 0; i < selected.length; i++) {
				try {
					TypeInfo typeInfo= selected[i];
					history.accessed(typeInfo);
					IType type= typeInfo.resolveType(fScope);
					if (type == null) {
						String title= JavaUIMessages.MultiTypeSelectionDialog_dialogTitle; 
						String message= Messages.format(JavaUIMessages.MultiTypeSelectionDialog_dialogMessage, typeInfo.getPath()); 
						MessageDialog.openError(getShell(), title, message);
					} else {
						result.add(type);
					}
				} catch (JavaModelException e) {
					String title= JavaUIMessages.MultiTypeSelectionDialog_errorTitle; 
					String message= JavaUIMessages.MultiTypeSelectionDialog_errorMessage; 
					ErrorDialog.openError(getShell(), title, message, e.getStatus());
				}
			}
		}
		setResult(result);
	}
	
	/**
	 * Initializes itself from the dialog settings with the same state
	 * as at the previous invocation.
	 */
	private void readSettings() {
		try {
			int x= fSettings.getInt("x"); //$NON-NLS-1$
			int y= fSettings.getInt("y"); //$NON-NLS-1$
			fLocation= new Point(x, y);
			int width= fSettings.getInt("width"); //$NON-NLS-1$
			int height= fSettings.getInt("height"); //$NON-NLS-1$
			fSize= new Point(width, height);

		} catch (NumberFormatException e) {
			fLocation= null;
			fSize= null;
		}
	}

	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeSettings() {
		Point location= getShell().getLocation();
		fSettings.put("x", location.x); //$NON-NLS-1$
		fSettings.put("y", location.y); //$NON-NLS-1$

		Point size= getShell().getSize();
		fSettings.put("width", size.x); //$NON-NLS-1$
		fSettings.put("height", size.y); //$NON-NLS-1$
	}	
	
	private void ensureConsistency() throws InvocationTargetException, InterruptedException {
		final ICompilationUnit[] primaryWorkingCopies= JavaCore.getWorkingCopies(null);
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask("", primaryWorkingCopies.length); //$NON-NLS-1$
				monitor.setTaskName("Reconcling pending working copies...");
				for (int i= 0; i < primaryWorkingCopies.length; i++) {
					ICompilationUnit curr= primaryWorkingCopies[i];
					try {
						JavaModelUtil.reconcile(curr);
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
					monitor.worked(1);
					if (monitor.isCanceled())
						throw new OperationCanceledException();
				}
				TypeInfoHistory.getInstance().checkConsistency();
				monitor.done();
			}
		};
		ISchedulingRule[] rules= new ISchedulingRule[primaryWorkingCopies.length];
		for (int i= 0; i < primaryWorkingCopies.length; i++) {
			rules[i]= primaryWorkingCopies[i].getSchedulingRule();
		}
		MultiRule rule= new MultiRule(rules);
		(fRunnableContext != null 
			? fRunnableContext 
			: PlatformUI.getWorkbench().getProgressService()).run(
			true, true, new WorkbenchRunnableAdapter(runnable, rule));
	}
}