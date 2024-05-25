# Comp2024 Project

## Group members

- João Miguel da Silva Lourenço (up202108863@edu.fe.up.pt)
- Isabel Maria Lima Moutinho (up202108767@edu.fe.up.pt)
- Tomás Filipe Fernandes Xavier (up202108759@edu.fe.up.pt)

## Work Distribution

- João Miguel da Silva Lourenço - 33.33 %
- Isabel Maria Lima Moutinho - 33.33 %
- Tomás Filipe Fernandes Xavier - 33.33 %
- Our glorious blue-eyed king Gojou Satoru - 00.01%

## Self-Assessment

- We think our project deserves a 19 out of 20.

## Extra Elements

Our project has in place 4 optimizations:

- [Constant Propagation](#ast-optimizations), by manipulating the AST
- [Constant Folding](#ast-optimizations), by manipulating the AST
- [Register Allocation](#ollir-optimizations), after generating the OLLIR code
- [Instruction Selection](#jasmin-optimizations), during the Jasmin code generation

### AST Optimizations

This is done using a `visitor` for each one of these optimizations that changes the `AST`, in a loop. When no more changes to the `AST` have been made, the loop stops. 

```java
@Override
public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {

    var visitor = new ASTVisitor(semanticsResult.getSymbolTable());
    visitor.visit(semanticsResult.getRootNode());

    if (CompilerConfig.getOptimize(semanticsResult.getConfig())) {
        while (true) {
            var constPropagationVisitor = new ASTConstPropagationVisitor();
            constPropagationVisitor.visit(semanticsResult.getRootNode());

            var constFoldingVisitor = new ASTConstFoldingVisitor();
            constFoldingVisitor.visit(semanticsResult.getRootNode());

            // If none of the visitors made changes we end the loop
            if (!constPropagationVisitor.madeChanges() && !constFoldingVisitor.madeChanges()) break;
        }
    }
    return semanticsResult;
}
```

Additionally, along with the `ASTConstPropagationVisitor` and the `ASTConstFoldingVisitor`, there is another visitor - the `ASTVisitor`. This visitor is unrelated to the optimizations taking place, which is noticeable by the fact that it's outside the loop that checks for the optimization flag.

This `ASTVisitor` is used specifically to support the `varargs` feature. Because `varargs` is just syntactic sugar for an array, we can just create an `ArrayInit` node and put all the arguments that belong to the `varargs` as children of the `ArrayInit` node:

```java
private Void visitFunctionCall(JmmNode node, Void unused) {

        // Get the function name
        var funcName = node.get("func");

        // Check if function is local method, if not we don't change the AST
        if (!table.getMethods().contains(funcName)) return null;

        // Get the function arguments
        var arguments = table.getParameters(funcName);

        // Check if last argument is varargs
        if(arguments.isEmpty()) return null;

        var lastArg = arguments.get(arguments.size() - 1);
        var isVarArgs = lastArg.getType().getObject("isVarargs").equals(true);

        // If the last parameter is not varargs, we do nothing
        if(!isVarArgs) return null;

        var argumentCount = arguments.size();
        var nonVarArgsCount = argumentCount - 1;
        if(nonVarArgsCount < 0) nonVarArgsCount = 0;
        var paramsPassed = NodeUtils.getIntegerAttribute(node, "numArgs", "0");

        // Get all the arguments that are part of the varargs
        var numVarArgs = paramsPassed - nonVarArgsCount;
        ArrayList<JmmNode> varArgs = new ArrayList<>();
        for (int i = 0; i < numVarArgs; i++) {
            var arg = node.getChild(1 + nonVarArgsCount + i);
            varArgs.add(arg);
        }

        for(var arg : varArgs){
            node.removeChild(arg);
        }

        // Create ArrayInitCall node
        JmmNodeImpl arrayInit = new JmmNodeImpl(ARRAY_INIT.toString());
        arrayInit.setChildren(varArgs);

        // Set ArrayInit Attributes
        arrayInit.putObject("isArray", true);
        arrayInit.putObject("numArrayArgs", numVarArgs);

        // Set ArrayInit Hierarchy
        Collection<String> hierarchy = new ArrayList<>();
        hierarchy.add(ARRAY_INIT.toString());
        hierarchy.add("Expr");
        arrayInit.setHierarchy(hierarchy);

        node.add(arrayInit);

        // Update function call numArgs
        String newNumArgs = String.valueOf(nonVarArgsCount + 1);
        node.putObject("numArgs", newNumArgs);

        return null;
    }
```

### OLLIR Optimizations

After generating the OLLIR code, to minimize the number of registers used, we used a `Register Allocation` optimization algorithm. This algorithm calculates the `use`, `def`, `live-in` and `live-out` sets for each instructions, builds an interference graph and colors it. The number of colors in the graph represents the number of registers.

We created a class called `RegAlloc` to deal with this optimization, and we call it in the `optimize()` method, in the `JmmOptimizationImpl` class:

```java
@Override
public OllirResult optimize(OllirResult ollirResult) {

    // Get the register allocation value from the config
    int maxRegisters = CompilerConfig.getRegisterAllocation(ollirResult.getConfig());

    // If it's -1, return the result without optimizing
    if (maxRegisters == -1) return ollirResult;
    
    // Otherwise, optimize the result
    int ogMaxReg = maxRegisters;
    boolean success;
    do {
        ollirResult.getOllirClass().buildCFGs();
        var CFG = ollirResult.getOllirClass();

        var regAlloc = new RegAlloc(CFG, maxRegisters);
        success = regAlloc.allocateRegisters();

        maxRegisters++;
    } while (!success);
    maxRegisters--;

    //If we had to increment the max register
    if (maxRegisters!=ogMaxReg) {
        var message = String.format("%s register(s) is not enough. Cannot allocate with less than %s", ogMaxReg, maxRegisters);
        Report error = Report.newError(
                Stage.OPTIMIZATION,
                0,
                0,
                message,
                null);
        ollirResult.getReports().add(error);
    }
    return ollirResult;
}
```

The `allocateRegisters()` method looks like this:

```java
public boolean allocateRegisters(){
    buildSets();
    buildGraph();
    buildEdges();
    return colorGraph();
}
```

It calls upon 4 methods:

- `buildSets()`: This method builds the `live-in` and `live-out` sets for each instruction, with the help of the `use` and `def` sets.
- `buildGraph()`: This method builds the **Interference Graph**
- `buildEdges()`: This method adds the edges to the **Interference Graph**
- `colorGraph()`: This method uses the `Graph Coloring` algorithm to color the graph

If the `Graph Coloring` algorithm does not succeed, we increment the number of registers to try, and run the algorithm again. If for an input `n` larger than `0`, the algorithm does not succeed with, at most, `n` registers, then we create an error report and keep going until we find the minimum possible number of registers.

### Jasmin Optimizations

#### Instruction Selection:

- `iload_x`, `istore_x`, `astore_x`, `aload_x`:
All that needs to be done is check whether or not the reg number is between `0` and `3`:

```java
    private String isByte(int value){
        if(value < 4) return "_" + value;
        return " " + value;
    }
```

This function is called following every addition of load/store to the code string

- `iconst_0`, `bipush`, `sipush`, `ldc`: Also relatively simple, just need to check if the variable is between the apropriate intervals:

```java
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
```

- `iinc`:
We calculate the literal value (including the combinations of negative simbols)
and if the value is inbetween -128 and 127:

```java
    //On the left is an operand and on the right a literal 
    if(rhs.getLeftOperand() instanceof Operand left && rhs.getRightOperand() instanceof LiteralElement right){
        int leftReg = currentMethod.getVarTable().get(left.getName()).getVirtualReg();
        int literalInt = Integer.parseInt(right.getLiteral());
        literalInt = valueTranslation(literalInt, rhs.getOperation().getOpType());
        if(leftReg == reg && literalInt >= -128 && literalInt <= 127){
            code.append("iinc ").append(reg).append(" ").append(literalInt).append(NL);
            return code.toString();
        }
    }
    //On the left is a literal and on the right an operand  
    else if (rhs.getLeftOperand() instanceof LiteralElement left && rhs.getRightOperand() instanceof Operand right) {
        int rightReg = currentMethod.getVarTable().get(right.getName()).getVirtualReg();
        int literalInt = Integer.parseInt(left.getLiteral());
        literalInt = valueTranslation(literalInt, rhs.getOperation().getOpType());
        //if the literal is on the left the operation has to be an add because for example i = 1 - i can't be written with an iinc 
        if(rightReg == reg && rhs.getOperation().getOpType().equals(OperationType.ADD) && literalInt >= -128 && literalInt <= 127){
            code.append("iinc ").append(reg).append(" ").append(literalInt).append(NL);
            return code.toString();
        }
    }
```
Value translation just does the operation itself:

```java
    private int valueTranslation(int value, OperationType opType){
        if(opType == OperationType.ADD)
            return value;
        else if(opType == OperationType.SUB)
            return -value;
        else
            return 128;
    }
```

- `iflt`, `ifne`, `etc`: 
Added the boolean cases to binary operation switch case:

```java
    String op = switch (binaryOp.getOperation().getOpType()) {
        case ADD -> "iadd";
        case SUB, EQ, NEQ, LTH, LTE, GTH, GTE  -> "isub";
        case MUL -> "imul";
        case DIV -> "idiv";
        default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
    };
```

Added switch case to extract apropriate if instructuion:

```java
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
```

Witch is now used for 2 situations:
- A binary operation on an assign:

```java
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
```

- And any other regular Branch instruction:

```java
    private String generateOpCondition(OpCondInstruction opCond) {
        var code = new StringBuilder();
        code.append(generators.apply(opCond.getCondition()));

        String op = extractIf(opCond.getCondition().getOperation().getOpType());
        checkStackSize();
        stackSize--;
        code.append(op).append(opCond.getLabel()).append(NL);
        return code.toString();
    }
```
