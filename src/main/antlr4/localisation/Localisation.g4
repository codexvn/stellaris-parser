grammar Localisation;
//https://stellaris.paradoxwikis.com/Localisation_modding
root:
    language_type COLON
    localisation_item* EOF
    | EOF
    ;

localisation_item :
    title COLON re_translation_flag? title_val
    | title_desc COLON re_translation_flag? title_desc_val
    ;
re_translation_flag: INTEGER ;
language_type
    : 'l_english'
    | 'l_braz_por'
    | 'l_german'
    | 'l_french'
    | 'l_spanish'
    | 'l_polish'
    | 'l_russian'
    | 'l_simp_chinese'
    | 'l_japanese'
    | 'l_korean'
    ;

title: TITLE_KEY|INTEGER;
title_val: VAL ;
title_desc: TITLE_DESC_KEY;
title_desc_val: VAL;
//desc需要排在前面,保证优先级
// 分隔符
COLON    : ':';
INTEGER : [0-9]+;
VAL
    : '"' ~[\r\n]+ '"'
    | '""'
    ;
TITLE_DESC_KEY
    : [a-zA-Z0-9_\-."']+'_desc'
    ;
TITLE_KEY
    : [a-zA-Z0-9_\-."']+
    ;


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