grammar Imap;

list: 'LIST' SPACE? array SPACE? context=quotedString SPACE? name=quotedString ;

select
    : selectresponse+
    ;

selectresponse
    : flags
    | exists
    | recent
    | uidnext
    | uidvalidity
    | permanentflags
    | highestmodseq
    | SPACE
    ;

flags
    : 'FLAGS' SPACE array NL ;
exists
    : value=INT SPACE 'EXISTS' NL ;
recent
    : value=INT SPACE 'RECENT' NL ;
uidnext
    : OK SPACE LBRACKET 'UIDNEXT' SPACE value=INT RBRACKET responseString ;
permanentflags
    : OK SPACE LBRACKET 'PERMANENTFLAGS' SPACE array RBRACKET responseString ;
uidvalidity
    : OK SPACE LBRACKET 'UIDVALIDITY' SPACE value=INT RBRACKET responseString ;
highestmodseq
    : OK SPACE LBRACKET 'HIGHESTMODSEQ' SPACE value=INT RBRACKET responseString ;

array
    : LPAREN (SPACE? value+=string)* RPAREN ;

string
    : (ALPHA | DIGIT | CONTROL_CHARACTER | UNDERSCORE | HYPHEN | DOT | UNICODE)+ ;
quotedString
    : QUOTE? (ESCAPED_QUOTE | ~QUOTE)*  QUOTE? ;
responseString
    : (SPACE? string)+ NL ;

LPAREN   : '(' ;
RPAREN   : ')' ;
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

NL: ('\r' | '\n')+;
