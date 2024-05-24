package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class ASTConstFoldingVisitor extends AJmmVisitor<Void, Void> {

    private boolean madeChanges;

    public ASTConstFoldingVisitor() {
        this.madeChanges=false;
    }

    public boolean madeChanges() {
        return madeChanges;
    }

    @Override
    protected void buildVisitor() {
        addVisit(BINARY_EXPR, this::visitBinaryExpr);

        setDefaultVisit(this::defaultVisit);
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, Void unused) {
        var op = binaryExpr.get("op");
        visit(binaryExpr.getChild(0));
        visit(binaryExpr.getChild(1));
        var left = binaryExpr.getChild(0);
        var right = binaryExpr.getChild(1);
        JmmNodeImpl literal;
        if (left.isInstance(INT_LITERAL) && right.isInstance(INT_LITERAL)) {
            int result;
            switch (op) {
                case "+":
                    result = Integer.parseInt(left.get("value")) + Integer.parseInt(right.get("value"));
                    break;
                case "-":
                    result = Integer.parseInt(left.get("value")) - Integer.parseInt(right.get("value"));
                    break;
                case "*":
                    result = Integer.parseInt(left.get("value")) * Integer.parseInt(right.get("value"));
                    break;
                case "/":
                    result = Integer.parseInt(left.get("value")) / Integer.parseInt(right.get("value"));
                    break;
                default:
                    return null;
            }
            // new IntLiteral node
            literal = new JmmNodeImpl(INT_LITERAL.toString());
            literal.putObject("value", result);
            literal.putObject("numArgs", 0);
            literal.putObject("numArrayArgs", 0);
            // hierarchy
            Collection<String> hierarchy = new ArrayList<>();
            hierarchy.add(INT_LITERAL.toString());
            hierarchy.add("Expr");
            literal.setHierarchy(hierarchy);

        } else if (left.isInstance(BOOL_LITERAL) && right.isInstance(BOOL_LITERAL)) {
            boolean result;
            switch (op) {
                case "&&":
                    result = Boolean.parseBoolean(left.get("value")) && Boolean.parseBoolean(right.get("value"));
                    break;
                case "||":
                    result = Boolean.parseBoolean(left.get("value")) || Boolean.parseBoolean(right.get("value"));
                    break;
                default:
                    return null;
            }
            // new BoolLiteral node
            literal = new JmmNodeImpl(BOOL_LITERAL.toString());
            literal.putObject("value", result);
            literal.putObject("numArgs", 0);
            literal.putObject("numArrayArgs", 0);
            // hierarchy
            Collection<String> hierarchy = new ArrayList<>();
            hierarchy.add(BOOL_LITERAL.toString());
            hierarchy.add("Expr");
            literal.setHierarchy(hierarchy);
        } else return null;

        var parent = binaryExpr.getParent();
        int i = binaryExpr.getIndexOfSelf();
        parent.removeChild(binaryExpr);
        parent.add(literal, i);
        this.madeChanges= true;
        return null;
    }

    private Void defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return null;
    }
}
