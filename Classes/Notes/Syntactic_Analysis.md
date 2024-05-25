# Syntactic Analysis

## Table of Contents

- [Derivation and Parse Trees](#derivation-and-parse-trees)
- [Ambiguous Grammars](#ambiguous-grammars)
	- [Ambiguity](#ambiguity)
	- [Eliminating Ambiguity](#eliminating-ambiguity)
	- [Criteria](#criteria)
- [Left Recursion](#left-recursion)
- [Right Recursion](#right-recursion)
- [Look Ahead Problem](#look-ahead-problem)
- [LL(1) Grammars](#ll1-grammars)
	- [Definition](#definition)
	- [First and Follow sets](#first-and-follow-sets)
		- [First set](#first-set)
		- [Follow set](#follow-set)
		- [Example](#example)

## Derivation and Parse Trees

There are 2 different types of derivation:

- Leftmost Derivation
- Rightmost Derivation

**Leftmost Derivation** refers to finding the leftmost non-terminal node and applying a production to it, a.k.a expanding it.

**Rightmost Derivation** refers to finding the rightmost non-terminal node and applying a production to it, a.k.a expanding it.

Usually, scanning is done Left to Right, which reflects Top-Down Parsing, where we start with the symbol node and end with a string of tokens.

Rightmost Derivation reflects Bottom-Up Parsing, where we start with a string of tokens and end with the start symbol.

## Ambiguous Grammars

### Ambiguity

Ambiguous Grammars refers to grammars which, for the same input, can produce different parse trees.
This is problematic because it leads to ambiguous results and a lot of the times straight up wrong results.

Take grammar G for example:

```g4
expr
	: expr op expr
	| (expr)
	| -expr
	| num
	;
op
	: +
	| *
	;
```


For the input `124 + 23.5 * 86` we can get 2 different derivations:

- One where we expand the right node into `expr op expr` and we end up with `124 + (23.5 * 86)` which equals 2145, which is correct.
- Another where we expand the left node into `expr op expr` and we end up with `(124 + 23.5) * 86` which equals 12685, which is incorrect.

### Eliminating ambiguity

Sometimes adding non-terminal nodes can eliminate the ambiguity

For example, let's rewrite grammar G:

```g4
expr
	: expr + term
	| term
	;
term
	: term * unit
	| unit
	;
unit
	: num
	| (expr)
	;
```

This way, we eliminated the ambiguity

### Criteria

- If a grammar has more than one leftmost derivation for a single *sentential form*, the grammar is **ambiguous**
- If a grammar has more than one rightmost derivation for a single *sentential form*, the grammar is **ambiguous**
- The leftmost and rightmost derivations for a *sentential form* may differ, even in an unambiguous grammar

## Left Recursion

It is related to Top-Down Parsing.

- Start at the root of the parse tree and grow toward leaves
- Pick a production and try to match the input
- Bad "pick" means it may need to backtrack
- Some grammars are backtrack-free (*predictive parsing*)

**Note**: Top-Down Parsers cannot handle left-recursive grammars

Because of this, for a top-down parser, any recursion must be right recursion.
We need to convert left recursion into right recursion.

Take this grammar:

```g4
Fee
	: Fee a
	| B
	;
```

We can rewrite this as, where ε represents the empty string:

```g4
Fee
	: B Fie
	;
Fie
	: a Fie
	| ε
	;
```

This grammar accepts the same language but only uses right recursion.

## Right Recursion


It is related to Bottom-Up Parsing

- Start at the leaves and grow toward root
- As input is consumed, encode possibilities in an internal state
- Start in a state valid for legal first tokens
- Bottom-up parsers handle a large class of grammars

## Look Ahead Problem

When implementing a parser, a parser may need to look ahead a lot before deciding .

Take grammar G, for example:

```g4
stmt
	: a long b
	| a long c
	;
long
	: x long | x
	;
```

This grammar is problematic for a simple reason:

- There can be an "infinite" number of `x` between an `a` and either a `b` or a `c`.

Because of this, the parser needs to look a lot ahead before deciding which production to choose.

## LL(1) Grammars

### Definition

**LL(1)** stands for "**L**eft to Right, **L**eftmost derivation, **1** lookahead".

This property is crucial for parsers because it allows them to make deterministic decisions without backtracking.

An **LL(1)** grammar can be parsed using a predictive parser, which is relatively simple to implement compared to other parsing techniques.
One kind of predictive parser is the *recursive descent* parser.

For a grammar to be **LL(1)**, it must meet the following criteria:

1. No left recursion
2. No ambiguity
3. No first/follow set conflicts. This means that the first and/or follow sets of different symbols must be disjoint.

By eliminating *left recursion* and *left factoring*, we can transform some **non-LL(1)** grammars into grammars that meet the **LL(1)** property. However, that is not always the case.

### First and Follow sets

#### FIRST set

- The $FIRST$ set of a non-terminal symbol $A$ is the set of terminal symbols that can appear as the first symbol of any string derivable from $A$.
- To find the $FIRST$ set of a non-terminal $A$, consider all the productions for $A$.
- If $A$ can derive a string starting with a terminal symbol $\alpha$, then $\alpha$ is in the $FIRST$ set of $A$.
- If $A$ can derive a string starting with another non-terminal $\beta$, then include the $FIRST$ set of $\beta$ in the $FIRST$ set of $A$.
- Repeat this process until no new symbols can be added to the $FIRST$ set.

#### FOLLOW set

- The $FOLLOW$ set of a non-terminal symbol $A$ is the set of terminal symbols that can appear immediately to the right of $A$ in any string derivable from the start symbol.
    
- To find the $FOLLOW$ set of a non-terminal $A$, consider all occurrences of $A$ in the grammar.
    
- If $A$ is the start symbol, then $EOF$ (end of file marker) is always in the $FOLLOW$ set of $A$.
    
- If $A$ appears on the right-hand side of a production, consider the symbols that can follow $A$ in that production:
    
    - If $A$ is followed by a terminal symbol $\alpha$, then $\alpha$ is in the $FOLLOW$ set of $A$.
        
    - If $A$ is followed by another non-terminal $\beta$, then include the $FIRST$ set of $\beta$ in the $FOLLOW$ set of $A$, excluding ε (empty string) if it's in the $FIRST$ set of $\beta$.
        
- If $A$ is at the end of a production or if $A$ can derive the empty string (ε), include the $FOLLOW$ set of the non-terminal containing the production.

#### Example

Let's take the following grammar:

```
S -> AB
A -> a | ε
B -> b | ε
```

1. **$FIRST$ Set of $S$**:
    
    - $S$ derives strings starting with $A$, so $FIRST(S)=\{a,ε\}$.
2. **$FIRST$ Set of $A$**:
    
    - $A$ derives strings starting with $a$ or $ε$, so $FIRST(A)=\{a,ε\}$.
3. **$FIRST$ Set of $B$**:
    
    - $B$ derives strings starting with $b$ or $ε$, so $FIRST(B)=\{b,ε\}$.
4. **$FOLLOW$ Set of $S$**:

	- $S$ is the start symbol, so $FOLLOW(S) = \{EOF\}$, where $ represents the end of input marker
5. **$FOLLOW$ Set of $A$**

	- $A$ appears immediately to the right of $S$, so $FOLLOW(A) = \{b, EOF\}$
6. **$FOLLOW$ Set of $B$**

	- $B$ appears immediately to the right of $A$, so $FOLLOW(B) = \{EOF\}$
