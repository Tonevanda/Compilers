package pt.up.fe.comp2024.optimization;

import org.w3c.dom.Node;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.comp2024.ast.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INT_LITERAL, this::visitInteger);
        addVisit(BOOL_LITERAL, this::visitBoolean);
        addVisit(ARRAY_INIT, this::visitArrayInit);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);
        addVisit(NEW_CLASS_OBJ, this::visitNewClassObj);
        addVisit(NEW_ARRAY, this::visitNewArray);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused){
        var boolType = new Type(TypeUtils.getBooleanTypeName(), false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String code = (node.get("value").equals("true") ? "1" : "0")  + ollirBoolType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitNewClassObj(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();
        var classType = TypeUtils.getExprType(node, table);
        var ollirType = OptUtils.toOllirType(classType);

        String code = OptUtils.getTemp() + ollirType;

        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                .append("new").append("(").append(classType.getName()).append(")").append(ollirType).append(END_STMT);

        computation.append("invokespecial(").append(code).append(", \"<init>\").V").append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused){
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        // Visit what's between brackets to get the computation
        var child = visit(node.getJmmChild(0));
        computation.append(child.getComputation());

        code.append("new").append("(").append("array").append(", ").append(child.getCode()).append(")")
                .append(OptUtils.toOllirType(TypeUtils.getExprType(node, table)));

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitArrayInit(JmmNode node, Void unused){

        var numArgs = Integer.parseInt(node.getObject("numArrayArgs").toString());

        String temp = OptUtils.getTemp();
        String code = temp + OptUtils.toOllirType(TypeUtils.getExprType(node, table));
        StringBuilder computation = new StringBuilder();

        // Initialize the array
        computation.append(code).append(SPACE).append(ASSIGN).append(OptUtils.toOllirType(TypeUtils.getExprType(node, table))).
                append(SPACE).append("new").append("(").append("array").append(", ").append(numArgs).append(".i32").append(")")
                .append(OptUtils.toOllirType(TypeUtils.getExprType(node, table))).append(END_STMT);

        // Get the computation of the children
        ArrayList<String> codeArguments = new ArrayList<>();
        for (int i = 0; i < numArgs; i++) {
            var child = visit(node.getJmmChild(i));
            computation.append(child.getComputation());
            codeArguments.add(child.getCode());
        }

        // Assign the values to the array
        for(int i = 0; i < codeArguments.size(); i++){
            computation.append(temp).append("[").append(i).append(".i32").append("]").append(OptUtils.toOllirType(TypeUtils.getExprType(node, table)))
                    .append(SPACE).append(ASSIGN).append(OptUtils.toOllirType(TypeUtils.getExprType(node, table))).append(SPACE)
                    .append(codeArguments.get(i)).append(END_STMT);
        }

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var methodName = node.get("func");

        StringBuilder computation = new StringBuilder();

        // Get the computation of the left function call
        computation.append(lhs.getComputation());

        var type = table.getReturnType(methodName);
        var assignStmt = node.getAncestor(ASSIGN_STMT);
        boolean isPartOfAssignment = assignStmt.isPresent();
        boolean isExprStmt = node.getParent().getKind().equals(EXPR_STMT.toString());
        if(isPartOfAssignment){
            var assignLHS = assignStmt.get().getJmmChild(0);
            type = TypeUtils.getExprType(assignLHS, table);
        }
        if(type == null){
            type = new Type("void", false);
        }

        String code = !isExprStmt ? OptUtils.getTemp() + OptUtils.toOllirType(type) : "";

        var numArgs = NodeUtils.getIntegerAttribute(node, "numArgs", "0");
        var arguments = node.getChildren().subList(1, node.getNumChildren());

        List<String> codeArguments = new ArrayList<>();
        for (var argument : arguments) {
            var argumentCode = visit(argument);

            computation.append(argumentCode.getComputation());
            codeArguments.add(argumentCode.getCode());
        }


        if(!isExprStmt){
            computation.append(code).append(SPACE).append(ASSIGN)
                    .append(OptUtils.toOllirType(type)).append(SPACE);
        }

        boolean isStatic = false;
        if(!node.getChild(0).isInstance(FUNCTION_CALL)){
            if(!isObject(node.getChild(0))){
                computation.append("invokestatic(");
                computation.append(node.getChild(0).get("name"));
                isStatic = true;
            } else if (methodName.equals(table.getClassName())){
                computation.append("invokespecial(this");
            } else {
                computation.append("invokevirtual(");
                computation.append(node.getChild(0).get("name"));
            }
        }

        if(!isStatic){
            computation.append(".");
            computation.append(TypeUtils.getExprType(node.getChild(0), table).getName());
        }

        computation.append(", ");
        computation.append(String.format("\"%s\"", methodName));
        for(int i = 1; i <= numArgs; i++){
            computation.append(", ");
            computation.append(codeArguments.get(i-1));
        }

        computation.append(")").append(OptUtils.toOllirType(type)).append(END_STMT);

        return new OllirExprResult(code, computation.toString());
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);

        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        StringBuilder computation = new StringBuilder();
        // If var is a field, we want to use getfield instead of the default variable reference
        if(isField(node)){
            var tempCode = OptUtils.getTemp() + ollirType;
            computation.append(tempCode).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                    .append("getfield(").append("this").append(", ").append(code).append(")").append(ollirType).append(END_STMT);
            code = tempCode;
        }

        return new OllirExprResult(code, computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

    private boolean isField(JmmNode node){
        var methodParentName = node.getAncestor(METHOD_DECL).get().get("name");
        if(table.getLocalVariables(methodParentName).stream().anyMatch(var -> var.getName().equals(node.get("name")))){
            return false;
        }
        if(table.getParameters(methodParentName).stream().anyMatch(var -> var.getName().equals(node.get("name")))){
            return false;
        }
        if(table.getFields().stream().anyMatch(var -> var.getName().equals(node.get("name")))){
            return true;
        }
        return false;
    }

    private boolean isObject(JmmNode node){
        var methodParentName = node.getAncestor(METHOD_DECL).get().get("name");
        if(node.get(("name")).equals("this")){
            return true;
        }
        if(table.getLocalVariables(methodParentName).stream().anyMatch(var -> var.getName().equals(node.get("name")))){
            return true;
        }
        if(table.getParameters(methodParentName).stream().anyMatch(var -> var.getName().equals(node.get("name")))){
            return true;
        }
        if(table.getFields().stream().anyMatch(var -> var.getName().equals(node.get("name")))){
            return true;
        }
        return false;
    }
}
