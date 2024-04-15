grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

// OPERATORS SORTED BY PRECEDENCE
// 1. Level 16
LPAREN : '(' ;
RPAREN : ')' ;
LBRACK : '[' ;
RBRACK : ']' ;
DOT : '.' ;

// 2. Level 14
NOT : '!' ;

// 3. Level 13
NEW : 'new' ;

// 4. Level 12
MUL : '*' ;
DIV : '/' ;

// 5. Level 11
ADD : '+' ;
SUB : '-' ;

// 6. Level 9
LT : '<' ;
LE : '<=' ;
GT : '>' ;
GE : '>=' ;

// 7. Level 8
EQ : '==' ;
NEQ : '!=' ;

// 8. Level 4
AND : '&&' ;

// 9. Level 3
OR : '||' ;

// 10. Level 1
EQUALS : '=' ;

// STATEMENTS
IMPORT : 'import' ;
CLASS : 'class' ;
EXTENDS : 'extends' ;
PUBLIC : 'public' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
RETURN : 'return' ;

// TYPES
INT : 'int' ;
BOOLEAN : 'boolean' ;

// VALUES
TRUE : 'true';
FALSE : 'false';
THIS : 'this';
INTEGER : '0' | [1-9] [0-9]* ;
ID : [a-zA-Z_$][0-9a-zA-Z_$]* ;

SEMI : ';' ;
COMMA : ',' ;
LCURLY : '{' ;
RCURLY : '}' ;
LINECOMMENT : '//' ~('\n'|'\r')* -> skip ; // Single Line Comment
COMMENT : '/*' .*? '*/' -> skip ;          // Multi-Line Comment

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
    ;


classDecl
    : CLASS name=ID
        (EXTENDS superName=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

importDecl
    : IMPORT name+=ID (DOT name+=ID)* SEMI
    ;

varDecl
    : type name=ID SEMI
    ;

type locals[boolean isArray=false, boolean isVarargs=false]
    : name=INT LBRACK RBRACK {$isArray=true;}   // Int Array
    | name=ID LBRACK RBRACK  {$isArray=true;}   // Other Array Types
    | name=INT '...'         {$isVarargs=true; $isArray=true;}
    | name=INT
    | name=BOOLEAN
    | name=ID
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false, int numParams=0]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param {$numParams+=1;} (COMMA param {$numParams+=1;})*)? RPAREN
        LCURLY varDecl* stmt* RCURLY
    | (PUBLIC {$isPublic=true;})?
        'static' {$isStatic=true;} type name=ID
        LPAREN param {$numParams+=1;} RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY #MultStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | expr EQUALS expr SEMI #AssignStmt
    | RETURN expr SEMI #ReturnStmt
    ;

expr locals[int numArgs=0]
    : expr LBRACK expr RBRACK #ArrAccessExpr
    | LPAREN expr RPAREN #ParenExpr
    // TODO: need to change this length to ID or else can't create variable length
    | expr DOT func='length' #LengthCall
    | expr DOT func=ID LPAREN (expr {$numArgs+=1;} (COMMA expr {$numArgs+=1;})*)? RPAREN #FunctionCall
    | expr DOT name=ID #FieldCall
    | op=NOT expr #UnaryExpr
    | NEW INT LBRACK expr RBRACK #NewArray
    | NEW name=ID LPAREN RPAREN #NewClassObj
    | expr op=(MUL | DIV) expr #BinaryExpr
    | expr op=(ADD | SUB) expr #BinaryExpr
    | expr op=(LT | LE | GT | GE) expr #BinaryExpr
    | expr op=AND expr #BinaryExpr
    | expr op=OR expr #BinaryExpr
    | LBRACK (expr (COMMA expr)*)? RBRACK #ArrayInit
    | value=INTEGER #IntLiteral
    | value=(TRUE | FALSE) #BoolLiteral
    | name=ID #Var
    | name=THIS #This
    ;
