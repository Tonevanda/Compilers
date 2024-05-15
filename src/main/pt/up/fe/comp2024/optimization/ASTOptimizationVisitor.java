package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.ArrayList;

import static pt.up.fe.comp2024.ast.Kind.*;

public class ASTOptimizationVisitor extends AJmmVisitor<Void, Void> {

    private final SymbolTable table;

    public ASTOptimizationVisitor(SymbolTable table) {
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

        // Get the function arguments
        var arguments = table.getParameters(funcName);

        // Check if last argument is varargs
        var lastArg = arguments.get(arguments.size() - 1);
        var isVarArgs = lastArg.getType().getObject("isVarargs") != null;
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
        JmmNode arrayInit = new JmmNodeImpl(ARRAY_INIT.toString());
        arrayInit.setChildren(varArgs);
        node.add(arrayInit);

        System.out.println("Function children: " + node.getChildren());
        System.out.println("ArrayInit children: " + arrayInit.getChildren());
        System.out.println(node.getChildren().subList(1, node.getNumChildren()));

        return null;
    }

    private Void defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return null;
    }
}
