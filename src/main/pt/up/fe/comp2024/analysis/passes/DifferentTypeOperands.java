package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

/**
 * Checks if the array index is of type int
 */
public class DifferentTypeOperands extends AnalysisVisitor{

    private String currentMethod;

    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }
    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table){

        // Get each operand
        var operand1 = binaryExpr.getChild(0);
        var operand2 = binaryExpr.getChild(1);

        // Get the type of each operand
        var operand1_type = TypeUtils.getExprType(operand1, table);
        var operand2_type = TypeUtils.getExprType(operand2, table);

        // If they are the same type
        if(operand1_type.getName().equals(operand2_type.getName())){
            // If none of them are arrays, return
            if (!operand1_type.isArray() && !operand2_type.isArray()) {
                return null;
            }
        }

        // Create error report
        var message = String.format("'%s' and '%s' are of different types.", operand1.getKind(), operand2.getKind());
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(binaryExpr),
                NodeUtils.getColumn(binaryExpr),
                message,
                null)
        );

        return null;

    }
}
