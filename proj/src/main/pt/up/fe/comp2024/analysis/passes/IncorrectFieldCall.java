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
public class IncorrectFieldCall extends AnalysisVisitor{

    @Override
    public void buildVisitor(){

        addVisit(Kind.LENGTH_CALL, this::visitLengthCall);
    }

    private Void visitLengthCall(JmmNode lengthCall, SymbolTable table){

        // Check if the length call is the last parameter
        var funcName = lengthCall.get("func");
        if(funcName.equals("length"))
            return null;

        // Create error report
        var message = String.format("Expected function name 'length', but got '%s'", funcName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(lengthCall),
                NodeUtils.getColumn(lengthCall),
                message,
                null)
        );

        return null;

    }
}
