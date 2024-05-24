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

Our project has in place 3 optimizations:

- Constant Propagation, by manipulating the AST
- Constant Folding, by manipulating the AST
- Instruction Selection, during the Jasmin code generation

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

### Jasmin Optimizations

