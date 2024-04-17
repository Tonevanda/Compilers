package pt.up.fe.comp2024.backend;

import org.hamcrest.core.AnyOf;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Map;
/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        
        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(GetFieldInstruction.class, this::getField);
        generators.put(PutFieldInstruction.class, this::putField);
        generators.put(CallInstruction.class, this::callMethod);
    }

    public String callMethod(CallInstruction instruction) {
        StringBuilder ret = new StringBuilder();
        String className = ((ClassType) instruction.getCaller().getType()).getName();
        Operand caller = (Operand) instruction.getCaller();
        if(this.currentMethod.getVarTable().get(caller.getName()) != null){
            ret.append(generators.apply((Operand) instruction.getCaller()));
        }
        switch (instruction.getInvocationType().toString()) {
            case "invokevirtual": {
                String arguments = "";
                if (instruction.getArguments().size()!=0) {
                    for (Element argument : instruction.getArguments()) {
                        ret.append(generators.apply(argument));
                        arguments += translateType(argument.getType());
                    }
                }
                ret.append("invokevirtual ").append(translateClassPath(className)).append("/").append(((LiteralElement) instruction.getMethodName()).getLiteral().replace("\"", "")).append("(").append(arguments).append(")").append(translateType(instruction.getReturnType())).append(NL);
                break;
            }
            case "invokestatic": {
                String arguments = "";
                if (instruction.getArguments().size()!=0) {
                    for (Element argument : instruction.getArguments()) {
                        ret.append(generators.apply(argument));
                        arguments += translateType(argument.getType());
                    }
                }
                ret.append("invokestatic ").append(translateClassPath(((Operand) instruction.getCaller()).getName())).append("/").append(((LiteralElement) instruction.getMethodName()).getLiteral().replace("\"", "")).append("(").append(arguments).append(")").append(translateType(instruction.getReturnType())).append(NL);
                break;
            }
            case "invokespecial":{
                ret.append("invokespecial ").append(translateClassPath(className)).append("/<init>()V").append(NL);
                break;
            }
            case "NEW": {
                ret.append("new ").append(translateClassPath(className)).append(NL).append("dup").append(NL);
                break;
            }
        }
        /*
                case "invokevirtual": {
                    if (currentMethod.isStaticMethod()) {
                        ret.append("new ").append(this.ollirResult.getOllirClass().getClassName()).append(NL).append("dup").append(NL).append("invokespecial ").append(this.ollirResult.getOllirClass().getClassName()).append("/<init>()V").append(NL).append("astore_0").append(NL);
                    }
                    ret.append("aload_0").append(NL).append("iconst_1").append(NL).append("invokevirtual ").append(className).append("/").append(((LiteralElement) instruction.getMethodName()).getLiteral().replace("\"", "")).append("(I)I").append(NL);
                    break;
                }
            //if it exists already then i just load, if it doesn't then i create it
            HashMap<String, Descriptor> map = currentMethod.getVarTable();

            Optional<String> keyWithSearchString = map.entrySet().stream()
                    .filter(entry -> entry.getValue().getVarType() instanceof ClassType)
                    .filter(entry -> entry.getKey().toString().equals(className))
                    .map(Map.Entry::getKey)
                    .findFirst();
        }*/
        return ret.toString();
    }

    public String getField(GetFieldInstruction instruction){
        // Assuming the field name is "fieldName"
        StringBuilder ret = new StringBuilder();
        String originClassName = ((ClassType) instruction.getOperands().get(0).getType()).getName();
        String fieldName = instruction.getField().getName();
        ret.append("aload_0").append(NL)
                .append("getfield ").append(originClassName).append("/").append(fieldName)
                .append(" ").append(translateType(instruction.getField().getType())).append(NL);
        //IN CASE I DO NEED TO DIFFERENTIATE
        //if (instruction.getOperands().get(0).getType().getTypeOfElement().name() == "THIS") {
        return ret.toString();
    }

    public String putField(PutFieldInstruction instruction){
        StringBuilder ret = new StringBuilder();
        int test = currentMethod.getVarTable().keySet().size();
        LiteralElement temp = (LiteralElement) instruction.getOperands().get(2);
        ret.append("aload_0").append(NL);
        if (temp.isLiteral()){
            ret.append(generateLiteral(temp));
        }
        String originClassName = ((ClassType) instruction.getOperands().get(0).getType()).getName();
        String fieldName = instruction.getField().getName();
        ret.append("putfield ").append(originClassName).append("/").append(fieldName)
                .append(" ").append(translateType(instruction.getField().getType())).append(NL);
        //IN CASE I DO NEED TO DIFFERENTIATE
        //if (instruction.getOperands().get(0).getType().getTypeOfElement().name() == "THIS") {
        return ret.toString();
    }
    public List<Report> getReports() {
        return reports;
    }

    public String build() {
        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }

    private String translateType(Type type) {
        return switch (type.toString()) {
            case "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "STRING" -> "Ljava/lang/String;";
            case "VOID" -> "V";
            default -> "L"+translateClassPath(((ClassType) type).getName())+";";
        };
    }

    private String translateAccessModifier(AccessModifier accessModifier) {
        return switch (accessModifier) {
            case DEFAULT -> "";
            case PUBLIC -> "public ";
            case PRIVATE -> "private ";
            case PROTECTED -> "protected ";
            default -> throw new NotImplementedException(accessModifier);
        };
    }

    private String translateClassPath(String className){
        for (String str : ollirResult.getOllirClass().getImports()){
            String[] parts = str.split("\\.");
            String lastWord = parts[parts.length - 1];
            if (lastWord.startsWith(".")) {
                lastWord = lastWord.substring(1);
            }
            if(lastWord.equals(className)){
                return str.replace(".","/");
            }
        }
        return className;
    }


    private String generateClassUnit(ClassUnit classUnit) {
        var code = new StringBuilder();
        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL);
        String extend= "";
        if (classUnit.getSuperClass() == null || classUnit.getSuperClass().equals("Object")){
            code.append(".super java/lang/Object");
            extend = "java/lang/Object";
        }
        else {
            extend = translateClassPath(ollirResult.getOllirClass().getSuperClass().toString());
            code.append(".super ").append(extend);
        }
        code.append(NL).append(NL);

        for(var field : classUnit.getFields()){
            code.append(".field ").append(translateAccessModifier(field.getFieldAccessModifier())).append(field.getFieldName())
                    .append(" ").append(translateType(field.getFieldType()))
                    .append(" = ").append(field.getInitialValue()).append(NL);
        }

        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    """;
        defaultConstructor+= "    invokespecial " + extend+"/<init>()V\n";
        defaultConstructor+="""
                        return
                    .end method
                    """;
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();
        code.append("\n.method ").append(modifier);
        if(methodName.equals("main")){
            code.append("static ").append(methodName).append("([Ljava/lang/String;)V").append(NL);
        }
        else {
            code.append(methodName).append("(");
            for (Element param : method.getParams()) {
                code.append(translateType(param.getType()));
            }
            code.append(")").append(translateType(method.getReturnType())).append(NL);
        }
        //.method public methodname(...


        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // TODO: Hardcoded for int type, needs to be expanded
        if(operand.getType().toString().equals("INT32") || operand.getType().toString().equals("BOOLEAN")){
            code.append("istore ").append(reg).append(NL);
        }
        else{
            code.append("astore ").append(reg).append(NL);
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        if(operand.getName().equals("this")){
            return "aload 0" + NL;
        }
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        return switch (operand.getType().toString()) {
            case "INT32", "BOOLEAN" -> "iload " + reg + NL;
            //case "STRING" ->  "aload " + reg + NL;
            default -> "aload " + reg + NL;
        };
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case SUB -> "isub";
            case MUL -> "imul";
            case DIV -> "idiv";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();
        if (returnInst.getOperand() != null) {
            code.append(generators.apply(returnInst.getOperand()));
            switch (returnInst.getReturnType().getTypeOfElement().name().toString()) {
                case "INT32", "BOOLEAN" -> {
                    code.append("ireturn").append(NL);
                }
                //case "STRING" ->  "aload " + reg + NL;
                default -> {
                    code.append("areturn").append(NL);
                }
            }
        }
        else
            code.append("return").append(NL);

        return code.toString();
    }

}
