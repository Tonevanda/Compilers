package pt.up.fe.comp2024.optimization;

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

    private boolean methodIsVarargs;

    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(MULT_STMT, this::visitMultStmt);
        addVisit(IF_STMT, this::visitIfStmt);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitExprStmt(JmmNode node, Void unused) {

        var expr = exprVisitor.visit(node.getJmmChild(0));
        StringBuilder code = new StringBuilder();

        code.append(expr.getComputation());
        //code.append(expr.getCode());

        return code.toString();
    }

    private String visitMultStmt(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        for (var child : node.getChildren()) {
            code.append(visit(child));
        }

        return code.toString();
    }

    private String visitIfStmt(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        var condition = exprVisitor.visit(node.getJmmChild(0));
        var thenStmt = node.getJmmChild(1);
        var elseStmt = node.getJmmChild(2);

        code.append(condition.getComputation());

        // Get the next labels
        var labels = OptUtils.getIfLabels();
        var ifLabel = labels.get(0);
        var endifLabel = labels.get(1);

        code.append("if ");
        code.append("(");
        code.append(condition.getCode());
        code.append(") ");
        code.append("goto ");
        code.append(ifLabel);
        code.append(END_STMT);

        // Append all the code inside the else statement
        code.append(visit(elseStmt));

        code.append("goto ");
        code.append(endifLabel);
        code.append(END_STMT);

        code.append(ifLabel);
        code.append(":");
        code.append(NL);

        code.append(visit(thenStmt));

        code.append(endifLabel);
        code.append(":");
        code.append(NL);

        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();

        // Get the next labels
        var labels = OptUtils.getWhileLabels();
        var whileCondLabel = labels.get(0);
        var whileLoopLabel = labels.get(1);
        var whileEndLabel = labels.get(2);

        // Loop pre-condition
        code.append(whileCondLabel);
        code.append(":");
        code.append(NL);

        var condition = exprVisitor.visit(node.getJmmChild(0));
        var whileBody = node.getJmmChild(1);

        code.append(condition.getComputation());

        code.append("if ");
        code.append("(");
        code.append(condition.getCode());
        code.append(") ");
        code.append("goto ");
        code.append(whileLoopLabel);
        code.append(END_STMT);
        code.append("goto ");
        code.append(whileEndLabel);
        code.append(END_STMT);

        // Loop body
        code.append(whileLoopLabel);
        code.append(":");
        code.append(NL);

        code.append(visit(whileBody));

        code.append("goto ");
        code.append(whileCondLabel);
        code.append(END_STMT);

        // Loop post-condition
        code.append(whileEndLabel);
        code.append(":");
        code.append(NL);

        return code.toString();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        // If LHS is a field, we want to use putfield instead of the default assignment
        if(isField(node.getJmmChild(0))){
            StringBuilder computation = new StringBuilder();

            var ollirType = OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0), table));
            var ollirCode = node.getJmmChild(0).get("name") + ollirType;

            var rhs = exprVisitor.visit(node.getJmmChild(1));
            String tempCode = rhs.getCode();

            computation.append(rhs.getComputation());
            computation.append("putfield(").append("this").append(", ").append(ollirCode).append(", ").append(tempCode).append(")").append(".V").append(END_STMT);

            return computation.toString();
        }
        var node1 = node.getJmmChild(0);
        var node2 = node.getJmmChild(1);

        node1.put("isLeftSide", "true");

        var lhs = exprVisitor.visit(node1);
        var rhs = exprVisitor.visit(node2);

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

        // get number of parameters
        var numParams = NodeUtils.getIntegerAttribute(node, "numParams", "0");

        // Check if method is varargs by checking if the last parameter is varargs
        if (numParams > 0) {
            var lastParam = node.getJmmChild(numParams).getChild(0);
            if(lastParam.getObject("isVarargs").equals(true)) code.append("varargs ");
        }

        // name
        var name = node.get("name");
        code.append(name);


        // visit every parameter
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
            var childCode = visit(child);
            code.append(childCode);
        }

        // void methods don't have return, but their ollir representation does
        if(table.getReturnType(name).getName().equals("void")){
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
            code.append(".field public ");
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

    // Returns true if the node is a field, not a local or parameter
    public boolean isField(JmmNode node){
        // If the node is not a variable, it is not a field
        if(!node.getKind().equals(VAR.toString())) return false;

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
