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
public class IncorrectVarargs extends AnalysisVisitor{

    @Override
    public void buildVisitor(){
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table){
        // Check if variable is varargs
        if(varDecl.getChild(0).get("isVarargs").equals("true")){
            // Create error report
            var message = String.format("Incorrect varargs usage. Variable '%s' was declared as varargs", varDecl.get("name"));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl),
                    message,
                    null)
            );
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table){

        // Get the method name
        var methodName = methodDecl.get("name");

        // Check if return type is varargs
        if(table.getReturnType(methodName).getObject("isVarargs").toString().equals("true")){
            // Create error report
            var message = String.format("Method '%s' has return type varargs", methodName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodDecl),
                    NodeUtils.getColumn(methodDecl),
                    message,
                    null)
            );
            return null;
        }

        // Get the parameters of the method
        var parameters = table.getParameters(methodName);

        // If no parameters are varargs, return
        if(parameters.stream().
                allMatch(parameter -> parameter.getType().getObject("isVarargs").toString().equals("false"))){
            return null;
        }

        // Count how many varargs there are
        if(parameters.stream()
                .filter(parameter -> parameter.getType().getObject("isVarargs").toString().equals("true"))
                .count() > 1) {
            // Create error report
            var message = String.format("Method %s has more than one varargs parameter", methodName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodDecl),
                    NodeUtils.getColumn(methodDecl),
                    message,
                    null)
            );
        }

        // If method only has one parameter, and it's varargs, return
        if(parameters.size() == 1 && parameters.get(0).getType().getObject("isVarargs").toString().equals("true")){
            return null;
        }
        // If only the last parameter is varargs, return
        if(parameters.get(parameters.size() - 1).getType().getObject("isVarargs").toString().equals("true") &&
                parameters.stream().limit(parameters.size() - 2)
                .allMatch(parameter -> parameter.getType().getObject("isVarargs").toString().equals("false"))){
            return null;
        }

        // Create error report
        var message = String.format("Varargs must be the last parameter in method %s", methodName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(methodDecl),
                NodeUtils.getColumn(methodDecl),
                message,
                null)
        );

        return null;

    }
}
