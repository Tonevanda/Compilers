package pt.up.fe.comp2024.optimization;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class ASTConstPropagationVisitor extends AJmmVisitor<Void, Void> {

    // Map of variable names and their constant values
    private HashMap<String, Integer> constants;

    public ASTConstPropagationVisitor() {
        this.constants = new HashMap<>();
    }

    @Override
    protected void buildVisitor() {
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR, this::visitVar);

        setDefaultVisit(this::defaultVisit);
    }

    private Void visitAssignStmt(JmmNode assignStmt, Void unused) {
        // Get assignment left side
        var assignVal = assignStmt.getChild(1);
        // Check if a constant value is being assigned
        if (Objects.equals(assignVal.getKind(), INT_LITERAL.toString())) {
            var assignId = assignStmt.getChild(0).get("name");
            // Add to constants map
            this.constants.put(assignId, Integer.parseInt(assignVal.get("value")));
            // Remove assign statement
            assignStmt.getParent().removeChild(assignStmt);
        } else { // If not continue visiting
            defaultVisit(assignStmt, unused);
        }
        return null;
    }

    private Void visitVar(JmmNode var, Void unused) {
        // If the visiting var isn't a constant nothing must be done
        if (this.constants.containsKey(var.get("name"))) return null;

        var index = var.getIndexOfSelf();
        var varParent = var.getParent();
        varParent.removeChild(index);

        // new IntLiteral node
        JmmNodeImpl intLiteral = new JmmNodeImpl(INT_LITERAL.toString());
        intLiteral.putObject("value", this.constants.get(var.get("name")));

        //add hierarchy
        Collection<String> hierarchy = new ArrayList<>();
        hierarchy.add(INT_LITERAL.toString());
        hierarchy.add("Expr");
        intLiteral.setHierarchy(hierarchy);

        varParent.add(intLiteral,index);

        return null;
    }

    private Void defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return null;
    }
}
