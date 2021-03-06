/*-******************************************************************************
 * Copyright (c) 2014 Iwao AVE!.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Iwao AVE! - initial API and implementation and/or initial documentation
 *******************************************************************************/

package net.harawata.mybatipse.mybatis;

import java.util.List;

import net.harawata.mybatipse.Activator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * @author Iwao AVE!
 */
public class JavaQuickAssistProcessor implements IQuickAssistProcessor
{
	public JavaQuickAssistProcessor()
	{
		super();
	}

	@Override
	public boolean hasAssists(IInvocationContext context) throws CoreException
	{
		return false;
	}

	@Override
	public IJavaCompletionProposal[] getAssists(IInvocationContext context,
		IProblemLocation[] locations) throws CoreException
	{
		ICompilationUnit compilationUnit = context.getCompilationUnit();
		IJavaElement[] elements = compilationUnit.codeSelect(context.getSelectionOffset(),
			context.getSelectionLength());
		for (IJavaElement element : elements)
		{
			if (element.getElementType() == IJavaElement.METHOD)
			{
				IMethod method = (IMethod)element;
				if (!method.getDeclaringType().isInterface() || method.getParameters().length == 0)
					return null;

				CompilationUnit astNode = JavaMapperUtil.getAstNode(compilationUnit);
				astNode.recordModifications();
				MethodDeclaration methodDeclaration = JavaMapperUtil.getMethodDeclaration(astNode,
					method);
				if (methodDeclaration == null)
					return null;

				return new IJavaCompletionProposal[]{
					new IJavaCompletionProposal()
					{
						private CompilationUnit astNode;

						private MethodDeclaration method;

						@SuppressWarnings("unchecked")
						@Override
						public void apply(IDocument document)
						{
							List<SingleVariableDeclaration> params = method.parameters();
							for (SingleVariableDeclaration param : params)
							{
								List<IExtendedModifier> modifiers = param.modifiers();
								if (!hasParamAnnotation(modifiers))
								{
									if (JavaMapperUtil.TYPE_ROW_BOUNDS.equals(param.resolveBinding()
										.getType()
										.getQualifiedName()))
										continue;
									AST ast = param.getAST();
									SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
									annotation.setTypeName(ast.newName("Param"));
									StringLiteral paramValue = ast.newStringLiteral();
									paramValue.setLiteralValue(param.getName().getFullyQualifiedName());
									annotation.setValue(paramValue);
									param.modifiers().add(annotation);
								}
							}
							TextEdit textEdit = astNode.rewrite(document, null);
							try
							{
								textEdit.apply(document);
							}
							catch (MalformedTreeException e)
							{
								Activator.log(Status.ERROR, e.getMessage(), e);
							}
							catch (BadLocationException e)
							{
								Activator.log(Status.ERROR, e.getMessage(), e);
							}
						}

						private boolean hasParamAnnotation(List<IExtendedModifier> modifiers)
						{
							for (IExtendedModifier modifier : modifiers)
							{
								if (modifier.isAnnotation()
									&& "Param".equals(((Annotation)modifier).getTypeName()
										.getFullyQualifiedName()))
								{
									return true;
								}
							}
							return false;
						}

						@Override
						public Point getSelection(IDocument document)
						{
							return null;
						}

						@Override
						public String getAdditionalProposalInfo()
						{
							return null;
						}

						@Override
						public String getDisplayString()
						{
							return "Add @Param to parameters";
						}

						@Override
						public Image getImage()
						{
							return Activator.getIcon();
						}

						@Override
						public IContextInformation getContextInformation()
						{
							return null;
						}

						@Override
						public int getRelevance()
						{
							return 500;
						}

						private IJavaCompletionProposal init(CompilationUnit astNode,
							MethodDeclaration method)
						{
							this.astNode = astNode;
							this.method = method;
							return this;
						}
					}.init(astNode, methodDeclaration)
				};
			}
		}
		return null;
	}
}
