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
package org.walkmod.javalang.visitors;

import groovy.io.PlatformLineWriter;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilationFailedException;
import org.walkmod.exceptions.WalkModException;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ParseException;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.templates.TemplateEngine;
import org.walkmod.templates.TemplatesAware;
import org.walkmod.walkers.VisitorContext;

public class DefaultTemplateVisitor implements TemplatesAware {

	private List<String> templates;
	private List<File> templateFiles;
	private List<String> missingTemplates = new LinkedList<String>();
	private File propertiesFile = null;

	private TemplateEngine templateEngine;
	private String rootLabel = null;
	private String output;
	private static Logger log = Logger.getLogger(DefaultTemplateVisitor.class);

	public DefaultTemplateVisitor() {
	}

	public DefaultTemplateVisitor(TemplateEngine engine) {
		setTemplateEngine(engine);
	}

	public synchronized void visit(CompilationUnit cu, VisitorContext context)
			throws IOException {
		if (rootLabel == null) {
			setRootLabel("cu");
		}
		if (propertiesFile == null) {
			setProperties("template.properties");
		}
		context.put(getRootLabel(), cu);
		if (templateEngine == null) {
			Object bean = context.getBean(
					"org.walkmod.templates.groovy.GroovyTemplateEngine", null);
			if (bean != null && bean instanceof TemplateEngine) {
				templateEngine = (TemplateEngine) bean;
				log.info("Applying [groovy] as a default template engine");
			} else {
				throw new WalkModException("Template engine not found");
			}
		}

		templateEngine.initialize(context, cu);

		if (templateFiles != null && templates != null
				&& templateFiles.size() == templates.size()) {

			for (File template : templateFiles) {
				String templateResult = templateEngine.applyTemplate(template,
						propertiesFile);
				CompilationUnit newCU;
				try {
					newCU = ASTManager.parseCompilationUnit(templateResult,
							true);

					context.addResultNode(newCU);

				} catch (ParseException e) {
					if (output != null) {
						String outputFile = output;

						// validates if it is a template name to reduce
						// computation
						char[] chars = outputFile.toCharArray();
						boolean isGString = false;
						for (int i = 0; i < chars.length && !isGString; i++) {
							isGString = chars[i] == '$' || chars[i] == '<';
						}

						if (isGString) {
							GStringTemplateEngine engine = new GStringTemplateEngine();
							try {

								Template templateName = engine
										.createTemplate(output);
								StringWriter stringWriter = new StringWriter();
								Writer platformWriter = new PlatformLineWriter(
										stringWriter);
								templateName.make(context).writeTo(
										platformWriter);
								outputFile = platformWriter.toString();

							} catch (CompilationFailedException e2) {
								throw new WalkModException(e);
							} catch (ClassNotFoundException e2) {
								throw new WalkModException(e);
							}
						}
						PrintWriter out = null;
						File file = new File(outputFile);
						if (file.createNewFile()) {
							try {
								out = new PrintWriter(new BufferedWriter(
										new FileWriter(outputFile, true)));
								out.println(templateResult);
							} catch (IOException e1) {
								throw new WalkModException(e);
							} finally {
								if (out != null) {
									out.close();
								}
							}
						}

					} else {
						try{
						//it is java code. we need to know which line fails.
						 ASTManager.parseCompilationUnit(templateResult,
									false);
						}catch(Exception e2){
							throw new WalkModException(e2);
						}
					}
				}

			}
		} else {
			if (!missingTemplates.isEmpty()) {
				for (String missing : missingTemplates) {
					log.error("The template " + missing + " is missing");
				}
			}
			throw new WalkModException(
					"There are missing or unexitent templates.");
		}

	}

	public List<String> getTemplates() {
		return templates;
	}

	public void setTemplates(List<String> templates) {
		this.templates = templates;
		if (templates != null) {
			templateFiles = new LinkedList<File>();

			for (String template : templates) {
				File aux = new File(template);
				if (aux.exists()) {
					templateFiles.add(aux);
				} else {
					missingTemplates.add(template);
				}
			}
		}
	}

	public void setTemplateEngine(TemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	@Override
	public TemplateEngine getTemplateEngine() {
		return templateEngine;
	}

	@Override
	public String getRootLabel() {
		return rootLabel;
	}

	@Override
	public void setRootLabel(String rootLabel) {
		this.rootLabel = rootLabel;
	}

	public void setProperties(String propertiesFile) {
		File properties = new File(propertiesFile);
		if (properties.exists()) {
			this.propertiesFile = properties;
		}

	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getOutput() {
		return output;
	}

}
