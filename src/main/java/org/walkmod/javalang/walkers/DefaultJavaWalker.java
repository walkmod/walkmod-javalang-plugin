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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.walkmod.conf.entities.Configuration;
import org.walkmod.exceptions.WalkModException;
import org.walkmod.javalang.actions.Action;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.compiler.symbols.RequiresSemanticAnalysis;
import org.walkmod.javalang.compiler.symbols.SymbolVisitorAdapter;
import org.walkmod.modelchecker.Constraint;
import org.walkmod.modelchecker.ConstraintProvider;
import org.walkmod.walkers.AbstractWalker;
import org.walkmod.walkers.ChangeLogPrinter;
import org.walkmod.walkers.Parser;
import org.walkmod.walkers.VisitorContext;

public class DefaultJavaWalker extends AbstractWalker {

   private File originalFile;

   public static final String ORIGINAL_FILE_KEY = "original_file_key";

   private static Logger log = Logger.getLogger(DefaultJavaWalker.class);

   private boolean reportChanges = true;

   private boolean onlyWriteChanges = true;

   private boolean onlyIncrementalWrites = true;

   private boolean ignoreErrors = false;

   private boolean silent = false;

   private Map<String, Integer> added = new HashMap<String, Integer>();

   private Map<String, Integer> deleted = new HashMap<String, Integer>();

   private Map<String, Integer> updated = new HashMap<String, Integer>();

   private Map<String, Integer> unmodified = new HashMap<String, Integer>();

   private String encoding = "UTF-8";

   public static final String ACTIONS_TO_APPY_KEY = "actions_to_apply_key";

   private Parser<CompilationUnit> parser;

   private Boolean requiresSemanticAnalysis = null;

   private ClassLoader classLoader;

   private ClasspathEvaluator classpathEvaluator = null;

   private List<ConstraintProvider<?>> constraintProv = null;

   private List<String> constraintProviders = null;

   public void accept(File file) throws Exception {
      originalFile = file;
      visit(file);
   }

   public DefaultJavaWalker() {

   }

   public void setSilent(Boolean silent) {
      this.silent = silent;
   }

   public DefaultJavaWalker(ClasspathEvaluator classpathEvaluator) {
      this.classpathEvaluator = classpathEvaluator;
   }

   public void setOnlyIncrementalWrites(boolean onlyIncrementalWrites) {
      this.onlyIncrementalWrites = onlyIncrementalWrites;
   }

   public void setConstraintProviders(List<String> constraints) {
      this.constraintProviders = constraints;
   }

   public List<String> getConstraintProviders() {
      return constraintProviders;
   }

   public File getCurrentFile() {
      return originalFile;
   }

   public boolean requiresSemanticAnalysis() {

      List<Object> visitors = getVisitors();
      if (visitors != null) {
         for (Object visitor : visitors) {

            if (visitor.getClass().isAnnotationPresent(RequiresSemanticAnalysis.class)) {
               return true;
            }
         }
      }
      return false;
   }

   public void calculateClasspath() {
      if (classpathEvaluator == null) {
         Configuration conf = getChainConfig().getConfiguration();

         try {
            classpathEvaluator = (ClasspathEvaluator) conf
                  .getBean("org.walkmod:walkmod-javalang-plugin:classpath-evaluator", null);
         } catch (Exception e) {
         }
      }
      if (classpathEvaluator != null) {
         classpathEvaluator.evaluate(this);
      }
   }

   public void setClasspathEvaluator(ClasspathEvaluator classpathEvaluator) {
      this.classpathEvaluator = classpathEvaluator;
   }

   public ClasspathEvaluator getClasspathEvaluator() {
      return classpathEvaluator;
   }

   @Override
   public void execute() throws Exception {
      calculateClasspath();
      List<String> consProv = getConstraintProviders();
      if (consProv != null) {
         Configuration conf = getChainConfig().getConfiguration();
         constraintProv = new LinkedList<ConstraintProvider<?>>();
         for (String cons : consProv) {
            if (conf.containsBean(cons)) {
               ConstraintProvider<?> cp = (ConstraintProvider) conf.getBean(cons, null);
               constraintProv.add(cp);
            }
         }
      }
      super.execute();
   }

