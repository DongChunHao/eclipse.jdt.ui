/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - created core version based on CodeStyleFix
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocationCore;

/**
 * A fix which fixes code style issues.
 */
public class CodeStyleFixCore extends CompilationUnitRewriteOperationsFixCore {

	public final static class CodeStyleVisitor extends GenericVisitor {

		private final List<CompilationUnitRewriteOperation> fResult;
		private final ImportRewrite fImportRewrite;
		private final boolean fFindUnqualifiedAccesses;
		private final boolean fFindUnqualifiedStaticAccesses;
		private final boolean fFindUnqualifiedMethodAccesses;
		private final boolean fFindUnqualifiedStaticMethodAccesses;

		public CodeStyleVisitor(CompilationUnit compilationUnit,
				boolean findUnqualifiedAccesses,
				boolean findUnqualifiedStaticAccesses,
				boolean findUnqualifiedMethodAccesses,
				boolean findUnqualifiedStaticMethodAccesses,
				List<CompilationUnitRewriteOperation> resultingCollection) {

			fFindUnqualifiedAccesses= findUnqualifiedAccesses;
			fFindUnqualifiedStaticAccesses= findUnqualifiedStaticAccesses;
			fFindUnqualifiedMethodAccesses= findUnqualifiedMethodAccesses;
			fFindUnqualifiedStaticMethodAccesses= findUnqualifiedStaticMethodAccesses;
			fImportRewrite= StubUtility.createImportRewrite(compilationUnit, true);
			fResult= resultingCollection;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			if (!fFindUnqualifiedStaticAccesses && !fFindUnqualifiedStaticMethodAccesses
					&& !fFindUnqualifiedAccesses && !fFindUnqualifiedMethodAccesses && node.isInterface())
				return false;

			return super.visit(node);
		}

		@Override
		public boolean visit(QualifiedName node) {
			if (fFindUnqualifiedAccesses || fFindUnqualifiedStaticAccesses) {
				ASTNode simpleName= node;
				while (simpleName instanceof QualifiedName) {
					simpleName= ((QualifiedName) simpleName).getQualifier();
				}
				if (simpleName instanceof SimpleName) {
					handleSimpleName((SimpleName)simpleName);
				}
			}
			return false;
		}

		@Override
		public boolean visit(SimpleName node) {
			if (fFindUnqualifiedAccesses || fFindUnqualifiedStaticAccesses) {
				handleSimpleName(node);
			}
			return false;
		}

		@Override
		public boolean visit(MethodInvocation node) {
			if (!fFindUnqualifiedMethodAccesses && !fFindUnqualifiedStaticMethodAccesses)
				return true;

			if (node.getExpression() != null)
				return true;

			IBinding binding= node.getName().resolveBinding();
			if (!(binding instanceof IMethodBinding))
				return true;

			handleMethod(node.getName(), (IMethodBinding)binding);
			return true;
		}

		private void handleSimpleName(SimpleName node) {
			ASTNode firstExpression= node.getParent();
			if (firstExpression instanceof FieldAccess) {
				while (firstExpression instanceof FieldAccess) {
					firstExpression= ((FieldAccess)firstExpression).getExpression();
				}
				if (!(firstExpression instanceof SimpleName))
					return;

				node= (SimpleName)firstExpression;
			} else if (firstExpression instanceof SuperFieldAccess)
				return;

			StructuralPropertyDescriptor parentDescription= node.getLocationInParent();
			if (parentDescription == VariableDeclarationFragment.NAME_PROPERTY || parentDescription == SwitchCase.EXPRESSION_PROPERTY || parentDescription == SwitchCase.EXPRESSIONS2_PROPERTY)
				return;

			IBinding binding= node.resolveBinding();
			if (!(binding instanceof IVariableBinding))
				return;

			handleVariable(node, (IVariableBinding) binding);
		}

