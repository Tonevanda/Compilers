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
public class IncompatibleArrayInit extends AnalysisVisitor{

    @Override
    public void buildVisitor(){
        addVisit(Kind.ARRAY_INIT, this::visitArrayInit);
    }

    private Void visitArrayInit(JmmNode arrayInit, SymbolTable table){

        // Checks if every element in the array is an int literal
        if(arrayInit.getChildren().stream().allMatch(child -> child.getKind().equals(Kind.INT_LITERAL.toString()))){
            return null;
        }

        // For every element, if it is a var, check if the type is int
        boolean allInt = true;
        for(var child : arrayInit.getChildren()){
            var varType = TypeUtils.getExprType(child, table).getName();
            if (!varType.equals(TypeUtils.getIntTypeName())) {
                allInt = false;
                break;
            }
        }

        // If all elements are int literals, return
        if(allInt){
            return null;
        }

        // Create error report
        var message = "Array initialization contains non integer elements.";
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(arrayInit),
                NodeUtils.getColumn(arrayInit),
                message,
                null)
        );

        return null;

    }
}
