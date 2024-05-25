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

    @Override
    public void buildVisitor(){
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table){

        // Get each operand
        var operand1 = binaryExpr.getChild(0);
        var operand2 = binaryExpr.getChild(1);

        // Get the type of each operand
        var operand1_type = TypeUtils.getExprType(operand1, table);
        var operand2_type = TypeUtils.getExprType(operand2, table);

        // If they are the same type
        boolean arrayPresent = false;
        if(operand1_type.getName().equals(operand2_type.getName())){
            // If none of them are arrays, return
            if (!operand1_type.isArray() && !operand2_type.isArray()) {
                return null;
            }
            else {
                arrayPresent = true;
            }
        }

        // Create error report
        String message;
        if(arrayPresent){
            message = "Can't operate between array and non-array";
        }
        else{
            message = String.format("Can't operate on '%s' and '%s'. They are different types", operand1_type.getName(), operand2_type.getName());
        }
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
