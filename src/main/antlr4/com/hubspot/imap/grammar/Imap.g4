grammar Imap;

listresponse: 'LIST' WS array WS context=QUOTEDSTRING WS name=QUOTEDSTRING;
array: LPAREN (WS? value=STRING)* RPAREN;

LPAREN : '(' ;
RPAREN : ')' ;

STRING : [a-zA-Z0-9/\\\[\]]+;
QUOTEDSTRING : '"' LENIENTSTRING (LENIENTSTRING | SPACE)* '"';
fragment LENIENTSTRING : [a-zA-Z0-9/\\\[\]\s];
fragment SPACE : ' ';

WS: (' ' | '\r' | '\n')+;
