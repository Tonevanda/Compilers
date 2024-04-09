package pt.up.fe.comp2024.analysis.passes;

import org.w3c.dom.Node;
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

/**
 * Checks if the type trying to be indexed is an array
 */
public class IncorrectMainDeclaration extends AnalysisVisitor{

    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    public Void visitMethodDecl(JmmNode methodDecl, SymbolTable table){

        if(!table.getMethods().contains("main")){
            // Create error report
            var message = "Main method not found.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    0, // don't know what to put here
                    0, // here neither
                    message,
                    null)
            );
        }

        // Get name of method
        var methodName = methodDecl.get("name");

        if(!methodName.equals("main"))
            return null;

        boolean isVoid = methodDecl.getChild(0).get("name").equals("void");
        boolean isStatic = NodeUtils.getBooleanAttribute(methodDecl, "isStatic", "false");
        boolean isPublic = NodeUtils.getBooleanAttribute(methodDecl, "isPublic", "false");
        int numParams = NodeUtils.getIntegerAttribute(methodDecl, "numParams", "0");

        if(isPublic && isStatic && isVoid && numParams == 1){
            var param = methodDecl.getChild(1).getChild(0);
            var paramType = TypeUtils.getType(param);
            if(paramType.getName().equals("String") && paramType.isArray()){
                return null;
            }
        }

        // Create error report
        var message = "Incorrect declaration of main method.";
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
