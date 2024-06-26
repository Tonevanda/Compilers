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
import java.util.Set;
import java.util.HashSet;
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
    int stackSize = 0;
    int maxStackSize = 0;
    private final FunctionClassMap<TreeNode, String> generators;
    int idCounter = 0;
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
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCondition);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(OpCondInstruction.class, this::generateOpCondition);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
    }

    private void checkStackSize(){
        if(stackSize > maxStackSize){
            maxStackSize = stackSize;
        }
    }

    private String isByte(int value){
        if(value < 4) return "_" + value;
        return " " + value;
    }

    private int valueTranslation(int value, OperationType opType){
        if(opType == OperationType.ADD)
            return value;
        else if(opType == OperationType.SUB)
            return -value;
        else
            return 128;
    }

    private String extractIf(OperationType opType){
        return switch (opType) {
            case EQ -> "ifeq ";
            case NEQ, AND, OR, ANDB, ORB, NOT, NOTB -> "ifne ";
            case LTH -> "iflt ";
            case LTE -> "ifle ";
            case GTH -> "ifgt ";
            case GTE -> "ifge ";
            default -> throw new NotImplementedException(opType);
        };
    }

    private String translateType(Type type) {
        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
            case ARRAYREF -> "[I";
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

    public String callMethod(CallInstruction instruction) {
        StringBuilder ret = new StringBuilder();

        Operand caller = (Operand) instruction.getCaller();
        if(this.currentMethod.getVarTable().get(caller.getName()) != null){
            ret.append(generators.apply(caller));
        }
        //if the type is not castable to ClassType than it doesn't have a className, so it is an array
        String className = "";
        if(instruction.getCaller().getType() instanceof ClassType Callertype){
            className = Callertype.getName();
        }

        switch (instruction.getInvocationType()) {
            case invokevirtual, invokestatic: {
                int added = stackSize;
                String arguments = "";
                if (!instruction.getArguments().isEmpty()) {
                    for (Element argument : instruction.getArguments()) {
                        ret.append(generators.apply(argument));
                        arguments += translateType(argument.getType());
                    }
                }
                checkStackSize();
                stackSize = added;
                if(instruction.getInvocationType().equals(CallType.invokevirtual)){
                    ret.append("invokevirtual ").append(translateClassPath(className)).append("/").append(((LiteralElement) instruction.getMethodName()).getLiteral().replace("\"", "")).append("(").append(arguments).append(")").append(translateType(instruction.getReturnType())).append(NL);
                }
                else {
                    ret.append("invokestatic ").append(translateClassPath(((Operand) instruction.getCaller()).getName())).append("/").append(((LiteralElement) instruction.getMethodName()).getLiteral().replace("\"", "")).append("(").append(arguments).append(")").append(translateType(instruction.getReturnType())).append(NL);
                }
                break;
            }
            case invokespecial:{
                checkStackSize();
                stackSize--;
                ret.append("invokespecial ").append(translateClassPath(className)).append("/<init>()V").append(NL);
                break;
            }
            case NEW: {
                if(className.isEmpty()){
                    if (!instruction.getArguments().isEmpty()) {
                        ret.append(generators.apply(instruction.getArguments().get(0)));
                        ret.append("newarray int").append(NL);
                    }
                }
                else {
                    ret.append("new ").append(translateClassPath(className)).append(NL);
                    stackSize++;
                }
                break;
            }
            case arraylength: {
                ret.append("arraylength").append(NL);
                break;
            }
        }
        return ret.toString();
    }

    //get the field of a class
    public String getField(GetFieldInstruction instruction){
        StringBuilder ret = new StringBuilder();
        String originClassName = ((ClassType) instruction.getOperands().get(0).getType()).getName();
        String fieldName = instruction.getField().getName();
        String returnType = translateType(instruction.getField().getType());
        stackSize++;
        ret.append("aload_0").append(NL)
                .append("getfield ").append(originClassName).append("/").append(fieldName)
                .append(" ").append(returnType).append(NL);
        return ret.toString();
    }

    //put in the field of a class
    public String putField(PutFieldInstruction instruction){
        StringBuilder ret = new StringBuilder();
        ret.append("aload_0").append(NL);
        stackSize++;
        ret.append(generators.apply(instruction.getOperands().get(2)));

        String originClassName = ((ClassType) instruction.getOperands().get(0).getType()).getName();
        String fieldName = instruction.getField().getName();
        String inputType = translateType(instruction.getField().getType());
        ret.append("putfield ").append(originClassName).append("/").append(fieldName)
                .append(" ").append(inputType).append(NL);

        checkStackSize();
        stackSize-=2;

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
        String className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL);
        String extend;
        if (classUnit.getSuperClass() == null || classUnit.getSuperClass().equals("Object")){
            extend = "java/lang/Object";
            code.append(".super java/lang/Object");
        }
        else {
            extend = translateClassPath(ollirResult.getOllirClass().getSuperClass().toString());
            code.append(".super ").append(extend);
        }
        code.append(NL).append(NL);

        for(var field : classUnit.getFields()){
            code.append(".field ").append(translateAccessModifier(field.getFieldAccessModifier())).append(field.getFieldName())
                    .append(" ").append(translateType(field.getFieldType())).append(NL);
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

        String methodName = method.getMethodName();
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

        var temp = new StringBuilder();
        for (var inst : method.getInstructions()) {
            for(var label : method.getLabels(inst)){
                temp.append(label).append(":").append(NL);
            }
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));
            temp.append(instCode);
            if (inst instanceof CallInstruction && !(((CallInstruction) inst).getReturnType().toString().equals("VOID"))){

                temp.append(TAB).append("pop").append(NL);
                checkStackSize();
                this.stackSize--;
            }
        }
        temp.append(".end method\n");

        code.append(TAB).append(".limit stack ").append(maxStackSize).append(NL);
        Set<Integer> registers = new HashSet<>();
        for(Descriptor var : method.getVarTable().values()){
            if(var.getScope().toString().equals("FIELD")){
                continue;
            }
            if(!registers.contains(var.getVirtualReg())){
                registers.add(var.getVirtualReg());
            }
        }
        if(!registers.contains(0) && !methodName.equals("main")){
            code.append(TAB).append(".limit locals ").append(registers.size()+1).append(NL);
        }
        else {
            code.append(TAB).append(".limit locals ").append(registers.size()).append(NL);
        }
        code.append(temp);
        // unset method
        currentMethod = null;
        maxStackSize = 0;
        stackSize = 0;
        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // store value in the stack in destination
        Element lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        // get register
        int reg = currentMethod.getVarTable().get(((Operand) lhs).getName()).getVirtualReg();

        if(assign.getRhs() instanceof BinaryOpInstruction rhs){
            if(rhs.getLeftOperand() instanceof Operand left && rhs.getRightOperand() instanceof LiteralElement right){
                int leftReg = currentMethod.getVarTable().get(left.getName()).getVirtualReg();
                int literalInt = Integer.parseInt(right.getLiteral());
                literalInt = valueTranslation(literalInt, rhs.getOperation().getOpType());
                if(leftReg == reg && literalInt >= -128 && literalInt <= 127){
                    code.append("iinc ").append(reg).append(" ").append(literalInt).append(NL);
                    return code.toString();
                }
            } else if (rhs.getLeftOperand() instanceof LiteralElement left && rhs.getRightOperand() instanceof Operand right) {
                int rightReg = currentMethod.getVarTable().get(right.getName()).getVirtualReg();
                int literalInt = Integer.parseInt(left.getLiteral());
                literalInt = valueTranslation(literalInt, rhs.getOperation().getOpType());
                //if the literal is on the left the operation has to be an add
                if(rightReg == reg && rhs.getOperation().getOpType().equals(OperationType.ADD) && literalInt >= -128 && literalInt <= 127){
                    code.append("iinc ").append(reg).append(" ").append(literalInt).append(NL);
                    return code.toString();
                }
            }
        }
        if(lhs instanceof ArrayOperand lhsArray){
            stackSize++;
            code.append("aload").append(isByte(reg)).append(NL);
            code.append(generators.apply(lhsArray.getIndexOperands().get(0)));
            code.append(generators.apply(assign.getRhs()));
            code.append("iastore").append(NL);
            checkStackSize();
            stackSize-=3;

            return code.toString();
        }
        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        if(lhs.getType().toString().equals("INT32")){
            code.append("istore").append(isByte(reg)).append(NL);
        } else if (lhs.getType().toString().equals("BOOLEAN")) {
            if(assign.getRhs() instanceof BinaryOpInstruction binaryOp){
                String op = extractIf(binaryOp.getOperation().getOpType());
                code.append(op).append("boolSaveJump_").append(idCounter).append(NL);
                code.append("iconst_0").append(NL);
                code.append("goto ").append("boolSaveEnd_").append(idCounter).append(NL);
                code.append("boolSaveJump_").append(idCounter).append(":").append(NL);
                code.append("iconst_1").append(NL);
                code.append("boolSaveEnd_").append(idCounter).append(":").append(NL);
                code.append("istore").append(isByte(reg)).append(NL);
                idCounter++;
                checkStackSize();
                stackSize--;
            }
            else {
                code.append("istore").append(isByte(reg)).append(NL);
            }
        } else{
            code.append("astore").append(isByte(reg)).append(NL);
        }
        checkStackSize();
        stackSize--;
        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        stackSize++;
        int literalInt = Integer.parseInt(literal.getLiteral());
        if(literalInt == -1){
            return "iconst_m1" + NL;
        }
        else if (literalInt >= 0 && literalInt <= 5){
            return "iconst_" + literalInt + NL;
        }
        else if (literalInt >= -128 && literalInt <= 127){
            return "bipush " + literalInt + NL;
        }
        else if (literalInt >= -32768 && literalInt <= 32767) {
            return "sipush " + literalInt + NL;
        }
        else {
            return "ldc " + literal.getLiteral() + NL;
        }
    }

    private String generateOperand(Operand operand) {
        var code = new StringBuilder();

        stackSize++;
        if(operand.getName().equals("this")){
            return "aload_0" + NL;
        }
        int reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if(operand instanceof ArrayOperand){
            code.append("aload").append(isByte(reg)).append(NL);
            code.append(generators.apply(((ArrayOperand) operand).getIndexOperands().get(0)));
            code.append("iaload").append(NL);
            checkStackSize();
            stackSize--;
            return code.toString();
        }
        return switch (operand.getType().toString()) {
            case "INT32", "BOOLEAN" -> "iload" + isByte(reg) + NL;
            default -> "aload" + isByte(reg) + NL;
        };
    }

    private String generateSingleOpCondition(SingleOpCondInstruction singleOpCond) {
        var code = new StringBuilder();

        code.append(generators.apply(singleOpCond.getOperands().get(0)));
        // apply operation
        code.append("ifne ").append(singleOpCond.getLabel()).append(NL);
        return code.toString();
    }

    private String generateOpCondition(OpCondInstruction opCond) {
        var code = new StringBuilder();
        code.append(generators.apply(opCond.getCondition()));

        String op = extractIf(opCond.getCondition().getOperation().getOpType());
        checkStackSize();
        stackSize--;
        code.append(op).append(opCond.getLabel()).append(NL);
        return code.toString();
    }

    private String generateGoto(GotoInstruction gotoInst) {
        return "goto " + gotoInst.getLabel() + NL;
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOp) {
        var code = new StringBuilder();

        if(unaryOp.getOperand().isLiteral()) {
            if (((LiteralElement) unaryOp.getOperand()).getLiteral().equals("1")) {
                code.append("iconst_0");
            } else {
                code.append("iconst_1");
            }
            stackSize++;
        }
        else{
            code.append(generators.apply(unaryOp.getOperand()));
            code.append("iconst_1\nixor");
            stackSize++;
            checkStackSize();
            stackSize--;
        }
        code.append(NL);

        return code.toString();
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));


        // apply operation
        String op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case SUB, EQ, NEQ, LTH, LTE, GTH, GTE  -> "isub";
            case MUL -> "imul";
            case DIV -> "idiv";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };
        checkStackSize();
        stackSize--;
        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();
        if (returnInst.getOperand() != null) {
            code.append(generators.apply(returnInst.getOperand()));
            checkStackSize();
            switch (returnInst.getReturnType().getTypeOfElement().name().toString()) {
                case "INT32", "BOOLEAN" -> {
                    code.append("ireturn").append(NL);
                }
                default -> {
                    code.append("areturn").append(NL);
                }
            }
            stackSize--;
        }
        else
            code.append("return").append(NL);

        return code.toString();
    }

}
