grammar Iceberg;

root : (printStatement SEMICOLON)* EOF;

printStatement : PRINT expression;

expression : NUMBER;

PRINT : 'print';
NUMBER : '0' | [1-9][0-9]*;
SEMICOLON : ';';

WS: [ \n\t\r]+ -> skip;
