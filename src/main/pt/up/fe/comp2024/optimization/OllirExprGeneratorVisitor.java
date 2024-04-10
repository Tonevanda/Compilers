package pt.up.fe.comp2024.optimization;

import org.w3c.dom.Node;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import java.util.ArrayList;
import java.util.List;

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

    private OllirExprResult visitNewClassObj(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();
        var classType = TypeUtils.getExprType(node, table);
        var ollirType = OptUtils.toOllirType(classType);

        String code = OptUtils.getTemp() + ollirType;

        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                .append("new").append("(").append(classType.getName()).append(")").append(ollirType).append(END_STMT);


        // this assumes that a new object init is always in an assign statement

        computation.append("invokespecial(").append(code).append(", \"<init>\").V").append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    // TODO: Implement the visitFunctionCall method
    //  invokevirtual: used for instance methods
    //  invokestatic: used for static methods
    //  invokespecial: used for constructors and methods of the super class
    //  This currently does not work if the function call is being used in an assignment
    //  Code should just be the temp value at the end of the computation
    //  Computation includes calculating the parameter, i.e 1+2 is t0 := 1 + 2
    //  Computation also includes assigning the invoke code to a temp value
    //  a = this.bar() is t0 := invokevirtual(this, "bar");
    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        var methodName = node.get("func");
        var numArgs = NodeUtils.getIntegerAttribute(node, "numArgs", "0");

        // get the function call arguments
        var arguments = node.getChildren().subList(1, node.getNumChildren());

        StringBuilder computation = new StringBuilder();

        // for every argument, get the computation and the code
        List<String> codeArguments = new ArrayList<>();
        for (var argument : arguments) {
            var argumentCode = visit(argument);
            codeArguments.add(argumentCode.getCode());
            computation.append(argumentCode.getComputation());
        }

        // if method is static or has not been declared (assume it exists in imported or extended class)
        // TODO: expand upon this later
        boolean isStatic = false;
        if(!table.getMethods().contains(methodName)){
            code.append("invokestatic(");
            code.append(node.getChild(0).get("name"));
            isStatic = true;
        } else if (methodName.equals(table.getClassName())){
            code.append("invokespecial(this");
        } else {
            code.append("invokevirtual(");
            code.append(node.getChild(0).get("name"));
        }

        if(!isStatic){
            code.append(".");
            code.append(TypeUtils.getExprType(node.getChild(0), table).getName());
        }

        code.append(", ");
        code.append(String.format("\"%s\"", methodName));
        for(int i = 1; i <= numArgs; i++){
            code.append(", ");
            code.append(codeArguments.get(i-1));
        }

        code.append(")");

        // Get the method's return type
        var retType = table.getReturnType(methodName);
        if(retType != null){
            code.append(OptUtils.toOllirType(retType));
        } else {
            code.append(".V");
        }

        return new OllirExprResult(code.toString(), computation.toString());
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
