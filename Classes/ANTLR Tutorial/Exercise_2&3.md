# Exercise 2 & 3

Adapt the **expression** rule to include parenthesis in operations. The rule should allow one to prioritise operations, such as ` (1 + 2) * 3;`, meaning that the sum must be produced before the multiplication.

## Steps

1. Add a new line to the **expression** rule to include parenthesis. The new **expression** rule looks like this:

```g4
expression  
: '(' expression ')' #Parenthesis  
| expression op=('*' | '/') expression #BinaryOp  
| expression op=('+' | '-') expression #BinaryOp  
| value=INTEGER #Integer  
| value=ID #Identifier  
;
```

2. Create the `dealWithParenthesis()` method:

```java
private String dealWithParenthesis(JmmNode jmmNode, String s) {  
    return "(" + visit(jmmNode.getChildren().get(0)) + ")";  
}
```

3. Furthermore, we need to add a new line to the `buildVisitor` method, to include the **parenthesis** situation:

```java
protected void buildVisitor() {  
    addVisit("Program", this::dealWithProgram);
    addVisit("Assignment", this::dealWithAssignment);
    addVisit("ExprStmt", this::dealWithExprStmt);
    addVisit("Parenthesis", this::dealWithParenthesis);
    addVisit("BinaryOp", this::dealWithBinaryOp);
    addVisit("Integer", this::dealWithLiteral);
    addVisit("Identifier", this::dealWithLiteral);
}
```

This 3rd step is necessary because, without it, the program would not know what to do when encountering a parenthesis expression node otherwise.
