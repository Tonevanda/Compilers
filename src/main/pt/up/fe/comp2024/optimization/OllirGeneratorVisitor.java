package pt.up.fe.comp2024.optimization;

import org.w3c.dom.Node;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";

    private boolean methodIsStatic;

    private boolean methodIsPublic;

    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        // TODO: PERGUNTAR AO STOR SE É PRECISO PARA ESTE CHECKPOINT CENAS TIPO B = new B();
        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);

        setDefaultVisit(this::defaultVisit);
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);

        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    // TODO: Implement the visitFunctionCall method
    //  invokevirtual: used for instance methods
    //  invokestatic: used for static methods
    //  invokespecial: used for constructors, private methods and methods of the super class
    //  NOTE: In cases like this.foo(), the first argument of the invokevirtual this + the name of the class
    //  NOTE: The way the visitor is implemented now, functionCalls only get visited if they are direct
    //  children of a method declaration, so I kinda need to change the visitAssignStmt to visit the functionCall children
    private String visitFunctionCall(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        var methodName = node.get("func");
        var numArgs = NodeUtils.getIntegerAttribute(node, "numArgs", "0");

        // get the function call arguments
        var arguments = node.getChildren().subList(1, node.getNumChildren());

        // for every argument, get the computation and append to code
        for (var argument : arguments) {
            var argumentCode = exprVisitor.visit(argument).getComputation();
            code.append(argumentCode);
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
            var param = node.getJmmChild(i);
            var paramCode = exprVisitor.visit(param);
            code.append(paramCode.getCode());
        }

        code.append(")");

        // Get the method's return type
        var retType = table.getReturnType(methodName);
        if(retType != null){
            code.append(OptUtils.toOllirType(retType));
        } else {
            code.append(".V");
        }

        code.append(END_STMT);
        System.out.println("Final code: " + code);

        return code.toString();
    }

    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }

    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        // these are class fields, so I can use them in the visitFunctionCall
        this.methodIsPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");
        this.methodIsStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");

        if (methodIsPublic) {
            code.append("public ");
        }

        if (methodIsStatic){
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // get number of parameters
        var numParams = NodeUtils.getIntegerAttribute(node, "numParams", "0");

        // visit every parameter
        // TODO: if parameter is array, need to append .array between the name and the type
        code.append("(");
        for(int i = 1; i <= numParams; i++){
            if(i != 1){
                code.append(", ");
            }
            var param = node.getJmmChild(i);
            var paramCode = visit(param);
            code.append(paramCode);
        }
        code.append(")");

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        var afterParam = numParams + 1;
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            //System.out.println("Child: " + child);
            var childCode = visit(child);
            System.out.println("Child code: " + childCode);
            code.append(childCode);
        }

        // the main method does not have a return statement but its ollir representation should have one
        // kinda hard coded, but it's fine since it'll always be ret.V
        if(name.equals("main")){
            code.append("ret.V;\n");
        }
        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());
        if(table.getSuper() != null){
            code.append(" extends ");
            code.append(table.getSuper());
        }
        code.append(L_BRACKET);

        code.append(NL);

        table.getFields().forEach(field -> {
            code.append(".field private ");
            code.append(field.getName());
            code.append(OptUtils.toOllirType(field.getType()));
            code.append(END_STMT);
            code.append(NL);
        });

        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }

    private String visitImportDecl(JmmNode node, Void unused) {
        List<String> importNames = TypeUtils.getImportNames(node);
        StringBuilder importString = new StringBuilder();

        for (String name : importNames) {
            if (!importString.isEmpty()) {
                importString.append(".");
            }
            importString.append(name);
        }

        return "import " + importString + ";\n";
    }

    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
