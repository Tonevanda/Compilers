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

/**
 * Checks if the array index is of type int
 */
public class IncompatibleReturn extends AnalysisVisitor{

    private String currentMethod;

    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        var methodReturnType = table.getReturnType(currentMethod);
        if(!methodReturnType.getName().equals("void")){
            if(method.getChildren(Kind.RETURN_STMT).isEmpty()){
                // Create error report
                var message = String.format("Missing return statement in method '%s'. Expected return type '%s'.",
                        currentMethod, methodReturnType.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }
            var children = method.getChildren();
            var lastChild = children.get(children.size() - 1);
            if(!lastChild.getKind().equals(Kind.RETURN_STMT.toString())){
                // Create error report
                var message = String.format("Last statement in method '%s' is not return", currentMethod);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }

        }

        return null;
    }

    // TODO: se calhar funçoes que retornam array têm de ser int[] e não só int
    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check return type of method
        var methodReturnType = table.getReturnType(currentMethod);

        // Get the return expression
        var returnExpr = returnStmt.getChild(0);

        // Get the type of the return expression
        var returnExprType = TypeUtils.getExprType(returnExpr, table);

        // If it returnExprType is null, it means there are no methods with the name methodName
        // We already check this in UndeclaredMethod.java
        // If UndeclaredMethod.java doesn't throw an error, it means we can assume this method exists, so we return
        if(returnExprType == null) return null;

        // If they are the same, return
        if (methodReturnType.getName().equals(returnExprType.getName())){
            if(methodReturnType.isArray() && returnExprType.isArray() || !methodReturnType.isArray() && !returnExprType.isArray()){
                return null;
            }
        }

        // Create error report
        var message = String.format("Incompatible return type. Expected '%s' but got '%s'.",
                methodReturnType.getName(), returnExprType.getName());

        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(returnStmt),
                NodeUtils.getColumn(returnStmt),
                message,
                null)
        );

        return null;

    }
}
