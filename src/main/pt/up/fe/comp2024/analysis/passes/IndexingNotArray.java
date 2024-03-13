package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

public class IndexingNotArray extends AnalysisVisitor{

    private String currentMethod;

    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARR_ACCESS_EXPR, this::visitArrAccessExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    public Void visitArrAccessExpr(JmmNode arrAccessExpr, SymbolTable table){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Get variable name of arrAccessExpr
        var varName = arrAccessExpr.getChild(0).get("name");

        // Check if variable is in local variables
        if(table.getLocalVariables(currentMethod).stream().anyMatch(var -> var.getName().equals(varName))){
            var varType = table.getLocalVariables(currentMethod).stream()
                .filter(var -> var.getName().equals(varName))
                .map(type -> type.getType())
                .findFirst();

            if(varType.get().isArray()){
                return null;
            }
        }

        // Check if variable is in method parameters
        if(table.getParameters(currentMethod).stream().anyMatch(var -> var.getName().equals(varName))){
            var varType = table.getParameters(currentMethod).stream()
                    .filter(var -> var.getName().equals(varName))
                    .map(type -> type.getType())
                    .findFirst();

            if(varType.get().isArray()){
                return null;
            }
        }

        // Check if variable is in class fields
        if(table.getFields().stream().anyMatch(var -> var.getName().equals(varName))){
            var varType = table.getFields().stream()
                    .filter(var -> var.getName().equals(varName))
                    .map(type -> type.getType())
                    .findFirst();

            if(varType.get().isArray()){
                return null;
            }
        }

        // Create error report
        var message = String.format("Variable '%s' is not an array.", varName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(arrAccessExpr),
                NodeUtils.getColumn(arrAccessExpr),
                message,
                null)
        );

        return null;
    }
}
