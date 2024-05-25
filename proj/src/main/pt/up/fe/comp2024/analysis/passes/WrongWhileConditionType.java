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
public class WrongWhileConditionType extends AnalysisVisitor{

    @Override
    public void buildVisitor(){
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
    }

    private Void visitWhileStmt(JmmNode whileStmt, SymbolTable table){

        var exprType = TypeUtils.getExprType(whileStmt.getChild(0), table);
        if(exprType.getName().equals(TypeUtils.getBooleanTypeName())) return null;

        // Create error report
        var message = "While condition must be of type boolean.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(whileStmt),
                NodeUtils.getColumn(whileStmt),
                message,
                null)
        );

        return null;

    }
}
