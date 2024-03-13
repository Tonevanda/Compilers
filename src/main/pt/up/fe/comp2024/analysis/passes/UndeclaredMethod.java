package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

public class UndeclaredMethod extends AnalysisVisitor{

    private String currentMethod;

    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNCTION_CALL, this::visitFunctionCall);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    public Void visitFunctionCall(JmmNode functionCall, SymbolTable table){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // If class extends another, we assume the extended class has the method
        if(table.getSuper() != null){
            return null;
        }

        // Get method name of FunctionCall node
        var methodName = functionCall.get("func");

        // If methodName is in method list, return
        if(table.getMethods().stream()
                .anyMatch(methodDecl -> methodDecl.equals(methodName))){
            return null;
        }

        // Get name and type of variable that called the method
        var varName = functionCall.getChild(0).get("name");
        var varType = table.getLocalVariables(currentMethod).stream()
                .filter(var -> var.getName().equals(varName))
                .map(type -> type.getType())
                .findFirst();

        // If className is in import list, return
        if(table.getImports().stream()
                .anyMatch(importName -> importName.equals(varType.get().getName()))){
            return null;
        }

        // Create error report
        var message = String.format("Method '%s' does not exist.", methodName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(functionCall),
                NodeUtils.getColumn(functionCall),
                message,
                null)
        );

        return null;
    }
}