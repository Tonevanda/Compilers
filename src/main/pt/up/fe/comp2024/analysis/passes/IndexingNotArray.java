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
 * Checks if the type trying to be indexed is an array
 */
public class IndexingNotArray extends AnalysisVisitor{

    @Override
    public void buildVisitor(){
        addVisit(Kind.ARR_ACCESS_EXPR, this::visitArrAccessExpr);
    }

    public Void visitArrAccessExpr(JmmNode arrAccessExpr, SymbolTable table){

        // Get variable name of arrAccessExpr
        var varName = arrAccessExpr.getChild(0);

        // Get type of variable
        var varType = TypeUtils.getExprType(varName, table);

        // If it's an array, return
        if (varType.isArray()) {
            return null;
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
