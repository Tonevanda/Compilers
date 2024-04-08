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

        // If correct number of arguments
        if(parameters.size() == arguments.size()){
            // Check if the arguments are of the correct type
            for(int i = 0; i < parameters.size(); i++){
                var parameter = parameters.get(i);
                var argument = TypeUtils.getExprType(arguments.get(i), table).getName();
                if(!parameter.equals(argument)){
                    // Create error report
                    var message = String.format("Incompatible arguments. Expected '%s' but got '%s'.", parameter, argument);
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
            return null;
        }
        // If incorrect number of arguments
        else{
            // Create error report
            var message = String.format("Incompatible arguments. Expected %d arguments but got %d.", parameters.size(), arguments.size());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(functionCall),
                    NodeUtils.getColumn(functionCall),
                    message,
                    null)
            );
        }


        return null;
    }
}
