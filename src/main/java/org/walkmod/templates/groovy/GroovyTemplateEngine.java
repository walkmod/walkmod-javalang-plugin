/*
 * Copyright (C) 2013 Raquel Pau and Albert Coroleu.
 *
 * Walkmod is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Walkmod is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Walkmod. If not, see <http://www.gnu.org/licenses/>.
 */
package org.walkmod.templates.groovy;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;

import org.walkmod.exceptions.WalkModException;
import org.walkmod.javalang.visitors.VisitorSupport;
import org.walkmod.templates.TemplateEngine;
import org.walkmod.walkers.VisitorContext;

public class GroovyTemplateEngine extends VisitorSupport<VisitorContext> implements TemplateEngine {

    private VisitorContext context;

    private GStringTemplateEngine engine;

    private Map<String, Template> cache = new HashMap<String, Template>();

    @Override
    public void initialize(VisitorContext context, Object rootNode) {
        this.context = context;
        engine = new GStringTemplateEngine(context.getClassLoader());
        super.initialize(context, rootNode);
    }

    @Override
    public String applyTemplate(File file) {
        Template template = cache.get(file.getAbsolutePath());
        if (template == null) {
            try {
                template = engine.createTemplate(file);
            } catch (Exception e) {
                throw new WalkModException(e);
            }
        }
        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.putAll(context);
        bindings.put("query", getQueryEngine());
        Writable wr = template.make(bindings);
        StringWriter sw = new StringWriter();
        try {
            wr.writeTo(sw);
        } catch (IOException e) {
            throw new WalkModException(e);
        }
        return sw.toString();
    }

    @Override
    public String applyTemplate(File template, File properties) {
        return applyTemplate(template);
    }
}
