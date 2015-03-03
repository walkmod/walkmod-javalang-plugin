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
import org.walkmod.javalang.ast.body.FieldDeclaration;
import org.walkmod.javalang.ast.body.JavadocComment;
import org.walkmod.javalang.ast.body.MethodDeclaration;
import org.walkmod.javalang.ast.expr.MethodCallExpr;
import org.walkmod.javalang.ast.stmt.ExpressionStmt;
import org.walkmod.javalang.ast.type.ClassOrInterfaceType;
import org.walkmod.javalang.ast.type.ReferenceType;
import org.walkmod.javalang.walkers.ChangeLogVisitor;
import org.walkmod.javalang.walkers.DefaultJavaParser;
import org.walkmod.walkers.VisitorContext;

public class TestActionsInferred {
	
	@Test
	public void testNoActions() throws Exception{
		String code = "public class A { private int name;}";
		DefaultJavaParser parser = new DefaultJavaParser();
		
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
	public void testRemoveActions() throws Exception{
		String code = "public class A { private String name;}";
		DefaultJavaParser parser = new DefaultJavaParser();
		
		CompilationUnit cu = parser.parse(code, false);
		
		String code2 = "public class A { private String name; private int age;}";
		CompilationUnit cu2 = parser.parse(code2, false);
		
		
		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu);
		visitor.visit((CompilationUnit) cu2, ctx);
		List<Action> actions = visitor.getActionsToApply();
		
		//39-54
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(39, actions.get(0).getBeginColumn());
		Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());
		
