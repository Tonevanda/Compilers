
## Activation Records

### What are Activation Records (AR) and what are they used for?

Activation Records, also known as stack frames, are data structures that compilers use to manage information needed for function execution. They are essential for implementing function calls, maintaining the state of a function, and enabling the proper return to the caller function once execution is complete.

### What information does an AR capture and where are they allocated and why?

An Activation Record typically captures the following information:

1. **Return Address:** The address to return to after the function call completes.
2. **Dynamic Link (Control Link):** A pointer to the activation record of the caller function, which helps in maintaining the call stack.
3. **Static Link (Access Link):** A pointer to the activation record of the lexically enclosing function, useful in languages with nested functions.
4. **Local Variables:** Storage for the local variables declared within the function.
5. **Parameters:** Storage for the function's parameters passed during the call.
6. **Saved Registers:** Registers that need to be preserved across function calls, typically including the base pointer and any callee-saved registers.
7. **Temporary Values:** Space for intermediate values and expressions calculated during function execution.

**Activation Records** are typically allocated on the call **stack**. The reasons for this include:

- **Efficiency:** Stacks allow for efficient last-in, first-out (LIFO) allocation and deallocation, which matches the function call and return sequence.
- **Organization:** The call stack naturally organizes activation records in the order of function calls, making it straightforward to manage the return sequence and dynamic linking.
- **Memory Management:** Using a stack helps in managing memory for function calls without the need for complex memory allocation algorithms.

However, if a procedure can outlive its caller or if it can return an object that can reference its execution state, then the **Activation Record** must be kept in the **Heap**.

### What is an Access Link (Static Link)?

The Access Link, also known as the Static Link, is a pointer in an activation record that points to the activation record of the lexically enclosing function. It is used to access non-local variables that are not global but are defined in a lexically enclosing scope.

### What is a Dynamic Link?

The Dynamic Link, also known as the Control Link, is a pointer in an activation record that points to the activation record of the calling function. It represents the dynamic (runtime) call chain.

## Display Mechanism

#### What is the Display Mechanism?

The display mechanism uses an array, called the display, to keep track of the activation records of the active procedures (functions) at each nesting level. Each entry in the display array points to the activation record of the most recent invocation of a procedure at a specific lexical level.

#### Purpose of the Display Mechanism

The primary purpose of the display mechanism is to provide fast access to non-local variables from lexically enclosing scopes, without the overhead of following multiple static links. It is particularly useful in languages with extensive use of nested procedures.

#### How the Display Mechanism Works

1. **Display Array:** An array where each element points to the activation record of the most recent function call at the corresponding lexical level.
2. **Updating the Display:** When a function is called, the display is updated to point to the new activation record at the appropriate lexical level.
3. **Restoring the Display:** On function exit, the display is restored to the previous state for that lexical level.

## Object-Oriented Programming

### Method Overloading

#### What is Method Overloading?

Method overloading is the ability to define multiple methods in the same class with the same name but different parameter lists (either in number, type, or both). The correct method to be called is determined at compile time based on the method signature.

#### How it Works

1. **Method Signature:** Each overloaded method has a unique signature composed of the method name and its parameter types.
2. **Compile-Time Resolution:** The compiler determines which method to call based on the argument types at the call site.
3. **Activation Records:** Each overloaded method has its own activation record structure, similar to any other function or method.

### Class Hierarchy and Inheritance

#### What is Class Hierarchy?

Class hierarchy in object-oriented programming involves classes arranged in a parent-child relationship through inheritance. A derived (or child) class inherits properties and methods from its base (or parent) class, allowing for code reuse and polymorphism.

#### Method Overriding

Method overriding occurs when a derived class provides a specific implementation for a method that is already defined in its base class. This enables polymorphism, where the method call is resolved at runtime based on the actual object type.

#### How it Works

1. **Virtual Table (vtable):** To support dynamic dispatch, the compiler uses a vtable (virtual table) for each class that has virtual methods.
    
    - Each entry in the vtable corresponds to a method and points to the method implementation.
    - Derived classes have their vtables, which may override entries from the base class's vtable.
2. **Dynamic Dispatch:** When a virtual method is called on an object, the runtime system uses the vtable to resolve the method call to the correct implementation based on the actual object's class.

For overridden methods, the activation record includes a pointer to the vtable of the object's actual class.

#### Steps in Creating a vtable

1. **Initialize vtable with Parent's Methods:**
    
    - The vtable for the current class starts by copying the entries from the vtable of its parent class.
    - This ensures that all inherited methods are initially included in the vtable.
2. **Add/Replace Current Class Methods:**
    
    - Methods defined in the current class are added to the vtable.
    - If a method in the current class overrides a method in the parent class, the entry in the vtable corresponding to that method is replaced with the overridden method from the current class.
3. **Include New Methods:**
    
    - Any additional methods defined only in the current class are added to the vtable as new entries.
