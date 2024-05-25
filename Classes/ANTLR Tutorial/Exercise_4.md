# Exercise 4

Adapt the **expression** rule to include the logical and (`&&`) operator and the less-than (`<`) operator. Do not forget to check Java operators precedence. Do you need to change the `JavaCalcVisitor` class?

## Steps 

1. Add a new line to the **expression** rule for each new operator:

```g4
expression  
    : '(' expression ')' #Parenthesis  
    | expression op=('*' | '/') expression #BinaryOp  
    | expression op=('+' | '-') expression #BinaryOp  
    | expression op=('<' | '>' | '<=' | '>=') expression #BinaryOp  
    | expression op=('&&' | '||' | '^') expression #BinaryOp  
    | value=INTEGER #Integer  
    | value=ID #Identifier  
    ;
```

Because the less-than (`<`) operator has higher precedence compared to logical operator such as `&&` it comes up first in the **expression** rule.
I also went ahead and added some of the other operators of the same precedence.

Also, because the new operators I added are also binary operators, and I used the same identifier of `#BinaryOp` for both of them, there is no need to change the `JavaCalcVisitor` class, since they will just get treated as binary operations by the `buildVisitor()`.
