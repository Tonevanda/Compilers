# Lexical Analysis

## Table of Contents

- [NFA to DFA Conversion](#nfa-to-dfa-conversion)
    - [Step 1](#step-1---create-nfas-transition-table)
    - [Step 2](#step-2---create-dfas-start-state)
    - [Step 3](#step-3---create-dfa-transition-table)
    - [Step 4](#step-4---create-dfa-final-state)
    - [Step 5](#step-5---simplify-dfa)
    - [Step 6](#step-6---repeat)
- [DFA to RE Conversion](#dfa-to-re-conversion)
    - [Step 1](#step-1---create-new-start-state)
    - [Step 2](#step-2---create-new-final-state)
    - [Step 3](#step-3---reduction) 

## NFA to DFA Conversion

**Reminder**: The closure of a state is the set of states that can be reached from it using only a certain symbol

## Converting from NFA to DFA

### Step 1 - Create NFA's Transition Table

To convert the NFA to its equivalent transition table, we need to list all the states, input symbols, and the transition rules. The transition rules are represented in the form of a matrix, where the rows represent the current state, the columns represent the input symbol, and the cells represent the next state.

### Step 2 - Create DFA's Start State

The DFA's start state is the ε-closure of an NFA's start state. This means the start state for the DFA is the set of states in the NFA that can be reached without consuming a symbol.

### Step 3 - Create DFA' Transition Table

Just like the NFA transition table, except instead of individual states we represent sets of states.
For each input symbol, the corresponding cell in the transition table contains the epsilon closure of the set of states obtained by following the transition rules in the NFA’s transition table.

### Step 4 - Create DFA Final State

The DFA's final state, much like the start state, is the set of states that contains at least one NFA final state.

### Step 5 - Simplify DFA

To simplify the DFA we can do the following:

- Remove unreachable states: States that cannot be reached from the start state
- Remove dead states: States that cannot reach a final state
- Merge equivalent states: States that have the same transitions for every input can be merged into a single state.

### Step 6 - Repeat

Rinse and repeat steps 3 - 5 until no further simplification is possible.

## DFA to RE Conversion

## Step 1 - Create new Start State

Add a new start state with an $\epsilon$ transition to the old start state.

**Note**: This step is unnecessary if the old start state is already *lonely* (has in-degree 0).

## Step 2 - Create new Final State

Create a new final state with ε transition from all final states.

## Step 3 - Reduction

Pick an internal state to *rip out*. Let's call that state $R$.

1. If $R$ doesn't have a self-loop, replace every $A \overset{r_{in}}{\longrightarrow} R \overset{r_{out}}{\longrightarrow} B$ with: $A \overset{r_{in}r_{out}}{\longrightarrow} B$
2. If $R$ has a self-loop labeled $r_{self}$, replace every $A \overset{r_{in}}{\longrightarrow} R \overset{r_{out}}{\longrightarrow} B$ with: $A \overset{r_{in} r*_{self} r_{out}}{\longrightarrow} B$ 
3. If this results in multiple edges between two states $A$ and $B$, replace them with one edge labeled with the union of their labels.

Repeat this step until the only states left are the start state and final state.
The transition remaining will represent the Regular Expression of the language recognized by the DFA.