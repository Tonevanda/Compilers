package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class ASTConstPropagationVisitor extends AJmmVisitor<Void, Void> {

    // Map of int variable names and their constant values
    private final HashMap<String, Integer> intConstants;
    private final HashMap<String, Boolean> boolConstants;

    private boolean madeChanges;

    public ASTConstPropagationVisitor() {
        this.intConstants = new HashMap<>();
        this.boolConstants = new HashMap<>();
        this.madeChanges = false;
    }

    public boolean madeChanges() {
        return madeChanges;
    }

    @Override
    protected void buildVisitor() {
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR, this::visitVar);
        addVisit(WHILE_STMT, this::visitWhileStmt);

        setDefaultVisit(this::defaultVisit);
    }

    private Void visitAssignStmt(JmmNode assignStmt, Void unused) {
        // Get assignment's right side
        var assignVal = assignStmt.getChild(1);
        if (!assignStmt.getChild(0).isInstance(VAR)) return null;
        var assignId = assignStmt.getChild(0).get("name");
        // Check if a constant value is being assigned
        if (Objects.equals(assignVal.getKind(), INT_LITERAL.toString())) {
            // Add to constants map
            this.intConstants.put(assignId, Integer.parseInt(assignVal.get("value")));
        } else if (Objects.equals(assignVal.getKind(), BOOL_LITERAL.toString())) {
            // Add to constants map
            this.boolConstants.put(assignId, Boolean.parseBoolean(assignVal.get("value")));
        } else { // If not continue visiting
            this.intConstants.remove(assignId);
            this.boolConstants.remove(assignId);
            visit(assignVal);
        }
        return null;
    }

    private Void visitVar(JmmNode var, Void unused) {
        Kind kind;
        // If the visiting var is a constant, we must change it
        if (this.intConstants.containsKey(var.get("name"))) kind = INT_LITERAL;
        else if (this.boolConstants.containsKey(var.get("name"))) kind = BOOL_LITERAL;
        else return null;

        var index = var.getIndexOfSelf();
        var varParent = var.getParent();
        varParent.removeChild(index);

        // new IntLiteral node
        JmmNodeImpl literal = new JmmNodeImpl(kind.toString());
        literal.putObject("value", ((kind==INT_LITERAL)?this.intConstants:this.boolConstants).get(var.get("name")));
        literal.putObject("numArgs", 0);
        literal.putObject("numArrayArgs", 0);

        //add hierarchy
        Collection<String> hierarchy = new ArrayList<>();
        hierarchy.add(kind.toString());
        hierarchy.add("Expr");
        literal.setHierarchy(hierarchy);

        varParent.add(literal, index);
        this.madeChanges = true;

        return null;
    }
    
    private Void visitWhileStmt(JmmNode whileStmt, Void unused) {
        for (var child : whileStmt.getChild(1).getChildren()) {
            if (child.isInstance(ASSIGN_STMT) && child.getChild(0).isInstance(VAR)){
                String var = child.getChild(0).get("name");
                this.intConstants.remove(var);
                this.boolConstants.remove(var);
            }
        }
        visit(whileStmt.getChild(1));
        visit(whileStmt.getChild(0));
        return null;
    }

    private Void defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return null;
    }
}
