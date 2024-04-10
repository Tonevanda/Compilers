package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";
    private static final String STRING_TYPE_NAME = "String";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static String getBooleanTypeName(){return BOOLEAN_TYPE_NAME;}

    public static String getStringTypeName(){return STRING_TYPE_NAME;}

    public static Type getType(JmmNode type_node) {
        String type_name = type_node.get("name");
        boolean isArray = Boolean.parseBoolean(type_node.get("isArray")); // Receives a string and turns it into a boolean
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

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case PAREN_EXPR -> getExprType(expr.getChild(0), table);
            case FUNCTION_CALL -> getFunctionCallType(expr, table);
            case ARR_ACCESS_EXPR -> getExprType(expr.getChild(0), table);
            case VAR -> getVarExprType(expr, table);
            case THIS -> new Type(table.getClassName(), false);
            case NEW_CLASS_OBJ -> getNewClassObjType(expr);
            case INT_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOL_LITERAL, UNARY_EXPR -> new Type(BOOLEAN_TYPE_NAME, false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "/", "-" -> new Type(INT_TYPE_NAME, false);
            case "<", ">", "<=", ">=", "&&", "||" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    private static Type getNewClassObjType(JmmNode newClassObj){
        // Get the class name
        var className = newClassObj.get("name");

        return new Type(className, false);
    }

    private static Type getFunctionCallType(JmmNode functionCall, SymbolTable table){
        // Get the method name
        var methodName = functionCall.get("func");

        // Check return type of method
        if(table.getMethods().stream()
                .anyMatch(method -> method.equals(methodName))){
            return table.getReturnType(methodName);
        }
        return null;
    }

    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        Type type = null;

        // Get the variable name
        var varName = varRefExpr.get("name");

        if(table.getImports().stream().anyMatch(imported -> imported.contains(varName))){
            var importedType = new Type(varName, false);
            importedType.putObject("isImported", true);
            return importedType;
        }

        // Get the method where the variable is being used
        JmmNode method = varRefExpr.getAncestor(Kind.METHOD_DECL).get();
        var methodName = method.get("name");

        // If the variable is a local variable
        if(table.getLocalVariables(methodName).stream()
                .anyMatch(localVar -> localVar.getName().equals(varName))){
            type = table.getLocalVariables(methodName).stream()
                    .filter(varDecl -> varDecl.getName().equals(varName))
                    .findFirst()
                    .get()
                    .getType();
            return type;
        }

        // If the variable is a parameter
        if(table.getParameters(methodName).stream()
                .anyMatch(param -> param.getName().equals(varName))){
            type = table.getParameters(methodName).stream()
                    .filter(param -> param.getName().equals(varName))
                    .findFirst()
                    .get()
                    .getType();
            return type;
        }

        // If the variable is a field
        if(table.getFields().stream()
                .anyMatch(field -> field.getName().equals(varName))){
            type = table.getFields().stream()
                    .filter(field -> field.getName().equals(varName))
                    .findFirst()
                    .get()
                    .getType();
            return type;
        }


        return type;
    }

    /**
     * Get the names of the imports from a node
     * @param node
     * @return
     */
    public static List<String> getImportNames(JmmNode node) {
        String name = node.get("name");
        name = name.substring(1, name.length() - 1); // Remove the brackets
        return Arrays.stream(name.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
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
