package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;
    private static int ifLabelNumber = -1;
    private static int whileLabelNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static List<String> getIfLabels() {

        return getIfLabels("if", "endif");
    }

    public static List<String> getIfLabels(String prefix1, String prefix2) {

        int labelNum = getNextIfLabelNum();

        ArrayList<String> labels = new ArrayList<>();

        // Add if and corresponding endif
        labels.add(prefix1 + labelNum);
        labels.add(prefix2 + labelNum);

        return labels;
    }

    public static int getNextIfLabelNum() {

        ifLabelNumber += 1;
        return ifLabelNumber;
    }

    public static List<String> getWhileLabels() {

        return getWhileLabels("whileCond", "whileLoop", "whileEnd");
    }

    public static List<String> getWhileLabels(String prefix1, String prefix2, String prefix3) {

        int labelNum = getNextWhileLabelNum();

        ArrayList<String> labels = new ArrayList<>();

        // Add condition, body and end labels
        labels.add(prefix1 + labelNum);
        labels.add(prefix2 + labelNum);
        labels.add(prefix3 + labelNum);

        return labels;
    }

    public static int getNextWhileLabelNum() {

        whileLabelNumber += 1;
        return whileLabelNumber;
    }

    public static String toOllirType(JmmNode typeNode) {

        TYPE.checkOrThrow(typeNode);

        String typeName = typeNode.get("name");
        var isArray = typeNode.get("isArray").equals("true");

        return toOllirType(typeName, isArray);
    }

    public static String toOllirType(Type type) {
        var isArray = type.isArray();
        return toOllirType(type.getName(), isArray);
    }

    private static String toOllirType(String typeName, boolean isArray) {

        String type = (isArray ? ".array" : "") + "." + switch (typeName) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "String" -> "String";
            case "void" -> "V";
            default -> typeName;
        };

        return type;
    }
}
