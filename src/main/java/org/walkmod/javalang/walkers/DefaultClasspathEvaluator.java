package org.walkmod.javalang.walkers;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.walkmod.conf.ConfigurationProvider;
import org.walkmod.conf.entities.ChainConfig;
import org.walkmod.conf.entities.Configuration;
import org.walkmod.conf.entities.InitializerConfig;

public class DefaultClasspathEvaluator implements ClasspathEvaluator {

    @Override
    public void evaluate(DefaultJavaWalker walker) {

        if (walker.requiresSemanticAnalysis() && requiresToEvaluateClasspath(walker)) {

            runClassLoaderInitializers(walker);

        }
    }

    protected boolean requiresToEvaluateClasspath(DefaultJavaWalker walker) {
        ChainConfig currentChain = walker.getChainConfig();
        Configuration conf = currentChain.getConfiguration();
        boolean alreadyExecuted = false;

        Map<String, Object> params = conf.getParameters();
        if (params.containsKey("classLoader")) {
            alreadyExecuted = true;
        }

        Collection<ChainConfig> chainConfigs = conf.getChainConfigs();
        Iterator<ChainConfig> it = chainConfigs.iterator();
        return !(alreadyExecuted && it.next() == currentChain);
    }

    protected void runClassLoaderInitializers(DefaultJavaWalker walker) {
        ChainConfig currentChain = walker.getChainConfig();
        Configuration conf = currentChain.getConfiguration();
        List<InitializerConfig> initCfgs = conf.getInitializers();
        Map<String, Object> params = conf.getParameters();
        if (initCfgs == null || initCfgs.isEmpty()) {

            BasicClasspathInitializer basicInitializer = new BasicClasspathInitializer(walker.getReaderPath(), walker.getParser());
            basicInitializer.init(conf);
            basicInitializer.load();

        } else {
            for (InitializerConfig ic : initCfgs) {
                String name = ic.getType().substring(0, ic.getType().indexOf("-initializer"));
                Object cfgProvider = conf.getBean(name, null);
                if (cfgProvider != null) {
                    ConfigurationProvider cfg = (ConfigurationProvider) cfgProvider;
                    cfg.init(conf);
                    cfg.load();
                }
            }
        }
        updateVisitorClassLoaders(walker, (ClassLoader) params.get("classLoader"));
    }

    protected void updateVisitorClassLoaders(DefaultJavaWalker walker, ClassLoader classLoader) {
        walker.setClassLoader(classLoader);
        List<Object> visitors = walker.getVisitors();
        if (visitors != null) {
            for (Object visitor : visitors) {
                try {
                    Method method = visitor.getClass().getMethod("setClassLoader", ClassLoader.class);
                    method.invoke(visitor, walker.getClassLoader());
                } catch (Throwable e) {
                }
            }
        }
    }

}
