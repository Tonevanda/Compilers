package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import java.util.Arrays;

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


    //
    public Void visitFunctionCall(JmmNode functionCall, SymbolTable table){
        String methodName = functionCall.get("func");

        if (table.getMethods().stream().noneMatch(method -> method.equals(methodName))) {

            // if the class extends another class, we assume that said method is in the super class
            if (table.getSuper() != null) {
                return null;
            }

            var varType = TypeUtils.getExprType(functionCall.getChild(0), table);

            // if varType is from import    s, we accept
            if (table.getImports().stream().flatMap(importName -> Arrays.stream(importName.substring(1, importName.length() - 1).split(",")))
                    .anyMatch(importName -> importName.trim().equals(varType.getName()))) {
                return null;
            }

            var message = String.format("Method '%s' is not declared in class '%s'.", methodName, table.getClassName());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(functionCall),
                    NodeUtils.getColumn(functionCall),
                    message,
                    null
            ));
        }
        return null;
    }
}
