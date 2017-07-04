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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.walkmod.exceptions.WalkModException;
import org.walkmod.javalang.actions.Action;
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
import org.walkmod.javalang.ast.body.BodyDeclaration;
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
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.body.VariableDeclaratorId;
import org.walkmod.javalang.ast.expr.AnnotationExpr;
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
import org.walkmod.javalang.ast.expr.TypeExpr;
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
import org.walkmod.javalang.ast.stmt.Statement;
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

    private LinkedList<Action> actionsToApply = new LinkedList<Action>();

    private int indentationLevel = 0;

    private int indentationSize = 0;

    private List<Comment> comments = new LinkedList<Comment>();

    private Stack<Position> position = new Stack<Position>();

    public ChangeLogVisitor() {
        addedNodes = new HashMap<String, Integer>();
        deletedNodes = new HashMap<String, Integer>();
        updatedNodes = new HashMap<String, Integer>();
        unmodifiedNodes = new HashMap<String, Integer>();
        setReportingPropertiesPath(reportingPropertiesPath);

        pushPosition(new Position(0, 0));
    }

    private void increaseIndentation() {
        indentationLevel++;
    }

    public <T extends Node> int inferIndentationSize(Node parent, List<T> children) {
        if (parent != null && children != null && !children.isEmpty()) {

            Node first = children.get(0);
            int i = 1;
            while (first.isNewNode() && i < children.size()) {
                first = children.get(i);
                i++;
            }
            int aux = 0;
            if (!first.isNewNode()) {
                if (first.getBeginLine() != parent.getBeginLine()) {

                    Node auxParent = parent;
                    while (auxParent != null && auxParent.getBeginColumn() > first.getBeginColumn()) {
                        auxParent = auxParent.getParentNode();
                    }
                    if (auxParent != null) {
                        parent = auxParent;
                    }
                    aux = first.getBeginColumn() - parent.getBeginColumn();
                    if (aux < 0) {
                        aux = first.getBeginColumn() - parent.getEndColumn() - 1;
                    }
                    if (aux < 0) {
                        aux = aux * (-1);
                    }

                }
                if (aux > 0) {
                    indentationSize = aux;
                }
            }
        }
        return indentationSize;
    }

    private void decreaseIndentation() {
        if (indentationLevel > 0) {
            indentationLevel--;
        }
    }

    public String getReportingPropertiesPath() {
        return reportingPropertiesPath;
    }

    public List<Action> getActionsToApply() {
        return actionsToApply;
    }

    public void setGenerateActions(boolean generateActions) {
        this.generateActions = generateActions;
    }

    public void setReportingPropertiesPath(String reportingPropertiesPath) {
        this.reportingPropertiesPath = reportingPropertiesPath;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(this.reportingPropertiesPath);
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
                //log.warn("The system cannot found the reporting.properties in the classpath. It will report all code changes");
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

    private void updatePosition(int line, int column) {
        Position top = position.peek();
        top.setLine(line);
        top.setColumn(column);
    }

    private void applyRemove(Node oi) {

        if (generateActions) {
            RemoveAction action = null;

            int beginLine = oi.getBeginLine();
            int beginColumn = oi.getBeginColumn();

            if (oi instanceof BodyDeclaration) {
                JavadocComment jc = ((BodyDeclaration) oi).getJavaDoc();
                if (jc != null) {
                    beginLine = jc.getBeginLine();
                    beginColumn = jc.getBeginColumn();
                }
            }

            if (!actionsToApply.isEmpty()) {

                Iterator<Action> inverseIt = actionsToApply.descendingIterator();
                boolean isContained = false;

                action = new RemoveAction(beginLine, beginColumn, oi.getEndLine(), oi.getEndColumn(), oi);
                Action last = null;
                while (inverseIt.hasNext() && !isContained) {
                    last = inverseIt.next();
                    isContained = last.contains(action);
                }
                if (!isContained) {
                    inverseIt = actionsToApply.descendingIterator();
                    boolean added = false;
                    int pos = actionsToApply.size();
                    while (inverseIt.hasNext() && !added) {
                        Action current = inverseIt.next();
                        if (action.isPreviousThan(current.getEndLine(), current.getEndColumn())) {
                            pos--;
                        } else {
                            added = true;
                            actionsToApply.add(pos, action);
                        }
                    }
                    if (!added) {
                        actionsToApply.add(0, action);
                    }

                }

            } else {
                action = new RemoveAction(beginLine, beginColumn, oi.getEndLine(), oi.getEndColumn(), oi);
                actionsToApply.add(action);

            }
        }

        increaseDeletedNodes(oi.getClass());

    }

    private void applyAppend(Node id) {

        AppendAction action = null;
        if (generateActions) {
            Position pos = position.peek();
            int extraLines = 0;
            if (((id instanceof AnnotationExpr) || !(id instanceof Expression)) && pos.getLine() > 1) {
                extraLines++;
            }
            if (id instanceof BodyDeclaration) {
                extraLines++;
            }
            action = new AppendAction(pos.getLine(), pos.getColumn(), id, indentationLevel, indentationSize,
                    extraLines);
            actionsToApply.add(action);
        }
        increaseAddedNodes(id.getClass());
    }

    private boolean requiresCurrentIndentation(Node node) {
        if (node instanceof ObjectCreationExpr) {
            return ((ObjectCreationExpr) node).getAnonymousClassBody() != null;
        }
        return (node instanceof BlockStmt);
    }

    private void applyUpdate(Node newNode, Node oldNode) {
        if (generateActions) {
            Action action;
            int beginLine = oldNode.getBeginLine();
            int beginColumn = oldNode.getBeginColumn();

            if (oldNode instanceof BodyDeclaration) {
                JavadocComment jc = ((BodyDeclaration) oldNode).getJavaDoc();
                if (jc != null) {
                    beginLine = jc.getBeginLine();
                    beginColumn = jc.getBeginColumn();
                }
            }

            int indent = indentationLevel;

            if (!requiresCurrentIndentation(newNode)) {
                if (indent > 0) {
                    indent--;
                }
            } else if (!requiresCurrentIndentation(oldNode)) {
                indent++;
            }

            if (!actionsToApply.isEmpty()) {

                Iterator<Action> inverseIt = actionsToApply.descendingIterator();
                boolean finish = false;
                action = new RemoveAction(beginLine, beginColumn, oldNode.getEndLine(), oldNode.getEndColumn(),
                        oldNode);
                boolean isComment = newNode instanceof Comment;

                boolean isInternalComment = false;
                while (inverseIt.hasNext() && !finish) {
                    Action last = inverseIt.next();
                    if (action.contains(last)) {
                        if (!isComment) {
                            inverseIt.remove();
                        } else {
                            isInternalComment = true;
                        }
                    } else if (isComment && !last.isPreviousThan(action.getBeginLine(), action.getBeginColumn())) {
                        //overlaps
                        isInternalComment = true;
                    }
                }
                if (!isInternalComment) {
                    action = new ReplaceAction(beginLine, beginColumn, oldNode, newNode, indent, indentationSize,
                            comments);
                    actionsToApply.add(action);
                }

            } else {
                action = new ReplaceAction(beginLine, beginColumn, oldNode, newNode, indent, indentationSize, comments);
                actionsToApply.add(action);
            }
        }
    }

    private <T extends Node> void inferASTChanges(List<T> nodes1, List<T> nodes2) {
        if (nodes1 != null) {
            List<Integer> removedNodes = new LinkedList<Integer>();
            if (nodes2 != null) {
                int i = 0;
                for (T oi : nodes2) {
                    boolean found = false;
                    Iterator<T> it = nodes1.iterator();
                    T id = null;
                    while (it.hasNext() && !found) {
                        id = it.next();
                        found = (id.getBeginLine() == oi.getBeginLine() && id.getBeginColumn() == oi.getBeginColumn());
                    }
                    if (!found) {
                        applyRemove(oi);
                        removedNodes.add(i);
                    }
                    i++;
                }
            }
            int i = 0;

            for (T id : nodes1) {
                if (id.isNewNode()) {
                    if (!removedNodes.contains(i)) {
                        boolean pushed = false;

                        boolean found = false;
                        int k = i + 1;
                        while (k < nodes1.size() && !found) {
                            found = !nodes1.get(k).isNewNode();
                            if (!found) {
                                k++;
                            }
                        }
                        if (found) {
                            T aux = nodes1.get(k);
                            pushPosition(aux);
                            pushed = true;
                        }

                        applyAppend(id);
                        if (pushed) {
                            popPosition();
                        }
                    } else {
                        int k = i;
                        while (removedNodes.contains(k)) {
                            k++;
                        }
                        applyUpdate(id, nodes2.get(k - 1));
                        i = k - 1;
                    }

                } else {
                    if (nodes2 != null) {
                        boolean found = false;
                        Iterator<T> it2 = nodes2.iterator();
                        T oi = null;
                        while (it2.hasNext() && !found) {
                            oi = it2.next();
                            found = (id.getBeginLine() == oi.getBeginLine()
                                    && id.getBeginColumn() == oi.getBeginColumn());
                        }
                        if (found) {
                            VisitorContext vc = new VisitorContext();
                            vc.put(NODE_TO_COMPARE_KEY, oi);
                            // it does the update if its necessary

                            pushPosition(new Position(id.getEndLine(), id.getEndColumn()));
                            id.accept(this, vc);
                            popPosition();
                        } else {
                            applyRemove(id);
                        }
                    }
                }

                i++;
            }

        } else {
            if (nodes2 != null) {
                for (T elem : nodes2) {
                    applyRemove(elem);

                }
            }
        }
    }

    private <T extends Node> void inferASTChangesList(List<List<T>> nodes1, List<List<T>> nodes2) {
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

                CompilationUnit oldCU = (CompilationUnit) o;
                List<Comment> thisComments = null;
                List<Comment> otherComments = null;
                // we print the new comments at the beginning if they are not
                // javadoc
                if (n.getComments() != null) {
                    List<Comment> newComments = new LinkedList<Comment>();
                    thisComments = new LinkedList<Comment>(n.getComments());
                    Iterator<Comment> it = thisComments.iterator();
                    while (it.hasNext()) {
                        Comment c = it.next();
                        if (!(c instanceof JavadocComment)) {
                            if (c.isNewNode()) {
                                newComments.add(c);
                                it.remove();
                            }
                        } else {
                            it.remove();
                        }
                    }
                    comments = new LinkedList<Comment>(thisComments);
                    inferASTChanges(newComments, null);
                }
                if (oldCU.getComments() != null) {
                    otherComments = new LinkedList<Comment>(oldCU.getComments());
                    Iterator<Comment> oldIt = otherComments.iterator();
                    while (oldIt.hasNext()) {
                        Comment c = oldIt.next();
                        if (c instanceof JavadocComment) {
                            oldIt.remove();
                        }
                    }
                }

                List<TypeDeclaration> types = oldCU.getTypes();

                inferASTChanges(n.getPackage(), oldCU.getPackage());
                if (types != null && !types.isEmpty()) {
                    TypeDeclaration td = types.get(0);

                    int line = td.getBeginLine();
                    int col = td.getBeginColumn();

                    JavadocComment comment = td.getJavaDoc();
                    if (comment != null) {
                        line = comment.getBeginLine();
                        col = comment.getBeginColumn();
                    }
                    updatePosition(line, col);
                }

                inferASTChanges(n.getImports(), oldCU.getImports());
                List<Action> importsActions = new LinkedList<Action>(actionsToApply);
                actionsToApply = new LinkedList<Action>();
                inferASTChanges(n.getTypes(), oldCU.getTypes());
                inferASTChanges(thisComments, otherComments);

                if (!importsActions.isEmpty()) {
                    actionsToApply.addAll(0, importsActions);
                }
                if (!isUpdated()) {
                    increaseUnmodifiedNodes(CompilationUnit.class);
                } else {
                    increaseUpdatedNodes(CompilationUnit.class);
                }
            }
            setIsUpdated(backup || isUpdated);
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
        }
    }

    public void visit(ClassOrInterfaceDeclaration n, VisitorContext ctx) {

        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof ClassOrInterfaceDeclaration) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            ClassOrInterfaceDeclaration aux = (ClassOrInterfaceDeclaration) o;

            boolean equals = n.getName().equals(aux.getName()) && n.getModifiers() == aux.getModifiers();

            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getTypeParameters(), aux.getTypeParameters());
            inferASTChanges(n.getExtends(), aux.getExtends());
            inferASTChanges(n.getImplements(), aux.getImplements());

            popPosition();
            pushPosition(pos);

            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            increaseIndentation();
            inferIndentationSize(aux, aux.getMembers());
            inferASTChanges(n.getMembers(), aux.getMembers());
            decreaseIndentation();
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(ClassOrInterfaceDeclaration.class);
                } else {
                    increaseUpdatedNodes(ClassOrInterfaceDeclaration.class);
                }
            } else {
                applyUpdate(n, aux, n.getExtends(), aux.getExtends());
                applyUpdate(n, aux, n.getImplements(), aux.getImplements());
                applyUpdate(n, aux, n.getTypeParameters(), aux.getTypeParameters());
                increaseUpdatedNodes(ClassOrInterfaceDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
        }
    }

    public void visit(TypeExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof TypeExpr) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            TypeExpr aux = (TypeExpr) o;
            inferASTChanges(n.getType(), aux.getType());

            if (!isUpdated()) {

                increaseUnmodifiedNodes(TypeParameter.class);

            } else {

                increaseUpdatedNodes(TypeExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        }
    }

    public void visit(TypeParameter n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof TypeParameter) {
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
        }
    }

    public void visit(MethodDeclaration n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof MethodDeclaration) {
            boolean backup = isUpdated();
            setIsUpdated(false);

            MethodDeclaration aux = (MethodDeclaration) o;
            boolean equals = n.getName().equals(aux.getName()) && n.getArrayCount() == aux.getArrayCount()
                    && n.getModifiers() == aux.getModifiers() && n.isDefault() == aux.isDefault();
            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getTypeParameters(), aux.getTypeParameters());
            inferASTChanges(n.getType(), aux.getType());
            inferASTChanges(n.getParameters(), aux.getParameters());
            inferASTChanges(n.getThrows(), aux.getThrows());
            popPosition();
            pushPosition(pos);
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            inferASTChanges(n.getBody(), aux.getBody());
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(MethodDeclaration.class);
                } else {
                    increaseUpdatedNodes(MethodDeclaration.class);
                }
            } else {
                applyUpdate(n, aux, n.getTypeParameters(), aux.getTypeParameters());
                applyUpdate(n, aux, n.getParameters(), aux.getParameters());
                applyUpdate(n, aux, n.getThrows(), aux.getThrows());
                increaseUpdatedNodes(MethodDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
        }
    }

    public void visit(FieldDeclaration n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof FieldDeclaration) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            FieldDeclaration aux = (FieldDeclaration) o;
            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getType(), aux.getType());
            inferASTChanges(n.getVariables(), aux.getVariables());
            popPosition();
            pushPosition(pos);
            if (!isUpdated) {
                if (n.getModifiers() == aux.getModifiers()) {
                    increaseUnmodifiedNodes(FieldDeclaration.class);
                } else {
                    applyUpdate(n, aux);
                    increaseUpdatedNodes(FieldDeclaration.class);
                }
            } else {
                applyUpdate(n, aux, n.getVariables(), aux.getVariables());
                increaseUpdatedNodes(FieldDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
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
            if (equals) {
                increaseUnmodifiedNodes(BlockComment.class);
            } else {
                increaseUpdatedNodes(BlockComment.class);
            }
            setIsUpdated(backup || isUpdated());
        }
    }

    private <T extends Node, K extends Node> boolean applyUpdate(T thisNode, T other, List<K> theseNodes,
            List<K> otherNodes) {
        if (otherNodes == null && theseNodes == null) {
            return false;
        } else if (otherNodes == null || theseNodes == null) {
            applyUpdate(thisNode, (Node) other);
        } else if (theseNodes.size() != otherNodes.size()) {
            applyUpdate(thisNode, (Node) other);
        } else {
            boolean equals = true;
            Iterator<K> it1 = theseNodes.iterator();
            Iterator<K> it2 = otherNodes.iterator();

            while (it1.hasNext() && equals) {
                K n1 = it1.next();
                K n2 = it2.next();
                equals = n1.isInEqualLocation(n2);
            }

            if (!equals) {
                applyUpdate(thisNode, (Node) other);
            } else {
                return false;
            }
        }
        return true;
    }

    private <T extends Node, K extends Node> boolean applyUpdateList(T thisNode, T other, List<List<K>> theseNodes,
            List<List<K>> otherNodes) {
        if (otherNodes == null && theseNodes == null) {
            return false;
        } else if (otherNodes == null || theseNodes == null) {
            applyUpdate(thisNode, (Node) other);
        } else if (theseNodes.size() != otherNodes.size()) {
            applyUpdate(thisNode, (Node) other);
        } else {
            boolean equals = true;
            Iterator<List<K>> it1 = theseNodes.iterator();
            Iterator<List<K>> it2 = otherNodes.iterator();
            while (it1.hasNext() && equals) {
                List<K> n1 = it1.next();
                List<K> n2 = it2.next();
                equals = !applyUpdate(thisNode, other, n1, n2);
            }
            return !equals;
        }
        return true;
    }

    private <T extends Node> void applyUpdate(T thisNode, T other, Node theseProperty, Node otherProperty) {
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
            boolean equals = n.getName().equals(aux.getName()) && n.getModifiers() == aux.getModifiers();

            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getImplements(), aux.getImplements());
            popPosition();
            pushPosition(pos);

            if (!equals) {
                applyUpdate(n, (Node) o);
            }

            increaseIndentation();
            inferIndentationSize(aux, aux.getMembers());
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
                applyUpdate(n, aux, n.getImplements(), aux.getImplements());
                applyUpdate(n, aux, theseEntries, otherEntries);
                increaseUpdatedNodes(EnumDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(EmptyTypeDeclaration n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof EmptyTypeDeclaration) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            EmptyTypeDeclaration aux = (EmptyTypeDeclaration) o;
            String nName = n.getName();
            String auxName = aux.getName();

            boolean equals = ((nName == null && auxName == null)
                    || (nName != null && auxName != null && nName.equals(auxName)))
                    && n.getModifiers() == aux.getModifiers();

            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            popPosition();
            pushPosition(pos);
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            inferIndentationSize(aux, aux.getMembers());
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
                increaseUpdatedNodes(EmptyTypeDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(EnumConstantDeclaration n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof EnumConstantDeclaration) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            EnumConstantDeclaration aux = (EnumConstantDeclaration) o;

            boolean equals = n.getName().equals(aux.getName());

            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getArgs(), aux.getArgs());
            popPosition();
            pushPosition(pos);

            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            increaseIndentation();
            inferASTChanges(n.getClassBody(), aux.getClassBody());
            decreaseIndentation();

            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(EnumConstantDeclaration.class);
                } else {
                    increaseUpdatedNodes(EnumConstantDeclaration.class);
                }
            } else {
                applyUpdate(n, aux, n.getArgs(), aux.getArgs());
                increaseUpdatedNodes(EnumConstantDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(AnnotationDeclaration n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof AnnotationDeclaration) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            AnnotationDeclaration aux = (AnnotationDeclaration) o;

            boolean equals = n.getName().equals(aux.getName()) && n.getModifiers() == aux.getModifiers();

            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            popPosition();
            pushPosition(pos);
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            increaseIndentation();
            inferIndentationSize(aux, aux.getMembers());
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
                increaseUpdatedNodes(AnnotationDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(AnnotationMemberDeclaration n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof AnnotationMemberDeclaration) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            AnnotationMemberDeclaration aux = (AnnotationMemberDeclaration) o;

            boolean equals = n.getName().equals(aux.getName()) && n.getModifiers() == aux.getModifiers();
            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getDefaultValue(), aux.getDefaultValue());
            inferASTChanges(n.getType(), aux.getType());
            popPosition();
            pushPosition(pos);
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(AnnotationMemberDeclaration.class);
                } else {
                    increaseUpdatedNodes(AnnotationMemberDeclaration.class);
                }
            } else {
                increaseUpdatedNodes(AnnotationMemberDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(ConstructorDeclaration n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof ConstructorDeclaration) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            ConstructorDeclaration aux = (ConstructorDeclaration) o;

            boolean equals = n.getName().equals(aux.getName()) && n.getModifiers() == aux.getModifiers();

            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getTypeParameters(), aux.getTypeParameters());
            inferASTChanges(n.getParameters(), aux.getParameters());
            inferASTChanges(n.getThrows(), aux.getThrows());
            popPosition();
            pushPosition(pos);
            inferASTChanges(n.getBlock(), aux.getBlock());
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(ConstructorDeclaration.class);
                } else {
                    increaseUpdatedNodes(ConstructorDeclaration.class);
                }
            } else {
                applyUpdate(n, aux, n.getTypeParameters(), aux.getTypeParameters());
                applyUpdate(n, aux, n.getParameters(), aux.getParameters());
                increaseUpdatedNodes(ConstructorDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(MultiTypeParameter n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof MultiTypeParameter) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            MultiTypeParameter aux = (MultiTypeParameter) o;
            boolean equals = n.getModifiers() == aux.getModifiers();

            Position pos = popPosition();
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getTypes(), aux.getTypes());
            inferASTChanges(n.getId(), aux.getId());
            pushPosition(pos);
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(Parameter.class);
                } else {
                    increaseUpdatedNodes(Parameter.class);
                }
            } else {
                applyUpdate(n, aux, n.getTypes(), aux.getTypes());
                increaseUpdatedNodes(Parameter.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(Parameter n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof Parameter) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            Parameter aux = (Parameter) o;

            boolean equals = n.getModifiers() == aux.getModifiers();

            Position pos = popPosition();
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getType(), aux.getType());
            inferASTChanges(n.getId(), aux.getId());
            pushPosition(pos);
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(Parameter.class);
                } else {
                    increaseUpdatedNodes(Parameter.class);
                }
            } else {

                increaseUpdatedNodes(Parameter.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(EmptyMemberDeclaration n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof EmptyMemberDeclaration) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            Position pos = popPosition();
            pushPosition(n);
            EmptyMemberDeclaration aux = (EmptyMemberDeclaration) o;
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            popPosition();
            pushPosition(pos);
            if (!isUpdated()) {
                increaseUnmodifiedNodes(EmptyMemberDeclaration.class);
            } else {

                increaseUpdatedNodes(EmptyMemberDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(InitializerDeclaration n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof InitializerDeclaration) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            InitializerDeclaration aux = (InitializerDeclaration) o;
            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getJavaDoc(), aux.getJavaDoc());
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            popPosition();
            pushPosition(pos);
            inferASTChanges(n.getBlock(), aux.getBlock());

            if (!isUpdated()) {
                increaseUnmodifiedNodes(InitializerDeclaration.class);
            } else {
                increaseUpdatedNodes(InitializerDeclaration.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(ClassOrInterfaceType n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof ClassOrInterfaceType) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            ClassOrInterfaceType aux = (ClassOrInterfaceType) o;

            boolean equals = n.getName().equals(aux.getName());

            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getScope(), aux.getScope());
            inferASTChanges(n.getTypeArgs(), aux.getTypeArgs());
            popPosition();
            pushPosition(pos);
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            popPosition();
            pushPosition(pos);
            if (equals) {
                increaseUnmodifiedNodes(PrimitiveType.class);
            } else {

                increaseUpdatedNodes(PrimitiveType.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(ReferenceType n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof ReferenceType) {
            boolean backup = isUpdated();
            setIsUpdated(false);
            ReferenceType aux = (ReferenceType) o;

            boolean equals = n.getType().equals(aux.getType()) && n.getArrayCount() == aux.getArrayCount();

            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChangesList(n.getArraysAnnotations(), aux.getArraysAnnotations());
            popPosition();
            pushPosition(pos);
            if (!equals) {
                applyUpdate(n, (Node) o);
            }

            if (equals) {
                increaseUnmodifiedNodes(PrimitiveType.class);
            } else {
                applyUpdateList(n, aux, n.getArraysAnnotations(), aux.getArraysAnnotations());
                increaseUpdatedNodes(PrimitiveType.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(VoidType n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null) {
            increaseUnmodifiedNodes(VoidType.class);

        }
    }

    public void visit(WildcardType n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof WildcardType) {
            WildcardType aux = (WildcardType) o;
            boolean backup = isUpdated();
            setIsUpdated(false);
            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getExtends(), aux.getExtends());
            inferASTChanges(n.getSuper(), aux.getSuper());
            popPosition();
            pushPosition(pos);
            if (!isUpdated()) {
                increaseUnmodifiedNodes(WildcardType.class);
            } else {
                applyUpdate(n, aux, n.getExtends(), aux.getExtends());
                increaseUpdatedNodes(WildcardType.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
            Position pos = popPosition();
            pushPosition(n);
            inferASTChangesList(n.getArraysAnnotations(), aux.getArraysAnnotations());
            inferASTChanges(n.getDimensions(), aux.getDimensions());
            inferASTChanges(n.getType(), aux.getType());
            popPosition();
            pushPosition(pos);
            inferASTChanges(n.getInitializer(), aux.getInitializer());
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(ArrayCreationExpr.class);
                } else {
                    applyUpdate(n, aux, n.getInitializer(), aux.getInitializer());
                    increaseUpdatedNodes(ArrayCreationExpr.class);
                }
            } else {
                increaseUpdatedNodes(ArrayCreationExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(AssignExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof AssignExpr) {
            AssignExpr aux = (AssignExpr) o;
            boolean backup = isUpdated();
            setIsUpdated(false);

            boolean equals = n.getOperator().name().equals(aux.getOperator().name());
            inferASTChanges(n.getTarget(), aux.getTarget());
            inferASTChanges(n.getValue(), aux.getValue());
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(BinaryExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof BinaryExpr) {
            BinaryExpr aux = (BinaryExpr) o;
            boolean backup = isUpdated();
            setIsUpdated(false);
            boolean equals = n.getOperator().name().equals(aux.getOperator().name());

            inferASTChanges(n.getRight(), aux.getRight());
            inferASTChanges(n.getLeft(), aux.getLeft());
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(BinaryExpr.class);
                } else {
                    increaseUpdatedNodes(BinaryExpr.class);
                }
            } else {
                applyUpdate(n, (Node) o);
                increaseUpdatedNodes(BinaryExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
                increaseUpdatedNodes(ConditionalExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(FieldAccessExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof FieldAccessExpr) {
            FieldAccessExpr aux = (FieldAccessExpr) o;
            boolean backup = isUpdated();
            setIsUpdated(false);
            boolean equals = n.getField().equals(aux.getField());
            inferASTChanges(n.getScope(), aux.getScope());
            inferASTChanges(n.getTypeArgs(), aux.getTypeArgs());
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(FieldAccessExpr.class);
                } else {
                    increaseUpdatedNodes(FieldAccessExpr.class);
                }
            } else {
                applyUpdate(n, aux, n.getTypeArgs(), aux.getTypeArgs());
                increaseUpdatedNodes(FieldAccessExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(NullLiteralExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null) {
            increaseUnmodifiedNodes(NullLiteralExpr.class);
        }
    }

    public void visit(MethodCallExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof MethodCallExpr) {
            MethodCallExpr aux = (MethodCallExpr) o;

            boolean equals = n.getName().equals(aux.getName());

            boolean backup = isUpdated();
            setIsUpdated(false);
            Position pos = popPosition();

//            position.push(new Position(n.getBeginLine(), n.getBeginColumn()));
            pushPosition(aux);
            inferASTChanges(n.getScope(), aux.getScope());
            inferASTChanges(n.getTypeArgs(), aux.getTypeArgs());
            List<Expression> theseArgs = n.getArgs();
            List<Expression> otherArgs = aux.getArgs();
            inferASTChanges(theseArgs, otherArgs);

            popPosition();
            pushPosition(pos);
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(MethodCallExpr.class);
                } else {
                    increaseUpdatedNodes(MethodCallExpr.class);
                }
            } else {
                int sizeArgs = theseArgs == null ? 0 : theseArgs.size();
                int sizeOtherArgs = otherArgs == null ? 0 : otherArgs.size();
                if (sizeArgs != sizeOtherArgs) {
                    applyUpdate(n, aux, theseArgs, otherArgs);
                }
                applyUpdate(n, aux, n.getScope(), aux.getScope());
                applyUpdate(n, aux, n.getTypeArgs(), aux.getTypeArgs());
                increaseUpdatedNodes(MethodCallExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
                applyUpdate((Node) o, n);
                increaseUpdatedNodes(NameExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(ObjectCreationExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof ObjectCreationExpr) {
            ObjectCreationExpr aux = (ObjectCreationExpr) o;
            boolean backup = isUpdated();
            setIsUpdated(false);
            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getScope(), aux.getScope());
            inferASTChanges(n.getType(), aux.getType());
            inferASTChanges(n.getTypeArgs(), aux.getTypeArgs());
            inferASTChanges(n.getArgs(), aux.getArgs());
            popPosition();
            pushPosition(pos);
            increaseIndentation();
            inferIndentationSize(aux, aux.getAnonymousClassBody());
            inferASTChanges(n.getAnonymousClassBody(), aux.getAnonymousClassBody());
            decreaseIndentation();
            if (!isUpdated()) {
                increaseUnmodifiedNodes(ObjectCreationExpr.class);
            } else {
                applyUpdate(n, aux, n.getArgs(), aux.getArgs());
                applyUpdate(n, aux, n.getTypeArgs(), aux.getTypeArgs());
                increaseUpdatedNodes(ObjectCreationExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(QualifiedNameExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof QualifiedNameExpr) {
            QualifiedNameExpr aux = (QualifiedNameExpr) o;
            boolean backup = isUpdated();
            setIsUpdated(false);

            boolean equals = n.getName().equals(aux.getName());
            inferASTChanges(n.getQualifier(), aux.getQualifier());
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(UnaryExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof UnaryExpr) {
            UnaryExpr aux = (UnaryExpr) o;
            boolean backup = isUpdated();
            setIsUpdated(false);

            boolean equals = n.getOperator().name().equals(aux.getOperator().name());

            inferASTChanges(n.getExpr(), aux.getExpr());
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(VariableDeclarationExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof VariableDeclarationExpr) {
            VariableDeclarationExpr aux = (VariableDeclarationExpr) o;
            boolean backup = isUpdated();
            setIsUpdated(false);

            boolean equals = n.getModifiers() == aux.getModifiers();
            Position pos = popPosition();
            pushPosition(n);
            inferASTChanges(n.getAnnotations(), aux.getAnnotations());
            inferASTChanges(n.getVars(), aux.getVars());
            inferASTChanges(n.getType(), aux.getType());
            popPosition();
            pushPosition(pos);
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
            if (!isUpdated()) {
                if (equals) {
                    increaseUnmodifiedNodes(VariableDeclarationExpr.class);
                } else {
                    increaseUpdatedNodes(VariableDeclarationExpr.class);
                }
            } else {
                applyUpdate(n, aux, n.getVars(), aux.getVars());
                increaseUpdatedNodes(VariableDeclarationExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(SingleMemberAnnotationExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof SingleMemberAnnotationExpr) {
            SingleMemberAnnotationExpr aux = (SingleMemberAnnotationExpr) o;
            boolean backup = isUpdated();
            setIsUpdated(false);
            inferASTChanges(n.getName(), aux.getName());
            inferASTChanges(n.getMemberValue(), aux.getMemberValue());
            if (!isUpdated()) {
                increaseUnmodifiedNodes(SingleMemberAnnotationExpr.class);
            } else {
                applyUpdate(n, aux, n.getMemberValue(), n.getMemberValue());
                increaseUpdatedNodes(SingleMemberAnnotationExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(NormalAnnotationExpr n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr aux = (NormalAnnotationExpr) o;
            boolean backup = isUpdated();
            setIsUpdated(false);
            inferASTChanges(n.getName(), aux.getName());
            inferASTChanges(n.getPairs(), aux.getPairs());

            if (!isUpdated()) {
                increaseUnmodifiedNodes(NormalAnnotationExpr.class);
            } else {
                applyUpdate(n, aux, n.getPairs(), n.getPairs());
                increaseUpdatedNodes(NormalAnnotationExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(ExplicitConstructorInvocationStmt n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof ExplicitConstructorInvocationStmt) {
            ExplicitConstructorInvocationStmt aux = (ExplicitConstructorInvocationStmt) o;
            boolean backup = isUpdated();
            setIsUpdated(false);
            inferASTChanges(n.getTypeArgs(), aux.getTypeArgs());
            inferASTChanges(n.getArgs(), aux.getArgs());
            inferASTChanges(n.getExpr(), aux.getExpr());
            if (!isUpdated()) {
                increaseUnmodifiedNodes(ExplicitConstructorInvocationStmt.class);
            } else {
                applyUpdate(n, aux, n.getArgs(), aux.getArgs());
                applyUpdate(n, aux, n.getTypeArgs(), aux.getTypeArgs());
                increaseUpdatedNodes(ExplicitConstructorInvocationStmt.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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

                increaseUpdatedNodes(AssertStmt.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(BlockStmt n, VisitorContext ctx) {

        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof BlockStmt && !n.isNewNode()) {
            BlockStmt aux = (BlockStmt) o;
            boolean backup = isUpdated();
            setIsUpdated(false);
            increaseIndentation();
            inferIndentationSize(aux, aux.getStmts());
            inferASTChanges(n.getStmts(), aux.getStmts());
            decreaseIndentation();
            if (!isUpdated()) {
                increaseUnmodifiedNodes(BlockStmt.class);
            } else {
                increaseUpdatedNodes(BlockStmt.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }

    }

    public void visit(LabeledStmt n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof LabeledStmt) {
            LabeledStmt aux = (LabeledStmt) o;
            boolean backup = isUpdated();
            setIsUpdated(false);
            boolean equals = n.getLabel().equals(aux.getLabel());
            inferASTChanges(n.getStmt(), aux.getStmt());
            if (!equals) {
                applyUpdate(n, (Node) o);
            }
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(EmptyStmt n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof EmptyStmt) {
            increaseUnmodifiedNodes(EmptyStmt.class);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
            inferIndentationSize(aux, aux.getEntries());

            inferASTChanges(n.getEntries(), aux.getEntries());
            decreaseIndentation();

            if (!isUpdated()) {
                increaseUnmodifiedNodes(SwitchStmt.class);
            } else {
                increaseUpdatedNodes(SwitchStmt.class);
            }
            setIsUpdated(backup || isUpdated());

        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
        }
    }

    public void visit(SwitchEntryStmt n, VisitorContext ctx) {
        Object o = ctx.get(NODE_TO_COMPARE_KEY);
        if (o != null && o instanceof SwitchEntryStmt) {
            SwitchEntryStmt aux = (SwitchEntryStmt) o;

            boolean backup = isUpdated();
            setIsUpdated(false);
            inferASTChanges(n.getLabel(), aux.getLabel());
            Position pos = popPosition();
            Position newPos = null;

            List<Statement> stmts = aux.getStmts();
            if (stmts != null && !stmts.isEmpty()) {
                Statement first = stmts.get(0);
                newPos = new Position(first.getBeginLine(), first.getBeginColumn());
            } else if (aux.getLabel() != null) {
                newPos = new Position(aux.getLabel().getEndLine(), aux.getLabel().getEndColumn());
            } else {
                pushPosition(pos);
            }

            if (newPos != null) {
                pushPosition(newPos);
            }
            inferIndentationSize(aux, aux.getStmts());
            inferASTChanges(n.getStmts(), aux.getStmts());
            popPosition();
            pushPosition(pos);
            if (!isUpdated()) {
                increaseUnmodifiedNodes(SwitchEntryStmt.class);
            } else {
                increaseUpdatedNodes(SwitchEntryStmt.class);
            }
            setIsUpdated(backup || isUpdated());

        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
                if (aux.getExpr() instanceof EnclosedExpr) {
                    //without applyUpdate return(true) => returntrue
                    applyUpdate(n, aux, n.getExpr(), aux.getExpr());
                }
                increaseUpdatedNodes(ReturnStmt.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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

                increaseUpdatedNodes(IfStmt.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
                applyUpdate(n, aux, n.getResources(), aux.getResources());
                increaseUpdatedNodes(TryStmt.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
                increaseUpdatedNodes(CatchClause.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
                applyUpdate(n, aux, n.getTypeParameters(), aux.getTypeParameters());
                increaseUpdatedNodes(MethodReferenceExpr.class);
            }
            setIsUpdated(backup || isUpdated());
        } else if (o != null) {
            setIsUpdated(true);
            applyUpdate(n, (Node) o);
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
                    if ("true".equalsIgnoreCase(properties.getProperty(key.toString()).toString())) {
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
                    if ("true".equalsIgnoreCase(properties.getProperty(key.toString()).toString())) {
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
                    if ("true".equalsIgnoreCase(properties.getProperty(key.toString()).toString())) {
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
                    if ("true".equalsIgnoreCase(properties.getProperty(key.toString()).toString())) {
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

    private void pushPosition(Node n) {
        pushPosition(new Position(n.getBeginLine(), n.getBeginColumn()));
    }

    private Position pushPosition(Position pos) {
        return position.push(pos);
    }

    private Position popPosition() {
        return position.pop();
    }

    // for testing
    public Deque<Position> getPositionStack() {
        return new ArrayDeque<Position>(position);
    }

}
