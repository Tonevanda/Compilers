# Exercise 1

Change the **statement** rule so that the production for assignments (ID=INTEGER) accepts any expression instead of a single integer. The rule should allow something like:

`a = 2 * 3 + 4`

## Steps

1. Go to the file `Javamm.g4` and change the assignment of the statement rule so it accepts expressions in general:
```g4
statement  
: expression ';' #ExprStmt  
| var=ID '=' value=expression ';' #Assignment   
;
```

2. Afterwards, we need to change the `dealWithAssignment` method so it's compatible with this change
```java
private String dealWithAssignment(JmmNode jmmNode, String s) {  
    return "int " + jmmNode.get("var")  
            + " = " + visit(jmmNode.getChildren().get(0))  
            + ";";  
}
```

This works because the assignment expression always has 1 child in either case:

- `Integer (value: x)`, in case it's a literal assignment
- `BinaryOp (op: x)` , in case it's an expression assignment

In either case, we can just call the method `jmmNode.getChildren().get(0)` to get the first child of the assignment node, and then call the `visit()` method to traverse the tree with the first child of the assignment node as root. 

`visit()` will always either return :

- An operator, in case it encounters a `BinaryOp` node
- A numeric value, in case it encounters an `Integer` node 
- An alphabetical value, in case it encounters an `Identifier` node
