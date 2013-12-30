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
package org.walkmod.javalang.writers;

import java.io.File;
import java.util.List;

import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.util.FileUtils;
import org.walkmod.walkers.VisitorContext;
import org.walkmod.writers.AbstractFileWriter;

public class StringWriter extends AbstractFileWriter {

	@Override
	public String getContent(Object n, VisitorContext vc) {
		return n.toString();
	}

	@Override
	public File createOutputDirectory(Object o) {
		File out = null;
		if (o instanceof CompilationUnit) {
			CompilationUnit n = (CompilationUnit) o;
			List<TypeDeclaration> types = n.getTypes();
			if (types != null) {
				out = FileUtils.getSourceFile(getOutputDirectory(), n.getPackage(),
						n.getTypes().get(0));
				if (!out.exists()) {
					try {
						FileUtils.createSourceFile(getOutputDirectory(), out);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return out;
	}
	


}
