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
INTS : 'int''...' ; //VARARGS INTs
BOOLEAN : 'boolean' ;

// VALUES
TRUE : 'true';
FALSE : 'false';
THIS : 'this';
INTEGER : [0-9]+ ;
ID : [a-zA-Z][0-9a-zA-Z_$]* ;

SEMI : ';' ;
COMMA : ',' ;
LCURLY : '{' ;
RCURLY : '}' ;

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
    : IMPORT name=ID (DOT name=ID)* SEMI
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : type '['']'
    | name=INT
    | name=BOOLEAN
    | name=INTS
    | name=ID //To discuss: Are other types always uppercase in the first letter? if so create new token or is that later dealt with elewhere
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY varDecl* stmt* RCURLY
    | (PUBLIC {$isPublic=true;})?
        'static' type name='main'
        LPAREN type name=ID RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY #MultStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprCall
    | expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt //To discuss: should tecnically be in methodDecl or at least a separate stmt so it can be put there
    ;

expr
    : expr LBRACK expr RBRACK #ArrAccessExpr
    | LPAREN expr RPAREN #ParenExpr
    | expr DOT func='length' #LengthCall
    | expr DOT func=ID LPAREN (expr (COMMA expr)*)? RPAREN #FunctionCall
    | op=NOT expr #UnaryBexpr
    | NEW INT LBRACK expr RBRACK #NewArray
    | NEW ID LPAREN RPAREN #NewClassObj
    | expr op=(MUL | DIV) expr #BinaryAExpr
    | expr op=(ADD | SUB) expr #BinaryAExpr
    | expr op=(LT | LE | GT | GE) expr #BinaryBExpr
    | expr op=(EQ | NEQ) expr #BinaryBExpr
    | expr op=AND expr #BinaryBExpr
    | expr op=OR expr #BinaryBExpr
    | LBRACK (expr (COMMA expr)*)? RBRACK #ArrayInit
    | value=INTEGER #IntLiteral
    | value=(TRUE | FALSE) #BoolLiteral
    | name=ID #Var
    | name=THIS #This
    ;