		private void handleVariable(SimpleName node, IVariableBinding varbinding) {
			if (!varbinding.isField())
				return;

			if (varbinding.isEnumConstant())
				return;

			ITypeBinding declaringClass= varbinding.getDeclaringClass();
			if (Modifier.isStatic(varbinding.getModifiers())) {
				if (fFindUnqualifiedStaticAccesses) {
					Initializer initializer= ASTNodes.getParent(node, Initializer.class);
					//Do not qualify assignments to static final fields in static initializers (would result in compile error)
					StructuralPropertyDescriptor parentDescription= node.getLocationInParent();
					if (initializer != null && Modifier.isStatic(initializer.getModifiers())
							&& Modifier.isFinal(varbinding.getModifiers()) && parentDescription == Assignment.LEFT_HAND_SIDE_PROPERTY)
						return;

					//Do not qualify static fields if defined inside an anonymous class
					if (declaringClass.isAnonymous())
						return;

					fResult.add(new AddStaticQualifierOperation(declaringClass, node));
				}
			} else if (fFindUnqualifiedAccesses){
				String qualifier= getThisExpressionQualifier(declaringClass, fImportRewrite, node);
				if (qualifier == null)
					return;

				if (qualifier.length() == 0)
					qualifier= null;

				fResult.add(new AddThisQualifierOperation(qualifier, node));
			}
		}

		private void handleMethod(SimpleName node, IMethodBinding binding) {
			ITypeBinding declaringClass= binding.getDeclaringClass();
			if (Modifier.isStatic(binding.getModifiers())) {
				if (fFindUnqualifiedStaticMethodAccesses) {
					//Do not qualify static fields if defined inside an anonymous class
					if (declaringClass.isAnonymous())
						return;

					fResult.add(new AddStaticQualifierOperation(declaringClass, node));
				}
			} else {
				if (fFindUnqualifiedMethodAccesses) {
					String qualifier= getThisExpressionQualifier(declaringClass, fImportRewrite, node);
					if (qualifier == null)
						return;

					if (qualifier.length() == 0)
						qualifier= null;

					fResult.add(new AddThisQualifierOperation(qualifier, node));
				}
			}
		}
	}

	public static class ThisQualifierVisitor extends GenericVisitor {

		private final CompilationUnit fCompilationUnit;
		private final List<CompilationUnitRewriteOperation> fOperations;
		private final boolean fRemoveFieldQualifiers;
		private final boolean fRemoveMethodQualifiers;

		public ThisQualifierVisitor(boolean removeFieldQualifiers,
									boolean removeMethodQualifiers,
									CompilationUnit compilationUnit,
									List<CompilationUnitRewriteOperation> result) {
			fRemoveFieldQualifiers= removeFieldQualifiers;
			fRemoveMethodQualifiers= removeMethodQualifiers;
			fCompilationUnit= compilationUnit;
			fOperations= result;
		}

		@Override
		public boolean visit(final FieldAccess node) {
			if (!fRemoveFieldQualifiers)
				return true;

			Expression expression= node.getExpression();
			if (!(expression instanceof ThisExpression))
				return true;

			final SimpleName name= node.getName();
			if (hasConflict(expression.getStartPosition(), name, ScopeAnalyzer.VARIABLES | ScopeAnalyzer.CHECK_VISIBILITY))
				return true;

			Name qualifier= ((ThisExpression) expression).getQualifier();
			if (qualifier != null) {
				ITypeBinding outerClass= (ITypeBinding) qualifier.resolveBinding();
				if (outerClass == null)
					return true;

				IVariableBinding nameBinding= (IVariableBinding) name.resolveBinding();
				if (nameBinding == null)
					return true;

				ITypeBinding variablesDeclaringClass= nameBinding.getDeclaringClass();
				if (outerClass != variablesDeclaringClass)
					//be conservative: We have a reference to a field of an outer type, and this type inherited
					//the field. It's possible that the inner type inherits the same field. We must not remove
					//the qualifier in this case.
					return true;

				ITypeBinding enclosingTypeBinding= Bindings.getBindingOfParentType(node);
				if (enclosingTypeBinding == null || Bindings.isSuperType(variablesDeclaringClass, enclosingTypeBinding))
					//We have a reference to a field of an outer type, and this type inherited
					//the field. The inner type inherits the same field. We must not remove
					//the qualifier in this case.
					return true;
			}

			fOperations.add(new CompilationUnitRewriteOperation() {
				@Override
				public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
					ASTRewrite rewrite= cuRewrite.getASTRewrite();
					TextEditGroup group= createTextEditGroup(FixMessages.CodeStyleFix_removeThis_groupDescription, cuRewrite);
					rewrite.replace(node, rewrite.createCopyTarget(name), group);
				}
			});
			return super.visit(node);
		}