   protected void performSemanticAnalysis(CompilationUnit cu) {
      if (requiresSemanticAnalysis == null) {
         List<Object> visitors = getVisitors();
         Iterator<Object> it = visitors.iterator();
         while (it.hasNext() && requiresSemanticAnalysis == null) {
            Object current = it.next();
            if (current.getClass().isAnnotationPresent(RequiresSemanticAnalysis.class)) {
               requiresSemanticAnalysis = true;
            }
         }
         if (requiresSemanticAnalysis == null) {
            requiresSemanticAnalysis = false;
         }
      }
      if (requiresSemanticAnalysis) {

         ClassLoader cl = getClassLoader();
         if (cl != null) {
            SymbolVisitorAdapter<HashMap<String, Object>> visitor = new SymbolVisitorAdapter<HashMap<String, Object>>();
            visitor.setClassLoader(cl);
            try {
               visitor.visit(cu, new HashMap<String, Object>());
            } catch (Throwable e) {
               String message = "Error processing the analysis of [" + cu.getQualifiedName() + "]";
               WalkModException e1 = new WalkModException(message, e);
               e1.setStackTrace(e.getStackTrace());
               if (!ignoreErrors) {
                  throw e1;
               } else {
                  if (!silent) {
                     log.error(message, e1);
                  }
                  return;
               }
            }
         } else {
            throw new WalkModException("There is no available project classpath to apply " + "a semantic analysis");
         }
      }

   }

   protected void addConstraints(CompilationUnit cu) {
      if (constraintProv != null) {
         List<Constraint> constraints = new LinkedList<Constraint>();
         for (ConstraintProvider cp : constraintProv) {
            Constraint<?> c = cp.getConstraint(cu);
            constraints.add(c);
         }
         cu.setConstraints(constraints);
      }
   }

   public void visit(File file) throws Exception {
      if (file.getName().endsWith(".java")) {
         CompilationUnit cu = null;
         try {
            cu = parser.parse(file, encoding);
         } catch (Exception e) {
            throw new WalkModException("The file " + file.getPath() + " has an invalid java format", e);
         }
         if (getRootNamespace() != null && !"".equals(getRootNamespace())) {
            String qualifiedName = getResource().getNearestNamespace(file, NAMESPACE_SEPARATOR);
            if (getRootNamespace().startsWith(qualifiedName)) {
               return;
            }
         }
         if (cu != null) {
            performSemanticAnalysis(cu);
            addConstraints(cu);
            if (!silent) {
               log.debug(file.getPath() + " [ visiting ]");
            }
            visit(cu);
            if (!silent) {
               log.debug(file.getPath() + " [ visited ]");
            }
         } else {
            if (!silent) {
               log.warn("Empty compilation unit");
            }
         }
      }
   }

   public ClassLoader getClassLoader() {
      return classLoader;
   }

   public void setClassLoader(ClassLoader classLoader) {
      this.classLoader = classLoader;
   }

   private void mapSum(Map<String, Integer> op1, Map<String, Integer> op2) {
      Set<String> keys = op2.keySet();
      for (String key : keys) {
         Integer aux = op1.get(key);
         if (aux == null) {
            op1.put(key, op2.get(key));
         } else {
            op1.put(key, op2.get(key) + aux);
         }
      }
   }

   protected CompilationUnit recoverOriginalCU(VisitorContext vc) {
      try {
         return parser.parse(originalFile, encoding);
      } catch (Exception e) {
         throw new WalkModException("Exception writing results of " + originalFile.getPath(), e);
      }
   }

   protected VisitorContext buildWriterContext(VisitorContext oldVC) throws Exception {
      VisitorContext vc = new VisitorContext(getChainConfig());
      vc.put(ORIGINAL_FILE_KEY, originalFile);
      vc.put("onlyWriteChanges", onlyWriteChanges);
      vc.put("reportChanges", reportChanges);
      return vc;
   }

