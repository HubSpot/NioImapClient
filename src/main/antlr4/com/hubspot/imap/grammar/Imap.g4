grammar Imap;

list: 'LIST' SPACE? array SPACE? context=quotedString SPACE? name=quotedString;

select: (flags | exists | recent | uidnext | SPACE)+;

flags: 'FLAGS' SPACE array;
exists: value=INT SPACE 'EXISTS';
recent: value=INT SPACE 'RECENT';
uidnext: OK LBRACKET 'UIDNEXT' INT RBRACKET string;

array: LPAREN (SPACE? value+=string)* RPAREN;

string: (ALPHA | DIGIT | CONTROL_CHARACTER | UNDERSCORE | HYPHEN | DOT | UNICODE)+;
quotedString : QUOTE (ESCAPED_QUOTE | ~QUOTE)*  QUOTE;

LPAREN : '(' ;
RPAREN : ')' ;

LBRACKET : '[' ;
RBRACKET : ']' ;

OK: 'OK' ;

INT: [0-9]+;
ALPHA : [a-zA-Z];
DIGIT : [0-9];
UNICODE : '\u007B'..'\uFFFF';
CONTROL_CHARACTER : [\[\]!#$%&'*+/=?^`{|}~\\];
HYPHEN : '-';
UNDERSCORE : '_';
//SPACE : [\u0009-\u000D\ \u0085\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000];
SPACE: ' ';
DOT : '.';

QUOTE : '"';
ESCAPED_QUOTE : '\\"';

NL: ('\r' | '\n')+ -> skip;
