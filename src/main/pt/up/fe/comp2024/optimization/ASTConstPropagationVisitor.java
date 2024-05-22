package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class ASTConstPropagationVisitor extends AJmmVisitor<Void, Void> {

    // Map of int variable names and their constant values
    private HashMap<String, Integer> int_constants;
    private HashMap<String, Boolean> bool_constants;

    public ASTConstPropagationVisitor() {
        this.int_constants = new HashMap<>();
        this.bool_constants = new HashMap<>();
    }

    @Override
    protected void buildVisitor() {
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR, this::visitVar);

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
            this.int_constants.put(assignId, Integer.parseInt(assignVal.get("value")));
            // Remove assign statement
            assignStmt.getParent().removeChild(assignStmt);
        } else if (Objects.equals(assignVal.getKind(), BOOL_LITERAL.toString())) {
            // Add to constants map
            this.bool_constants.put(assignId, Boolean.parseBoolean(assignVal.get("value")));
            // Remove assign statement
            assignStmt.getParent().removeChild(assignStmt);
        } else { // If not continue visiting
            this.int_constants.remove(assignId);
            visit(assignVal);
        }
        return null;
    }

    private Void visitVar(JmmNode var, Void unused) {
        Kind type;
        // If the visiting var is a constant, we must change it
        if (this.int_constants.containsKey(var.get("name"))) type = INT_LITERAL;
        else if (this.bool_constants.containsKey(var.get("name"))) type = BOOL_LITERAL;
        else return null;

        var index = var.getIndexOfSelf();
        var varParent = var.getParent();
        varParent.removeChild(index);

        // new IntLiteral node
        JmmNodeImpl literal = new JmmNodeImpl(type.toString());
        literal.putObject("value", ((type==INT_LITERAL)?this.int_constants:this.bool_constants).get(var.get("name")));

        //add hierarchy
        Collection<String> hierarchy = new ArrayList<>();
        hierarchy.add(type.toString());
        hierarchy.add("Expr");
        literal.setHierarchy(hierarchy);

        varParent.add(literal, index);

        return null;
    }

    private Void defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return null;
    }
}
