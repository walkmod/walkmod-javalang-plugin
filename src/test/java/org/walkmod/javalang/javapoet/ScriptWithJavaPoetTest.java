package org.walkmod.javalang.javapoet;


import org.junit.Assert;
import org.junit.Test;
import org.springframework.jndi.support.SimpleJndiBeanFactory;
import org.walkmod.conf.entities.impl.ChainConfigImpl;
import org.walkmod.conf.entities.impl.ConfigurationImpl;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.scripting.ScriptProcessor;
import org.walkmod.walkers.VisitorContext;


/**
 * Created by raquelpau on 27/3/17.
 */
public class ScriptWithJavaPoetTest {

    @Test
    public void testJavaPoetResult() throws Exception{
        ScriptProcessor processor = new ScriptProcessor();
        CompilationUnit unit = ASTManager.parse("public class Person{}");

        ChainConfigImpl dummy = new ChainConfigImpl();
        dummy.setConfiguration(new ConfigurationImpl());
        dummy.getConfiguration().setBeanFactory(new SimpleJndiBeanFactory());
        VisitorContext vc = new VisitorContext(dummy);

        processor.setContent(
                "import com.squareup.javapoet.*; " +
                "import javax.lang.model.element.Modifier; " +
                "import org.walkmod.javalang.ASTManager; "+
                "MethodSpec main = MethodSpec.methodBuilder(\"main\")\n" +
                "                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)\n" +
                "                .returns(void.class)\n" +
                "                .addParameter(String[].class, \"args\")\n" +
                "                .addStatement(\"\\$T.out.println(\\$S)\", System.class, node.types[0].name)\n" +
                "                .build();\n" +
                "\n" +
                "        TypeSpec helloWorld = TypeSpec.classBuilder(node.types[0].name)\n" +
                "                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)\n" +
                "                .addMethod(main)\n" +
                "                .build();\n" +
                "\n" +
                "        JavaFile javaFile = JavaFile.builder(\"com.example.helloworld\", helloWorld)\n" +
                "                .build();\n" +
                "\n" +
                "        context.addResultNode(ASTManager.parse(javaFile.toString()));");
        processor.visit(unit, vc);

        Assert.assertNotNull(vc.getResultNodes());

        Assert.assertEquals(1, vc.getResultNodes().size());

        CompilationUnit newCu = (CompilationUnit)vc.getResultNodes().iterator().next();

        Assert.assertEquals("Person", newCu.getTypes().get(0).getName());
    }


}
