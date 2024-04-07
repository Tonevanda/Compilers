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

import java.util.Arrays;

/**
 * Checks if the type trying to be indexed is an array
 */
public class IncompatibleArguments extends AnalysisVisitor{

    private String currentMethod;

    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNCTION_CALL, this::visitFunctionCall);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    public Void visitFunctionCall(JmmNode functionCall, SymbolTable table){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Get variable that calls the function and its type
        var variable = functionCall.getChild(0);
        var varType = TypeUtils.getExprType(variable, table);

        // If type is in imports, assume the arguments are correct
        if(table.getImports().stream()
                .flatMap(importName -> Arrays.stream(importName.substring(1, importName.length() - 1).split(","))) // Remove the square brackets and split by comma
                .anyMatch(importName -> importName.trim().equals(varType.getName()))){
            return null;
        }

        // If variable is of type current class, and the class extends another, we assume the function
        // is being called from the parent class and therefore the arguments are correct
        if(varType.getName().equals(table.getClassName()) && table.getSuper() != null){
            return null;
        }

        // TODO: Still need to check one by one the arguments and their types
        //  No idea how i'll handle varargs, maybe when i encounter a varargs argument i immediately return null
        //  since another pass will check if the varargs is the last parameter
        //  I could probably see if the function is a varargs function by checking the last parameter of the function
        //  Then i could check if the size of the arguments is the same as the number of parameters
        //  If they're the same, i could check if the types are the same one by one

        // Create error report
        var message = String.format("Incompatible arguments. Expected '%s' but got '%s'.",
                table.getReturnType(currentMethod).getName(), varType.getName());
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(functionCall),
                NodeUtils.getColumn(functionCall),
                message,
                null)
        );

        return null;
    }
}
