package org.walkmod.javalang.actions;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.BlockComment;
import org.walkmod.javalang.ast.Comment;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.TypeParameter;
import org.walkmod.javalang.ast.body.BodyDeclaration;
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.JavadocComment;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.ModifierSet;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.body.VariableDeclaratorId;
import org.walkmod.javalang.ast.expr.AnnotationExpr;
import org.walkmod.javalang.ast.expr.BinaryExpr;
import org.walkmod.javalang.ast.expr.EnclosedExpr;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;
import org.walkmod.javalang.ast.expr.MarkerAnnotationExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.NormalAnnotationExpr;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.stmt.BlockStmt;
import org.walkmod.javalang.ast.stmt.BreakStmt;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.stmt.IfStmt;
import org.walkmod.javalang.ast.stmt.ReturnStmt;
import org.walkmod.javalang.ast.stmt.Statement;
import org.walkmod.javalang.ast.stmt.SwitchEntryStmt;
import org.walkmod.javalang.ast.stmt.SwitchStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.PrimitiveType;
import org.walkmod.javalang.ast.type.PrimitiveType.Primitive;
import org.walkmod.javalang.ast.type.ReferenceType;
import org.walkmod.javalang.walkers.ChangeLogVisitor;
import org.walkmod.javalang.walkers.DefaultJavaParser;
import org.walkmod.walkers.ParseException;
import org.walkmod.walkers.VisitorContext;

public class TestActionsInferred {

   DefaultJavaParser parser = new DefaultJavaParser();

   @Test
   public void testNoActions() throws Exception {
      String code = "public class A { private int name;}";

      CompilationUnit cu = parser.parse(code, false);

      CompilationUnit cu2 = parser.parse(code, false);

      List<Action> actions = getActions(cu, cu2);
      Assert.assertTrue(actions.isEmpty());
   }

   @Test
   public void testNoActionsWithEmptyDeclaration() throws Exception {
      String code = "package at.foo.bar.annotations; " + "import java.lang.annotation.ElementType; "
            + "import java.lang.annotation.Retention; " + "import java.lang.annotation.RetentionPolicy; "
            + "import java.lang.annotation.Target;" + " public @interface Exportable { enum Type { A, B;};}";

      CompilationUnit cu = parser.parse(code, false);

      CompilationUnit cu2 = parser.parse(code, false);

      List<Action> actions = getActions(cu, cu2);
      Assert.assertTrue(actions.isEmpty());
   }

