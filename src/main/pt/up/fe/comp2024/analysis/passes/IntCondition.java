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
public class IntCondition extends AnalysisVisitor{

    private String currentMethod;

    @Override
    public void buildVisitor(){
        addVisit(Kind.IF_STMT, this::visitIfStmt);
    }

    private Void visitIfStmt(JmmNode ifStmt, SymbolTable table){

        var exprType = TypeUtils.getExprType(ifStmt.getChild(0), table);
        if(exprType.getName().equals(TypeUtils.getBooleanTypeName())) return null;

        // Create error report
        var message = "If condition must be of type int.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(ifStmt),
                NodeUtils.getColumn(ifStmt),
                message,
                null)
        );

        return null;

    }
}
