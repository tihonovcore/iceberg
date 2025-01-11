grammar Iceberg;

@header {
package iceberg.antlr;
}

file : (printStatement SEMICOLON)* EOF;

printStatement : PRINT expression;

expression
  : NUMBER
  | FALSE
  | TRUE
  | STRING
  ;

PRINT : 'print';

NUMBER : '0' | '-'? [1-9][0-9]*;
FALSE: 'false';
TRUE: 'true';

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