		@Override
		public boolean visit(final MethodInvocation node) {
			if (!fRemoveMethodQualifiers)
				return true;

			Expression expression= node.getExpression();
			if (!(expression instanceof ThisExpression))
				return true;

			final SimpleName name= node.getName();
			if (name.resolveBinding() == null)
				return true;

			if (hasConflict(expression.getStartPosition(), name, ScopeAnalyzer.METHODS | ScopeAnalyzer.CHECK_VISIBILITY))
				return true;

			Name qualifier= ((ThisExpression)expression).getQualifier();
			if (qualifier != null) {
				ITypeBinding declaringClass= ((IMethodBinding)name.resolveBinding()).getDeclaringClass();
				if (declaringClass == null)
					return true;

				ITypeBinding caller= getDeclaringType(node);
				if (caller == null)
					return true;

				ITypeBinding callee= (ITypeBinding)qualifier.resolveBinding();
				if (callee == null)
					return true;

				if (callee.isAssignmentCompatible(declaringClass) && caller.isAssignmentCompatible(declaringClass))
					return true;
			}

			fOperations.add(new CompilationUnitRewriteOperation() {
				@Override
				public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
					ASTRewrite rewrite= cuRewrite.getASTRewrite();
					TextEditGroup group= createTextEditGroup(FixMessages.CodeStyleFix_removeThis_groupDescription, cuRewrite);
					rewrite.remove(node.getExpression(), group);
				}
			});
			return super.visit(node);
		}

		private ITypeBinding getDeclaringType(MethodInvocation node) {
			ASTNode p= node;
			while (p != null) {
				p= p.getParent();
				if (p instanceof AbstractTypeDeclaration) {
					return ((AbstractTypeDeclaration)p).resolveBinding();
				}
			}
			return null;
        }

		private boolean hasConflict(int startPosition, SimpleName name, int flag) {
			ScopeAnalyzer analyzer= new ScopeAnalyzer(fCompilationUnit);
			for (IBinding decl : analyzer.getDeclarationsInScope(startPosition, flag)) {
				IBinding nameBinding= name.resolveBinding();
				if (decl.getName().equals(name.getIdentifier()) && nameBinding != decl) {
					if ((decl instanceof IVariableBinding) && (nameBinding instanceof IVariableBinding)) {
						IVariableBinding declVarBinding= (IVariableBinding)decl;
						IVariableBinding nameVarBinding= (IVariableBinding)nameBinding;
						if (declVarBinding.isField() && nameVarBinding.isField()) {
							if (declVarBinding.getVariableDeclaration().isEqualTo(nameVarBinding.getVariableDeclaration())) {
								return false;
							}
						}
					}
					return true;
				}
			}
			return false;
		}
	}

	public final static class AddThisQualifierOperation extends CompilationUnitRewriteOperation {

		private final String fQualifier;
		private final SimpleName fName;

		public AddThisQualifierOperation(String qualifier, SimpleName name) {
			fQualifier= qualifier;
			fName= name;
		}

		public String getDescription() {
			String nameLabel= BasicElementLabels.getJavaElementName(fName.getIdentifier());
			String qualifierLabel;
			if (fQualifier == null) {
				qualifierLabel= "this"; //$NON-NLS-1$
			} else {
				qualifierLabel= BasicElementLabels.getJavaElementName(fQualifier + ".this"); //$NON-NLS-1$
			}

			return Messages.format(FixMessages.CodeStyleFix_QualifyWithThis_description, new Object[] {nameLabel, qualifierLabel});
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(getDescription(), cuRewrite);
			AST ast= rewrite.getAST();

			FieldAccess fieldAccess= ast.newFieldAccess();

			ThisExpression thisExpression= ast.newThisExpression();
			if (fQualifier != null)
				thisExpression.setQualifier(ast.newName(fQualifier));

			fieldAccess.setExpression(thisExpression);
			fieldAccess.setName((SimpleName) rewrite.createMoveTarget(fName));

			rewrite.replace(fName, fieldAccess, group);
		}
	}

	public final static class AddStaticQualifierOperation extends CompilationUnitRewriteOperation {

		private final SimpleName fName;
		private final ITypeBinding fDeclaringClass;

		public AddStaticQualifierOperation(ITypeBinding declaringClass, SimpleName name) {
			super();
			fDeclaringClass= declaringClass;
			fName= name;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			CompilationUnit compilationUnit= cuRewrite.getRoot();
			Type type= importType(fDeclaringClass, fName, cuRewrite.getImportRewrite(), compilationUnit, TypeLocation.OTHER);
			TextEditGroup group;
			if (fName.resolveBinding() instanceof IMethodBinding) {
				group= createTextEditGroup(FixMessages.CodeStyleFix_QualifyMethodWithDeclClass_description, cuRewrite);
			} else {
				group= createTextEditGroup(FixMessages.CodeStyleFix_QualifyFieldWithDeclClass_description, cuRewrite);
			}

			IJavaElement javaElement= fDeclaringClass.getJavaElement();

			if (javaElement instanceof IType) {
				IType javaElementType= (IType) javaElement;

				boolean imported= !type.isNameQualifiedType() && (!type.isSimpleType() || !(((SimpleType) type).getName() instanceof QualifiedName));
				Name qualifierName= compilationUnit.getAST()
						.newName(imported ? javaElementType.getElementName() : javaElementType.getFullyQualifiedName());
				SimpleName simpleName= (SimpleName)rewrite.createMoveTarget(fName);
				QualifiedName qualifiedName= compilationUnit.getAST().newQualifiedName(qualifierName, simpleName);
				rewrite.replace(fName, qualifiedName, group);
			}
		}
	}

	public final static class ToStaticAccessOperation extends CompilationUnitRewriteOperation {

		private final ITypeBinding fDeclaringTypeBinding;
		private final Expression fQualifier;
		private final HashMap<ASTNode, Block> fCreatedBlocks;

		public ToStaticAccessOperation(ITypeBinding declaringTypeBinding, Expression qualifier, HashMap<ASTNode, Block> createdBlocks) {
			fDeclaringTypeBinding= declaringTypeBinding;
			fQualifier= qualifier;
			fCreatedBlocks= createdBlocks;
		}

		public String getAccessorName() {
			return fDeclaringTypeBinding.getName();
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.CodeStyleFix_ChangeAccessUsingDeclaring_description, cuRewrite);

			if (fQualifier instanceof MethodInvocation || fQualifier instanceof ClassInstanceCreation)
				extractQualifier(fQualifier, cuRewrite, group);

			Type type= importType(fDeclaringTypeBinding, fQualifier, cuRewrite.getImportRewrite(), cuRewrite.getRoot(), TypeLocation.UNKNOWN);
			cuRewrite.getASTRewrite().replace(fQualifier, type, group);
		}

		private void extractQualifier(Expression qualifier, CompilationUnitRewrite cuRewrite, TextEditGroup group) {
			Statement statement= ASTResolving.findParentStatement(qualifier);
			if (statement == null)
				return;

			ASTRewrite astRewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getAST();

			Expression expression= (Expression) astRewrite.createMoveTarget(qualifier);
			ExpressionStatement newStatement= ast.newExpressionStatement(expression);

			if (statement.getParent() instanceof Block) {
				Block block= (Block) statement.getParent();
				ListRewrite listRewrite= astRewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);

				listRewrite.insertBefore(newStatement, statement, group);
			} else {
				Block block;
				if (fCreatedBlocks.containsKey(statement.getParent())) {
					block= fCreatedBlocks.get(statement.getParent());
				} else {
					block= ast.newBlock();
				}

				ListRewrite listRewrite= astRewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);

				ASTNode lastStatement;
				if (!fCreatedBlocks.containsKey(statement.getParent())) {
					fCreatedBlocks.put(statement.getParent(), block);

					lastStatement= astRewrite.createMoveTarget(statement);
					listRewrite.insertLast(lastStatement, group);

					ASTNode parent= statement.getParent();
					astRewrite.set(parent, statement.getLocationInParent(), block, group);
				} else {
					List<?> rewrittenList= listRewrite.getRewrittenList();
					lastStatement= (ASTNode) rewrittenList.get(rewrittenList.size() - 1);
				}

				listRewrite.insertBefore(newStatement, lastStatement, group);
			}
		}
	}

	public static CompilationUnitRewriteOperationsFixCore[] createNonStaticAccessFixes(CompilationUnit compilationUnit, IProblemLocationCore problem) {
		if (!isNonStaticAccess(problem))
			return null;

		ToStaticAccessOperation operations[]= createToStaticAccessOperations(compilationUnit, new HashMap<ASTNode, Block>(), problem, false);
		if (operations == null)
			return null;

		String label1= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStatic_description, operations[0].getAccessorName());
		CompilationUnitRewriteOperationsFixCore fix1= new CompilationUnitRewriteOperationsFixCore(label1, compilationUnit, operations[0]);

		if (operations.length > 1) {
			String label2= Messages.format(FixMessages.CodeStyleFix_ChangeAccessToStaticUsingInstanceType_description, operations[1].getAccessorName());
			CompilationUnitRewriteOperationsFixCore fix2= new CompilationUnitRewriteOperationsFixCore(label2, compilationUnit, operations[1]);
			return new CompilationUnitRewriteOperationsFixCore[] {fix1, fix2};
		}
		return new CompilationUnitRewriteOperationsFixCore[] {fix1};
	}

	public static CompilationUnitRewriteOperationsFixCore createAddFieldQualifierFix(CompilationUnit compilationUnit, IProblemLocationCore problem) {
		if (IProblem.UnqualifiedFieldAccess != problem.getProblemId())
			return null;

		AddThisQualifierOperation operation= getUnqualifiedFieldAccessResolveOperation(compilationUnit, problem);
		if (operation == null)
			return null;

		String groupName= operation.getDescription();
		return new CodeStyleFixCore(groupName, compilationUnit, new CompilationUnitRewriteOperation[] {operation});
	}

	public static CompilationUnitRewriteOperationsFixCore createIndirectAccessToStaticFix(CompilationUnit compilationUnit, IProblemLocationCore problem) {
		if (!isIndirectStaticAccess(problem))
			return null;

		ToStaticAccessOperation operations[]= createToStaticAccessOperations(compilationUnit, new HashMap<ASTNode, Block>(), problem, false);
		if (operations == null)
			return null;

		String label= Messages.format(FixMessages.CodeStyleFix_ChangeStaticAccess_description, operations[0].getAccessorName());
		return new CodeStyleFixCore(label, compilationUnit, new CompilationUnitRewriteOperation[] {operations[0]});
	}

	public static ICleanUpFixCore createCleanUp(CompilationUnit compilationUnit,
			boolean addThisQualifier,
			boolean changeNonStaticAccessToStatic,
			boolean qualifyStaticFieldAccess,
			boolean changeIndirectStaticAccessToDirect,
			boolean qualifyMethodAccess,
			boolean qualifyStaticMethodAccess,
			boolean removeFieldQualifier,
			boolean removeMethodQualifier) {

		if (!addThisQualifier && !changeNonStaticAccessToStatic && !qualifyStaticFieldAccess && !changeIndirectStaticAccessToDirect && !qualifyMethodAccess && !qualifyStaticMethodAccess && !removeFieldQualifier && !removeMethodQualifier)
			return null;

		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		if (addThisQualifier || qualifyStaticFieldAccess || qualifyMethodAccess || qualifyStaticMethodAccess) {
			CodeStyleVisitor codeStyleVisitor= new CodeStyleVisitor(compilationUnit, addThisQualifier, qualifyStaticFieldAccess, qualifyMethodAccess, qualifyStaticMethodAccess, operations);
			compilationUnit.accept(codeStyleVisitor);
		}

		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocationCore[] locations= new IProblemLocationCore[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocationCore(problems[i]);
		}
		addToStaticAccessOperations(compilationUnit, locations, changeNonStaticAccessToStatic, changeIndirectStaticAccessToDirect, operations);

		if (removeFieldQualifier || removeMethodQualifier) {
			ThisQualifierVisitor visitor= new ThisQualifierVisitor(removeFieldQualifier, removeMethodQualifier, compilationUnit, operations);
			compilationUnit.accept(visitor);
		}

		if (operations.isEmpty())
			return null;

		CompilationUnitRewriteOperation[] operationsArray= operations.toArray(new CompilationUnitRewriteOperation[operations.size()]);
		return new CodeStyleFixCore(FixMessages.CodeStyleFix_change_name, compilationUnit, operationsArray);
	}

	public static ICleanUpFixCore createCleanUp(CompilationUnit compilationUnit, IProblemLocationCore[] problems,
			boolean addThisQualifier,
			boolean changeNonStaticAccessToStatic,
			boolean changeIndirectStaticAccessToDirect) {

		if (!addThisQualifier && !changeNonStaticAccessToStatic && !changeIndirectStaticAccessToDirect)
			return null;

		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		if (addThisQualifier) {
			for (IProblemLocationCore problem : problems) {
				if (problem.getProblemId() == IProblem.UnqualifiedFieldAccess) {
					AddThisQualifierOperation operation= getUnqualifiedFieldAccessResolveOperation(compilationUnit, problem);
					if (operation != null)
						operations.add(operation);
				}
			}
		}

		addToStaticAccessOperations(compilationUnit, problems, changeNonStaticAccessToStatic, changeIndirectStaticAccessToDirect, operations);

		if (operations.isEmpty())
			return null;

		CompilationUnitRewriteOperation[] operationsArray= operations.toArray(new CompilationUnitRewriteOperation[operations.size()]);
		return new CodeStyleFixCore(FixMessages.CodeStyleFix_change_name, compilationUnit, operationsArray);
	}

	public static void addToStaticAccessOperations(CompilationUnit compilationUnit, IProblemLocationCore[] problems, boolean changeNonStaticAccessToStatic, boolean changeIndirectStaticAccessToDirect, List<CompilationUnitRewriteOperation> result) {
		if (!changeNonStaticAccessToStatic && !changeIndirectStaticAccessToDirect)
			return;

		List<ToStaticAccessOperation> operations= new ArrayList<>();
		HashMap<ASTNode, Block> createdBlocks= new HashMap<>();
		for (IProblemLocationCore problem : problems) {
			boolean isNonStaticAccess= changeNonStaticAccessToStatic && isNonStaticAccess(problem);
			boolean isIndirectStaticAccess= changeIndirectStaticAccessToDirect && isIndirectStaticAccess(problem);
			if (isNonStaticAccess || isIndirectStaticAccess) {
				ToStaticAccessOperation[] nonStaticAccessInformation= createToStaticAccessOperations(compilationUnit, createdBlocks, problem, true);
				if (nonStaticAccessInformation != null) {
					ToStaticAccessOperation op= nonStaticAccessInformation[0];

					Expression qualifier= op.fQualifier;
					for (CompilationUnitRewriteOperation oper : result) { // see bug 346230
						if (oper instanceof CodeStyleFixCore.AddThisQualifierOperation
								&& ((CodeStyleFixCore.AddThisQualifierOperation) oper).fName.equals(qualifier)) {
							result.remove(oper);
							break;
						}
					}
					operations.add(op);
				}
			}
		}
		// Make sure qualifiers are processed inside-out and left-to-right, so that
		// ToStaticAccessOperation#extractQualifier(..) extracts qualifiers in execution order:
		Collections.sort(operations, (o1, o2) -> {
			if (ASTNodes.isParent(o1.fQualifier, o2.fQualifier)) {
				return -1;
			} else if (ASTNodes.isParent(o2.fQualifier, o1.fQualifier)) {
				return 1;
			} else {
				return o1.fQualifier.getStartPosition() - o2.fQualifier.getStartPosition();
			}
		});
		result.addAll(operations);
	}

	public static boolean isIndirectStaticAccess(IProblemLocationCore problem) {
		return (problem.getProblemId() == IProblem.IndirectAccessToStaticField
				|| problem.getProblemId() == IProblem.IndirectAccessToStaticMethod);
	}

	public static boolean isNonStaticAccess(IProblemLocationCore problem) {
		return (problem.getProblemId() == IProblem.NonStaticAccessToStaticField
				|| problem.getProblemId() == IProblem.NonStaticAccessToStaticMethod
				|| problem.getProblemId() == IProblem.NonStaticOrAlienTypeReceiver);
	}

	public static ToStaticAccessOperation[] createToStaticAccessOperations(CompilationUnit astRoot, HashMap<ASTNode, Block> createdBlocks, IProblemLocationCore problem, boolean conservative) {
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return null;
		}

		Expression qualifier= null;
		IBinding accessBinding= null;

		if (selectedNode instanceof SimpleName) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode instanceof QualifiedName) {
			QualifiedName name= (QualifiedName) selectedNode;
			qualifier= name.getQualifier();
			accessBinding= name.resolveBinding();
		} else if (selectedNode instanceof MethodInvocation) {
			MethodInvocation methodInvocation= (MethodInvocation) selectedNode;
			qualifier= methodInvocation.getExpression();
			accessBinding= methodInvocation.getName().resolveBinding();
		} else if (selectedNode instanceof FieldAccess) {
			FieldAccess fieldAccess= (FieldAccess) selectedNode;
			qualifier= fieldAccess.getExpression();
			accessBinding= fieldAccess.getName().resolveBinding();
		}

		if (accessBinding != null && qualifier != null) {
			if (conservative && ASTResolving.findParentStatement(qualifier) == null)
				return null;

			ToStaticAccessOperation declaring= null;
			ITypeBinding declaringTypeBinding= getDeclaringTypeBinding(accessBinding);
			if (declaringTypeBinding != null) {
				declaringTypeBinding= declaringTypeBinding.getTypeDeclaration(); // use generic to avoid any type arguments
				int modifiers= declaringTypeBinding.getModifiers();
				if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isPrivate(modifiers)) {
					PackageDeclaration packageDecl= astRoot.getPackage();
					if (packageDecl == null) {
						if (declaringTypeBinding.getPackage() != null) {
							return null;
						}
					} else {
					    if (!declaringTypeBinding.getPackage().isEqualTo(packageDecl.resolveBinding())) {
					    	return null;
					    }
					}
				}
				declaring= new ToStaticAccessOperation(declaringTypeBinding, qualifier, createdBlocks);
			}
			ToStaticAccessOperation instance= null;
			ITypeBinding instanceTypeBinding= Bindings.normalizeTypeBinding(qualifier.resolveTypeBinding());
			if (instanceTypeBinding != null) {
				instanceTypeBinding= instanceTypeBinding.getTypeDeclaration();  // use generic to avoid any type arguments
				if (instanceTypeBinding.getTypeDeclaration() != declaringTypeBinding) {
					instance= new ToStaticAccessOperation(instanceTypeBinding, qualifier, createdBlocks);
				}
			}
			if (declaring != null && instance != null) {
				return new ToStaticAccessOperation[] {declaring, instance};
			} else {
				return new ToStaticAccessOperation[] {declaring};
			}
		}
		return null;
	}

	private static ITypeBinding getDeclaringTypeBinding(IBinding accessBinding) {
		if (accessBinding instanceof IMethodBinding) {
			return ((IMethodBinding) accessBinding).getDeclaringClass();
		} else if (accessBinding instanceof IVariableBinding) {
			return ((IVariableBinding) accessBinding).getDeclaringClass();
		}
		return null;
	}

	public static AddThisQualifierOperation getUnqualifiedFieldAccessResolveOperation(CompilationUnit compilationUnit, IProblemLocationCore problem) {
		SimpleName name= getName(compilationUnit, problem);
		if (name == null)
			return null;

		IBinding binding= name.resolveBinding();
		if (binding == null || binding.getKind() != IBinding.VARIABLE)
			return null;

		ImportRewrite imports= StubUtility.createImportRewrite(compilationUnit, true);

		String replacement= getThisExpressionQualifier(((IVariableBinding) binding).getDeclaringClass(), imports, name);
		if (replacement == null)
			return null;

		if (replacement.length() == 0)
			replacement= null;

		return new AddThisQualifierOperation(replacement, name);
	}

	private static String getThisExpressionQualifier(ITypeBinding declaringClass, ImportRewrite imports, SimpleName name) {
		ITypeBinding parentType= Bindings.getBindingOfParentType(name);
		ITypeBinding currType= parentType;
		while (currType != null && !Bindings.isSuperType(declaringClass, currType)) {
			currType= currType.getDeclaringClass();
		}
		if (currType == null) {
			declaringClass= declaringClass.getTypeDeclaration();
			currType= parentType;
			while (currType != null && !Bindings.isSuperType(declaringClass, currType)) {
				currType= currType.getDeclaringClass();
			}
		}
		if (currType != parentType) {
			if (currType == null)
				return null;

			if (currType.isAnonymous())
				//If we access a field of a super class of an anonymous class
				//then we can only qualify with 'this' but not with outer.this
				//see bug 115277
				return null;

			return imports.addImport(currType);
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	private static SimpleName getName(CompilationUnit compilationUnit, IProblemLocationCore problem) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

		while (selectedNode instanceof QualifiedName) {
			selectedNode= ((QualifiedName) selectedNode).getQualifier();
		}
		if (!(selectedNode instanceof SimpleName)) {
			return null;
		}
		return (SimpleName) selectedNode;
	}

	private CodeStyleFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] operations) {
		super(name, compilationUnit, operations);
	}

}
