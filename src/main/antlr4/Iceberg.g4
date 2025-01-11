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
  ;

PRINT : 'print';

NUMBER : '0' | '-'? [1-9][0-9]*;
FALSE: 'false';
TRUE: 'true';

SEMICOLON : ';';

WS: [ \n\t\r]+ -> skip;
