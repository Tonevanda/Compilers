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
public class IncompatibleAssignment extends AnalysisVisitor{

    private String currentMethod;

    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitAssignStmt(JmmNode returnStmt, SymbolTable table){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Get the assigned variable and the assignee
        var assignedVar = returnStmt.getChild(0);
        var assignee = returnStmt.getChild(1);

        // Get the type of the assigned variable and the assignee
        var assignedVarType = TypeUtils.getExprType(assignedVar, table);
        var assigneeType = TypeUtils.getExprType(assignee, table);

        // If they are the same, return
        if (assignedVarType.getName().equals(assigneeType.getName())) return null;

        // Create error report
        var message = String.format("Incompatible assignment. Expected '%s' but got '%s'.",
                assignedVarType.getName(), assigneeType.getName());
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
