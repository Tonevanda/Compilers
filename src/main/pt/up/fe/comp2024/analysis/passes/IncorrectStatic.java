package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Arrays;

/**
 * Checks if the array index is of type int
 */
public class IncorrectStatic extends AnalysisVisitor{

    private String currentMethod;
    private boolean isStatic;

    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.THIS, this::visitThis);
        addVisit(Kind.VAR, this::visitVar);
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table){
        currentMethod = methodDecl.get("name");
        isStatic = methodDecl.get("isStatic").equals("true");
        if(!currentMethod.equals("name") && isStatic){
            // Create error report
            var message = String.format("Method '%s' cannot be declared static", currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodDecl),
                    NodeUtils.getColumn(methodDecl),
                    message,
                    null)
            );
        }
        return null;
    }

    private Void visitVar(JmmNode varNode, SymbolTable table){
        // Check if current method is static
        if(!isStatic)
            return null;

        var varName = varNode.get("name");
        // Check if variable is local
        if(table.getLocalVariables(currentMethod).stream()
                .anyMatch(localVar -> localVar.getName().equals(varName))){
            return null;
        }

        if(table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varName))){
            return null;
        }

        // Checks if the class with name varName is in the imports list
        if(table.getImports().stream()
                .flatMap(importName -> Arrays.stream(importName.substring(1, importName.length() - 1).split(","))) // Remove the square brackets and split by comma
                .anyMatch(importName -> importName.trim().equals(varName))){
            return null;
        }

        // Create error report
        var message = String.format("Static method cannot access field variable '%s'", varName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varNode),
                NodeUtils.getColumn(varNode),
                message,
                null)
        );

        return null;
    }

    private Void visitThis(JmmNode thisNode, SymbolTable table){

        // Check if current method is static
        // In this case, only main can be static
        if(!currentMethod.equals("main"))
            return null;

        // Create error report
        var message = "Static method cannot call 'this'";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(thisNode),
                NodeUtils.getColumn(thisNode),
                message,
                null)
        );

        return null;

    }

}
