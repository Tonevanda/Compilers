package pt.up.fe.comp2024.optimization;

import org.w3c.dom.Node;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

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
        addVisit(FUNCTION_CALL, this::visitFunctionCall);
        addVisit(NEW_CLASS_OBJ, this::visitNewClassObj);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    // TODO: Currently does not support constructors with arguments
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

    //  invokevirtual: used for instance methods
    //  invokestatic: used for static methods
    //  invokespecial: used for constructors and methods of the super class
    //  This currently does not work if the function call is being used in an assignment
    //  Code should just be the temp value at the end of the computation
    //  Computation includes calculating the parameter, i.e 1+2 is t0 := 1 + 2
    //  Computation also includes assigning the invoke code to a temp value
    //  a = this.bar() is t0 := invokevirtual(this, "bar");
    // TODO: Function Calls outside assignments should not be assigned to a temp value
    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var methodName = node.get("func");

        StringBuilder computation = new StringBuilder();
        computation.append(lhs.getComputation());

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
        String code = isPartOfAssignment ? OptUtils.getTemp() + OptUtils.toOllirType(type) : "";

        var numArgs = NodeUtils.getIntegerAttribute(node, "numArgs", "0");

        var arguments = node.getChildren().subList(1, node.getNumChildren());

        List<String> codeArguments = new ArrayList<>();
        for (var argument : arguments) {
            var argumentCode = visit(argument);
            computation.append(argumentCode.getComputation());
            codeArguments.add(argumentCode.getCode());
        }

        if(isPartOfAssignment){
            computation.append(code).append(SPACE).append(ASSIGN)
                    .append(OptUtils.toOllirType(type)).append(SPACE);
        }

        boolean isStatic = false;
        if(!table.getMethods().contains(methodName) || methodName.equals("main")){
            computation.append("invokestatic(");
            computation.append(node.getChild(0).get("name"));
            isStatic = true;
        } else if (methodName.equals(table.getClassName())){
            computation.append("invokespecial(this");
        } else {
            computation.append("invokevirtual(");
            computation.append(node.getChild(0).get("name"));
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

        System.out.println(computation);

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
        /*if(type.getAttributes().contains("isImported")){
            if(type.getObject("isImported").equals(true)){
                return new OllirExprResult(id);
            }
        }*/

        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
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

}
