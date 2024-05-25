# Expressions and Binary Operations

We need to add two more rules to the `buildVisitor()` method:

- `addVisit("ExprStmt", this::dealWithExprStmt)` , to deal with statements such as `a*2` 
- `addVisit("BinaryOp", this::dealWithBinaryOp)`, to deal with binary operations

To do this, we need to create a method to deal with each one:

- `dealWithExprStmt`
- `dealWithBinaryOp`

For expressions such as `a*2`, we want to print them to the console, so we should encapsulate the expression inside a `System.out.println()` method.
The `dealWithExprStmt` method looks like this:

```java
private String dealWithExprStmt(JmmNode jmmNode, String s) {  
    return "System.out.println(" + visit(jmmNode.getChildren().get(0)) + ");";  
}
```

As we can see, we return a string containing:

- "System.out.println("
- The information of the nodes inside the expression
- ")"


For binary operations, we just the informations inside the operation:

```java
private String dealWithBinaryOp(JmmNode jmmNode, String s) {  
    return visit(jmmNode.getChildren().get(0)) + " " + 
    jmmNode.get("op") + " " + visit(jmmNode.getChildren().get(1));  
}
```

As we can see, we return a string containing:

- The information to the left of the operator
- The operator
- The information to the right of the operator

Therefore, with this input:

```txt
a = 2;  
b = 3;  
a* b;  
c = 4;  
a + 2 * b / c;
```

We get this output:

```java
public class Calculator {
	public static void main(String[] args) {
		int a = 2;
		int b = 3;
		System.out.println(a * b);
		int c = 4;
		System.out.println(a + 2 * b / c);
	}
}
```
