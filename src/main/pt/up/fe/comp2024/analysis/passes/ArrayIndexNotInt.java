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

    private Void visitArrAccessExpr(JmmNode arrAccessExpr, SymbolTable table){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var varName = arrAccessExpr.getChild(0).get("name");

        var index = arrAccessExpr.getChild(1);

        // TODO: Need to implement the getVarRefType method
        var index_type = TypeUtils.getExprType(index, table);

        if (index_type.getName().equals(TypeUtils.getIntTypeName())) return null;

        // Create error report
        var message = String.format("Variable '%s' has non integer index.", varName);
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
