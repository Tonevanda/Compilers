## What is an Available Expression?


An available expression in data flow analysis is an expression that has been computed and its value remains unchanged along all paths to a certain point in the program. This means the expression can be used at that point without needing to be recomputed.

## Gen and Kill Sets

### Gen Set

If a Basic Block (or instruction) defines the expression then the expression number is in the Gen Set for that Basic Block (or instruction)

### Kill Set

If a Basic Block (or instruction) (re)defines a variable in the expression then that expression number is in the Kill Set for that Basic Block (or instruction).
Expression is thus not valid after that Basic Block (or instruction)

### Example

Let's take this block:

```c
a = b + c; // 1
d = e + f; // 2
f = a + c; // 3
```

The Gen and Kill sets would be as follows:

- `a = b + c` -> 
	- Gen = {1}
	- Kill = {3}, a.k.a, any expression that uses `a`
- `d = e + f` ->
	- Gen = {2}
	- Kill = {}
- `f = a + c` ->
	- Gen = {3}
	- Kill = {2}

## Aggregate Gen and Kill sets

The Gen and Kill sets of an instructions should be propagated further down the program. To do that we do the following:

The `OutGEN` of an instruction is equal to the `gen` of that instruction and the union of the `InGEN` minus the `kill` set of that instruction:

$$OutGEN = gen \cup (InGEN - kill) $$

The `OutKILL` of an instruction is equal to the union of the `kill` set of that instruction and the `InKILL`:

$$OutKILL = kill \cup InKILL$$

We do this for every basic block.


## Algorithm for Available Expressions

1. Assign a number to each expression in the program
2. Compute Gen and Kill Sets for each Instruction
3. Compute **Aggregate** Gen and Kill Sets for each Basic Block
4. Initialize Available Set at each Basic Block as follows:
	1. `IN` and `OUT` as the Entire Set
	2. Exception: The `IN` for the first Basic Block should be $\emptyset$ 
5. Iteratively propagate available expression set over the CFG

### Propagate Available Expression Set

Any expression available at the input (in the `IN` set) and not killed should be available at the end.
$$OUT = gen \cup (IN - kill)$$

Expression is available only if it is available in ***All Input Paths***:

$$IN = \cap OUT$$

![[Pasted image 20240530174839.png]]

## Lattices

A **semi-lattice** is a partially ordered set (poset) with an additional binary operation that ensures certain algebraic properties. Semi-lattices come in two types: **meet semi-lattices** and **join semi-lattices**.

### Meet Semi-Lattice

A **meet semi-lattice** is a set 𝐿 equipped with a binary operation ∧ (called "meet"), which satisfies the following properties:

1. **Idempotent**: 𝑎 ∧ 𝑎 = 𝑎 for all 𝑎∈𝐿.
2. **Commutative**: 𝑎 ∧ 𝑏 = 𝑏 ∧ 𝑎 for all 𝑎,𝑏∈𝐿.
3. **Associative**: 𝑎 ∧ (𝑏 ∧ 𝑐) = (𝑎 ∧ 𝑏) ∧ 𝑐 for all 𝑎,𝑏,𝑐∈𝐿.

These properties ensure that any two elements in the set have a greatest lower bound (also called a "meet") within the set.

### Join Semi-Lattice

A **join semi-lattice** is a set 𝐿 equipped with a binary operation ∨ (called "join"), which satisfies similar properties:

1. **Idempotent**: 𝑎 ∨ 𝑎 = 𝑎 for all 𝑎∈𝐿.
2. **Commutative**: 𝑎 ∨ 𝑏 = 𝑏 ∨ 𝑎 for all 𝑎,𝑏∈𝐿.
3. **Associative**: 𝑎 ∨ (𝑏 ∨ 𝑐) = (𝑎 ∨ 𝑏) ∨ 𝑐 for all 𝑎,𝑏,𝑐∈𝐿.

These properties ensure that any two elements in the set have a least upper bound (also called a "join") within the set.

### Partial Order

A semi-lattice also imposes a partial order ≤ defined as follows:

- For a **meet semi-lattice**: 𝑎≤𝑏 if and only if 𝑎=𝑎∧𝑏.
- For a **join semi-lattice**: 𝑎≤𝑏 if and only if 𝑏=𝑎∨𝑏.

### Examples of Semi-Lattices

1. **Set Intersection** (Meet Semi-Lattice):
    
    - The set of all subsets of a given set 𝑆 with the intersection operation (∩) forms a meet semi-lattice. The meet of two subsets is their intersection.
2. **Set Union** (Join Semi-Lattice):
    
    - The set of all subsets of a given set 𝑆 with the union operation (∪) forms a join semi-lattice. The join of two subsets is their union.

### Application in Data-Flow Analysis

In data-flow analysis, semi-lattices are used to model the sets of data-flow facts. The meet or join operations are used to merge data-flow information from different control flow paths. This helps in determining properties like variable definitions, reaching definitions, live variables, etc., at different points in the program. The semi-lattice structure ensures that the iterative data-flow analysis converges to a fixed point, providing a stable and correct solution.
