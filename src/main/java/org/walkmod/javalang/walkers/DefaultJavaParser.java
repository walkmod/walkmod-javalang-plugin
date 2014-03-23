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
import java.io.Reader;

import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.walkers.ParseException;
import org.walkmod.walkers.Parser;

public class DefaultJavaParser implements Parser<CompilationUnit> {

	private String encoding = "UTF-8";

	@Override
	public CompilationUnit parse(String text) throws ParseException {
		try {
			return ASTManager.parseCompilationUnit(text, true);
		} catch (Exception e) {
			throw new ParseException(e.getCause());
		}
	}

	public CompilationUnit parse(String text, boolean withoutLocation)
			throws ParseException {
		try {
			return ASTManager.parseCompilationUnit(text, withoutLocation);
		} catch (Exception e) {
			throw new ParseException(e.getCause());
		}
	}

	@Override
	public CompilationUnit parse(File file) throws ParseException {
		try {
			return ASTManager.parse(file, encoding);
		} catch (Exception e) {
			throw new ParseException(e.getCause());
		}
	}

	@Override
	public CompilationUnit parse(Reader reader) throws ParseException {
		try {
			return ASTManager.parse(reader);
		} catch (Exception e) {
			throw new ParseException(e.getCause());
		}
	}

	@Override
	public CompilationUnit parse(File file, String encoding)
			throws ParseException {
		try {
			return ASTManager.parse(file, encoding);
		} catch (Exception e) {
			throw new ParseException(e.getCause());
		}
	}

}
