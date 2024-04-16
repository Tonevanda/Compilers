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

import java.util.HashSet;
import java.util.Set;

/**
 * Checks if the array index is of type int
 */
public class DuplicatedMisc extends AnalysisVisitor{

    @Override
    public void buildVisitor(){
        addVisit(Kind.PROGRAM, this::visitProgram);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table){
        // Check if more than 1 return statement
        var returnStatements = methodDecl.getChildren(Kind.RETURN_STMT);
        if(returnStatements.size() > 1){
            // Create error report
            var message = String.format("Method %s has more than one return statement.", methodDecl.get("name"));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodDecl),
                    NodeUtils.getColumn(methodDecl),
                    message,
                    null)
            );
            return null;
        }

        // Check duplicated parameters

        var methodName = methodDecl.get("name");
        var params = table.getParameters(methodName);

        Set<String> uniqueParams = new HashSet<>();
        for (var param : params) {
            if (!uniqueParams.add(param.getName())) {
                // Create error report
                var message = String.format("Parameter %s is duplicated in method %s.", param.getName(), methodName);
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

        // Check duplicated local variables

        var locals = table.getLocalVariables(methodName);
        Set<String> uniqueLocals = new HashSet<>();
        for (var local : locals) {
            if (!uniqueLocals.add(local.getName())) {
                // Create error report
                var message = String.format("Local variable %s is duplicated in method %s.", local.getName(), methodName);
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

        return null;
    }

    private Void visitProgram(JmmNode program, SymbolTable table){

        // Check duplicated Fields

        var fields = table.getFields();
        Set<String> uniqueFields = new HashSet<>();
        for (var field : fields) {
            if (!uniqueFields.add(field.getName())) {
                // Create error report
                var message = String.format("Field %s is duplicated.", field.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(program),
                        NodeUtils.getColumn(program),
                        message,
                        null)
                );
                return null;
            }
        }


        // Check duplicated Methods

        var methods = table.getMethods();

        // Iterate through methods, and if the same name appears twice, report an error
        for(int i = 0; i < methods.size(); i++){
            for(int j = i + 1; j < methods.size(); j++){
                if(methods.get(i).equals(methods.get(j))){
                    // Create error report
                    var message = String.format("There is more than one method with the name %s.", methods.get(i));
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(program),
                            NodeUtils.getColumn(program),
                            message,
                            null)
                    );

                    return null;
                }
            }
        }

        // Check duplicated Imports

        var imports = table.getImports();

        // Iterate through imports, and if the same name appears twice, report an error
        for(int i = 0; i < imports.size(); i++){
            for(int j = i + 1; j < imports.size(); j++){

                // Check if the same import path appears twice
                if(imports.get(i).equals(imports.get(j))){
                    // Create error report
                    var message = String.format("There is more than one import with the name %s.", imports.get(i));
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(program),
                            NodeUtils.getColumn(program),
                            message,
                            null)
                    );

                    return null;
                }
                // Check if the same class is being imported from different paths
                else {
                    // Split the import strings by comma and get the last elements
                    String[] import1Elements = imports.get(i).substring(1, imports.get(i).length() - 1).split(", ");
                    String[] import2Elements = imports.get(j).substring(1, imports.get(j).length() - 1).split(", ");
                    String lastElement1 = import1Elements[import1Elements.length - 1];
                    String lastElement2 = import2Elements[import2Elements.length - 1];

                    // Check if the last elements are the same
                    if(lastElement1.equals(lastElement2)){
                        // Create error report
                        var message = String.format("Invalid duplicated class import in %s and %s.", imports.get(i), imports.get(j));
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(program),
                                NodeUtils.getColumn(program),
                                message,
                                null)
                        );

                        return null;
                    }
                }
            }
        }


        return null;

    }
}
