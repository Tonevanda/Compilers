
## Induction Variables

An induction variable in a loop is a variable that is incremented or decremented by a constant value with each iteration. They are classified into two types:

1. **Basic Induction Variables**: These are directly incremented or decremented by a constant value. For example, in the loop `for i = 1 to 10`, the variable `i` is a basic induction variable because it is incremented by 1 in each iteration.
    
2. **Derived Induction Variables**: These are linear functions of basic induction variables. For instance, if `i` is a basic induction variable, a variable `j` defined as `j = c1 * i + c2` (where `c1` and `c2` are constants) is a derived induction variable​​.

Induction variables are represented by a tuple of the form `(variable, coefficient, constant)`, where:

- `variable` is the induction variable itself
- `coefficient` is what indicates how the variable changes with each iteration, like the **step size**
- `constant` is the initial value or offset added to the induction variable

So, for example, the expression `i = i + 1` would be represented as `(i, 1, 0)` because each step increments the variable `i` by `1`, and there is no additional constant added to the variable.

For the expression `j = 2 * i + 3`, the representation would be `(i, 2, 3)`, because each step we increment the variable `i` by its `double`, and there is an additional constant `3` added to the variable.
