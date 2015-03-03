/* 
  Copyright (C) 2013 Raquel Pau and Albert Coroleu.
 
 Walkmod is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Walkmod is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public License
 along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.javalang.walkers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.walkmod.exceptions.WalkModException;
import org.walkmod.javalang.actions.Action;
import org.walkmod.javalang.actions.ActionType;
import org.walkmod.javalang.actions.AppendAction;
import org.walkmod.javalang.actions.RemoveAction;
import org.walkmod.javalang.actions.ReplaceAction;
import org.walkmod.javalang.ast.BlockComment;
import org.walkmod.javalang.ast.Comment;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.LineComment;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.PackageDeclaration;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.AnnotationDeclaration;
import org.walkmod.javalang.ast.body.AnnotationMemberDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.ConstructorDeclaration;
import org.walkmod.javalang.ast.body.EmptyMemberDeclaration;
import org.walkmod.javalang.ast.body.EmptyTypeDeclaration;
import org.walkmod.javalang.ast.body.EnumConstantDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.InitializerDeclaration;
import org.walkmod.javalang.ast.body.JavadocComment;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.MultiTypeParameter;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.body.VariableDeclaratorId;
import org.walkmod.javalang.ast.expr.ArrayAccessExpr;
import org.walkmod.javalang.ast.expr.ArrayCreationExpr;
import org.walkmod.javalang.ast.expr.ArrayInitializerExpr;
import org.walkmod.javalang.ast.expr.AssignExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr;
import org.walkmod.javalang.ast.expr.BooleanLiteralExpr;
import org.walkmod.javalang.ast.expr.CastExpr;
import org.walkmod.javalang.ast.expr.CharLiteralExpr;
import org.walkmod.javalang.ast.expr.ClassExpr;
import org.walkmod.javalang.ast.expr.ConditionalExpr;
import org.walkmod.javalang.ast.expr.DoubleLiteralExpr;
import org.walkmod.javalang.ast.expr.EnclosedExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.FieldAccessExpr;
import org.walkmod.javalang.ast.expr.InstanceOfExpr;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;
import org.walkmod.javalang.ast.expr.IntegerLiteralMinValueExpr;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.ast.expr.LongLiteralExpr;
import org.walkmod.javalang.ast.expr.LongLiteralMinValueExpr;
import org.walkmod.javalang.ast.expr.MarkerAnnotationExpr;
import org.walkmod.javalang.ast.expr.MemberValuePair;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.MethodReferenceExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.NormalAnnotationExpr;
import org.walkmod.javalang.ast.expr.NullLiteralExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.expr.QualifiedNameExpr;
import org.walkmod.javalang.ast.expr.SingleMemberAnnotationExpr;
import org.walkmod.javalang.ast.expr.StringLiteralExpr;
import org.walkmod.javalang.ast.expr.SuperExpr;
import org.walkmod.javalang.ast.expr.ThisExpr;
import org.walkmod.javalang.ast.expr.UnaryExpr;
import org.walkmod.javalang.ast.expr.VariableDeclarationExpr;
import org.walkmod.javalang.ast.stmt.AssertStmt;
import org.walkmod.javalang.ast.stmt.BlockStmt;
import org.walkmod.javalang.ast.stmt.BreakStmt;
import org.walkmod.javalang.ast.stmt.CatchClause;
import org.walkmod.javalang.ast.stmt.ContinueStmt;
import org.walkmod.javalang.ast.stmt.DoStmt;
import org.walkmod.javalang.ast.stmt.EmptyStmt;
import org.walkmod.javalang.ast.stmt.ExplicitConstructorInvocationStmt;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.ForStmt;
import org.walkmod.javalang.ast.stmt.ForeachStmt;
import org.walkmod.javalang.ast.stmt.IfStmt;
import org.walkmod.javalang.ast.stmt.LabeledStmt;
import org.walkmod.javalang.ast.stmt.ReturnStmt;
import org.walkmod.javalang.ast.stmt.SwitchEntryStmt;
import org.walkmod.javalang.ast.stmt.SwitchStmt;
import org.walkmod.javalang.ast.stmt.SynchronizedStmt;
import org.walkmod.javalang.ast.stmt.ThrowStmt;
import org.walkmod.javalang.ast.stmt.TryStmt;
import org.walkmod.javalang.ast.stmt.TypeDeclarationStmt;
import org.walkmod.javalang.ast.stmt.WhileStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.PrimitiveType;
import org.walkmod.javalang.ast.type.ReferenceType;
import org.walkmod.javalang.ast.type.VoidType;
import org.walkmod.javalang.ast.type.WildcardType;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;
import org.walkmod.walkers.VisitorContext;

public class ChangeLogVisitor extends VoidVisitorAdapter<VisitorContext> {

	private Map<String, Integer> addedNodes;

	private Map<String, Integer> deletedNodes;

	private Map<String, Integer> updatedNodes;

	private Map<String, Integer> unmodifiedNodes;

	public static final String NODE_TO_COMPARE_KEY = "node_to_compare_key";

	public static final String MAP_TO_UPDATE_KEY = "map_to_update_key";

	public static final String UPDATE_ACTION_KEY = "update_action_key";

	public static final String ADD_ACTION_KEY = "add_action_key";

	public static final String DELETE_ACTION_KEY = "delete_action_key";

	private boolean isUpdated = false;

	private static Properties properties = null;

	private static Logger log = Logger.getLogger(ChangeLogVisitor.class);

	private String reportingPropertiesPath = "reporting.properties";

	private boolean generateActions = true;

	private int line = 0;

	private int column = 0;

	private LinkedList<Action> actionsToApply = new LinkedList<Action>();

	private int indentationLevel = 0;

	private int indentationSize = 0;

	private List<Comment> comments = new LinkedList<Comment>();

	public ChangeLogVisitor() {
		addedNodes = new HashMap<String, Integer>();
		deletedNodes = new HashMap<String, Integer>();
		updatedNodes = new HashMap<String, Integer>();
		unmodifiedNodes = new HashMap<String, Integer>();
		setReportingPropertiesPath(reportingPropertiesPath);
	}

	private void increaseIndentation() {
		indentationLevel++;
	}

	public <T extends Node> void inferIndentationSize(Node parent,
			List<T> children) {
		if (parent != null && children != null && !children.isEmpty()) {

			Node first = children.get(0);
			if (first.getBeginLine() != parent.getBeginLine()) {
				indentationSize = first.getBeginColumn()
						- parent.getBeginColumn();
			} else {
				indentationSize = 0;
			}
		}
	}

	private void decreaseIndentation() {
		indentationLevel--;
	}

	public String getReportingPropertiesPath() {
		return reportingPropertiesPath;
	}

	public List<Action> getActionsToApply() {
		return actionsToApply;
	}

	public void setReportingPropertiesPath(String reportingPropertiesPath) {
		this.reportingPropertiesPath = reportingPropertiesPath;
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader
				.getResourceAsStream(this.reportingPropertiesPath);
		try {
			if (stream != null) {
				properties = new Properties();
				try {
					properties.load(stream);
				} catch (IOException e) {
					throw new WalkModException(e);
				}
			} else {
				properties = null;
				log.warn("The system cannot found the reporting.properties in the classpath. It will report all code changes");
			}
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				throw new WalkModException(e);
			}
		}
	}

	public void increaseAddedNodes(Class<?> clazz) {
		String name = clazz.getSimpleName();
		Integer count = addedNodes.get(name);
		if (count == null) {
			count = 0;
		}
		addedNodes.put(name, count + 1);
		setIsUpdated(true);
	}

	public void increaseDeletedNodes(Class<?> clazz) {
		String name = clazz.getSimpleName();
		Integer count = deletedNodes.get(name);
		if (count == null) {
			count = 0;
		}
		deletedNodes.put(name, count + 1);
		setIsUpdated(true);
	}

	public void increaseUpdatedNodes(Class<?> clazz) {
		String name = clazz.getSimpleName();
		Integer count = updatedNodes.get(name);
		if (count == null) {
			count = 0;
		}
		updatedNodes.put(name, count + 1);
		setIsUpdated(true);
	}

	public void increaseUnmodifiedNodes(Class<?> clazz) {
		String name = clazz.getSimpleName();
		Integer count = unmodifiedNodes.get(name);
		if (count == null) {
			count = 0;
		}
		unmodifiedNodes.put(name, count + 1);
	}

	private void updatePosition(Node node) {

		line = node.getEndLine();
		column = node.getEndColumn();

	}

	private void updatePosition(int line, int column) {
		this.line = line;
		this.column = column;
	}

	private void applyRemove(Node oi) {
		VisitorContext vc = new VisitorContext();
		vc.put(NODE_TO_COMPARE_KEY, null);
		vc.put(MAP_TO_UPDATE_KEY, DELETE_ACTION_KEY);
		if (generateActions) {
			RemoveAction action = null;
			if (!actionsToApply.isEmpty()) {
				Action lastAction = actionsToApply
						.get(actionsToApply.size() - 1);
				if (lastAction.getType().equals(ActionType.REMOVE)) {

					action = new RemoveAction(oi.getBeginLine(),
							oi.getBeginColumn(), oi.getEndLine(),
							oi.getEndColumn(), oi);

					if (!((RemoveAction) action).isPreviousThan(
							lastAction.getEndLine(), lastAction.getEndColumn())) {

						actionsToApply.add(action);

					}
				}
			} else {
				action = new RemoveAction(oi.getBeginLine(),
						oi.getBeginColumn(), oi.getEndLine(),
						oi.getEndColumn(), oi);
				actionsToApply.add(action);
			}
		}
		oi.accept(this, vc);
	}

	private void applyAppend(Node id) {
		VisitorContext vc = new VisitorContext();
		vc.put(NODE_TO_COMPARE_KEY, null);
		vc.put(MAP_TO_UPDATE_KEY, ADD_ACTION_KEY);
		AppendAction action = null;
		if (generateActions) {
			if (!actionsToApply.isEmpty()) {
				Action lastAction = actionsToApply
						.get(actionsToApply.size() - 1);
				if (lastAction.getType().equals(ActionType.APPEND)) {
					// we evaluate if it is an internal node or what we generate
					// is after the
					// last appending action
					action = new AppendAction(line, column, id,
							indentationLevel, indentationSize);
					if (!((AppendAction) action).isPreviousThan(
							lastAction.getEndLine(), lastAction.getEndColumn())) {
						actionsToApply.add(action);
					}
				}
			} else {
				// parent last position?
				action = new AppendAction(line, column, id, indentationLevel,
						indentationSize);
				actionsToApply.add(action);

			}
		}
		id.accept(this, vc);
	}

	private void applyUpdate(Node newNode, Node oldNode) {
		if (generateActions) {
			Action action = null;
			if (!actionsToApply.isEmpty()) {

				Iterator<Action> inverseIt = actionsToApply
						.descendingIterator();
				boolean finish = false;
				action = new RemoveAction(oldNode.getBeginLine(),
						oldNode.getBeginColumn(), oldNode.getEndLine(),
						oldNode.getEndColumn(), oldNode);
				while (inverseIt.hasNext() && !finish) {
					Action last = inverseIt.next();
					if (!last.isPreviousThan(action.getBeginLine(),
							action.getBeginColumn())) {
						inverseIt.remove();
					}
				}
				action = new ReplaceAction(oldNode.getBeginLine(),
						oldNode.getBeginColumn(), oldNode, newNode,
						indentationLevel, indentationSize, comments);
				actionsToApply.add(action);

			} else {
				action = new ReplaceAction(oldNode.getBeginLine(),
						oldNode.getBeginColumn(), oldNode, newNode,
						indentationLevel, indentationSize, comments);
				actionsToApply.add(action);
			}
		}
	}

	private <T extends Node> void inferASTChanges(List<T> nodes1, List<T> nodes2) {
		if (nodes1 != null) {
			for (T id : nodes1) {
				if (id.isNewNode()) {
					applyAppend(id);
				} else {
					if (nodes2 != null) {
						boolean found = false;
						Iterator<T> it2 = nodes2.iterator();
						T oi = null;
						while (it2.hasNext() && !found) {
							oi = it2.next();
							found = (id.getBeginLine() == oi.getBeginLine() && id
									.getBeginColumn() == oi.getBeginColumn());
						}
						if (found) {
							VisitorContext vc = new VisitorContext();
							vc.put(NODE_TO_COMPARE_KEY, oi);
							// it does the update if its necessary
							int line = this.line;
							int column = this.column;
							updatePosition(id);
							id.accept(this, vc);
							updatePosition(line, column);
						} else {
							applyRemove(id);
						}
					}
				}
			}
			if (nodes2 != null) {
				for (T oi : nodes2) {
					boolean found = false;
					Iterator<T> it = nodes1.iterator();
					T id = null;
					while (it.hasNext() && !found) {
						id = it.next();
						found = (id.getBeginLine() == oi.getBeginLine() && id
								.getBeginColumn() == oi.getBeginColumn());// ((Node)
																			// id).isInEqualLocation((Node)
																			// oi);
					}
					if (!found) {
						applyRemove(oi);

					}
				}
			}
		} else {
			if (nodes2 != null) {
				for (T elem : nodes2) {
					applyRemove(elem);

				}
			}
		}
	}

	private <T extends Node> void inferASTChangesList(List<List<T>> nodes1,
			List<List<T>> nodes2) {
		if (nodes1 != null) {
			if (nodes2 == null) {
				for (List<T> elem : nodes1) {
					inferASTChanges(null, elem);
				}
			} else {
				Iterator<List<T>> it1 = nodes1.iterator();
				Iterator<List<T>> it2 = nodes2.iterator();
				while (it1.hasNext() || it2.hasNext()) {
					if (it1.hasNext() && it2.hasNext()) {
						inferASTChanges(it1.next(), it2.next());
					} else if (!it1.hasNext()) {
						inferASTChanges(null, it2.next());
					} else {
						inferASTChanges(it1.next(), null);
					}
				}
			}
		} else {
			if (nodes2 != null) {
				for (List<T> elem : nodes2) {
					inferASTChanges(null, elem);
				}
			}
		}

	}

	private <T extends Node> void inferASTChanges(T n1, T n2) {
		if (n1 != null) {

			if (n2 == null) {
				applyAppend(n1);
			} else {
				VisitorContext vc = new VisitorContext();
				vc.put(NODE_TO_COMPARE_KEY, n2);
				n1.accept(this, vc);
			}
		} else {

			if (n2 != null) {
				applyRemove(n2);
			}
		}
	}

	public void setIsUpdated(boolean isUpdated) {
		this.isUpdated = isUpdated;
	}

	public boolean isUpdated() {
		return isUpdated;
	}

	public void visit(CompilationUnit n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof CompilationUnit) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			if (o instanceof CompilationUnit) {
				if (n.getComments() != null) {
					comments = new LinkedList<Comment>(n.getComments());
				}
				CompilationUnit oldCU = (CompilationUnit) o;
				inferASTChanges(n.getPackage(), oldCU.getPackage());
				inferASTChanges(n.getImports(), oldCU.getImports());
				inferASTChanges(n.getTypes(), oldCU.getTypes());
				inferASTChanges(n.getComments(), oldCU.getComments());
				if (!isUpdated()) {
					increaseUnmodifiedNodes(CompilationUnit.class);
				} else {
					increaseUpdatedNodes(CompilationUnit.class);
				}
			}
			setIsUpdated(backup || isUpdated);
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(CompilationUnit.class);
					inferASTChanges(n.getPackage(), null);
					inferASTChanges(n.getImports(), null);
					inferASTChanges(n.getTypes(), null);
					inferASTChanges(n.getComments(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(CompilationUnit.class);
					inferASTChanges(null, n.getPackage());
					inferASTChanges(null, n.getImports());
					inferASTChanges(null, n.getTypes());
					inferASTChanges(null, n.getComments());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(PackageDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof PackageDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			PackageDeclaration aux = (PackageDeclaration) o;
			inferASTChanges(n.getName(), aux.getName());
			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(PackageDeclaration.class);
			} else {
				increaseUpdatedNodes(PackageDeclaration.class);
			}
			setIsUpdated(backup || isUpdated);
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(PackageDeclaration.class);
					inferASTChanges(n.getName(), null);
					inferASTChanges(n.getAnnotations(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(PackageDeclaration.class);
					inferASTChanges(null, n.getName());
					inferASTChanges(null, n.getAnnotations());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ImportDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ImportDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			ImportDeclaration aux = (ImportDeclaration) o;
			inferASTChanges(n.getName(), aux.getName());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ImportDeclaration.class);
			} else {
				increaseUpdatedNodes(ImportDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ImportDeclaration.class);
					inferASTChanges(n.getName(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ImportDeclaration.class);
					inferASTChanges(null, n.getName());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ClassOrInterfaceDeclaration n, VisitorContext ctx) {

		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ClassOrInterfaceDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			ClassOrInterfaceDeclaration aux = (ClassOrInterfaceDeclaration) o;

			boolean equals = n.getName().equals(aux.getName())
					&& n.getModifiers() == aux.getModifiers();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getExtends(), aux.getExtends());
			inferASTChanges(n.getImplements(), aux.getImplements());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());

			inferASTChanges(n.getTypeParameters(), aux.getTypeParameters());
			increaseIndentation();
			inferIndentationSize(n, n.getMembers());
			inferASTChanges(n.getMembers(), aux.getMembers());
			decreaseIndentation();
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(ClassOrInterfaceDeclaration.class);
				} else {
					increaseUpdatedNodes(ClassOrInterfaceDeclaration.class);
				}
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				applyUpdate(n, aux, n.getExtends(), aux.getExtends());
				applyUpdate(n, aux, n.getImplements(), aux.getImplements());
				applyUpdate(n, aux, n.getTypeParameters(),
						aux.getTypeParameters());

				increaseUpdatedNodes(ClassOrInterfaceDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ClassOrInterfaceDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getExtends(), null);
					inferASTChanges(n.getImplements(), null);
					inferASTChanges(n.getJavaDoc(), null);
					increaseIndentation();
					inferIndentationSize(n, n.getMembers());
					inferASTChanges(n.getMembers(), null);
					decreaseIndentation();
					inferASTChanges(n.getTypeParameters(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ClassOrInterfaceDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getExtends());
					inferASTChanges(null, n.getImplements());
					inferASTChanges(null, n.getJavaDoc());
					increaseIndentation();
					inferIndentationSize(n, n.getMembers());
					inferASTChanges(null, n.getMembers());
					decreaseIndentation();
					inferASTChanges(null, n.getTypeParameters());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(TypeParameter n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && n instanceof TypeParameter) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			TypeParameter aux = (TypeParameter) o;

			boolean equals = n.getName().equals(aux.getName());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getTypeBound(), aux.getTypeBound());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(TypeParameter.class);
				} else {

					increaseUpdatedNodes(TypeParameter.class);
				}
			} else {

				increaseUpdatedNodes(TypeParameter.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(TypeParameter.class);
					inferASTChanges(n.getTypeBound(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(TypeParameter.class);
					inferASTChanges(null, n.getTypeBound());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(MethodDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && n instanceof MethodDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);

			MethodDeclaration aux = (MethodDeclaration) o;
			boolean equals = n.getName().equals(aux.getName())
					&& n.getArrayCount() == aux.getArrayCount()
					&& n.getModifiers() == aux.getModifiers()
					&& n.isDefault() == aux.isDefault();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}
			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getBody(), aux.getBody());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());

			inferASTChanges(n.getParameters(), aux.getParameters());

			inferASTChanges(n.getThrows(), aux.getThrows());
			inferASTChanges(n.getType(), aux.getType());
			inferASTChanges(n.getTypeParameters(), aux.getTypeParameters());

			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(MethodDeclaration.class);
				} else {
					increaseUpdatedNodes(MethodDeclaration.class);
				}
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				applyUpdate(n, (Node) o, n.getParameters(), aux.getParameters());
				applyUpdate(n, (Node) o, n.getTypeParameters(),
						aux.getTypeParameters());
				increaseUpdatedNodes(MethodDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(MethodDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getBody(), null);
					inferASTChanges(n.getJavaDoc(), null);
					inferASTChanges(n.getParameters(), null);
					inferASTChanges(n.getThrows(), null);
					inferASTChanges(n.getType(), null);
					inferASTChanges(n.getTypeParameters(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(MethodDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getBody());
					inferASTChanges(null, n.getJavaDoc());
					inferASTChanges(null, n.getParameters());
					inferASTChanges(null, n.getThrows());
					inferASTChanges(null, n.getType());
					inferASTChanges(null, n.getTypeParameters());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(FieldDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && n instanceof FieldDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			FieldDeclaration aux = (FieldDeclaration) o;
			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
			inferASTChanges(n.getType(), aux.getType());
			inferASTChanges(n.getVariables(), aux.getVariables());
			if (!isUpdated && n.getModifiers() == aux.getModifiers()) {
				increaseUnmodifiedNodes(FieldDeclaration.class);
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				applyUpdate(n, (Node) o, n.getVariables(), aux.getVariables());
				increaseUpdatedNodes(FieldDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(FieldDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getJavaDoc(), null);
					inferASTChanges(n.getType(), null);
					inferASTChanges(n.getVariables(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(FieldDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getJavaDoc());
					inferASTChanges(null, n.getType());
					inferASTChanges(null, n.getVariables());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(LineComment n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof LineComment) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			LineComment aux = (LineComment) o;

			boolean equals = n.getContent().equals(aux.getContent());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			if (equals) {
				increaseUnmodifiedNodes(LineComment.class);
			} else {
				increaseUpdatedNodes(LineComment.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(LineComment.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(LineComment.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(BlockComment n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof BlockComment) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			BlockComment aux = (BlockComment) o;
			boolean equals = n.getContent().equals(aux.getContent());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}
			if (n.getContent().equals(aux.getContent())) {
				increaseUnmodifiedNodes(BlockComment.class);
			} else {
				increaseUpdatedNodes(BlockComment.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(BlockComment.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(BlockComment.class);
				}
			}
			setIsUpdated(true);
		}
	}

	private <T extends Node> void applyUpdate(T thisNode, T other,
			List<?> theseNodes, List<?> otherNodes) {
		if (otherNodes == null && theseNodes == null) {
			return;
		} else if (otherNodes == null || theseNodes == null) {
			applyUpdate(thisNode, (Node) other);
		} else if (theseNodes.size() != otherNodes.size()) {
			applyUpdate(thisNode, (Node) other);
		}

	}

	private <T extends Node> void applyUpdate(T thisNode, T other,
			Node theseProperty, Node otherProperty) {
		if (otherProperty == null && theseProperty == null) {
			return;
		} else if (otherProperty == null || theseProperty == null) {
			applyUpdate(thisNode, (Node) other);
		} else if (!otherProperty.equals(theseProperty)) {
			applyUpdate(thisNode, (Node) other);
		}

	}

	public void visit(EnumDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof EnumDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			EnumDeclaration aux = (EnumDeclaration) o;
			boolean equals = n.getName().equals(aux.getName())
					&& n.getModifiers() == aux.getModifiers();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}
			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getImplements(), aux.getImplements());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
			increaseIndentation();

			inferIndentationSize(n, n.getMembers());
			List<EnumConstantDeclaration> theseEntries = n.getEntries();
			List<EnumConstantDeclaration> otherEntries = aux.getEntries();

			inferASTChanges(theseEntries, otherEntries);
			inferASTChanges(n.getMembers(), aux.getMembers());

			decreaseIndentation();
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(EnumDeclaration.class);
				} else {
					increaseUpdatedNodes(EnumDeclaration.class);
				}
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				applyUpdate(n, aux, n.getImplements(), aux.getImplements());
				applyUpdate(n, aux, theseEntries, otherEntries);

				increaseUpdatedNodes(EnumDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(EnumDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getImplements(), null);
					inferASTChanges(n.getJavaDoc(), null);
					increaseIndentation();
					inferASTChanges(n.getEntries(), null);
					inferIndentationSize(n, n.getMembers());
					inferASTChanges(n.getMembers(), null);
					decreaseIndentation();

				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(EnumDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getImplements());
					inferASTChanges(null, n.getJavaDoc());
					increaseIndentation();
					inferIndentationSize(n, n.getMembers());
					inferASTChanges(n.getEntries(), null);
					inferASTChanges(null, n.getMembers());
					decreaseIndentation();

				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(EmptyTypeDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof EmptyTypeDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			EmptyTypeDeclaration aux = (EmptyTypeDeclaration) o;
			boolean equals = n.getName().equals(aux.getName())
					&& n.getModifiers() == aux.getModifiers();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
			inferIndentationSize(n, n.getMembers());
			increaseIndentation();
			inferASTChanges(n.getMembers(), aux.getMembers());
			decreaseIndentation();
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(EmptyTypeDeclaration.class);
				} else {
					applyUpdate(n, (Node) o);
					increaseUpdatedNodes(EmptyTypeDeclaration.class);
				}
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				increaseUpdatedNodes(EmptyTypeDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(EmptyTypeDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getJavaDoc(), null);
					inferIndentationSize(n, n.getMembers());
					increaseIndentation();
					inferASTChanges(n.getMembers(), null);
					decreaseIndentation();
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(EmptyTypeDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getJavaDoc());
					inferIndentationSize(n, n.getMembers());
					increaseIndentation();
					inferASTChanges(null, n.getMembers());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(EnumConstantDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof EnumConstantDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			EnumConstantDeclaration aux = (EnumConstantDeclaration) o;

			boolean equals = n.getName().equals(aux.getName());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
			inferASTChanges(n.getArgs(), aux.getArgs());
			inferASTChanges(n.getClassBody(), aux.getClassBody());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(EnumConstantDeclaration.class);
				} else {
					increaseUpdatedNodes(EnumConstantDeclaration.class);
				}
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				applyUpdate(n, aux, n.getArgs(), aux.getArgs());
				increaseUpdatedNodes(EnumConstantDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(EnumConstantDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getJavaDoc(), null);
					inferASTChanges(n.getArgs(), null);
					inferASTChanges(n.getClassBody(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(EnumConstantDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getJavaDoc());
					inferASTChanges(null, n.getArgs());
					inferASTChanges(null, n.getClassBody());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(AnnotationDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof AnnotationDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			AnnotationDeclaration aux = (AnnotationDeclaration) o;

			boolean equals = n.getName().equals(aux.getName())
					&& n.getModifiers() == aux.getModifiers();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}
			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
			increaseIndentation();
			inferIndentationSize(n, n.getMembers());
			inferASTChanges(n.getMembers(), aux.getMembers());
			decreaseIndentation();
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(AnnotationDeclaration.class);
				} else {
					increaseUpdatedNodes(AnnotationDeclaration.class);
					setIsUpdated(true);
				}
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				increaseUpdatedNodes(AnnotationDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(AnnotationDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getJavaDoc(), null);
					increaseIndentation();
					inferIndentationSize(n, n.getMembers());
					inferASTChanges(n.getMembers(), null);
					decreaseIndentation();
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(AnnotationDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getJavaDoc());
					increaseIndentation();
					inferIndentationSize(n, n.getMembers());
					inferASTChanges(null, n.getMembers());
					decreaseIndentation();
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(AnnotationMemberDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof AnnotationMemberDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			AnnotationMemberDeclaration aux = (AnnotationMemberDeclaration) o;

			boolean equals = n.getName().equals(aux.getName())
					&& n.getModifiers() == aux.getModifiers();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
			inferASTChanges(n.getDefaultValue(), aux.getDefaultValue());
			inferASTChanges(n.getType(), aux.getType());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(AnnotationMemberDeclaration.class);
				} else {
					increaseUpdatedNodes(AnnotationMemberDeclaration.class);
				}
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				applyUpdate(n, aux, n.getDefaultValue(), aux.getDefaultValue());
				increaseUpdatedNodes(AnnotationMemberDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(AnnotationMemberDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getJavaDoc(), null);
					inferASTChanges(n.getDefaultValue(), null);
					inferASTChanges(n.getType(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(AnnotationMemberDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getJavaDoc());
					inferASTChanges(null, n.getDefaultValue());
					inferASTChanges(null, n.getType());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(VariableDeclarator n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof VariableDeclarator) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			VariableDeclarator aux = (VariableDeclarator) o;
			inferASTChanges(n.getId(), aux.getId());
			inferASTChanges(n.getInit(), aux.getInit());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(VariableDeclarator.class);
			} else {
				increaseUpdatedNodes(VariableDeclarator.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(VariableDeclarator.class);
					inferASTChanges(n.getId(), null);
					inferASTChanges(n.getInit(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(VariableDeclarator.class);
					inferASTChanges(null, n.getId());
					inferASTChanges(null, n.getInit());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(VariableDeclaratorId n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof VariableDeclaratorId) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			VariableDeclaratorId aux = (VariableDeclaratorId) o;

			boolean equals = n.getName().equals(aux.getName());

			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			if (equals) {
				increaseUnmodifiedNodes(VariableDeclaratorId.class);
			} else {
				increaseUpdatedNodes(VariableDeclaratorId.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(VariableDeclaratorId.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(VariableDeclaratorId.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ConstructorDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ConstructorDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			ConstructorDeclaration aux = (ConstructorDeclaration) o;

			boolean equals = n.getName().equals(aux.getName())
					&& n.getModifiers() == aux.getModifiers();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getBlock(), aux.getBlock());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
			inferASTChanges(n.getParameters(), aux.getParameters());
			inferASTChanges(n.getThrows(), aux.getThrows());
			inferASTChanges(n.getTypeParameters(), aux.getTypeParameters());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(ConstructorDeclaration.class);
				} else {
					increaseUpdatedNodes(ConstructorDeclaration.class);
				}
			} else {
				applyUpdate(n, aux, n.getParameters(), aux.getParameters());
				applyUpdate(n, aux, n.getThrows(), aux.getThrows());
				applyUpdate(n, aux, n.getTypeParameters(),
						aux.getTypeParameters());
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				increaseUpdatedNodes(ConstructorDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ConstructorDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getBlock(), null);
					inferASTChanges(n.getJavaDoc(), null);
					inferASTChanges(n.getParameters(), null);
					inferASTChanges(n.getThrows(), null);
					inferASTChanges(n.getTypeParameters(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ConstructorDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getBlock());
					inferASTChanges(null, n.getJavaDoc());
					inferASTChanges(null, n.getParameters());
					inferASTChanges(null, n.getThrows());
					inferASTChanges(null, n.getTypeParameters());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(MultiTypeParameter n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof MultiTypeParameter) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			MultiTypeParameter aux = (MultiTypeParameter) o;
			boolean equals = n.getModifiers() == aux.getModifiers();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getId(), aux.getId());
			inferASTChanges(n.getTypes(), aux.getTypes());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(Parameter.class);
				} else {
					increaseUpdatedNodes(Parameter.class);
				}
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getTypes(), aux.getTypes());
				increaseUpdatedNodes(Parameter.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(Parameter.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getId(), null);
					inferASTChanges(n.getTypes(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(Parameter.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getId());
					inferASTChanges(null, n.getTypes());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(Parameter n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof Parameter) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			Parameter aux = (Parameter) o;

			boolean equals = n.getModifiers() == aux.getModifiers();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getId(), aux.getId());
			inferASTChanges(n.getType(), aux.getType());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(Parameter.class);
				} else {
					increaseUpdatedNodes(Parameter.class);
				}
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				increaseUpdatedNodes(Parameter.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(Parameter.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getId(), null);
					inferASTChanges(n.getType(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(Parameter.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getId());
					inferASTChanges(null, n.getType());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(EmptyMemberDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof EmptyMemberDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			EmptyMemberDeclaration aux = (EmptyMemberDeclaration) o;
			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(EmptyMemberDeclaration.class);
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				increaseUpdatedNodes(EmptyMemberDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(EmptyMemberDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getJavaDoc(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(EmptyMemberDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getJavaDoc());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(InitializerDeclaration n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof InitializerDeclaration) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			InitializerDeclaration aux = (InitializerDeclaration) o;
			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			inferASTChanges(n.getBlock(), aux.getBlock());
			inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(InitializerDeclaration.class);
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getJavaDoc(), aux.getJavaDoc());
				increaseUpdatedNodes(InitializerDeclaration.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(InitializerDeclaration.class);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getBlock(), null);
					inferASTChanges(n.getJavaDoc(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(InitializerDeclaration.class);
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getBlock());
					inferASTChanges(null, n.getJavaDoc());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(JavadocComment n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof JavadocComment) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			JavadocComment aux = (JavadocComment) o;

			boolean equals = n.getContent().equals(aux.getContent());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			if (equals) {
				increaseUnmodifiedNodes(JavadocComment.class);
			} else {
				increaseUpdatedNodes(JavadocComment.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(JavadocComment.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(JavadocComment.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ClassOrInterfaceType n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ClassOrInterfaceType) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			ClassOrInterfaceType aux = (ClassOrInterfaceType) o;

			boolean equals = n.getName().equals(aux.getName());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getScope(), aux.getScope());
			inferASTChanges(n.getTypeArgs(), aux.getTypeArgs());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(ClassOrInterfaceType.class);
				} else {
					increaseUpdatedNodes(ClassOrInterfaceType.class);
				}
			} else {
				applyUpdate(n, aux, n.getTypeArgs(), aux.getTypeArgs());
				increaseUpdatedNodes(ClassOrInterfaceType.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ClassOrInterfaceType.class);
					inferASTChanges(n.getScope(), null);
					inferASTChanges(n.getTypeArgs(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ClassOrInterfaceType.class);
					inferASTChanges(null, n.getScope());
					inferASTChanges(null, n.getTypeArgs());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(PrimitiveType n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof PrimitiveType) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			PrimitiveType aux = (PrimitiveType) o;

			boolean equals = n.getType().equals(aux.getType());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			if (equals) {
				increaseUnmodifiedNodes(PrimitiveType.class);
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				increaseUpdatedNodes(PrimitiveType.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(PrimitiveType.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(PrimitiveType.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ReferenceType n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ReferenceType) {
			boolean backup = isUpdated();
			setIsUpdated(false);
			ReferenceType aux = (ReferenceType) o;

			boolean equals = n.getType().equals(aux.getType())
					&& n.getArrayCount() == aux.getArrayCount();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChangesList(n.getArraysAnnotations(),
					aux.getArraysAnnotations());
			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			if (equals) {
				increaseUnmodifiedNodes(PrimitiveType.class);
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getArraysAnnotations(),
						aux.getArraysAnnotations());
				increaseUpdatedNodes(PrimitiveType.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ReferenceType.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ReferenceType.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(VoidType n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null) {
			increaseUnmodifiedNodes(VoidType.class);
			setIsUpdated(false);
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(VoidType.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(VoidType.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(WildcardType n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof WildcardType) {
			WildcardType aux = (WildcardType) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getExtends(), aux.getExtends());
			inferASTChanges(n.getSuper(), aux.getSuper());
			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(WildcardType.class);
			} else {
				applyUpdate(n, aux, n.getSuper(), aux.getSuper());
				applyUpdate(n, aux, n.getExtends(), aux.getExtends());
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());

				increaseUpdatedNodes(WildcardType.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(WildcardType.class);
					inferASTChanges(n.getExtends(), null);
					inferASTChanges(n.getSuper(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(WildcardType.class);
					inferASTChanges(null, n.getExtends());
					inferASTChanges(null, n.getSuper());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ArrayAccessExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ArrayAccessExpr) {
			ArrayAccessExpr aux = (ArrayAccessExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getIndex(), aux.getIndex());
			inferASTChanges(n.getName(), aux.getName());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ArrayAccessExpr.class);
			} else {
				increaseUpdatedNodes(ArrayAccessExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ArrayAccessExpr.class);
					inferASTChanges(n.getIndex(), null);
					inferASTChanges(n.getName(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ArrayAccessExpr.class);
					inferASTChanges(null, n.getIndex());
					inferASTChanges(null, n.getName());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ArrayCreationExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ArrayCreationExpr) {
			ArrayCreationExpr aux = (ArrayCreationExpr) o;

			boolean equals = n.getArrayCount() == aux.getArrayCount();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChangesList(n.getArraysAnnotations(),
					aux.getArraysAnnotations());
			inferASTChanges(n.getDimensions(), aux.getDimensions());
			inferASTChanges(n.getType(), aux.getType());
			inferASTChanges(n.getInitializer(), aux.getInitializer());

			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(ArrayCreationExpr.class);
				} else {
					applyUpdate(n, aux, n.getInitializer(),
							aux.getInitializer());
					applyUpdate(n, aux, n.getArraysAnnotations(),
							aux.getArraysAnnotations());
					increaseUpdatedNodes(ArrayCreationExpr.class);
				}
			} else {
				increaseUpdatedNodes(ArrayCreationExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ArrayCreationExpr.class);
					inferASTChanges(n.getDimensions(), null);
					inferASTChanges(n.getType(), null);
					inferASTChanges(n.getInitializer(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ArrayCreationExpr.class);
					inferASTChanges(null, n.getDimensions());
					inferASTChanges(null, n.getType());
					inferASTChanges(null, n.getInitializer());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ArrayInitializerExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ArrayInitializerExpr) {
			ArrayInitializerExpr aux = (ArrayInitializerExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);

			inferASTChanges(n.getValues(), aux.getValues());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ArrayInitializerExpr.class);
			} else {
				applyUpdate(n, aux, n.getValues(), aux.getValues());
				increaseUpdatedNodes(ArrayInitializerExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ArrayInitializerExpr.class);
					inferASTChanges(n.getValues(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ArrayInitializerExpr.class);
					inferASTChanges(null, n.getValues());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(AssignExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof AssignExpr) {
			AssignExpr aux = (AssignExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);

			boolean equals = n.getOperator().name()
					.equals(aux.getOperator().name());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getTarget(), aux.getTarget());
			inferASTChanges(n.getValue(), aux.getValue());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(AssignExpr.class);
				} else {

					increaseUpdatedNodes(AssignExpr.class);
				}
			} else {
				increaseUpdatedNodes(AssignExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(AssignExpr.class);
					inferASTChanges(n.getTarget(), null);
					inferASTChanges(n.getValue(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(AssignExpr.class);
					inferASTChanges(null, n.getTarget());
					inferASTChanges(null, n.getValue());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(BinaryExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof BinaryExpr) {
			BinaryExpr aux = (BinaryExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			boolean equals = n.getOperator().name()
					.equals(aux.getOperator().name());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getRight(), aux.getRight());
			inferASTChanges(n.getLeft(), aux.getLeft());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(BinaryExpr.class);
				} else {
					increaseUpdatedNodes(BinaryExpr.class);
				}
			} else {
				increaseUpdatedNodes(BinaryExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(BinaryExpr.class);
					inferASTChanges(n.getRight(), null);
					inferASTChanges(n.getLeft(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(BinaryExpr.class);
					inferASTChanges(null, n.getRight());
					inferASTChanges(null, n.getLeft());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(CastExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof CastExpr) {
			CastExpr aux = (CastExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getExpr(), aux.getExpr());
			inferASTChanges(n.getType(), aux.getType());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(CastExpr.class);
			} else {
				increaseUpdatedNodes(CastExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(CastExpr.class);
					inferASTChanges(n.getExpr(), null);
					inferASTChanges(n.getType(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(CastExpr.class);
					inferASTChanges(null, n.getExpr());
					inferASTChanges(null, n.getType());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ClassExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ClassExpr) {
			ClassExpr aux = (ClassExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getType(), aux.getType());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ClassExpr.class);
			} else {
				increaseUpdatedNodes(ClassExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ClassExpr.class);
					inferASTChanges(n.getType(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ClassExpr.class);
					inferASTChanges(null, n.getType());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ConditionalExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ConditionalExpr) {
			ConditionalExpr aux = (ConditionalExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getCondition(), aux.getCondition());
			inferASTChanges(n.getThenExpr(), aux.getThenExpr());
			inferASTChanges(n.getElseExpr(), aux.getElseExpr());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ConditionalExpr.class);
			} else {
				applyUpdate(n, aux, n.getElseExpr(), aux.getElseExpr());
				increaseUpdatedNodes(ConditionalExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ConditionalExpr.class);
					inferASTChanges(n.getCondition(), null);
					inferASTChanges(n.getThenExpr(), null);
					inferASTChanges(n.getElseExpr(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ConditionalExpr.class);
					inferASTChanges(null, n.getCondition());
					inferASTChanges(null, n.getThenExpr());
					inferASTChanges(null, n.getElseExpr());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(EnclosedExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof EnclosedExpr) {
			EnclosedExpr aux = (EnclosedExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getInner(), aux.getInner());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(EnclosedExpr.class);
			} else {
				increaseUpdatedNodes(EnclosedExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(EnclosedExpr.class);
					inferASTChanges(n.getInner(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(EnclosedExpr.class);
					inferASTChanges(null, n.getInner());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(FieldAccessExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof FieldAccessExpr) {
			FieldAccessExpr aux = (FieldAccessExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			boolean equals = n.getField().equals(aux.getField());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}
			inferASTChanges(n.getScope(), aux.getScope());
			inferASTChanges(n.getTypeArgs(), aux.getTypeArgs());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(FieldAccessExpr.class);
				} else {
					applyUpdate(n, aux, n.getScope(), aux.getScope());
					applyUpdate(n, aux, n.getTypeArgs(), aux.getTypeArgs());
					increaseUpdatedNodes(FieldAccessExpr.class);
				}
			} else {
				increaseUpdatedNodes(FieldAccessExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(FieldAccessExpr.class);
					inferASTChanges(n.getScope(), null);
					inferASTChanges(n.getTypeArgs(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(FieldAccessExpr.class);
					inferASTChanges(null, n.getScope());
					inferASTChanges(null, n.getTypeArgs());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(InstanceOfExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof InstanceOfExpr) {
			InstanceOfExpr aux = (InstanceOfExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getExpr(), aux.getExpr());
			inferASTChanges(n.getType(), aux.getType());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(InstanceOfExpr.class);
			} else {
				increaseUpdatedNodes(InstanceOfExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(InstanceOfExpr.class);
					inferASTChanges(n.getExpr(), null);
					inferASTChanges(n.getType(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(InstanceOfExpr.class);
					inferASTChanges(null, n.getExpr());
					inferASTChanges(null, n.getType());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(StringLiteralExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof StringLiteralExpr) {
			StringLiteralExpr aux = (StringLiteralExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			if (n.getValue().equals(aux.getValue())) {
				increaseUnmodifiedNodes(StringLiteralExpr.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(StringLiteralExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(StringLiteralExpr.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(StringLiteralExpr.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(IntegerLiteralExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof IntegerLiteralExpr) {
			IntegerLiteralExpr aux = (IntegerLiteralExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			if (n.getValue().equals(aux.getValue())) {
				increaseUnmodifiedNodes(IntegerLiteralExpr.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(IntegerLiteralExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(IntegerLiteralExpr.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(IntegerLiteralExpr.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(LongLiteralExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof LongLiteralExpr) {
			LongLiteralExpr aux = (LongLiteralExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			if (n.getValue().equals(aux.getValue())) {
				increaseUnmodifiedNodes(LongLiteralExpr.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(LongLiteralExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(LongLiteralExpr.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(LongLiteralExpr.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(IntegerLiteralMinValueExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof IntegerLiteralMinValueExpr) {
			IntegerLiteralMinValueExpr aux = (IntegerLiteralMinValueExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			if (n.getValue().equals(aux.getValue())) {
				increaseUnmodifiedNodes(IntegerLiteralMinValueExpr.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(IntegerLiteralMinValueExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(IntegerLiteralMinValueExpr.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(IntegerLiteralMinValueExpr.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(LongLiteralMinValueExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof LongLiteralMinValueExpr) {
			LongLiteralMinValueExpr aux = (LongLiteralMinValueExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			if (n.getValue().equals(aux.getValue())) {
				increaseUnmodifiedNodes(LongLiteralMinValueExpr.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(LongLiteralMinValueExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(LongLiteralMinValueExpr.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(LongLiteralMinValueExpr.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(CharLiteralExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof CharLiteralExpr) {
			CharLiteralExpr aux = (CharLiteralExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			if (n.getValue().equals(aux.getValue())) {
				increaseUnmodifiedNodes(CharLiteralExpr.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(CharLiteralExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(CharLiteralExpr.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(CharLiteralExpr.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(DoubleLiteralExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof DoubleLiteralExpr) {
			DoubleLiteralExpr aux = (DoubleLiteralExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			if (n.getValue().equals(aux.getValue())) {
				increaseUnmodifiedNodes(DoubleLiteralExpr.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(DoubleLiteralExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(DoubleLiteralExpr.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(DoubleLiteralExpr.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(BooleanLiteralExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof BooleanLiteralExpr) {
			BooleanLiteralExpr aux = (BooleanLiteralExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			if (n.getValue() == aux.getValue()) {
				increaseUnmodifiedNodes(BooleanLiteralExpr.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(BooleanLiteralExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(BooleanLiteralExpr.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(BooleanLiteralExpr.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(NullLiteralExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null) {
			increaseUnmodifiedNodes(NullLiteralExpr.class);
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(NullLiteralExpr.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(NullLiteralExpr.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(MethodCallExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof MethodCallExpr) {
			MethodCallExpr aux = (MethodCallExpr) o;

			boolean equals = n.getName().equals(aux.getName());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getScope(), aux.getScope());
			inferASTChanges(n.getTypeArgs(), aux.getTypeArgs());

			List<Expression> theseArgs = n.getArgs();
			List<Expression> otherArgs = aux.getArgs();
			inferASTChanges(theseArgs, otherArgs);

			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(MethodCallExpr.class);
				} else {
					increaseUpdatedNodes(MethodCallExpr.class);
				}
			} else {
				applyUpdate(n, aux, theseArgs, otherArgs);
				applyUpdate(n, aux, n.getTypeArgs(), aux.getTypeArgs());
				increaseUpdatedNodes(MethodCallExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(MethodCallExpr.class);
					inferASTChanges(n.getScope(), null);
					inferASTChanges(n.getTypeArgs(), null);

					inferASTChanges(n.getArgs(), null);

				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(MethodCallExpr.class);
					inferASTChanges(null, n.getScope());
					inferASTChanges(null, n.getTypeArgs());

					inferASTChanges(n.getArgs(), null);

				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(NameExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof NameExpr) {
			NameExpr aux = (NameExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			if (n.getName().equals(aux.getName())) {
				increaseUnmodifiedNodes(NameExpr.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(NameExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(NameExpr.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(NameExpr.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ObjectCreationExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ObjectCreationExpr) {
			ObjectCreationExpr aux = (ObjectCreationExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getScope(), aux.getScope());
			inferASTChanges(n.getType(), aux.getType());
			inferASTChanges(n.getTypeArgs(), aux.getTypeArgs());
			inferASTChanges(n.getArgs(), aux.getArgs());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ObjectCreationExpr.class);
			} else {
				applyUpdate(n, aux, n.getArgs(), aux.getArgs());
				applyUpdate(n, aux, n.getTypeArgs(), aux.getTypeArgs());
				applyUpdate(n, aux, n.getScope(), aux.getScope());
				increaseUpdatedNodes(ObjectCreationExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ObjectCreationExpr.class);
					inferASTChanges(n.getScope(), null);
					inferASTChanges(n.getTypeArgs(), null);
					inferASTChanges(n.getType(), null);
					inferASTChanges(n.getArgs(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ObjectCreationExpr.class);
					inferASTChanges(null, n.getScope());
					inferASTChanges(null, n.getTypeArgs());
					inferASTChanges(null, n.getType());
					inferASTChanges(n.getArgs(), null);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(QualifiedNameExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof QualifiedNameExpr) {
			QualifiedNameExpr aux = (QualifiedNameExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);

			boolean equals = n.getName().equals(aux.getName());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getQualifier(), aux.getQualifier());
			if (!isUpdated()) {
				if (n.getName().equals(aux.getName())) {
					increaseUnmodifiedNodes(QualifiedNameExpr.class);
				} else {
					increaseUpdatedNodes(QualifiedNameExpr.class);
				}
			} else {
				increaseUpdatedNodes(QualifiedNameExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(QualifiedNameExpr.class);
					inferASTChanges(n.getQualifier(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(QualifiedNameExpr.class);
					inferASTChanges(null, n.getQualifier());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ThisExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ThisExpr) {
			ThisExpr aux = (ThisExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getClassExpr(), aux.getClassExpr());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ThisExpr.class);
			} else {
				applyUpdate(n, aux, n.getClassExpr(), aux.getClassExpr());
				increaseUpdatedNodes(ThisExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ThisExpr.class);
					inferASTChanges(n.getClassExpr(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ThisExpr.class);
					inferASTChanges(null, n.getClassExpr());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(SuperExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof SuperExpr) {
			SuperExpr aux = (SuperExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getClassExpr(), aux.getClassExpr());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(SuperExpr.class);
			} else {
				applyUpdate(n, aux, n.getClassExpr(), aux.getClassExpr());
				increaseUpdatedNodes(SuperExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(SuperExpr.class);
					inferASTChanges(n.getClassExpr(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(SuperExpr.class);
					inferASTChanges(null, n.getClassExpr());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(UnaryExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof UnaryExpr) {
			UnaryExpr aux = (UnaryExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);

			boolean equals = n.getOperator().name()
					.equals(aux.getOperator().name());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getExpr(), aux.getExpr());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(UnaryExpr.class);
				} else {
					increaseUpdatedNodes(UnaryExpr.class);
				}
			} else {
				increaseUpdatedNodes(UnaryExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(UnaryExpr.class);
					inferASTChanges(n.getExpr(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(UnaryExpr.class);
					inferASTChanges(null, n.getExpr());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(VariableDeclarationExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof VariableDeclarationExpr) {
			VariableDeclarationExpr aux = (VariableDeclarationExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);

			boolean equals = n.getModifiers() == aux.getModifiers();
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getVars(), n.getVars());
			inferASTChanges(n.getType(), aux.getType());
			inferASTChanges(n.getAnnotations(), aux.getAnnotations());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(VariableDeclarationExpr.class);
				} else {
					increaseUpdatedNodes(VariableDeclarationExpr.class);
				}
			} else {
				applyUpdate(n, aux, n.getAnnotations(), aux.getAnnotations());
				applyUpdate(n, aux, n.getVars(), n.getVars());
				increaseUpdatedNodes(VariableDeclarationExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(VariableDeclarationExpr.class);
					inferASTChanges(n.getVars(), null);
					inferASTChanges(n.getAnnotations(), null);
					inferASTChanges(n.getType(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(VariableDeclarationExpr.class);
					inferASTChanges(null, n.getVars());
					inferASTChanges(null, n.getAnnotations());
					inferASTChanges(null, n.getType());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(MarkerAnnotationExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof MarkerAnnotationExpr) {
			MarkerAnnotationExpr aux = (MarkerAnnotationExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getName(), aux.getName());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(MarkerAnnotationExpr.class);
			} else {
				increaseUpdatedNodes(MarkerAnnotationExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(MarkerAnnotationExpr.class);
					inferASTChanges(n.getName(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(MarkerAnnotationExpr.class);
					inferASTChanges(null, n.getName());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(SingleMemberAnnotationExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof SingleMemberAnnotationExpr) {
			SingleMemberAnnotationExpr aux = (SingleMemberAnnotationExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getMemberValue(), aux.getMemberValue());
			inferASTChanges(n.getName(), aux.getName());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(SingleMemberAnnotationExpr.class);
			} else {
				applyUpdate(n, aux, n.getMemberValue(), n.getMemberValue());
				increaseUpdatedNodes(SingleMemberAnnotationExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(SingleMemberAnnotationExpr.class);
					inferASTChanges(n.getMemberValue(), null);
					inferASTChanges(n.getName(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(SingleMemberAnnotationExpr.class);
					inferASTChanges(null, n.getMemberValue());
					inferASTChanges(null, n.getName());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(NormalAnnotationExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof NormalAnnotationExpr) {
			NormalAnnotationExpr aux = (NormalAnnotationExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getPairs(), aux.getPairs());
			inferASTChanges(n.getName(), aux.getName());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(NormalAnnotationExpr.class);
			} else {
				applyUpdate(n, aux, n.getPairs(), aux.getPairs());
				increaseUpdatedNodes(NormalAnnotationExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(NormalAnnotationExpr.class);
					inferASTChanges(n.getPairs(), null);
					inferASTChanges(n.getName(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(NormalAnnotationExpr.class);
					inferASTChanges(null, n.getPairs());
					inferASTChanges(null, n.getName());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(MemberValuePair n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof MemberValuePair) {
			MemberValuePair aux = (MemberValuePair) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getValue(), aux.getValue());

			boolean equals = n.getName().equals(aux.getName());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(MemberValuePair.class);
				} else {
					increaseUpdatedNodes(MemberValuePair.class);
				}
			} else {
				increaseUpdatedNodes(MemberValuePair.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(MemberValuePair.class);
					inferASTChanges(n.getValue(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(MemberValuePair.class);
					inferASTChanges(null, n.getValue());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ExplicitConstructorInvocationStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ExplicitConstructorInvocationStmt) {
			ExplicitConstructorInvocationStmt aux = (ExplicitConstructorInvocationStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getExpr(), aux.getExpr());
			inferASTChanges(n.getArgs(), aux.getArgs());
			inferASTChanges(n.getTypeArgs(), aux.getTypeArgs());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ExplicitConstructorInvocationStmt.class);
			} else {
				applyUpdate(n, aux, n.getArgs(), aux.getArgs());
				applyUpdate(n, aux, n.getTypeArgs(), aux.getTypeArgs());
				increaseUpdatedNodes(ExplicitConstructorInvocationStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ExplicitConstructorInvocationStmt.class);
					inferASTChanges(n.getExpr(), null);
					inferASTChanges(n.getArgs(), null);
					inferASTChanges(n.getTypeArgs(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ExplicitConstructorInvocationStmt.class);
					inferASTChanges(null, n.getExpr());
					inferASTChanges(null, n.getArgs());
					inferASTChanges(null, n.getTypeArgs());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(TypeDeclarationStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof TypeDeclarationStmt) {
			TypeDeclarationStmt aux = (TypeDeclarationStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getTypeDeclaration(), aux.getTypeDeclaration());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(TypeDeclarationStmt.class);
			} else {
				increaseUpdatedNodes(TypeDeclarationStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(TypeDeclarationStmt.class);
					inferASTChanges(n.getTypeDeclaration(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(TypeDeclarationStmt.class);
					inferASTChanges(null, n.getTypeDeclaration());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(AssertStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof AssertStmt) {
			AssertStmt aux = (AssertStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getCheck(), aux.getCheck());
			inferASTChanges(n.getMessage(), aux.getMessage());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(AssertStmt.class);
			} else {
				applyUpdate(n, aux, n.getMessage(), aux.getMessage());
				increaseUpdatedNodes(AssertStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(AssertStmt.class);
					inferASTChanges(n.getCheck(), null);
					inferASTChanges(n.getMessage(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(AssertStmt.class);
					inferASTChanges(null, n.getCheck());
					inferASTChanges(null, n.getMessage());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(BlockStmt n, VisitorContext ctx) {
		decreaseIndentation();
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof BlockStmt) {
			BlockStmt aux = (BlockStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			increaseIndentation();
			inferIndentationSize(n, n.getStmts());
			inferASTChanges(n.getStmts(), aux.getStmts());
			decreaseIndentation();
			if (!isUpdated()) {
				increaseUnmodifiedNodes(BlockStmt.class);
			} else {
				increaseUpdatedNodes(BlockStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(BlockStmt.class);
					increaseIndentation();
					inferIndentationSize(n, n.getStmts());
					inferASTChanges(n.getStmts(), null);
					decreaseIndentation();
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(BlockStmt.class);
					increaseIndentation();
					inferIndentationSize(n, n.getStmts());
					inferASTChanges(null, n.getStmts());
					decreaseIndentation();
				}
			}
			setIsUpdated(true);
		}
		decreaseIndentation();
	}

	public void visit(LabeledStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof LabeledStmt) {
			LabeledStmt aux = (LabeledStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);

			boolean equals = n.getLabel().equals(aux.getLabel());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getStmt(), aux.getStmt());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(LabeledStmt.class);
				} else {
					increaseUpdatedNodes(LabeledStmt.class);
				}
			} else {
				increaseUpdatedNodes(LabeledStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(LabeledStmt.class);
					inferASTChanges(n.getStmt(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(LabeledStmt.class);
					inferASTChanges(null, n.getStmt());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(EmptyStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof EmptyStmt) {
			increaseUnmodifiedNodes(EmptyStmt.class);
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(EmptyStmt.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(EmptyStmt.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ExpressionStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ExpressionStmt) {
			ExpressionStmt aux = (ExpressionStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getExpression(), aux.getExpression());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ExpressionStmt.class);
			} else {
				increaseUpdatedNodes(ExpressionStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ExpressionStmt.class);
					inferASTChanges(n.getExpression(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ExpressionStmt.class);
					inferASTChanges(null, n.getExpression());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(SwitchStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof SwitchStmt) {
			SwitchStmt aux = (SwitchStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getSelector(), aux.getSelector());
			increaseIndentation();
			inferASTChanges(n.getEntries(), aux.getEntries());
			decreaseIndentation();
			if (!isUpdated()) {
				increaseUnmodifiedNodes(SwitchStmt.class);
			} else {
				increaseUpdatedNodes(SwitchStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(SwitchStmt.class);
					inferASTChanges(n.getSelector(), null);
					increaseIndentation();
					inferASTChanges(n.getEntries(), null);
					decreaseIndentation();
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(SwitchStmt.class);
					inferASTChanges(null, n.getSelector());
					increaseIndentation();
					inferASTChanges(null, n.getEntries());
					decreaseIndentation();
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(SwitchEntryStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof SwitchEntryStmt) {
			SwitchEntryStmt aux = (SwitchEntryStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getLabel(), aux.getLabel());
			inferASTChanges(n.getStmts(), aux.getStmts());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(SwitchEntryStmt.class);
			} else {
				increaseUpdatedNodes(SwitchEntryStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(SwitchEntryStmt.class);
					inferASTChanges(n.getLabel(), null);
					inferASTChanges(n.getStmts(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(SwitchEntryStmt.class);
					inferASTChanges(null, n.getLabel());
					inferASTChanges(null, n.getStmts());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(BreakStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof BreakStmt) {
			BreakStmt aux = (BreakStmt) o;
			if (n.getId() == null || n.getId().equals(aux.getId())) {
				increaseUnmodifiedNodes(BreakStmt.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(BreakStmt.class);
			}
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(BreakStmt.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(BreakStmt.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ReturnStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ReturnStmt) {
			ReturnStmt aux = (ReturnStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getExpr(), aux.getExpr());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ReturnStmt.class);
			} else {
				applyUpdate(n, aux, n.getExpr(), aux.getExpr());
				increaseUpdatedNodes(ReturnStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ReturnStmt.class);
					inferASTChanges(n.getExpr(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ReturnStmt.class);
					inferASTChanges(null, n.getExpr());
					;
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(IfStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && n instanceof IfStmt) {
			IfStmt aux = (IfStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getCondition(), aux.getCondition());
			inferASTChanges(n.getThenStmt(), aux.getThenStmt());
			inferASTChanges(n.getElseStmt(), aux.getElseStmt());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(IfStmt.class);
			} else {
				applyUpdate(n, aux, n.getElseStmt(), aux.getElseStmt());
				increaseUpdatedNodes(IfStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(IfStmt.class);
					inferASTChanges(n.getCondition(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(IfStmt.class);
					inferASTChanges(null, n.getCondition());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(WhileStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof WhileStmt) {
			WhileStmt aux = (WhileStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getCondition(), aux.getCondition());
			inferASTChanges(n.getBody(), aux.getBody());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(WhileStmt.class);
			} else {
				increaseUpdatedNodes(WhileStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(WhileStmt.class);
					inferASTChanges(n.getBody(), null);
					inferASTChanges(n.getCondition(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(WhileStmt.class);
					inferASTChanges(null, n.getCondition());
					inferASTChanges(null, n.getBody());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ContinueStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ContinueStmt) {
			ContinueStmt aux = (ContinueStmt) o;
			if (n.getId() == null || n.getId().equals(aux.getId())) {
				increaseUnmodifiedNodes(ContinueStmt.class);
			} else {
				applyUpdate(n, (Node) o);
				increaseUpdatedNodes(ContinueStmt.class);
			}
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ContinueStmt.class);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ContinueStmt.class);
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(DoStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof DoStmt) {
			DoStmt aux = (DoStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getCondition(), aux.getCondition());
			inferASTChanges(n.getBody(), aux.getBody());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(DoStmt.class);
			} else {
				increaseUpdatedNodes(DoStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(DoStmt.class);
					inferASTChanges(n.getBody(), null);
					inferASTChanges(n.getCondition(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(DoStmt.class);
					inferASTChanges(null, n.getCondition());
					inferASTChanges(null, n.getBody());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ForeachStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ForeachStmt) {
			ForeachStmt aux = (ForeachStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getIterable(), aux.getIterable());
			inferASTChanges(n.getVariable(), aux.getVariable());
			inferASTChanges(n.getBody(), aux.getBody());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ForeachStmt.class);
			} else {
				increaseUpdatedNodes(ForeachStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ForeachStmt.class);
					inferASTChanges(n.getBody(), null);
					inferASTChanges(n.getIterable(), null);
					inferASTChanges(n.getVariable(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ForeachStmt.class);
					inferASTChanges(null, n.getVariable());
					inferASTChanges(null, n.getIterable());
					inferASTChanges(null, n.getBody());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ForStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ForStmt) {
			ForStmt aux = (ForStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getInit(), aux.getInit());
			inferASTChanges(n.getUpdate(), aux.getUpdate());
			inferASTChanges(n.getCompare(), aux.getCompare());
			inferASTChanges(n.getBody(), aux.getBody());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ForeachStmt.class);
			} else {

				increaseUpdatedNodes(ForeachStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ForeachStmt.class);
					inferASTChanges(n.getBody(), null);
					inferASTChanges(n.getInit(), null);
					inferASTChanges(n.getUpdate(), null);
					inferASTChanges(n.getCompare(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ForeachStmt.class);
					inferASTChanges(null, n.getInit());
					inferASTChanges(null, n.getUpdate());
					inferASTChanges(null, n.getBody());
					inferASTChanges(null, n.getCompare());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(ThrowStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof ThrowStmt) {
			ThrowStmt aux = (ThrowStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getExpr(), aux.getExpr());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(ThrowStmt.class);
			} else {
				increaseUpdatedNodes(ThrowStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(ThrowStmt.class);
					inferASTChanges(n.getExpr(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(ThrowStmt.class);
					inferASTChanges(null, n.getExpr());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(SynchronizedStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof SynchronizedStmt) {
			SynchronizedStmt aux = (SynchronizedStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getExpr(), aux.getExpr());
			inferASTChanges(n.getBlock(), aux.getBlock());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(SynchronizedStmt.class);
			} else {
				increaseUpdatedNodes(SynchronizedStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(SynchronizedStmt.class);
					inferASTChanges(n.getExpr(), null);
					inferASTChanges(n.getBlock(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(SynchronizedStmt.class);
					inferASTChanges(null, n.getExpr());
					inferASTChanges(null, n.getBlock());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(TryStmt n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof TryStmt) {
			TryStmt aux = (TryStmt) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getTryBlock(), aux.getTryBlock());
			inferASTChanges(n.getCatchs(), aux.getCatchs());
			inferASTChanges(n.getFinallyBlock(), aux.getFinallyBlock());
			inferASTChanges(n.getResources(), aux.getResources());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(TryStmt.class);
			} else {
				applyUpdate(n, aux, n.getCatchs(), aux.getCatchs());
				applyUpdate(n, aux, n.getResources(), aux.getResources());
				applyUpdate(n, aux, n.getFinallyBlock(), aux.getFinallyBlock());

				increaseUpdatedNodes(TryStmt.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(TryStmt.class);
					inferASTChanges(n.getTryBlock(), null);
					inferASTChanges(n.getCatchs(), null);
					inferASTChanges(n.getFinallyBlock(), null);
					inferASTChanges(n.getResources(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(TryStmt.class);
					inferASTChanges(null, n.getTryBlock());
					inferASTChanges(null, n.getCatchs());
					inferASTChanges(null, n.getFinallyBlock());
					inferASTChanges(null, n.getResources());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(CatchClause n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof CatchClause) {
			CatchClause aux = (CatchClause) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getCatchBlock(), aux.getCatchBlock());
			inferASTChanges(n.getExcept(), aux.getExcept());
			if (!isUpdated()) {
				increaseUnmodifiedNodes(CatchClause.class);
			} else {
				applyUpdate(n, aux, n.getExcept(), aux.getExcept());
				increaseUpdatedNodes(CatchClause.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(CatchClause.class);
					inferASTChanges(n.getCatchBlock(), null);
					inferASTChanges(n.getExcept(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(CatchClause.class);
					inferASTChanges(null, n.getCatchBlock());
					inferASTChanges(null, n.getExcept());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(LambdaExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof LambdaExpr) {
			LambdaExpr aux = (LambdaExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);
			inferASTChanges(n.getParameters(), aux.getParameters());
			inferASTChanges(n.getBody(), aux.getBody());

			if (!isUpdated()) {
				if (n.isParametersEnclosed() == aux.isParametersEnclosed()) {
					increaseUnmodifiedNodes(LambdaExpr.class);
				} else {
					applyUpdate(n, (Node) o);
					increaseUpdatedNodes(LambdaExpr.class);
				}
			} else {
				applyUpdate(n, aux, n.getParameters(), aux.getParameters());
				increaseUpdatedNodes(LambdaExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(LambdaExpr.class);
					inferASTChanges(n.getParameters(), null);
					inferASTChanges(n.getBody(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(LambdaExpr.class);
					inferASTChanges(null, n.getParameters());
					inferASTChanges(null, n.getBody());
				}
			}
			setIsUpdated(true);
		}
	}

	public void visit(MethodReferenceExpr n, VisitorContext ctx) {
		Object o = ctx.get(NODE_TO_COMPARE_KEY);
		if (o != null && o instanceof MethodReferenceExpr) {
			MethodReferenceExpr aux = (MethodReferenceExpr) o;
			boolean backup = isUpdated();
			setIsUpdated(false);

			boolean equals = n.getIdentifier().equals(aux.getIdentifier());
			if (!equals) {
				applyUpdate(n, (Node) o);
			}

			inferASTChanges(n.getScope(), aux.getScope());
			inferASTChanges(n.getTypeParameters(), aux.getTypeParameters());
			if (!isUpdated()) {
				if (equals) {
					increaseUnmodifiedNodes(MethodReferenceExpr.class);
				} else {
					increaseUpdatedNodes(MethodReferenceExpr.class);
				}
			} else {
				applyUpdate(n, aux, n.getTypeParameters(),
						aux.getTypeParameters());
				increaseUpdatedNodes(MethodReferenceExpr.class);
			}
			setIsUpdated(backup || isUpdated());
		} else {
			o = ctx.get(MAP_TO_UPDATE_KEY);
			if (o != null) {
				String action = (String) o;
				if (action.equals(ADD_ACTION_KEY)) {
					increaseAddedNodes(MethodReferenceExpr.class);
					inferASTChanges(n.getScope(), null);
					inferASTChanges(n.getTypeParameters(), null);
				} else if (action.equals(DELETE_ACTION_KEY)) {
					increaseDeletedNodes(MethodReferenceExpr.class);
					inferASTChanges(null, n.getScope());
					inferASTChanges(null, n.getTypeParameters());
				}
			}
			setIsUpdated(true);
		}
	}

	public Map<String, Integer> getAddedNodes() {
		if (properties == null) {
			return addedNodes;
		} else {
			Map<String, Integer> result = new HashMap<String, Integer>();
			Collection<Object> keys = properties.keySet();
			if (keys != null) {
				for (Object key : keys) {
					if ("true".equalsIgnoreCase(properties.getProperty(
							key.toString()).toString())) {
						if (addedNodes.containsKey(key.toString())) {
							result.put(key.toString(), addedNodes.get(key));
						} else {
							result.put(key.toString(), 0);
						}
					}
				}
			}
			return result;
		}
	}

	public Map<String, Integer> getDeletedNodes() {
		if (properties == null) {
			return deletedNodes;
		} else {
			Map<String, Integer> result = new HashMap<String, Integer>();
			Collection<Object> keys = properties.keySet();
			if (keys != null) {
				for (Object key : keys) {
					if ("true".equalsIgnoreCase(properties.getProperty(
							key.toString()).toString())) {
						if (deletedNodes.containsKey(key.toString())) {
							result.put(key.toString(), deletedNodes.get(key));
						} else {
							result.put(key.toString(), 0);
						}
					}
				}
			}
			return result;
		}
	}

	public Map<String, Integer> getUpdatedNodes() {
		if (properties == null) {
			return updatedNodes;
		} else {
			Map<String, Integer> result = new HashMap<String, Integer>();
			Collection<Object> keys = properties.keySet();
			if (keys != null) {
				for (Object key : keys) {
					if ("true".equalsIgnoreCase(properties.getProperty(
							key.toString()).toString())) {
						if (updatedNodes.containsKey(key.toString())) {
							result.put(key.toString(), updatedNodes.get(key));
						} else {
							result.put(key.toString(), 0);
						}
					}
				}
			}
			return result;
		}
	}

	public Map<String, Integer> getUnmodifiedNodes() {
		if (properties == null) {
			return unmodifiedNodes;
		} else {
			Map<String, Integer> result = new HashMap<String, Integer>();
			Collection<Object> keys = properties.keySet();
			if (keys != null) {
				for (Object key : keys) {
					if ("true".equalsIgnoreCase(properties.getProperty(
							key.toString()).toString())) {
						if (unmodifiedNodes.containsKey(key.toString())) {
							result.put(key.toString(), unmodifiedNodes.get(key));
						} else {
							result.put(key.toString(), 0);
						}
					}
				}
			}
			return result;
		}
	}
}