		ActionsApplier applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code2);
		applier.execute();
		
		Assert.assertEquals("public class A { private String name;                 }", applier.getModifiedText());
		
		code2 = "public class A { private String name; private void print(){ System.out.println(\"hello\");}}";
		cu2 = parser.parse(code2, false);
		
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu);
		visitor.visit((CompilationUnit) cu2, ctx);
		actions = visitor.getActionsToApply();
		//39-89
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(39, actions.get(0).getBeginColumn());
		Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());
		
		applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code2);
		applier.execute();
		
		String expectedReult = "public class A { private String name;                                                    }";
		Assert.assertEquals(expectedReult, applier.getModifiedText());
		
		
		//Javadoc
		code2 = "public class A { private String name; /**age**/ private int age;}";
		cu2 = parser.parse(code2, false);
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu);
		visitor.visit((CompilationUnit) cu2, ctx);
		actions = visitor.getActionsToApply();
	
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(code2.indexOf("/**age**/")+1, actions.get(0).getBeginColumn());
		Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());
		
		applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code2);
		applier.execute();
		
		expectedReult = "public class A { private String name;                           }";
		Assert.assertEquals(expectedReult, applier.getModifiedText());
	
		//inner comments
		code2 = "public class A { private String name;  private void print(){ /*comment*/ System.out.println(\"hello\");}}";
		cu2 = parser.parse(code2, false);
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu);
		visitor.visit((CompilationUnit) cu2, ctx);
		actions = visitor.getActionsToApply();
	
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(code2.indexOf("private void print()")+1, actions.get(0).getBeginColumn());
		Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());
		
		applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code2);
		applier.execute();
		expectedReult = "public class A { private String name;                                                                 }";
		Assert.assertEquals(expectedReult, applier.getModifiedText());
		
		
		code = "public class A{ static{ \n  int i = 1 /*first comment*/;\n  i++; }}";
		cu =  parser.parse(code, false);
		
		code2 = "public class A{ static{ \n                              \n  i++; }}";
		cu2 = parser.parse(code2, false);
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu);
		visitor.visit((CompilationUnit) cu2, ctx);
		actions = visitor.getActionsToApply();
	
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(3, actions.get(0).getBeginColumn());
		Assert.assertEquals("int i = 1 /*first comment*/".length()+3, actions.get(0).getEndColumn());
		Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());
		
		applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code2);
		applier.execute();
		
		Assert.assertEquals(code2, applier.getModifiedText());
		//remove children elements with char separator
		
		code =  "public enum A { FOO,      }";
		cu = parser.parse(code, false);
		code2 = "public enum A { FOO, BAR, }";
		cu2 = parser.parse(code2, false);
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(1, actions.get(0).getBeginColumn());
		Assert.assertEquals(code2.length(), actions.get(0).getEndColumn());
		Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());
		
		applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code2);
		applier.execute();                                                             
		Assert.assertEquals("public enum A {FOO}     ", applier.getModifiedText());
		
		//remove multiple elements
		code =  "public class A {                                      }";
		code2 = "public class A { private String name; private int age;}";
		cu = parser.parse(code, false);
		cu2 = parser.parse(code2, false);
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		actions = visitor.getActionsToApply();
		Assert.assertEquals(2, actions.size());
		
		Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());
		Assert.assertEquals(ActionType.REMOVE, actions.get(1).getType());
		applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code2);
		applier.execute();                                                             
		Assert.assertEquals(code, applier.getModifiedText());
	}
	
	@Test
	public void testRemoveImports() throws Exception{
		String code = "package org;\nimport foo.Bar;\nimport java.util.List;\nimport java.util.Collection;\nimport foo.Car;\n";
		String classCode = "public class A{\n public void foo(){\n }\n}";
		code+= classCode;
		
		DefaultJavaParser parser = new DefaultJavaParser();
		CompilationUnit cu = parser.parse(code, false);
		CompilationUnit cu2 = parser.parse(code, false);
		
		List<ImportDeclaration> imports = cu.getImports();
		imports.remove(1);
		imports.remove(1);
		
		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
		Assert.assertEquals(2, actions.size());
		
		Assert.assertEquals(ActionType.REMOVE, actions.get(0).getType());
		Assert.assertEquals(ActionType.REMOVE, actions.get(1).getType());
		
		Assert.assertEquals("import java.util.Collection;", ((RemoveAction)actions.get(1)).getText());
		ActionsApplier applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code);
		applier.execute();
		String result = "package org;\nimport foo.Bar;\n                      \n                            \nimport foo.Car;\n";
		Assert.assertEquals(result+classCode, applier.getModifiedText());
		
	}
	
	@Test
	public void testRefactorTypeReference() throws Exception{
		String code ="public class A { Foo b;}";
		DefaultJavaParser parser = new DefaultJavaParser();
		CompilationUnit cu = parser.parse(code, false);
		CompilationUnit cu2 = parser.parse(code, false);
		
		FieldDeclaration fd = (FieldDeclaration)cu.getTypes().get(0).getMembers().get(0);
		ClassOrInterfaceType type = (ClassOrInterfaceType) ((ReferenceType)fd.getType()).getType();
		type.setName("B");
		
		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());
		ActionsApplier applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code);
		applier.execute();
		Assert.assertEquals("public class A { B   b;}", applier.getModifiedText());
	}
	
	@Test
	public void testRefactorMethodCall() throws Exception{
		String code ="public class A { public void foo(){ this.equals(this); }}";
		
		DefaultJavaParser parser = new DefaultJavaParser();
		CompilationUnit cu = parser.parse(code, false);
		CompilationUnit cu2 = parser.parse(code, false);
		
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(0);
		ExpressionStmt stmt = (ExpressionStmt)md.getBody().getStmts().get(0);
		MethodCallExpr expr = (MethodCallExpr)stmt.getExpression();
		expr.setName("different");
		
		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());
		
		ActionsApplier applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code);
		applier.execute();
		Assert.assertEquals("public class A { public void foo(){ this.different(this); }}", applier.getModifiedText());
	}
	
	
	@Test
	public void testAppendActions() throws Exception{
		String code = "public class A {\n private String name;\n}";
		DefaultJavaParser parser = new DefaultJavaParser();
		
		CompilationUnit cu = parser.parse(code, false);
		
		String code2 = "public class A {\n private String name;\n}";
		
		FieldDeclaration fd = (FieldDeclaration) ASTManager.parse(
				FieldDeclaration.class, "private int age;", true);
		
		CompilationUnit cu2 = parser.parse(code2, false);
		cu.getTypes().get(0).getMembers().add(fd);
		
		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
		
		//37
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(1, actions.get(0).getBeginColumn());
		Assert.assertEquals(ActionType.APPEND, actions.get(0).getType());
		
		ActionsApplier applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code2);
		applier.execute();
		
		String result = applier.getModifiedText();
		Assert.assertEquals("public class A {\n private String name;\n private int age;\n}",result);
		
		//multiple appends
		fd = (FieldDeclaration) ASTManager.parse(
				FieldDeclaration.class, "private String surname;", true);
		cu.getTypes().get(0).getMembers().add(fd);
		
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		actions = visitor.getActionsToApply();
		
		Assert.assertEquals(2, actions.size());
		Assert.assertEquals(1, actions.get(1).getBeginColumn());
		Assert.assertEquals(ActionType.APPEND, actions.get(1).getType());
		
		applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code2);
		applier.execute();
		
		result = applier.getModifiedText();
		Assert.assertEquals("public class A {\n private String name;\n private int age;\n private String surname;\n}",result);
	
		//append a unique object: example: javadoc
		code = "public class A {\n private String name;\n}";
		cu = parser.parse(code, false);
		cu.getTypes().get(0).getMembers().get(0).setJavaDoc(new JavadocComment("javadoc"));
		cu2 = parser.parse(code, false);
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());
		applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code);
		applier.execute();
		
		result = applier.getModifiedText();
		Assert.assertTrue(result.contains("javadoc"));
		
		//test append license
		code = "public class A {\n private String name;\n}";
		cu = parser.parse(code, false);
		LinkedList<Comment> comments = new LinkedList<Comment>();
		comments.add(new BlockComment("license"));
		cu.setComments(comments);
		cu2 = parser.parse(code, false);
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(ActionType.APPEND, actions.get(0).getType());
		applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code);
		applier.execute();
		result = applier.getModifiedText();
		Assert.assertTrue(result.contains("license"));
		
	}
	
	@Test
	public void testReplaceActions() throws Exception{
		String code = "public interface A { public String getName();}";
		DefaultJavaParser parser = new DefaultJavaParser();
		
		CompilationUnit cu = parser.parse(code, false);
		CompilationUnit cu2 = parser.parse(code, false);
		
		MethodDeclaration md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(0);
		md.setName("getName_");
		
		ChangeLogVisitor visitor = new ChangeLogVisitor();
		VisitorContext ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		List<Action> actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());
		
		ReplaceAction action = (ReplaceAction)actions.get(0);
		Assert.assertEquals("public String getName_();", action.getNewText());
		
		ActionsApplier applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code);
		applier.execute();
		String result = applier.getModifiedText();
		Assert.assertEquals("public interface A { public String getName_();}", result);
		
		//modifications into a set of elements with , separators
		
		code =  "public enum A { FOO, FOO2 }";
		cu = parser.parse(code, false);
		String code2 = "public enum A { FOO, BAR, }";
		cu2 = parser.parse(code2, false);
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(22, actions.get(0).getBeginColumn());
		Assert.assertEquals(22+"BAR".length()-1, actions.get(0).getEndColumn());
		Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());
		
		applier = new ActionsApplier();
		applier.setActionList(actions);
		applier.setText(code2);
		applier.execute();                                                             
		Assert.assertEquals("public enum A { FOO, FOO2, }", applier.getModifiedText());
		
		//test comments
		code = "public class A { public String getName(){ /*comment*/ }}";
		
		parser = new DefaultJavaParser();
		
		cu = parser.parse(code, false);
		cu2 = parser.parse(code, false);
		
		md = (MethodDeclaration)cu.getTypes().get(0).getMembers().get(0);
		md.setName("getName_");
		
		visitor = new ChangeLogVisitor();
		ctx = new VisitorContext();
		ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, cu2);
		visitor.visit((CompilationUnit) cu, ctx);
		actions = visitor.getActionsToApply();
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals(ActionType.REPLACE, actions.get(0).getType());
		
		action = (ReplaceAction)actions.get(0);
		Assert.assertTrue(action.getNewText().contains("/*comment*/"));
	}
	
	
	
	@Test
	public void testIndentationChar(){
		ActionsApplier applier = new ActionsApplier();
		String code ="public class B { private int name;}";
		applier.inferIndentationChar(code.toCharArray());
		Assert.assertEquals(new Character(' '), applier.getIndentationChar());
		
		applier = new ActionsApplier();
		code ="public class B {\tprivate int name;}";
		applier.inferIndentationChar(code.toCharArray());
		Assert.assertEquals(new Character('\t'), applier.getIndentationChar());
		
		applier = new ActionsApplier();
		code ="public class B {\n\tprivate int name;}";
		applier.inferIndentationChar(code.toCharArray());
		Assert.assertEquals(new Character('\t'), applier.getIndentationChar());
		
		applier = new ActionsApplier();
		code ="public class B {\n private int name;}";
		applier.inferIndentationChar(code.toCharArray());
		Assert.assertEquals(new Character(' '), applier.getIndentationChar());
		
		applier = new ActionsApplier();
		code ="public class B {}";
		applier.inferIndentationChar(code.toCharArray());
		Assert.assertEquals(new Character('\0'), applier.getIndentationChar());
	}

}
