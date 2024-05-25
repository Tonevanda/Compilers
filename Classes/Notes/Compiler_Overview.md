# Overview of a Compiler

## Table of Contents

- [Traditional Two-Pass Compiler](#traditional-two-pass-compiler)
    - [Front-End](#front-end)
        - [Scanner](#scanner)
        - [Parser](#parser)
        - [Grammar](#grammar)
    - [Back-End](#back-end)
        - [Instruction Selection](#instruction-selection)
        - [Register Allocation](#register-allocation)
        - [Instruction Scheduling](#instruction-scheduling)
- [Traditional Three-Pass Compiler](#traditional-three-pass-compiler)
    - [Middle-End](#middle-end)

## Traditional Two-Pass Compiler

A traditional two-pass compiler is made up of two key parts:

- [Front-End](#front-end)
- [Back-End](#back-end)

### Front-End

The front-end of a compiler handles the initial stages of the compilation process.

It composed of two different stages:

- [Scanner](#scanner)
- [Parser](#parser)

Typically is O(n) or O(n log(n))

The context-free syntax is specified with a [grammar](#grammar).
It is written in a variant of Backus-Naur Form (**BNF**).

#### Scanner

- Maps character stream into words - the basic unit of syntax
- Produces pairs, also called a token
- Typical tokens include *number*, *identifier*, *+*, *while*, etc
- Eliminates white spaces (including comments)
- Speed is very important for this stage

#### Parser

- Recognises context-free syntax and reports errors
- Guides context-sensitive analysis a.k.a *type checking*
- Builds IR (intermediate representation) for source program

A parse can be represented by a tree (**parse tree** or **syntax tree**)
Compilers often use an **abstract syntax tree**
ASTs are one kind of *intermediate representation (IR)*

#### Grammar

Formally, a grammar G = (S,N,T,P)

- *S* is the start symbol
- N is a set of non-terminal symbols
- T is a set of terminal symbols or words
- P is a set of productions or rewrite rules

Let's take the following grammar:

```g4
goal
	: expr
	;
expr
	: expr op term
	| term
	;
term
	: number
	| id
	;
op
	: '+'
	| '-'
	;
```

This grammar can be represented as:
```txt
S = goal
N = {goal, expr, term, op}
T = {number, id, '+', '-'}
P = {1,2,3,4,5,6,7}
```

### Back-End

Responsibilities:

- Translate IR into target machine code
- Choose instructions to implement each IR operation
- Decide which value to keep in registers
- Ensure conformance with system interfaces

Automation has been *less* successful in the back-end

It is composed of 3 phases:

- [Instruction Selection](#instruction-selection)
- [Register Allocation](#register-allocation)
- [Instruction Scheduling](#instruction-scheduling)

Is NP-Complete

#### Instruction Selection

- Produce fast, compact code
- Take advantage of target features, such as addressing modes
- Usually viewed as a pattern matching problem

Was considered the problem of the future in 1978

#### Register Allocation

- Have each value in a register when it is used
- Manage a limited set of resources
- Can change instruction choices & insert LOADs & STOREs
- Optimal allocation is NP-Complete

Compilers approximate solutions to NP-Complete problems

#### Instruction Scheduling

- Avoid hardware stalls and interlocks
- Use all functional units productively
- Can increase lifetime of variables (changing the allocation)

Optimal scheduling is NP-Complete in nearly all cases

Heuristic techniques are well developed

## Traditional Three-Pass Compiler

Much like a two-pass compiler, it has a front-end and a back-end. 
However, between the front-end and the back-end it has a [Middle-End](#middle-end).

### Middle-End

The main point of the middle end is to perform optimizations that are easier to apply at this level than at the source code level, yet still have a significant impact on the performance of the final executable.

- Analyzes IR and rewrites (or *transforms*) IR
- Primary goal is to reduce running time of the compiled code
	- May also improve space, power consumption
- Must preserve "meaning" of the Code
	- Measured by values of named variables

Some common optimizations performed in the middle end include:

1. **Loop optimization:** Identifying loops in the code and applying transformations to improve their efficiency, such as loop unrolling, loop fusion, or loop-invariant code motion.
    
2. **Data-flow analysis:** Analyzing how data flows through the program and identifying opportunities for optimizations like dead code elimination or common subexpression elimination.
    
3. **Control-flow analysis:** Analyzing the control flow of the program to identify opportunities for optimizations such as control flow simplification or reducing the number of conditional branches.
    
4. **Inlining:** Automatically replacing function calls with the body of the called function, which can eliminate the overhead of the function call and potentially enable further optimizations.
    
5. **Register allocation:** Assigning variables to processor registers in a way that minimizes the number of memory accesses and maximizes the use of available resources.