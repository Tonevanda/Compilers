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
public class ArrayIndexNotInt extends AnalysisVisitor{

    @Override
    public void buildVisitor(){
        addVisit(Kind.ARR_ACCESS_EXPR, this::visitArrAccessExpr);
    }

    private Void visitArrAccessExpr(JmmNode arrAccessExpr, SymbolTable table){

        // Get the variable name and index of the array access expression
        var varName = arrAccessExpr.getChild(0).get("name");
        var index = arrAccessExpr.getChild(1);

        // Get the type of the index
        var index_type = TypeUtils.getExprType(index, table);

        // If the index is an integer, return
        if (index_type.getName().equals(TypeUtils.getIntTypeName())) return null;

        // Create error report
        var message = String.format("Array '%s' has non-integer index.", varName);
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
