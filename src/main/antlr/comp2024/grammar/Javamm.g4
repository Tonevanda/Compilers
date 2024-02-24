grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
DOT : '.';
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;

IMPORT : 'import';
CLASS : 'class' ;
EXTENDS : 'extends';
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;


INTEGER : [0-9] ;
ID : [a-zA-Z]+ ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
    ;


classDecl
    : CLASS name=ID
        (EXTENDS ID)?
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
    : name= INT
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    ;



