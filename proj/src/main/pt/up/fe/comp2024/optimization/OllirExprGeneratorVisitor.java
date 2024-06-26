package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import java.util.ArrayList;
import java.util.List;

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
        addVisit(THIS, this::visitThis);
        addVisit(PAREN_EXPR, this::visitParenExpr);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(UNARY_EXPR, this::visitUnExpr);
        addVisit(INT_LITERAL, this::visitInteger);
        addVisit(BOOL_LITERAL, this::visitBoolean);
        addVisit(ARRAY_INIT, this::visitArrayInit);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);
        addVisit(NEW_CLASS_OBJ, this::visitNewClassObj);
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(ARR_ACCESS_EXPR, this::visitArrAccessExpr);
        addVisit(LENGTH_CALL, this::visitLengthCall);

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

    private OllirExprResult visitThis(JmmNode node, Void unused){
        var type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);
        String code = "this" + ollirType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitParenExpr(JmmNode node, Void unused){
        var child = visit(node.getJmmChild(0));
        return new OllirExprResult(child.getCode(), child.getComputation());
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
        var classType = TypeUtils.getExprType(node, table);
        var ollirType = OptUtils.toOllirType(classType);

        String code = OptUtils.getTemp() + ollirType;

        // Visit what's between brackets to get the computation
        var child = visit(node.getJmmChild(0));
        computation.append(child.getComputation());

        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                .append("new").append("(").append("array").append(", ").append(child.getCode()).append(")")
                .append(ollirType).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitArrAccessExpr(JmmNode node, Void unused){

        // If it has the attribute isLeftSide, it means that the array access is on the left side of an assignment
        var isLeftSide = node.getOptionalObject("isLeftSide").isPresent() ? node.getObject("isLeftSide").toString() : "false";

        StringBuilder computation = new StringBuilder();
        var arrayNode = node.getJmmChild(0);
        var array = visit(node.getJmmChild(0));
        var index = visit(node.getJmmChild(1));

        computation.append(array.getComputation());
        computation.append(index.getComputation());

        String name;
        if(arrayNode.isInstance(ARRAY_INIT)){
            // Only the name of the array is needed, not the ollir type
            name = array.getCode().split("\\.")[0];
        } else {
            name = arrayNode.get("name");
        }

        // If the array access is on the left side of an assignment, we don't create a temp variable
        if(isLeftSide.equals("true")){
            String code = name + "[" + index.getCode() + "]" +
                    OptUtils.toOllirType(TypeUtils.getExprType(node, table));
            return new OllirExprResult(code, computation);
        }

        String code = OptUtils.getTemp() + OptUtils.toOllirType(TypeUtils.getExprType(node, table));
        computation.append(code).append(SPACE).append(ASSIGN).
                append(OptUtils.toOllirType(TypeUtils.getExprType(node, table))).append(SPACE)
                .append(name).append("[").append(index.getCode()).append("]")
                .append(OptUtils.toOllirType(TypeUtils.getExprType(node, table))).append(END_STMT);

        return new OllirExprResult(code, computation);
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

    private OllirExprResult visitLengthCall(JmmNode node, Void unused){
        var array = visit(node.getJmmChild(0));
        StringBuilder computation = new StringBuilder();

        // Compute the array being called
        computation.append(array.getComputation());
        var newArray = array.getCode();

        // Get the code
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE)
                .append("arraylength").append("(").append(newArray).append(")").append(".i32").append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();

        // Append the computation of the caller
        var lhs = visit(node.getJmmChild(0));
        computation.append(lhs.getComputation());

        var methodName = node.get("func");

        // Infer the type of the method
        var type = table.getReturnType(methodName);
        var assignStmt = node.getAncestor(ASSIGN_STMT);
        boolean isPartOfAssignment = assignStmt.isPresent();
        if(isPartOfAssignment){
            var assignLHS = assignStmt.get().getJmmChild(0);
            type = TypeUtils.getExprType(assignLHS, table);
        }
        if(type == null){
            type = new Type("void", false);
        }


        // Append the computation of the arguments
        var numArgs = NodeUtils.getIntegerAttribute(node, "numArgs", "0");
        var arguments = node.getChildren().subList(1, node.getNumChildren());

        List<String> codeArguments = new ArrayList<>();
        for (var argument : arguments) {
            var argumentCode = visit(argument);

            computation.append(argumentCode.getComputation());
            codeArguments.add(argumentCode.getCode());
        }

        // If it's not a void method, we assign the result to a temp variable
        boolean isVoid = type.getName().equals("void");
        String code = !isVoid ? OptUtils.getTemp() + OptUtils.toOllirType(type) : "";
        if(!isVoid){
            computation.append(code).append(SPACE).append(ASSIGN)
                    .append(OptUtils.toOllirType(type)).append(SPACE);
        }

        var callerType = TypeUtils.getExprType(node.getJmmChild(0), table);

        // Infer the type of invoke to use
        if(callerType.getName().equals("import") || (callerType.getOptionalObject("import").isPresent()) && callerType.getOptionalObject("import").get().equals(true)){
            computation.append("invokestatic(");
        } else if (methodName.equals(table.getClassName())){
            computation.append("invokespecial(");
        } else {
            computation.append("invokevirtual(");
        }

        // Append the code of the caller
        computation.append(lhs.getCode());

        // Append the code of the arguments
        computation.append(", ");
        computation.append(String.format("\"%s\"", methodName));
        for(int i = 1; i <= numArgs; i++){
            computation.append(", ");
            computation.append(codeArguments.get(i-1));
        }

        computation.append(")").append(OptUtils.toOllirType(type)).append(END_STMT);

        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult visitUnExpr(JmmNode node, Void unused){

        var child = visit(node.getJmmChild(0));

        StringBuilder computation = new StringBuilder();

        // Get the computation of the child
        computation.append(child.getComputation());

        // Get the code of the child
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE)
                .append(node.get("op")).append(resOllirType).append(SPACE)
                .append(child.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);

        // Specific case where we increment the variable by a constant value between -128 and 127
        // This is to avoid the creation of a temp variable
        // It's really ugly and I hate it but it's to simplify the jasmin generation to use the iinc instruction
        var assignNode = node.getAncestor(ASSIGN_STMT);
        if(assignNode.isPresent()){
            var assignLHS = assignNode.get().getJmmChild(0);
            if(assignLHS.getKind().equals(VAR.toString())){
                var varName = assignLHS.get("name");
                var assignRHSNode1 = node.getJmmChild(0);
                var assignRHSNode2 = node.getJmmChild(1);
                // If one of the binaryExpr operands is a variable and the other is an int literal
                if(assignRHSNode1.getKind().equals(VAR.toString()) && assignRHSNode2.getKind().equals(INT_LITERAL.toString())){
                    var varName2 = assignRHSNode1.get("name");
                    var intVal = Integer.parseInt(assignRHSNode2.get("value"));
                    // If the variable names are the same and the int value is between -128 and 127
                    if(varName.equals(varName2) && intVal >= -128 && intVal <= 127){
                        String code = lhs.getCode() + SPACE + node.get("op") + ".i32" + SPACE +
                                rhs.getCode();
                        return new OllirExprResult(code, computation);
                    }
                }
                // Same case as before but with the operands inverted
                else if (assignRHSNode2.getKind().equals(VAR.toString()) && assignRHSNode1.getKind().equals(INT_LITERAL.toString())){
                    var varName2 = assignRHSNode2.get("name");
                    var intVal = Integer.parseInt(assignRHSNode1.get("value"));
                    if(varName.equals(varName2) && intVal >= -128 && intVal <= 127){
                        String code = lhs.getCode() + SPACE + node.get("op") + ".i32" + SPACE +
                                rhs.getCode();
                        return new OllirExprResult(code, computation);
                    }

                }
            }
        }

        String code = OptUtils.getTemp() + resOllirType;

        var operator = node.get("op");

        // In case the operator is short-circuit
        if(operator.equals("&&")){
            var labels = OptUtils.getIfLabels();
            var trueLabel = labels.get(0);
            var falseLabel = labels.get(1);
            computation.append("if (").append(lhs.getCode()).append(") goto ").append(trueLabel).append(END_STMT);
            computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append("0").append(resOllirType).append(END_STMT);
            computation.append("goto ").append(falseLabel).append(END_STMT);
            computation.append(trueLabel).append(":").append("\n");
            computation.append(rhs.getComputation());
            computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(rhs.getCode()).append(END_STMT);
            computation.append(falseLabel).append(":").append("\n");
            return new OllirExprResult(code, computation);
        }

        computation.append(rhs.getComputation());
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

        if(type.getOptionalObject("import").isPresent() && type.getOptionalObject("import").get().equals(true)){
            return new OllirExprResult(id);
        }

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
     * @param node The node to visit
     * @param unused Unused parameter
     * @return An empty OllirExprResult
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
}
