grammar Iceberg;

@header {
package iceberg.antlr;
}

file : statement* EOF;

statement
  : expression SEMICOLON
  | printStatement SEMICOLON
  | defStatement SEMICOLON
  | ifStatement
  | whileStatement
  | functionDefinitionStatement
  | classDefinitionStatement
  | returnStatement SEMICOLON
  | block
  ;

printStatement : PRINT expression;

defStatement
  : DEF name=ID
  ( COLON type=ID (ASSIGN expression)?
  | ASSIGN expression
  );

ifStatement
  : IF condition=expression
    THEN thenStatement=statement
    (ELSE elseStatement=statement)?
  ;

whileStatement
  : WHILE expression THEN statement
  ;

classDefinitionStatement
  : CLASS name=ID OPEN_BRACE defStatement* functionDefinitionStatement* CLOSE_BRACE
  ;

functionDefinitionStatement
  : FUN name=ID OPEN_PARENTHESIS parameters CLOSE_PARENTHESIS
    (COLON returnType=ID)? block
  ;

parameters
  : ((parameter COMMA)* parameter)?
  ;

parameter : name=ID COLON type=ID;

functionCall
  : name=ID OPEN_PARENTHESIS arguments CLOSE_PARENTHESIS
  ;

arguments
  : ((expression COMMA)* expression)?
  ;

returnStatement
  : RETRUN expression?
  ;

block : OPEN_BRACE statement* CLOSE_BRACE;

expression
  : NOT atom                                              #negateExpression
  | MINUS atom                                            #unaryMinusExpression
  | expression DOT  (ID | functionCall)                   #memberExpression
  | left=expression (STAR | SLASH)      right=expression  #multiplicationExpression
  | left=expression (PLUS | MINUS)      right=expression  #additionExpression
  | left=expression (LE | GE | LT | GT) right=expression  #relationalExpression
  | left=expression (EQ | NEQ)          right=expression  #equalityExression
  | left=expression AND                 right=expression  #logicalAndExpression
  | left=expression OR                  right=expression  #logicalOrExpression
  | left=expression ASSIGN              right=expression  #assignExpression
  | atom                                                  #atomExpression
  ;

atom
  : OPEN_PARENTHESIS expression CLOSE_PARENTHESIS
  | functionCall
  | NUMBER
  | FALSE
  | TRUE
  | STRING
  | ID
  ;

PLUS  : '+';
MINUS : '-';
STAR  : '*';
SLASH : '/';
OPEN_PARENTHESIS  : '(';
CLOSE_PARENTHESIS : ')';
OPEN_BRACE        : '{';
CLOSE_BRACE       : '}';

EQ : '==';
NEQ : '!=';

LE : '<=';
GE : '>=';
LT : '<';
GT : '>';

NOT : 'not';
AND : 'and';
OR  : 'or';

PRINT  : 'print';
WHILE  : 'while';
IF     : 'if';
THEN   : 'then';
ELSE   : 'else';
FUN    : 'fun';
RETRUN : 'return';
CLASS  : 'class';

NUMBER : '0' | '-'? [1-9][0-9]*;
FALSE  : 'false';
TRUE   : 'true';
NULL   : 'null';

DEF    : 'def';
COLON  : ':';
ASSIGN : '=';
ID     : [A-Za-z_][A-Za-z_0-9]*;
DOT    : '.';

STRING
  : '"' (ESCAPE | CHAR)* '"'
  ;
fragment ESCAPE
  : '\\"'
  | '\\n'
  ;
fragment CHAR
  : ~ ["\\]
  ;

SEMICOLON : ';';
COMMA     : ',';

WS: [ \n\t\r]+ -> skip;
COMMENT : '//' ~[\n\r]* -> skip;