   private List<Action> getActions(CompilationUnit original, CompilationUnit modified) {
      ChangeLogVisitor visitor = new ChangeLogVisitor();
      Assert.assertEquals(1, visitor.getPositionStack().size());
      VisitorContext ctx = new VisitorContext();
      ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, original);
      visitor.visit(modified, ctx);
      // assert balanced push/pop calls
      Assert.assertEquals(1, visitor.getPositionStack().size());
      return visitor.getActionsToApply();
   }

   @Test
   public void testRemoveField() throws Exception {
      String modifiedCode = "public class A { private String name;}";

      CompilationUnit modifiedCu = parser.parse(modifiedCode, false);

      String original = "public class A { private String name; private int age;}";
      CompilationUnit originalCu = parser.parse(original, false);

      List<Action> actions = getActions(originalCu, modifiedCu);

      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(39, actions.get(0).getBeginColumn());
      Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());

      assertCode(actions, original, "public class A { private String name; }");

   }

   @Test
   public void testReplaceArgument() throws Exception {
      String code = "public class A { public void foo() { LOG.info(foo()); }}";
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      List<Statement> stmts = md.getBody().getStmts();
      ExpressionStmt eStmt = (ExpressionStmt) stmts.get(0);
      MethodCallExpr n = (MethodCallExpr) eStmt.getExpression();
      List<Expression> newArgs = new LinkedList<Expression>();
      newArgs.add(new NameExpr("e"));

      n.setArgs(newArgs);
      //MethodCallExpr mce = new MethodCallExpr(n.getScope(), "info", newArgs);
      //n.getParentNode().replaceChildNode(n, mce);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      ReplaceAction action = (ReplaceAction) actions.get(0);
      Assert.assertEquals("e", action.getNewText());

      assertCode(actions, code, "public class A { public void foo() { LOG.info(e); }}");
   }
   
   @Test
   public void testMethodCall() throws Exception {
      String code = "public class A { public void foo() { info(foo()); }}";
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      List<Statement> stmts = md.getBody().getStmts();
      ExpressionStmt eStmt = (ExpressionStmt) stmts.get(0);
      MethodCallExpr n = (MethodCallExpr) eStmt.getExpression();
      n.setName("warn");

      // make position invalid so it can't be used.
      // Like a replaceement with a new node would have
      n.setBeginLine(0);
      n.setBeginColumn(0);
      n.setEndLine(0);
      n.setEndColumn(0);

      // introduce a new scope to trigger change inside
      // position scope of "n"
      n.setScope(new NameExpr("LOG"));

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      ReplaceAction action = (ReplaceAction) actions.get(0);
      Assert.assertEquals("LOG.warn(foo())", action.getNewText());

      // when pushing "n.pos" instead of "aux.pos" in ChangeLogVisitor the error is having 2 actions and that results in:
      //  Expected :public class A { public void foo() { LOG.warn(foo()); }}
      //  Actual   :LOGpublic class A { public void foo() { LOG.warn(foo()); }}
      assertCode(actions, code, "public class A { public void foo() { LOG.warn(foo()); }}");
   }

   @Test
   public void testAddingMethodCallScope() throws Exception {
      String code = "public class A { public void foo() { info(foo()); }}";
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      List<Statement> stmts = md.getBody().getStmts();
      ExpressionStmt eStmt = (ExpressionStmt) stmts.get(0);
      MethodCallExpr n = (MethodCallExpr) eStmt.getExpression();

      // make position invalid so it can't be used.
      // (Like a replacement with a new node would have)
      n.setBeginLine(0);
      n.setBeginColumn(0);
      n.setEndLine(0);
      n.setEndColumn(0);

      // introduce a new scope to trigger change inside
      // position scope of "n"
      n.setScope(new NameExpr("LOG"));

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      ReplaceAction action = (ReplaceAction) actions.get(0);
      Assert.assertEquals("LOG.info(foo())", action.getNewText());

      assertCode(actions, code, "public class A { public void foo() { LOG.info(foo()); }}");
   }

   @Test
   public void testRemoveNonEmptyMethod() throws Exception {

      String modifiedCode = "public class A { private String name;}";
      CompilationUnit modifiedCu = parser.parse(modifiedCode, false);

      String original = "public class A { private String name; private void print(){ System.out.println(\"hello\");}}";
      CompilationUnit originalCu = parser.parse(original, false);

      List<Action> actions = getActions(originalCu, modifiedCu);

      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(39, actions.get(0).getBeginColumn());
      Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());

      assertCode(actions, original, "public class A { private String name; }");
      String expectedReult = "public class A { private String name; }";
      assertCode(actions, original, expectedReult);
   }

   @Test
   public void testRemoveCommentedMembers() throws Exception {
      String modifiedCode = "public class A { private String name;}";
      CompilationUnit modifiedCu = parser.parse(modifiedCode, false);

      String original = "public class A { private String name; /**age**/ private int age;}";
      CompilationUnit originalCu = parser.parse(original, false);

      List<Action> actions = getActions(originalCu, modifiedCu);

      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(original.indexOf("/**age**/") + 1, actions.get(0).getBeginColumn());
      Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());
   }

   @Test
   public void testRemoveBlockStmtWithInnerComments() throws Exception {
      String modifiedCode = "public class A { private String name;}";
      CompilationUnit modifiedCu = parser.parse(modifiedCode, false);

      String original = "public class A { private String name;  private void print(){ /*comment*/ System.out.println(\"hello\");}}";
      CompilationUnit originalCu = parser.parse(original, false);

      List<Action> actions = getActions(originalCu, modifiedCu);

      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(original.indexOf("private void print()") + 1, actions.get(0).getBeginColumn());
      Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());

      assertCode(actions, original, "public class A { private String name;  }");
   }

   @Test
   public void testRemoveMembersWithInnerComments() throws Exception {
      String modifiedCode = "public class A{ static{ \n                              \n  i++; }}";
      CompilationUnit modifiedCu = parser.parse(modifiedCode, false);

      String original = "public class A{ static{ \n  int i = 1 /*first comment*/;\n  i++; }}";
      CompilationUnit originalCu = parser.parse(original, false);

      List<Action> actions = getActions(originalCu, modifiedCu);

      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(3, actions.get(0).getBeginColumn());
      Assert.assertEquals("int i = 1 /*first comment*/".length() + 3, actions.get(0).getEndColumn());
      Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());

      assertCode(actions, original, "public class A{ static{ \n  i++; }}");
   }

   @Test
   public void testRemoveEnumerationLiterals() throws Exception {
      String modifiedCode = "public enum A { FOO,      }";
      CompilationUnit modifiedCu = parser.parse(modifiedCode, false);

      String original = "public enum A { FOO, BAR, }";
      CompilationUnit originalCu = parser.parse(original, false);

      List<Action> actions = getActions(originalCu, modifiedCu);

      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(1, actions.get(0).getBeginColumn());
      Assert.assertEquals(original.length(), actions.get(0).getEndColumn());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      assertCode(actions, original, "public enum A {FOO}");
   }

   @Test
   public void testRemoveMultipleClassMembers() throws Exception {
      String modifiedCode = "public class A {                                      }";

      CompilationUnit modifiedCu = parser.parse(modifiedCode, false);

      String original = "public class A { private String name; private int age;}";
      CompilationUnit originalCu = parser.parse(original, false);

      List<Action> actions = getActions(originalCu, modifiedCu);

      Assert.assertEquals(2, actions.size());

      Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());
      Assert.assertEquals(ActionType.REMOVE, actions.get(1).getType());

      assertCode(actions, original, "public class A {  }");
   }

   @Test
   public void testRemoveImports() throws Exception {
      String code = "package org;\nimport foo.Bar;\nimport java.util.List;\nimport java.util.Collection;\nimport foo.Car;\n";
      String classCode = "public class A{\n public void foo(){\n }\n}";
      code += classCode;

      CompilationUnit modifiedCu = parser.parse(code, false);
      CompilationUnit originalCu = parser.parse(code, false);

      List<ImportDeclaration> imports = modifiedCu.getImports();
      imports.remove(1);
      imports.remove(1);

      List<Action> actions = getActions(originalCu, modifiedCu);
      Assert.assertEquals(2, actions.size());

      Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());
      Assert.assertEquals(ActionType.REMOVE, actions.get(1).getType());

      Assert.assertEquals("import java.util.Collection;", ((RemoveAction) actions.get(1)).getText());

      assertCode(actions, code, "package org;\nimport foo.Bar;\nimport foo.Car;\n" + classCode);

   }

   @Test
   public void testAddImports() throws Exception {
      String code = "package org;\npublic class A {}";

      CompilationUnit modifiedCu = parser.parse(code, false);
      CompilationUnit originalCu = parser.parse(code, false);

      List<ImportDeclaration> imports = new LinkedList<ImportDeclaration>();

      imports.add(new ImportDeclaration(new NameExpr("org.walkmod.B"), false, false));
      modifiedCu.setImports(imports);

      List<Action> actions = getActions(originalCu, modifiedCu);
      Assert.assertEquals(1, actions.size());

      Assert.assertEquals(ActionType.APPEND, actions.get(0).getType());
      Assert.assertEquals(2, actions.get(0).getBeginLine());
      Assert.assertEquals(1, actions.get(0).getBeginColumn());

      assertCode(actions, code, "package org;\nimport org.walkmod.B;\npublic class A {}");

      code = "package org;\n@Override\npublic class A {}";
      parser = new DefaultJavaParser();
      modifiedCu = parser.parse(code, false);
      originalCu = parser.parse(code, false);

      imports = new LinkedList<ImportDeclaration>();

      imports.add(new ImportDeclaration(new NameExpr("org.walkmod.B"), false, false));
      modifiedCu.setImports(imports);
      modifiedCu.getTypes().get(0).getAnnotations().add(0, new NormalAnnotationExpr(new NameExpr("Foo"), null));

      actions = getActions(originalCu, modifiedCu);
      Assert.assertEquals(2, actions.size());

      Assert.assertEquals(ActionType.APPEND, actions.get(0).getType());
      Assert.assertEquals(2, actions.get(0).getBeginLine());
      Assert.assertEquals(1, actions.get(0).getBeginColumn());

      assertCode(actions, code, "package org;\nimport org.walkmod.B;\n@Foo()\n@Override\npublic class A {}");

   }

   @Test
   public void testRefactorTypeReference() throws Exception {
      String code = "public class A { Foo b;}";
      CompilationUnit modifiedCu = parser.parse(code, false);
      CompilationUnit originalCu = parser.parse(code, false);

      FieldDeclaration fd = (FieldDeclaration) modifiedCu.getTypes().get(0).getMembers().get(0);
      ClassOrInterfaceType type = (ClassOrInterfaceType) ((ReferenceType) fd.getType()).getType();
      type.setName("B");

      List<Action> actions = getActions(originalCu, modifiedCu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      assertCode(actions, code, "public class A { B b;}");

   }

   @Test
   public void testRefactorMethodCall() throws Exception {
      String code = "public class A { public void foo(){ this.equals(this); }}";

      CompilationUnit modifiedCu = parser.parse(code, false);
      CompilationUnit originalCu = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) modifiedCu.getTypes().get(0).getMembers().get(0);
      ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
      MethodCallExpr expr = (MethodCallExpr) stmt.getExpression();
      expr.setName("different");

      List<Action> actions = getActions(originalCu, modifiedCu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      assertCode(actions, code, "public class A { public void foo(){ this.different(this); }}");

   }

   @Test
   public void testAppendField() throws Exception {
      String code = "public class A {\n private String name;\n}";
      CompilationUnit modifiedCu = parser.parse(code, false);

      String code2 = "public class A {\n private String name;\n}";

      FieldDeclaration fd = (FieldDeclaration) ASTManager.parse(FieldDeclaration.class, "private int age;", true);

      CompilationUnit originalCu = parser.parse(code2, false);
      modifiedCu.getTypes().get(0).getMembers().add(fd);
      List<Action> actions = getActions(originalCu, modifiedCu);

      // 37
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(1, actions.get(0).getBeginColumn());
      Assert.assertEquals(ActionType.APPEND, actions.get(0).getType());
      assertCode(actions, code2, "public class A {\n private String name;\n private int age;\n}");
   }

   @Test
   public void testMultipleAppendsAsFields() throws Exception {
      String code = "public class A {\n private String name;\n}";
      String code2 = "public class A {\n private String name;\n}";

      CompilationUnit modifiedCu = parser.parse(code, false);
      FieldDeclaration fd = (FieldDeclaration) ASTManager.parse(FieldDeclaration.class, "private int age;", true);

      CompilationUnit originalCu = parser.parse(code2, false);
      modifiedCu.getTypes().get(0).getMembers().add(fd);
      // multiple appends
      fd = (FieldDeclaration) ASTManager.parse(FieldDeclaration.class, "private String surname;", true);
      modifiedCu.getTypes().get(0).getMembers().add(fd);

      List<Action> actions = getActions(originalCu, modifiedCu);

      Assert.assertEquals(2, actions.size());
      //Assert.assertEquals(1, actions.get(1).getBeginColumn());
      Assert.assertEquals(ActionType.APPEND, actions.get(1).getType());
      assertCode(actions, code2,
            "public class A {\n private String name;\n private int age;\n private String surname;\n}");

   }

   @Test
   public void testAppendJavadoc() throws Exception {

      String code = "public class A {\n private String name;\n}";
      CompilationUnit modifiedCu = parser.parse(code, false);
      modifiedCu.getTypes().get(0).getMembers().get(0).setJavaDoc(new JavadocComment("javadoc"));
      CompilationUnit originalCu = parser.parse(code, false);
      List<Action> actions = getActions(originalCu, modifiedCu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.APPEND, actions.get(0).getType());
      ActionsApplier applier = new ActionsApplier();
      applier.setActionList(actions);
      applier.setText(code);
      applier.execute();
      String result = applier.getModifiedText();
      Assert.assertTrue(result.contains("javadoc"));
   }

   @Test
   public void testMethodRefactor() throws Exception {
      String code = "public interface A { public String getName();}";
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      md.setName("getName_");

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      ReplaceAction action = (ReplaceAction) actions.get(0);
      Assert.assertEquals("public String getName_();", action.getNewText());

      assertCode(actions, code, "public interface A { public String getName_();}");
   }

   @Test
   public void testMethodRefactorWithInnerComments() throws Exception {
      String code = "public class A { public String getName(){ /*comment*/ }}";
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      md.setName("getName_");

      List<Action> actions = getActions(cu2, cu);

      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      ReplaceAction action = (ReplaceAction) actions.get(0);
      Assert.assertTrue(action.getNewText().contains("/*comment*/"));
   }

   @Test
   public void testEnumEntryRefactor() throws Exception {
      String code = "public enum A { FOO, BAR, }";
      CompilationUnit cu = parser.parse(code, false);
      String code2 = "public enum A { FOO, BAR, }";
      CompilationUnit cu2 = parser.parse(code2, false);
      EnumDeclaration ed = (EnumDeclaration) cu.getTypes().get(0);
      ed.getEntries().get(1).setName("FOO2");

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(22, actions.get(0).getBeginColumn());
      Assert.assertEquals(22 + "BAR".length() - 1, actions.get(0).getEndColumn());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      assertCode(actions, code2, "public enum A { FOO, FOO2, }");

   }

   @Test
   public void testIndentationChar() {
      ActionsApplier applier = new ActionsApplier();
      String code = "public class B { private int name;}";
      applier.inferIndentationChar(code.toCharArray());
      Assert.assertEquals(new Character(' '), applier.getIndentationChar());

      applier = new ActionsApplier();
      code = "public class B {\tprivate int name;}";
      applier.inferIndentationChar(code.toCharArray());
      Assert.assertEquals(new Character('\t'), applier.getIndentationChar());

      applier = new ActionsApplier();
      code = "public class B {\n\tprivate int name;}";
      applier.inferIndentationChar(code.toCharArray());
      Assert.assertEquals(new Character('\t'), applier.getIndentationChar());

      applier = new ActionsApplier();
      code = "public class B {\n private int name;}";
      applier.inferIndentationChar(code.toCharArray());
      Assert.assertEquals(new Character(' '), applier.getIndentationChar());

      applier = new ActionsApplier();
      code = "public class B {}";
      applier.inferIndentationChar(code.toCharArray());
      Assert.assertEquals(new Character('\0'), applier.getIndentationChar());
   }

   @Test
   public void testClassOrInterface() throws ParseException {
      String code = "public class B { private int name;}";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);
      List<ClassOrInterfaceType> implementsList = new LinkedList<ClassOrInterfaceType>();
      implementsList.add(new ClassOrInterfaceType("Serializable"));
      type.setImplements(implementsList);

      List<ImportDeclaration> imports = new LinkedList<ImportDeclaration>();
      imports.add(new ImportDeclaration(new NameExpr("java.io.Serializable"), false, false));
      cu.setImports(imports);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(2, actions.size());
   }

   @Test
   public void testAddLicense() throws ParseException {
      String code = "public class B { private int name;}";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);
      List<Comment> comments = new LinkedList<Comment>();
      comments.add(new BlockComment("license"));
      cu.setComments(comments);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "/*license*/\npublic class B { private int name;}");

   }

   @Test
   public void testInnerComments() throws ParseException {
      String code = "public class B { private /*final*/ int name;}";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      FieldDeclaration fd = (FieldDeclaration) cu.getTypes().get(0).getMembers().get(0);
      List<VariableDeclarator> vds = fd.getVariables();
      vds.add(new VariableDeclarator(new VariableDeclaratorId("surname")));
      Comment c = cu.getComments().get(0);
      c.setContent("static");

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "public class B { /*static*/private int name, surname;}");

   }

   @Test
   public void testChangeTextOfInnerComments() throws ParseException {
      String code = "public class B { private /*final*/ int name;}";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);
      Comment c = cu.getComments().get(0);
      c.setContent("static");

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "public class B { private /*static*/ int name;}");

   }

   @Test
   public void testAddParameter() throws ParseException {
      String code = "public class B { public void foo(){} }";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      List<Parameter> params = new LinkedList<Parameter>();
      params.add(new Parameter(new PrimitiveType(Primitive.Int), new VariableDeclaratorId("p")));
      md.setParameters(params);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "public class B { public void foo(int p) {} }");
   }

   @Test
   public void testModifyModifers() throws ParseException {
      String code = "public class B { public String foo; }";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      FieldDeclaration md = (FieldDeclaration) cu.getTypes().get(0).getMembers().get(0);
      md.setModifiers(ModifierSet.PRIVATE);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "public class B { private String foo; }");
   }

   @Test
   public void testModifyModifers2() throws ParseException {
      String code = "public class B {\n    public String foo;\n}";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      FieldDeclaration md = (FieldDeclaration) cu.getTypes().get(0).getMembers().get(0);
      md.setModifiers(ModifierSet.PRIVATE);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "public class B {\n    private String foo;\n}");
   }

   @Test
   public void testAddAnnotation() throws ParseException {
      String code = "public class B { public void foo(){} }";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);
      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

      List<AnnotationExpr> annotations = new LinkedList<AnnotationExpr>();
      annotations.add(new MarkerAnnotationExpr(new NameExpr("Override")));
      md.setAnnotations(annotations);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "public class B { @Override public void foo(){} }");

      code = "public class B {\n  public void foo(){\n  }\n }";

      cu = parser.parse(code, false);
      cu2 = parser.parse(code, false);
      md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

      annotations = new LinkedList<AnnotationExpr>();
      annotations.add(new MarkerAnnotationExpr(new NameExpr("Override")));
      md.setAnnotations(annotations);

      actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "public class B {\n  @Override\n  public void foo(){\n  }\n }");

   }

   @Test
   public void testAddAnnotation2() throws ParseException {
      String code = "public class B {\n\t public void foo(){\t\teo();\t\tbar();\n} }";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);
      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

      List<AnnotationExpr> annotations = new LinkedList<AnnotationExpr>();
      annotations.add(new MarkerAnnotationExpr(new NameExpr("Override")));
      md.setAnnotations(annotations);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "public class B {\n\t @Override\n\t public void foo(){\t\teo();\t\tbar();\n} }");
   }

   @Test
   public void testAddAnnotationInInnerClass() throws ParseException {
      String code = "public class Foo {\n\tpublic void aux(){\n\t\t new X(){\n\t\t\tvoid hello(){\n\t\t\t\ty();\n\t\t\t}\n\t\t};\n\t}\n}";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);
      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      List<Statement> stmts = md.getBody().getStmts();
      ExpressionStmt stmt = (ExpressionStmt) stmts.get(0);
      ObjectCreationExpr oce = (ObjectCreationExpr) stmt.getExpression();

      List<BodyDeclaration> body = oce.getAnonymousClassBody();

      MethodDeclaration md2 = (MethodDeclaration) body.get(0);
      List<AnnotationExpr> annotations = new LinkedList<AnnotationExpr>();
      annotations.add(new MarkerAnnotationExpr(new NameExpr("Override")));
      md2.setAnnotations(annotations);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code,
            "public class Foo {\n\tpublic void aux(){\n\t\t new X(){\n\t\t\t@Override\n\t\t\tvoid hello(){\n\t\t\t\ty();\n\t\t\t}\n\t\t};\n\t}\n}");

   }

   @Test
   public void testTypeParameters() throws ParseException {
      String code = "public class B<C> { public void foo(){} }";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      ClassOrInterfaceDeclaration dec = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);

      List<TypeParameter> types = dec.getTypeParameters();
      types.add(new TypeParameter("D", null));
      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      assertCode(actions, code, "public class B<C, D> {public void foo() {}}");

      cu = parser.parse(code, false);
      dec = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);
      types = dec.getTypeParameters();
      types.remove(0);
      types.add(new TypeParameter("D", null));

      actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      assertCode(actions, code, "public class B<D> {public void foo() {}}");

   }

   @Test
   public void testArgumentList() throws ParseException {
      String code = "public class B{ public void foo(){ print(3); } }";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      ExpressionStmt stmt = (ExpressionStmt) md.getBody().getStmts().get(0);
      MethodCallExpr expr = (MethodCallExpr) stmt.getExpression();
      expr.getArgs().add(new IntegerLiteralExpr("4"));

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

      assertCode(actions, code, "public class B{ public void foo(){ print(3, 4); } }");

   }

   private void assertCode(List<Action> actions, String code, String expected) {
      ActionsApplier applier = new ActionsApplier();
      applier.setActionList(actions);
      applier.setText(code);
      applier.execute();
      String result = applier.getModifiedText();
      Assert.assertEquals(expected, result);
   }

   @Test
   public void testIndentationSize() throws ParseException {
      String code = "public class A{\n\tpublic void foo(){\n\t\tprint(3);\n }}";
      DefaultJavaParser parser = new DefaultJavaParser();

      CompilationUnit cu = parser.parse(code, false);
      ChangeLogVisitor visitor = new ChangeLogVisitor();
      TypeDeclaration td = cu.getTypes().get(0);
      int indentation = visitor.inferIndentationSize(td, td.getMembers());
      Assert.assertEquals(1, indentation);
   }

   @Test
   public void testChangesOnStmts() throws ParseException {
      String code = "public class Bar{\n    public void foo() {\n        String s = null;\n        names.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());\n    }\n}";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu2.getTypes().get(0).getMembers().get(0);
      List<Statement> stmts = md.getBody().getStmts();
      stmts.remove(0);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());

      assertCode(actions, code,
            "public class Bar{\n    public void foo() {\n        names.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());\n    }\n}");
   }

   @Test
   public void testRemovesAndUpdatesOnStmts() throws Exception {
      String code = "public class Foo{\n\tpublic void foo() {\n\t\tif(a){\n\t\t\tif(b){\n\t\t\t\ti++;\n\t\t\t}\n\t\t}\n\t}\n}";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);
      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      List<Statement> stmts = md.getBody().getStmts();

      IfStmt ifStmt = (IfStmt) stmts.get(0);
      BlockStmt thenBlock = (BlockStmt) ifStmt.getThenStmt();
      IfStmt thenStmt = (IfStmt) thenBlock.getStmts().get(0);

      Expression condition = thenStmt.getCondition().clone();
      BinaryExpr be = new BinaryExpr(ifStmt.getCondition().clone(), condition, BinaryExpr.Operator.and);
      ifStmt.setCondition(be);
      ifStmt.setThenStmt(thenStmt.getThenStmt().clone());
      thenStmt.remove();

      List<Action> actions = getActions(cu2, cu);
      assertCode(actions, code, "public class Foo{\n\tpublic void foo() {\n\t\tif(a && b){\n\t\t\ti++;\n\t\t}\n\t}\n}");
   }

   @Test
   public void testRemoveJavadoc() throws ParseException {
      String code = "public class B { /**\n* This class is awesome**/\npublic void foo(){} }";
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      md.setJavaDoc(null);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "public class B { public void foo(){} }");
   }

   @Test
   public void testAddBlockStmtInSwitch() throws ParseException {
      String code = "public class B {\n\tpublic void foo(int val){\n\t\tswitch(val){\n\t\t\tcase 1:\n\t\t\t\tSystem.out.println(val);\n\t\t}\n\t}\n}";

      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

      SwitchStmt stmt = (SwitchStmt) md.getBody().getStmts().get(0);

      List<SwitchEntryStmt> entries = stmt.getEntries();
      List<Statement> stmts = new LinkedList<Statement>();

      stmts.add(new BlockStmt(entries.get(0).getStmts()));
      entries.get(0).setStmts(stmts);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code,
            "public class B {\n\tpublic void foo(int val){\n\t\tswitch(val){\n\t\t\tcase 1:\n\t\t\t\t{\n\t\t\t\t\tSystem.out.println(val);\n\t\t\t\t}\n\t\t}\n\t}\n}");
   }

   @Test
   public void testAddBlockStmtInSwitch2() throws ParseException {
      String code = "public class B {\n\tpublic void foo(int val){\n\t\tswitch(val){\n\t\t\tcase 1:\n\t\t\t\tSystem.out.println(val);\n\t\t\t\tSystem.out.println(val);\n\t\t\t\tbreak;\n\t\t}\n\t}\n}";

      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

      SwitchStmt stmt = (SwitchStmt) md.getBody().getStmts().get(0);

      List<SwitchEntryStmt> entries = stmt.getEntries();
      List<Statement> stmts = new LinkedList<Statement>();

      stmts.add(new BlockStmt(entries.get(0).getStmts()));
      entries.get(0).setStmts(stmts);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(3, actions.size());

      assertCode(actions, code,
            "public class B {\n\tpublic void foo(int val){\n\t\tswitch(val){\n\t\t\tcase 1:\n\t\t\t\t{\n\t\t\t\t\tSystem.out.println(val);\n\t\t\t\t\tSystem.out.println(val);\n\t\t\t\t\tbreak;\n\t\t\t\t}\n\t\t}\n\t}\n}");
   }

   @Test
   public void testAddBlockStmtInDefaultSwitch() throws ParseException {
      String code = "public class B {\n\tpublic void foo(int val){\n\t\tswitch(val){\n\t\t\tdefault:\n\t\t\t\tSystem.out.println(val);\n\t\t}\n\t}\n}";

      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

      SwitchStmt stmt = (SwitchStmt) md.getBody().getStmts().get(0);

      List<SwitchEntryStmt> entries = stmt.getEntries();
      List<Statement> stmts = new LinkedList<Statement>();

      stmts.add(new BlockStmt(entries.get(0).getStmts()));
      entries.get(0).setStmts(stmts);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code,
            "public class B {\n\tpublic void foo(int val){\n\t\tswitch(val){\n\t\t\tdefault:\n\t\t\t\t{\n\t\t\t\t\tSystem.out.println(val);\n\t\t\t\t}\n\t\t}\n\t}\n}");
   }

   @Test
   public void testRemoveParenthesesReturn() throws ParseException {
      String code = "public class B{ public boolean bar() { return(true); }}";

      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

      ReturnStmt stmt = (ReturnStmt) md.getBody().getStmts().get(0);

      EnclosedExpr expr = (EnclosedExpr) stmt.getExpr();

      stmt.replaceChildNode(expr, expr.getInner());

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());

      assertCode(actions, code, "public class B{ public boolean bar() { return true; }}");

   }

   @Test
   public void testSwitchBlockWithMultipleIndentationChars() throws ParseException {
      String code = "public class B{\n    public boolean bar() { \n        return true;\n    }\n\tpublic void bar(int x) {\n\t\tswitch(x) {\n\t\t\tcase 1:\n\t\t\t\tbreak;\n\t\t}\n\t}\n}";

      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
      BlockStmt blockStmt = md.getBody();
      SwitchStmt switchStmt = (SwitchStmt) blockStmt.getStmts().get(0);
      SwitchEntryStmt entry = (SwitchEntryStmt) switchStmt.getEntries().get(0);

      entry.replaceChildNode(entry.getStmts().get(0), new BlockStmt(new LinkedList<Statement>(entry.getStmts())));

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      assertCode(actions, code,
            "public class B{\n    public boolean bar() { \n        return true;\n    }\n\tpublic void bar(int x) {\n\t\tswitch(x) {\n\t\t\tcase 1:\n\t\t\t\t{\n\t\t\t\t\tbreak;\n\t\t\t\t}\n\t\t}\n\t}\n}");

   }

   @Test
   public void testSwitchBlockWithNonIndentionFragments() throws ParseException {
      String code = "public class B{\n    public boolean bar() { \n        return true;\n    }\n\tpublic void bar(int x) {\n\t\tswitch(x) {\n\t\tcase 1:\n\t\t\tbreak;\n\t\t}\n\t}\n}";

      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(1);
      BlockStmt blockStmt = md.getBody();
      SwitchStmt switchStmt = (SwitchStmt) blockStmt.getStmts().get(0);
      SwitchEntryStmt entry = (SwitchEntryStmt) switchStmt.getEntries().get(0);

      entry.replaceChildNode(entry.getStmts().get(0), new BlockStmt(new LinkedList<Statement>(entry.getStmts())));

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      assertCode(actions, code,
            "public class B{\n    public boolean bar() { \n        return true;\n    }\n\tpublic void bar(int x) {\n\t\tswitch(x) {\n\t\tcase 1:\n\t\t\t{\n\t\t\t\tbreak;\n\t\t\t}\n\t\t}\n\t}\n}");

   }

   @Test
   public void testLastIndentationCharIsForTheIndentationSize() throws ParseException {
      String code = "public class B{\n    @SuppressWarnings(\"PMD.MissingBreakInSwitch\")\n    @Override\n    public AxisIterator iterateAxis(byte axisNumber) {\n\tswitch (axisNumber) {\n\tcase Axis.ANCESTOR:\n\t    return null;\n\t}\n\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t    }\n}";

      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      BlockStmt blockStmt = md.getBody();
      SwitchStmt switchStmt = (SwitchStmt) blockStmt.getStmts().get(0);
      SwitchEntryStmt entry = (SwitchEntryStmt) switchStmt.getEntries().get(0);

      entry.replaceChildNode(entry.getStmts().get(0), new BlockStmt(new LinkedList<Statement>(entry.getStmts())));

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      
      assertCode(actions, code,
            "public class B{\n    @SuppressWarnings(\"PMD.MissingBreakInSwitch\")\n    @Override\n    public AxisIterator iterateAxis(byte axisNumber) {\n\tswitch (axisNumber) {\n\tcase Axis.ANCESTOR:\n\t    {\n\t        return null;\n\t    }\n\t}\n\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t    }\n}");

   }
   
   @Test
   public void testAddDefaultSwitchCase() throws ParseException{
      
      String code = "public class B{\n    public void foo(int x) {\n        switch(x){\n            case 1:\n                break;\n        }\n    }\n}";
   
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      BlockStmt blockStmt = md.getBody();
      SwitchStmt switchStmt = (SwitchStmt) blockStmt.getStmts().get(0);
      
      SwitchEntryStmt entry = new SwitchEntryStmt();
      List<Statement> stmts = new LinkedList<Statement>();
      stmts.add(new BreakStmt());
      
      entry.setStmts(stmts);
      switchStmt.getEntries().add(entry);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      
   
      assertCode(actions, code,
            "public class B{\n    public void foo(int x) {\n        switch(x){\n            case 1:\n                break;\n            default:\n                break;\n        }\n    }\n}");
      

   }

   @Test
   public void testAddDefaultSwitchCaseWithWrongIndent() throws ParseException{
      
      String code = "public class B{\n    public void foo(int x) {\n        switch(x){\n        case 1:\n            break;\n        }\n    }\n}";
   
      DefaultJavaParser parser = new DefaultJavaParser();
      CompilationUnit cu = parser.parse(code, false);
      CompilationUnit cu2 = parser.parse(code, false);

      MethodDeclaration md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);
      BlockStmt blockStmt = md.getBody();
      SwitchStmt switchStmt = (SwitchStmt) blockStmt.getStmts().get(0);
      
      SwitchEntryStmt entry = new SwitchEntryStmt();
      List<Statement> stmts = new LinkedList<Statement>();
      stmts.add(new BreakStmt());
      
      entry.setStmts(stmts);
      List<SwitchEntryStmt> entries = switchStmt.getEntries();
      entries.add(entry);
      switchStmt.setEntries(entries);

      List<Action> actions = getActions(cu2, cu);
      Assert.assertEquals(1, actions.size());
      
   
      assertCode(actions, code,
            "public class B{\n    public void foo(int x) {\n        switch(x){\n        case 1:\n            break;\n            default:\n                break;\n        }\n    }\n}");
      

   }
}
