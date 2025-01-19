grammar Iceberg;

@header {
package iceberg.antlr;
}

file : (statement SEMICOLON)* EOF;

statement
  : printStatement
  ;

printStatement : PRINT expression;

expression
  : NOT atom                                              #negateExpression
  | MINUS atom                                            #unaryMinusExpression
  | left=expression (STAR | SLASH)      right=expression  #multiplicationExpression
  | left=expression (PLUS | MINUS)      right=expression  #additionExpression
  | left=expression (LE | GE | LT | GT) right=expression  #relationalExpression
  | left=expression (EQ | NEQ)          right=expression  #equalityExression
  | left=expression AND                 right=expression  #logicalAndExpression
  | left=expression OR                  right=expression  #logicalOrExpression
  | atom                                                  #atomExpression
  ;

atom
  : OPEN_PARENTHESIS expression CLOSE_PARENTHESIS
  | NUMBER
  | FALSE
  | TRUE
  | STRING
  ;

PLUS  : '+';
MINUS : '-';
STAR  : '*';
SLASH : '/';
OPEN_PARENTHESIS  : '(';
CLOSE_PARENTHESIS : ')';

EQ : '==';
NEQ : '!=';

LE : '<=';
GE : '>=';
LT : '<';
GT : '>';

NOT : 'not';
AND : 'and';
OR  : 'or';

PRINT : 'print';

NUMBER : '0' | '-'? [1-9][0-9]*;
FALSE  : 'false';
TRUE   : 'true';
NULL   : 'null';

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

WS: [ \n\t\r]+ -> skip;
