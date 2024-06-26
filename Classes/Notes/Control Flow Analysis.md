
## Type of Optimizations

### Constant Propagation

Constant propagation refers to replacing a variable that holds a constant value, with just the constant value. Example.

```c
int i = 0;
int b = 3 * i;
```

Would become:

```c
int i = 0;
int b = 3 * 0;
```

### Algebraic Simplification

Consists in simplifying an expression regarding the rules of the operation. Example:

```c
int i = y * 0
```

Would become:

```c
int i = 0
```

Because we know anything multiplied by $0$ is $0$.

### Copy Propagation

Given an assignment $x = y$, replace later uses of $x$ with uses of $y$, provided there are no intervening assignments to $x$ or $y$.

### Common Sub-Expression Elimination

Consists of assigning the value of an expression to a variable, and using that variable later. Example:

```c
x = x + (4 * a / b) * i + (i + 1) * (i + 1);
```

Would become:

```c
t = i + 1;
x = x + (4 * a / b) * i + t * t;
```

### Dead Code Elimination

Consists of removing unused code, such as assigning a value to a variable that is never used. 

### Loop Invariant Removal

Loop invariant removal is an optimization technique where we identify computations inside a loop that do not change with each iteration and move them outside the loop. Example:

```python
def sum_of_squares(a, b, n):
    result = 0
    for i in range(n):
        result += (a * b) + (i * i)
    return result
```

Would become:

```python
def sum_of_squares(a, b, n):
    result = 0
    ab = a * b  # Calculate the invariant outside the loop
    for i in range(n):
        result += ab + (i * i)
    return result
```

### Strength Reduction

Strength reduction is an optimization technique in compiler design where expensive operations (such as multiplication or division) are replaced with equivalent but less costly operations (such as addition or bit shifts).

## Dominance Frontier

Let's consider this example:

```        
	Entry
	  |
	  v
	 (A)
	 / \
	v   v
   (B) (C)
	\   /
	 v v
	 (D)
	  |
	  v
	Exit
```

In this CFG:

- `Entry` is the entry node.
- `(A)` branches to `(B)` and `(C)`.
- `(B)` and `(C)` both converge at `(D)`.
- `Exit` is the exit node.

#### Dominators

First, we determine the dominators for each node:

- `Entry` dominates itself.
- `A` is dominated by `Entry`.
- `B` is dominated by `Entry` and `A`.
- `C` is dominated by `Entry` and `A`.
- `D` is dominated by `Entry` and  `A`.

#### Dominance Frontiers

Now, we compute the dominance frontiers:

- For `A`: The nodes `B` and `C` are successors of `A`. Both `B` and `C` are directly dominated by `A`, and they converge at `D`. Therefore, `D` is in the dominance frontier of `A` because `A` dominates `B` and `C` (the predecessors of `D`), but `A` does not strictly dominate `D`.
- For `B`: The node `D` is the successor of `B`. Node `B` dominates itself, and `B` dominates `D` through `B`. However, `B` does not strictly dominate `D` since `D` is also reached from `C`. Therefore, `D` is in the dominance frontier of `B`.
- For `C`: The node `D` is the successor of `C`. Node `C` dominates itself, and `C` dominates `D` through `C`. However, `C` does not strictly dominate `D` since `D` is also reached from `B`. Therefore, `D` is in the dominance frontier of `C`.
- For `D`: `D` has no successors in this simple example, so its dominance frontier is empty.

### Summary

- Dominance frontier of `A` is `{D}`.
- Dominance frontier of `B` is `{D}`.
- Dominance frontier of `C` is `{D}`.
- Dominance frontier of `D` is `{}`.

### Importance

Dominance frontiers are critical in the construction of SSA form because they help determine where `φ` (phi) functions need to be inserted. When a variable is assigned in multiple places, phi functions are inserted at the dominance frontier nodes to merge different control flow paths that modify the same variable. This ensures that the SSA form maintains a single definition for each variable at each point in the code.

## Webs

Two webs interfere if their live ranges overlap in time.
There is a more refine version, however. That version says that, if 2 webs interfere in a single instruction, and the variable for the web that ends at that instruction is in the RHS of an assignment, and the variable for the web that starts at that instruction is in the LHS of an assignment, those webs do not interfere.

### Graph coloring

When coloring a graph, a node cannot be connected to an edge with the same color as itself.
