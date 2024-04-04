package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";
    private static final String STRING_TYPE_NAME = "String";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static Type getType(JmmNode type_node) {
        String type_name = type_node.get("name");
        boolean isArray = Boolean.parseBoolean(type_node.get("isArray")); // Receives a string and turns it into a boolean
        boolean isVarargs = Boolean.parseBoolean(type_node.get("isVarargs"));
        if(isVarargs) isArray = true;

        return switch (type_name) {
            case INT_TYPE_NAME -> new Type(INT_TYPE_NAME, isArray);
            case BOOLEAN_TYPE_NAME -> new Type(BOOLEAN_TYPE_NAME, isArray);
            case STRING_TYPE_NAME -> new Type(STRING_TYPE_NAME, isArray);
            default -> new Type(type_name, isArray);
        };
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR -> getVarExprType(expr, table);
            case INT_LITERAL -> new Type(INT_TYPE_NAME, false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*" -> new Type(INT_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
        return new Type(INT_TYPE_NAME, false);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
