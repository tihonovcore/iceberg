grammar Iceberg;

@header {
package iceberg.antlr;
}

file : (printStatement SEMICOLON)* EOF;

printStatement : PRINT expression;

expression : NUMBER;

PRINT : 'print';
NUMBER : '0' | [1-9][0-9]*;
SEMICOLON : ';';

WS: [ \n\t\r]+ -> skip;
