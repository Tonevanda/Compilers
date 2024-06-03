## Local Register Allocators

### Top-Down

- Estimate benefit of putting a variable in a register in a certain basic block
- Calculate total benefit by multiplying the benefit of a variable in a block by the frequency of that block
- Assign the highest-payoff variables to registers

Example:

![[Pasted image 20240528171033.png]]

Assume bb2 repeats 10 times.

Benefit for basic blocks:

- Benefit(x,bb1) = 0
- Benefit(y,bb1) = 0
- Benefit(z,bb1) = 2
- Benefit(x,bb2) = 3
- Benefit(y,bb2) = 2
- Benefit(z,bb2) = 0
- Benefit(x,bb3) = 0
- Benefit(y,bb3) = 0
- Benefit(z,bb3) = 2

Frequency:

- bb1 = 1
- bb2 = 10
- bb3 = 1

Total benefit:

- TotBenefit(x) = $(0 * 1) + (3 * 10) + (0 * 1) = 30$ 
- TotBenefit(y) = $(0 * 1) + (2 * 10) + (0 * 1) = 20$
- TotBenefit(z) = $(2 * 1) + (0 * 10) + (2 * 1) = 4$

Let's assume only 2 registers are available.
*X* and *Y* are the highest-payoff variables, so they will be allocated to the registers.

### Bottom-Up 

Let's consider this example:

1. $a = b + c$
2. $d = a * e$
3. $f = d + g$

#### Step-by-Step Allocation:

1. **Instruction 1: `a = b + c`**
    
    - Allocate registers for `b` and `c`. Let's say `R1` for `b` and `R2` for `c`.
    - Perform the addition and store the result in a new register, say `R3`.
    - Register State: `R1` = `b`, `R2` = `c`, `R3` = `a`
    - `R3` is now occupied by `a`.
2. **Instruction 2: `d = a * e`**
    
    - Allocate a register for `e`. Let's say `R4` for `e`.
    - Use `R3` for `a` (already allocated) and `R4` for `e`.
    - Perform the multiplication and store the result in a new register, say `R5`.
    - Register State: `R1` = `b`, `R2` = `c`, `R3` = `a`, `R4` = `e`, `R5` = `d`
    - `R5` is now occupied by `d`.
3. **Instruction 3: `f = d + g`**
    
    - Allocate a register for `g`. Let's say `R6` for `g`.
    - Use `R5` for `d` (already allocated) and `R6` for `g`.
    - Perform the addition and store the result in a new register, say `R7`.
    - Register State: `R1` = `b`, `R2` = `c`, `R3` = `a`, `R4` = `e`, `R5` = `d`, `R6` = `g`, `R7` = `f`
    - `R7` is now occupied by `f`.

#### When Registers are Limited:

Suppose we only have 4 registers available (`R1`, `R2`, `R3`, `R4`):

1. **Instruction 1: `a = b + c`**
    
    - Allocate `R1` for `b` and `R2` for `c`.
    - Store result in `R3` for `a`.
    - Register State: `R1` = `b`, `R2` = `c`, `R3` = `a`
2. **Instruction 2: `d = a * e`**
    
    - `R1`, `R2`, and `R3` are occupied.
    - Allocate `R4` for `e`.
    - Store result in `R1` (spill `b` to memory if it is still needed later).
    - Register State: `R1` = `d`, `R2` = `c`, `R3` = `a`, `R4` = `e`
3. **Instruction 3: `f = d + g`**
    
    - `R1` = `d`, `R2` = `c`, `R3` = `a`, `R4` = `e`.
    - Spill `c` from `R2` (since `c` is no longer needed).
    - Allocate `R2` for `g`.
    - Store result in `R3` (spill `a` if it is still needed later).
    - Register State: `R1` = `d`, `R2` = `g`, `R3` = `f`, `R4` = `e`

The choice of which register to spill usually lies on the register with the highest ***Next*** value. ***Next*** value represents how many instructions are left until that register's value is used again. 
