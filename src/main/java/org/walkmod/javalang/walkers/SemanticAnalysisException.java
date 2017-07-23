package org.walkmod.javalang.walkers;

public class SemanticAnalysisException extends Exception {

    public SemanticAnalysisException(String message) {
        super(message);
    }

    public SemanticAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
