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
import java.io.IOException;
import java.util.List;

import org.walkmod.javalang.actions.Action;
import org.walkmod.javalang.actions.ActionsApplier;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.body.TypeDeclaration;
import org.walkmod.javalang.util.FileUtils;
import org.walkmod.javalang.visitors.DumpVisitor;
import org.walkmod.javalang.walkers.DefaultJavaWalker;
import org.walkmod.walkers.VisitorContext;
import org.walkmod.writers.AbstractFileWriter;

public class StringWriter extends AbstractFileWriter {

   private char indentationChar = ' ';

   private char indentationLevel = 0;

   private int indentationSize = 2;

   public void setIndentationChar(char indentationChar) {
      this.indentationChar = indentationChar;
   }

   public void setIndentationLevel(char indentationLevel) {
      this.indentationLevel = indentationLevel;
   }

   public void setIndentationSize(int indentationSize) {
      this.indentationSize = indentationSize;
   }

   @Override
   public String getContent(Object n, VisitorContext vc) {
      if (vc != null && vc.containsKey(DefaultJavaWalker.ACTIONS_TO_APPY_KEY)) {
         @SuppressWarnings("unchecked")
         List<Action> actions = (List<Action>) vc.get(DefaultJavaWalker.ACTIONS_TO_APPY_KEY);
         ActionsApplier actionsApplier = new ActionsApplier();
         File original = (File) vc.get(DefaultJavaWalker.ORIGINAL_FILE_KEY);
         actionsApplier.setActionList(actions);
         actionsApplier.setText(original);
         actionsApplier.execute();
         return actionsApplier.getModifiedText();

      } else {
         DumpVisitor visitor = new DumpVisitor();
         visitor.setIndentationChar(indentationChar);
         visitor.setIndentationLevel(indentationLevel);
         visitor.setIndentationSize(indentationSize);

         ((Node) n).accept(visitor, null);
         return visitor.getSource();
      }
   }

   @Override
   public File createOutputDirectory(Object o) {
      File out = null;
      if (o instanceof CompilationUnit) {
         CompilationUnit n = (CompilationUnit) o;
         List<TypeDeclaration> types = n.getTypes();
         if (types != null) {
            try {
               out = FileUtils.getSourceFile(getOutputDirectory(), n.getPackage(), n.getTypes().get(0))
                     .getCanonicalFile();

               if (!out.exists()) {
                  try {
                     FileUtils.createSourceFile(getOutputDirectory(), out);
                  } catch (Exception e) {
                     throw new RuntimeException(e);
                  }
               }
            } catch (IOException e1) {
               throw new RuntimeException(e1);
            }
         }
      }
      return out;
   }

}
