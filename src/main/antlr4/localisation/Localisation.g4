grammar Localisation;

localisation:
    language_type COLON
    localisation_item* EOF
    | EOF
    ;

localisation_item :
    title COLON title_val
    | title_desc COLON title_desc_val
    ;
//待补全
language_type
    : 'l_simp_chinese'
    | 'l_english'
    ;

title: TITLE_KEY;
title_val: VAL ;
title_desc: TITLE_DESC_KEY;
title_desc_val: VAL;
//desc需要排在前面,保证优先级
VAL
    : '"' ~[\r\n]+ '"'
    | '""'
    ;
TITLE_DESC_KEY
    : [a-zA-Z0-9_\-."]+'_desc'
    ;
TITLE_KEY
    : [a-zA-Z0-9_\-."]+
    ;

// 分隔符
COLON    : ':';

COMMENT
    : '#' ~[\r\n]* -> channel(HIDDEN)
    ;

SPACE
    : [ \t\f] -> channel(HIDDEN)
    ;

NL
    : [\r\n] -> channel(HIDDEN)
    ;
WS : [ \t\r\n]+ -> skip;

BOM: '\uFEFF' -> skip;