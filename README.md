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
    boolean success;
    do {
        ollirResult.getOllirClass().buildCFGs();
        var CFG = ollirResult.getOllirClass();

        var regAlloc = new RegAlloc(CFG, maxRegisters);
        success = regAlloc.allocateRegisters();

        maxRegisters++;
    } while (!success);

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

- `buildSets()`: This method builds the `use`, `def`, `live-in` and `live-out` sets for each instruction
- `buildGraph()`: This method builds the **Interference Graph**
- `buildEdges()`: This method adds the edges to the **Interference Graph**
- `colorGraph()`: This method uses the `Graph Coloring` algorithm to color the graph

If the `Graph Coloring` algorithm does not succeed, we increment the number of registers to try, and run the algorithm again. If for an input `n` larger than `0`, the algorithm does not succeed with, at most, `n` registers, then we create an error report and keep going until we find the minimum possible number of registers.

### Jasmin Optimizations

#### Instruction Selection:

- iload_x, istore_x, astore_x,aload_x:
All that need to be done is check wheater or not the reg number is between [0,3]:

```java
    private String isByte(int value){
        if(value < 4) return "_" + value;
        return " " + value;
    }
```
This function is called following every adition of load/store to the code string

- 