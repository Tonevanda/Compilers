package pt.up.fe.comp2024.analysis.passes;

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
public class ExtendsNotImported extends AnalysisVisitor{

    @Override
    public void buildVisitor(){

        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
    }

    private Void visitClassDecl(JmmNode classDecl, SymbolTable table){

        // Get the name of the super class
        var superClass = table.getSuper();

        if(superClass == null)
            return null;

        // Check if it's imported
        if(table.getImports().stream()
                .flatMap(importName -> Arrays.stream(importName.substring(1, importName.length() - 1).split(","))) // Remove the square brackets and split by comma
                .anyMatch(importName -> importName.trim().equals(superClass))){
            return null;
        }

        // Create error report
        var message = String.format("Extended class '%s' is not imported", superClass);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(classDecl),
                NodeUtils.getColumn(classDecl),
                message,
                null)
        );

        return null;

    }
}