   public boolean analyzeChanges(CompilationUnit original, CompilationUnit modified, VisitorContext vc)
         throws Exception {
      ChangeLogVisitor clv = new ChangeLogVisitor();
      clv.setGenerateActions(onlyIncrementalWrites);
      VisitorContext ctx = new VisitorContext();
      ctx.put(ChangeLogVisitor.NODE_TO_COMPARE_KEY, original);
      clv.visit((CompilationUnit) modified, ctx);
      boolean isUpdated = clv.isUpdated();
      vc.put("isUpdated", isUpdated);
      if (isUpdated) {
         if (onlyIncrementalWrites) {
            storeChanges(clv.getActionsToApply(), vc);
         }

         Map<String, Integer> auxAdded = clv.getAddedNodes();
         Map<String, Integer> auxUpdated = clv.getUpdatedNodes();
         Map<String, Integer> auxDeleted = clv.getDeletedNodes();
         Map<String, Integer> auxUnmodified = clv.getUnmodifiedNodes();
         if (added.isEmpty()) {
            added = auxAdded;
         } else {
            mapSum(added, auxAdded);
         }
         if (updated.isEmpty()) {
            updated = auxUpdated;
         } else {
            mapSum(updated, auxUpdated);
         }
         if (deleted.isEmpty()) {
            deleted = auxDeleted;
         } else {
            mapSum(deleted, auxDeleted);
         }
         if (unmodified.isEmpty()) {
            unmodified = auxUnmodified;
         } else {
            mapSum(unmodified, auxUnmodified);
         }
         ChangeLogPrinter printer = new ChangeLogPrinter(auxAdded, auxUpdated, auxDeleted, auxUnmodified);
         printer.print();
      }

      return isUpdated;
   }

   protected void storeChanges(List<Action> actions, VisitorContext vc) throws Exception {
      vc.put(ACTIONS_TO_APPY_KEY, actions);
   }

   @Override
   protected void write(Object element, VisitorContext oldVC) throws Exception {

      if (element != null && element instanceof CompilationUnit) {

         VisitorContext vc = buildWriterContext(oldVC);

         if (reportChanges) {
            CompilationUnit returningCU = (CompilationUnit) element;
            CompilationUnit cu = recoverOriginalCU(vc);

            if (cu != null) {
               // if the returning CU corresponds to the same File, then
               // check changes.

               boolean resolveWrite = false;
               if (cu.hasEqualFileName(returningCU)) {

                  boolean isUpdated = analyzeChanges(cu, returningCU, vc);

                  if (isUpdated) {
                     if (!silent) {
                        log.debug(originalFile.getPath() + " [with changes]");

                        String name = cu.getQualifiedName();
                        System.out.println(">> " + name);
                     }
                     File outputDir = new File(getWriterPath());
                     File inputDir = new File(getReaderPath());

                     if (!outputDir.equals(inputDir)) {
                        vc.remove(ORIGINAL_FILE_KEY);
                     }
                     super.write(element, vc);
                     if (!silent) {
                        log.debug(originalFile.getPath() + " [ written ]");
                     }
                  } else {
                     if (!silent) {
                        log.debug(originalFile.getPath() + " [ without changes ] ");
                     }
                     //may be we want to force writes
                     resolveWrite = true;
                  }
               } else {
                  //we need to write in a different directory
                  resolveWrite = true;
               }
               if (resolveWrite) {

                  if (returningCU.getTypes() != null) {
                     //it is not a package-info.java

                     File outputDir = new File(getWriterPath(), returningCU.getFileName()).getAbsoluteFile();

                     if (!silent) {
                        log.debug(outputDir.getAbsolutePath() + " [ output file ]");
                     }
                     if (!outputDir.exists()) {
                        //we are writing a new file
                        if (!silent) {
                           log.info("++ " + returningCU.getQualifiedName());
                        }
                        vc.remove(ORIGINAL_FILE_KEY);
                        super.write(element, vc);
                        if (!silent) {
                          
                           log.debug(outputDir.getPath() + " [ written ]");
                        }
                     } else {
                        if (!outputDir.equals(originalFile)) {
                           //we are rewriting an existing source file in the output directory
                           vc.put(ORIGINAL_FILE_KEY, outputDir);
                           super.write(element, vc);
                           if (!silent) {
                              log.debug(outputDir.getPath() + " [ overwritten ]");
                           }
                        } else if (!onlyWriteChanges) {
                           //the user forces writes
                           vc.put(ORIGINAL_FILE_KEY, outputDir);
                           super.write(element, vc);
                           if (!silent) {
                              log.debug(outputDir.getPath() + " [ overwritten ]");
                           }
                        } else {
                           if (!silent) {
                              log.debug(originalFile.getPath() + " [not written] ");
                           }
                        }

                     }
                  }
               }
            }
         } else {
            if (!silent) {
               System.out.println(">> " + originalFile.getPath());
            }
            vc.remove(ORIGINAL_FILE_KEY);
            super.write(element, vc);
         }
      }
   }

