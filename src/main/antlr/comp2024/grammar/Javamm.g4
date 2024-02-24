grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=' ;
SEMI : ';' ;
COMA : ',' ;
DOT : '.' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;

IMPORT : 'import' ;
CLASS : 'class' ;
EXTENDS : 'extends' ;
PUBLIC : 'public' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
RETURN : 'return' ;

INT : 'int' ;
INTL : 'int''['']' ; //INT list
INTS : 'int''...' ; //multiple INTs
BOOLEAN: 'boolean' ;

INTEGER : [0-9] ;
ID : [a-zA-Z][0-9a-zA-Z_$]* ;

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
    : name=INT
    | name=BOOLEAN
    | name=INTL
    | name=INTS
    | name=ID //To discuss: Are other types always uppercase in the first letter? if so create new token or is that later dealt with elewhere
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    | (PUBLIC {$isPublic=true;})?
        'static' 'void' 'main'
        LPAREN 'String' '['']' name=ID RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : (type name=ID (COMA type name=ID)*)?
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
    : expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    ;



