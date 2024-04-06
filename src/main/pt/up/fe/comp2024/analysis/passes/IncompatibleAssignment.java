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

    private Void visitAssignStmt(JmmNode assigntStmt, SymbolTable table){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Get the assigned variable and the assignee
        var assignedVar = assigntStmt.getChild(0);
        var assignee = assigntStmt.getChild(1);

        System.out.println("Assigned var: " + assignedVar);
        System.out.println("Assignee: " + assignee);

        // If assignee is an array initializer
        if(assignee.getKind().equals(Kind.ARRAY_INIT.toString())){
            // If assigned variable is an array, return
            if(TypeUtils.getExprType(assignedVar, table).isArray())
                return null;
            else {
                // Create error report
                var message = String.format("Variable '%s' is not an array and cannot be assigned an array.",
                        assignedVar.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assigntStmt),
                        NodeUtils.getColumn(assigntStmt),
                        message,
                        null)
                );
                return null;
            }
        }

        // Get the type of the assigned variable and the assignee
        var assignedVarType = TypeUtils.getExprType(assignedVar, table);
        var assigneeType = TypeUtils.getExprType(assignee, table);

        System.out.println("Assigned var type: " + assignedVarType);
        System.out.println("Assignee type: " + assigneeType);

        // TODO: I think we need to check if the assigned variable extends the assignee,
        //  but in this case we are only checking if they are the same
        // If they are the same, return
        if (assignedVarType.getName().equals(assigneeType.getName())) return null;

        // Create error report
        var message = String.format("Incompatible assignment. Expected '%s' but got '%s'.",
                assignedVarType.getName(), assignedVarType.getName());
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(assigntStmt),
                NodeUtils.getColumn(assigntStmt),
                message,
                null)
        );

        return null;

    }
}
