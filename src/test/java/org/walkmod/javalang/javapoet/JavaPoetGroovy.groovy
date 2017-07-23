package org.walkmod.javalang.javapoet

import com.squareup.javapoet.*
import org.walkmod.javalang.ASTManager
import javax.lang.model.element.Modifier


TypeSpec entity = TypeSpec.classBuilder(node.types[0].name).addModifiers(
        Modifier.PUBLIC).addSuperinterface(Serializable.class).build();

JavaFile javaFile = JavaFile.builder("com.example.model", entity).build();
context.addResultNode(ASTManager.parse(javaFile.toString()));

