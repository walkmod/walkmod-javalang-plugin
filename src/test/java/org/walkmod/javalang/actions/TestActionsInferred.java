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
import org.walkmod.javalang.ast.body.ClassOrInterfaceDeclaration;
import org.walkmod.javalang.ast.body.EnumDeclaration;
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.JavadocComment;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.body.VariableDeclaratorId;
import org.walkmod.javalang.ast.expr.AnnotationExpr;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;
import org.walkmod.javalang.ast.expr.MarkerAnnotationExpr;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.NormalAnnotationExpr;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
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

		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu);
		visitor.visit((CompilationUnit) cu2, ctx);
		List<Action> actions = visitor.getActionsToApply();
		Assert.assertTrue(actions.isEmpty());
	}

	@Test
	public void testNoActionsWithEmptyDeclaration() throws Exception {
		String code = "package at.foo.bar.annotations; " + "import java.lang.annotation.ElementType; "
				+ "import java.lang.annotation.Retention; " + "import java.lang.annotation.RetentionPolicy; "
				+ "import java.lang.annotation.Target;" + " public @interface Exportable { enum Type { A, B;};}";

		CompilationUnit cu = parser.parse(code, false);

		CompilationUnit cu2 = parser.parse(code, false);

		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu);
		visitor.visit((CompilationUnit) cu2, ctx);
		List<Action> actions = visitor.getActionsToApply();
		Assert.assertTrue(actions.isEmpty());
	}

	private List<Action> getActions(CompilationUnit original, CompilationUnit modified) {
		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, original);
		visitor.visit((CompilationUnit) modified, ctx);
		List<Action> actions = visitor.getActionsToApply();
		return actions;
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

		assertCode(actions, original, "public class A{ static{ \n  \n  i++; }}");
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

		assertCode(actions, original, "public enum A {FOO}     ");
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

		assertCode(actions, code, "package org;\nimport foo.Bar;\n\n\nimport foo.Car;\n" + classCode);

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
		modifiedCu.getTypes().get(0).getAnnotations().add(new NormalAnnotationExpr(new NameExpr("Foo"), null));

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

		assertCode(actions, code, "public class A { B   b;}");

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
		Assert.assertEquals(1, actions.get(1).getBeginColumn());
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

		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
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

		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
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

		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
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

		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
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

		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());

		assertCode(actions, code, "public class B { public void foo(int p) {} }");
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

		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());

		assertCode(actions, code, "public class B { @Override public void foo(){} }");

		code = "public class B {\n  public void foo(){\n  }\n }";

		cu = parser.parse(code, false);
		cu2 = parser.parse(code, false);
		md = (MethodDeclaration) cu.getTypes().get(0).getMembers().get(0);

		annotations = new LinkedList<AnnotationExpr>();
		annotations.add(new MarkerAnnotationExpr(new NameExpr("Override")));
		md.setAnnotations(annotations);

		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());

		assertCode(actions, code, "public class B {\n  @Override public void foo(){\n  }\n }");

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
		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());

		assertCode(actions, code, "public class B<C, D> {public void foo() {}}");

		cu = parser.parse(code, false);
		dec = (ClassOrInterfaceDeclaration) cu.getTypes().get(0);
		types = dec.getTypeParameters();
		types.remove(0);
		types.add(new TypeParameter("D", null));
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);

		actions = visitor.getActionsToApply();
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

		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
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

}
