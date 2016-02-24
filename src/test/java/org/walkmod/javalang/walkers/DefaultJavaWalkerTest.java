package org.walkmod.javalang.walkers;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.walkmod.conf.entities.TransformationConfig;
import org.walkmod.conf.entities.impl.ChainConfigImpl;
import org.walkmod.conf.entities.impl.TransformationConfigImpl;
import org.walkmod.conf.entities.impl.WalkerConfigImpl;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;
import org.walkmod.walkers.VisitorContext;

public class DefaultJavaWalkerTest {

   @Test
   public void testExceptionsMustDefineTheAffectedSourceFile() throws Exception {
      DefaultJavaWalker walker = new DefaultJavaWalker();
      File sampleDir = new File("src/test/resources/test1");
      if (sampleDir.exists()) {
         FileUtils.deleteDirectory(sampleDir);
      }
      sampleDir.mkdirs();
      File fooClass = new File(sampleDir, "Foo.java");
      fooClass.createNewFile();
      FileUtils.write(fooClass, "public class Foo {}");
      List<Object> visitors = new LinkedList<Object>();
      VisitorWithException instance = new VisitorWithException();
      visitors.add(instance);
      walker.setVisitors(visitors);
      walker.setParser(new DefaultJavaParser());

      ChainConfigImpl cfg = new ChainConfigImpl();
      WalkerConfigImpl walkerCfg = new WalkerConfigImpl();

      List<TransformationConfig> transformations = new LinkedList<TransformationConfig>();
      TransformationConfigImpl tcfg = new TransformationConfigImpl();
      tcfg.setVisitorInstance(instance);
      transformations.add(tcfg);
      walkerCfg.setTransformations(transformations);

      cfg.setWalkerConfig(walkerCfg);
      walker.setChainConfig(cfg);
      try {
         walker.accept(fooClass);
      } catch (Exception e) {
         String message = e.getMessage();
         Assert.assertTrue(message.contains("Error processing [" + fooClass.getCanonicalPath() + "]"));

      } finally {
         fooClass.delete();
         FileUtils.deleteDirectory(sampleDir);
      }
   }

   public class VisitorWithException extends VoidVisitorAdapter<VisitorContext> {
      @Override
      public void visit(CompilationUnit cu, VisitorContext vc) {
         throw new RuntimeException("Hello");
      }
   }
}
