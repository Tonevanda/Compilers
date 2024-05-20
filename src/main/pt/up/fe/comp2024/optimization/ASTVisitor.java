package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.NodeUtils;
import java.util.ArrayList;
import java.util.Collection;


import static pt.up.fe.comp2024.ast.Kind.*;

public class ASTVisitor extends AJmmVisitor<Void, Void> {

    private final SymbolTable table;

    public ASTVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(FUNCTION_CALL, this::visitFunctionCall);

        setDefaultVisit(this::defaultVisit);
    }

    private Void visitFunctionCall(JmmNode node, Void unused) {

        // Get the function name
        var funcName = node.get("func");

        // Check if function is local method, if not we don't change the AST
        if (!table.getMethods().contains(funcName)) return null;

        // Get the function arguments
        var arguments = table.getParameters(funcName);

        // Check if last argument is varargs
        if(arguments.isEmpty()) return null;

        var lastArg = arguments.get(arguments.size() - 1);
        var isVarArgs = lastArg.getType().getObject("isVarargs").equals(true);
        if(!isVarArgs) return null;

        var argumentCount = arguments.size();
        var nonVarArgsCount = argumentCount - 1;
        if(nonVarArgsCount < 0) nonVarArgsCount = 0;
        var paramsPassed = NodeUtils.getIntegerAttribute(node, "numArgs", "0");

        // Get all the arguments that are part of the varargs
        var numVarArgs = paramsPassed - nonVarArgsCount;
        ArrayList<JmmNode> varArgs = new ArrayList<>();
        for (int i = 0; i < numVarArgs; i++) {
            var arg = node.getChild(1 + nonVarArgsCount + i);
            varArgs.add(arg);
        }

        for(var arg : varArgs){
            node.removeChild(arg);
        }

        // Create ArrayInitCall node
        JmmNodeImpl arrayInit = new JmmNodeImpl(ARRAY_INIT.toString());
        arrayInit.setChildren(varArgs);

        // Set ArrayInit Attributes
        arrayInit.putObject("isArray", true);
        arrayInit.putObject("numArrayArgs", numVarArgs);

        // Set ArrayInit Hierarchy
        Collection<String> hierarchy = new ArrayList<>();
        hierarchy.add(ARRAY_INIT.toString());
        hierarchy.add("Expr");
        arrayInit.setHierarchy(hierarchy);

        node.add(arrayInit);

        // Update function call numArgs
        String newNumArgs = String.valueOf(nonVarArgsCount + 1);
        node.putObject("numArgs", newNumArgs);

        return null;
    }

    private Void defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return null;
    }
}
