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
import java.io.IOException;
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
import org.walkmod.util.location.LocationImpl;
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

    private boolean ignoreErrors = true;

    private boolean silent = false;

    private Map<String, Integer> added = new HashMap<String, Integer>();

    private Map<String, Integer> deleted = new HashMap<String, Integer>();

    private Map<String, Integer> updated = new HashMap<String, Integer>();

    private Map<String, Integer> unmodified = new HashMap<String, Integer>();

    private String encoding = "UTF-8";

    public static final String ACTIONS_TO_APPY_KEY = "actions_to_apply_key";

    private Parser<CompilationUnit> parser;

    private Boolean requiresSemanticAnalysis = null;

    private Boolean visitOnFailure = null;

    private ClassLoader classLoader;

    private ClasspathEvaluator classpathEvaluator = null;

    private List<ConstraintProvider<?>> constraintProv = null;

    private List<String> constraintProviders = null;

    private String sourceSubdirectories = "";

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
        deleteClassLoader();
    }
    
    private void deleteClassLoader(){
        getChainConfig().getConfiguration().getParameters().remove("classLoader");
    }

    protected void performSemanticAnalysis(CompilationUnit cu) throws SemanticAnalysisException {
        if (requiresSemanticAnalysis == null) {
            List<Object> visitors = getVisitors();
            Iterator<Object> it = visitors.iterator();
            while (it.hasNext() && requiresSemanticAnalysis == null) {
                Object current = it.next();
                if (current.getClass().isAnnotationPresent(RequiresSemanticAnalysis.class)) {
                    requiresSemanticAnalysis = true;
                    if (visitOnFailure == null) {
                        RequiresSemanticAnalysis annotation = current.getClass()
                                .getAnnotation(RequiresSemanticAnalysis.class);

                        visitOnFailure = annotation.optional();
                    }
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

                    SemanticAnalysisException ex = new SemanticAnalysisException(
                            "Error processing the analysis of [" + cu.getQualifiedName() + "]", e);
                    ex.setStackTrace(e.getStackTrace());
                    throw ex;

                }
            } else {
                throw new SemanticAnalysisException("There is no available project classpath to compile the sources");
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

            if (cu != null) {
                try {
                    resolveSourceSubdirs(file, cu);
                    performSemanticAnalysis(cu);
                    cu.withSymbols(true);
                    addConstraints(cu);
                    visit(cu);
                } catch (SemanticAnalysisException e) {
                    if (!ignoreErrors) {
                        throw e;
                    } else {
                        if (Boolean.TRUE.equals(visitOnFailure)) {
                            cu.withSymbols(false);
                            addConstraints(cu);
                            visit(cu);
                        }
                        return;
                    }
                } catch (InvalidSourceDirectoryException e) {
                    log.warn("The Java file " + file.getCanonicalPath() + " contains an invalid package.");
                    return;
                }
            }

        } else {
            if (!silent) {
                log.warn("Empty compilation unit");
            }
        }

    }

    public void resolveSourceSubdirs(File file, CompilationUnit cu) throws IOException, InvalidSourceDirectoryException {
        sourceSubdirectories = JavaSourceUtils.getSourceDirs(getReaderPath(), file, cu);
    }

    public String getSourceSubdirectories() {
        return sourceSubdirectories;
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

    protected void updateCU(Object element, VisitorContext vc, CompilationUnit cu) throws Exception {
        if (!silent) {
            log.debug(originalFile.getPath() + " [with changes]");

            String name = cu.getQualifiedName();
            System.out.println(">> " + name);
        }
        File outputDir = new File(getWriterPath());
        File inputDir = new File(getReaderPath());

        if (!outputDir.equals(inputDir)) {
            writeNewCU(element, vc, cu);
        } else {
            overwrite(element, vc, originalFile);

        }
    }

    protected void writeNewCU(Object element, VisitorContext vc, CompilationUnit returningCU) throws Exception {
        if (!silent) {
            log.info("++ " + returningCU.getQualifiedName());
        }
        vc.remove(ORIGINAL_FILE_KEY);
        super.write(element, vc);
        if (!silent) {
            log.debug(resolveFile(returningCU) + " [ written ]");
        }

    }

    protected void createOrUpdateExternalCU(Object element, VisitorContext vc, CompilationUnit returningCU)
            throws Exception {
        if (!isPackageInfo(returningCU)) {
            //it is not a package-info.java

            File outputJavaFile = resolveFile(returningCU);

            if (!outputJavaFile.exists()) {
                writeNewCU(element, vc, returningCU);

            } else {
                if (!outputJavaFile.equals(originalFile) || !onlyWriteChanges) {
                    //we are rewriting an existing source file in the output directory
                    overwrite(element, vc, outputJavaFile);
                } else {
                    if (!silent) {
                        log.debug(originalFile.getPath() + " [not written] ");
                    }
                }

            }
        }
    }

    protected void overwrite(Object element, VisitorContext vc, File outputDir) throws Exception {
        vc.put(ORIGINAL_FILE_KEY, outputDir);
        super.write(element, vc);
        if (!silent) {
            log.debug(outputDir.getPath() + " [ overwritten ]");
        }
    }

    protected File resolveFile(CompilationUnit returningCU) throws IOException {
        File parentDir = new File(getWriterPath());
        if (!"".equals(sourceSubdirectories)) {
            parentDir = new File(parentDir, sourceSubdirectories);
        }

        return new File(parentDir, returningCU.getFileName()).getCanonicalFile();
    }

    protected boolean isPackageInfo(CompilationUnit returningCU) {
        return returningCU.getTypes() == null;
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

                    if (cu.hasEqualFileName(returningCU)) {

                        boolean isUpdated = analyzeChanges(cu, returningCU, vc);

                        if (isUpdated) {
                            updateCU(element, vc, cu);
                        } else {
                            if (!silent) {
                                log.debug(originalFile.getPath() + " [ without changes ] ");
                            }
                            createOrUpdateExternalCU(element, vc, returningCU);
                        }
                    } else {
                        createOrUpdateExternalCU(element, vc, returningCU);
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
            String path = originalFile == null ? null : originalFile.getCanonicalPath();
            WalkModException e1 = new WalkModException("Error visiting a Java source file", e,
                    new LocationImpl("File Location", path));
            if (!ignoreErrors) {
                throw e1;
            } else {
                e1.fillInStackTrace();
                e1.printStackTrace();
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

    protected String getWriterPath() {
        return getChainConfig().getWriterConfig().getPath();
    }

    protected String getReaderPath() {
        return getChainConfig().getReaderConfig().getPath();
    }

    @Override
    protected Object getSourceNode(Object targetNode) {
        Object result = null;
        if (targetNode instanceof CompilationUnit) {
            CompilationUnit targetCU = (CompilationUnit) targetNode;
            try {
                File sourceFile = resolveFile(targetCU);
                if (sourceFile.exists()) {

                    result = parser.parse(sourceFile);

                } else {
                    result = targetNode;
                }
            } catch (Exception e) {
                throw new WalkModException(e);
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
    public Parser<CompilationUnit> getParser() {
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
