package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
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

    @Override
    public void buildVisitor(){
        addVisit(Kind.FUNCTION_CALL, this::visitFunctionCall);
    }

    public Void visitFunctionCall(JmmNode functionCall, SymbolTable table){

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

        // Get the function name
        var functionName = functionCall.get("func");

        // Get parameters of the function
        var parameters = table.getParameters(functionName).stream().map(p -> p.getType().getName()).toList();

        // Get the arguments that are being passed to the function
        // TODO: This only works for object function cals like a.foo()
        //  for normal function calls like foo() it should start at 0 however i need to figure out
        //  how to differentiate each case
        //  maybe check for dots in the function call, if there are dots then it's an object function call
        var arguments = functionCall.getChildren().subList(1, functionCall.getNumChildren());

        // If the method being called has varargs as parameter
        if(table.getParameters(functionName).stream().
                anyMatch(parameter -> parameter.getType().getObject("isVarargs").toString().equals("true"))){
            // If the arguments are all integers, return
            if(arguments.stream().
                    allMatch(arg -> TypeUtils.getExprType(arg, table).getName().equals(TypeUtils.getIntTypeName()))){
                return null;
            }
            else{
                // Create error report
                var message = "Incompatible arguments. Varargs parameters must be of type int.";
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

        // TODO: Still need to check one by one the arguments and their types
        //  No idea how i'll handle varargs, maybe when i encounter a varargs argument i immediately return null
        //  since another pass will check if the varargs is the last parameter
        //  I could probably see if the function is a varargs function by checking the last parameter of the function
        //  Then i could check if the size of the arguments is the same as the number of parameters
        //  If they're the same, i could check if the types are the same one by one

        // Create error report
        var message = String.format("Incompatible arguments. Expected '%s' but got '%s'.", parameters, arguments);
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