   @Override
   protected void write(Object element) throws Exception {
      write(element, null);
   }

   @Override
   protected String getLocation(VisitorContext ctx) {
      return ((File) ctx.get(ORIGINAL_FILE_KEY)).getAbsolutePath();
   }

   @Override
   protected void visit(Object element) throws Exception {
      VisitorContext context = new VisitorContext(getChainConfig());
      context.put(ORIGINAL_FILE_KEY, originalFile);
      try {
         visit(element, context);
      } catch (Throwable e) {
         String message;
         if (originalFile != null) {
            message = "Error processing [" + originalFile.getCanonicalPath() + "]";
         } else {
            message = "Error processing a Java file ";
         }
         WalkModException e1 = new WalkModException(message, e.getCause());
         Throwable cause = e.getCause();
         if (cause != null) {
            e1.setStackTrace(e.getCause().getStackTrace());
         } else {
            e1.setStackTrace(e.getStackTrace());
         }
         if (!ignoreErrors) {
            throw e1;
         } else {
            if (!silent) {
               log.error(message, e1);
            }
         }
      }
      addVisitorMessages(context);
   }

   @Override
   public int getNumModifications() {
      Integer aux = updated.get(CompilationUnit.class.getSimpleName());
      if (aux == null) {
         return 0;
      }
      return aux;
   }

   @Override
   public int getNumAdditions() {
      Integer aux = added.get(CompilationUnit.class.getSimpleName());
      if (aux == null) {
         return 0;
      }
      return aux;
   }

   @Override
   public int getNumDeletions() {
      Integer aux = deleted.get(CompilationUnit.class.getSimpleName());
      if (aux == null) {
         return 0;
      }
      return aux;
   }

   @Override
   public boolean reportChanges() {
      return reportChanges;
   }

   @Override
   public void setReportChanges(boolean reportChanges) {
      this.reportChanges = reportChanges;
   }

   public String getEncoding() {
      return encoding;
   }

   public void setEncoding(String encoding) {
      this.encoding = encoding;
   }

   private String getWriterPath() {
      return getChainConfig().getWriterConfig().getPath();
   }

   private String getReaderPath() {
      return getChainConfig().getReaderConfig().getPath();
   }

   @Override
   protected Object getSourceNode(Object targetNode) {
      Object result = null;
      if (targetNode instanceof CompilationUnit) {
         CompilationUnit targetCU = (CompilationUnit) targetNode;
         String path = getWriterPath() + File.separator + targetCU.getFileName();

         File sourceFile = new File(path);
         if (sourceFile.exists()) {
            try {
               result = parser.parse(sourceFile);
            } catch (Exception e) {
               throw new WalkModException(e);
            }
         } else {
            result = targetNode;
         }
      }
      return result;
   }

   @SuppressWarnings("unchecked")
   @Override
   public void setParser(Parser<?> parser) {
      this.parser = (Parser<CompilationUnit>) parser;
   }

   @Override
   public Parser<?> getParser() {
      return parser;
   }

   public void setOnlyWriteChanges(boolean onlyWriteChanges) {
      this.onlyWriteChanges = onlyWriteChanges;
   }

   public void setIgnoreErrors(boolean ignoreErrors) {
      this.ignoreErrors = ignoreErrors;
   }

   public boolean getRequiresSemanticAnalysis() {
      return requiresSemanticAnalysis;
   }

   public void setRequiresSemanticAnalysis(boolean requiresSemanticAnalysis) {
      this.requiresSemanticAnalysis = requiresSemanticAnalysis;
   }

}
