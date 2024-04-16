package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {

    public static JmmSymbolTable build(JmmNode root) {
        var imports = buildImports(root);
        var classDecl = root.getChild(root.getNumChildren()-1);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);

        String className = classDecl.get("name");
        String superName=null;
        if (classDecl.hasAttribute("superName")) superName = classDecl.get("superName");

        var fields = buildFields(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(imports, className, superName, fields, methods, returnTypes, params, locals);
    }

    private static List<String> buildImports(JmmNode root) {

        return root.getChildren(IMPORT_DECL).stream()
                .map(importNode -> importNode.get("name"))
                .toList();
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {

        return classDecl.getChildren(VAR_DECL).stream()
                .map(var -> new Symbol(TypeUtils.getType(var.getChild(0)), var.get("name")))
                .toList();
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> {
                    Type type = TypeUtils.getType(method.getChild(0));
                    type.putObject("isVarargs", method.getChild(0).get("isVarargs").equals("true"));
                    map.put(method.get("name"), type);
                });

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), method.getChildren(PARAM).stream().map(param -> {
                    Type type = TypeUtils.getType(param.getChild(0));
                    type.putObject("isVarargs", param.getChild(0).get("isVarargs").equals("true"));
                    return new Symbol(type, param.get("name"));
                }).toList()));

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        return methodDecl.getChildren(VAR_DECL).stream()
                .map(local -> {
                    Type type = TypeUtils.getType(local.getChild(0));
                    type.putObject("isVarargs", local.getChild(0).get("isVarargs").equals("true"));
                    return new Symbol(type, local.get("name"));
                }).toList();
    }
}
