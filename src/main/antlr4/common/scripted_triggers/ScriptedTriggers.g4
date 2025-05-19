grammar ScriptedTriggers;

root
    : (trigger_item|variable_item)* EOF
    ;
variable_item
    : variable ASSIGN val
    | key ASSIGN val
    ;
trigger_item:
    trigger_name ASSIGN trigger_body
;
trigger_name: ID;
trigger_body: LBRACE trigger_body_item* RBRACE;
trigger_body_item:
      logical_expr
    | object_compare_expr
    | value_compare_expr
    | if_else_expr
    | switch_expr
    | val
    ;
//条件宏
conditional_macroblock: LBRACKET LBRACKET BANG? key RBRACKET trigger_item  RBRACKET;


//a=b
value_compare_expr: key relational_operators val;
//a={b=c}
//a={a b}
object_compare_expr: key ASSIGN LBRACE (value_compare_expr|object_compare_expr|logical_expr|if_else_expr|switch_expr|val|conditional_macroblock)* RBRACE;
//逻辑门运算
// OR { A=C }
logical_expr: logical_operators ASSIGN LBRACE (value_compare_expr|object_compare_expr|logical_expr|if_else_expr|switch_expr|val|conditional_macroblock)* RBRACE ;
//条件运算
// if { A=C }
if_else_expr: if_else_key ASSIGN LBRACE (value_compare_expr|object_compare_expr|logical_expr|if_else_expr|switch_expr|val|conditional_macroblock)* RBRACE;
switch_expr: switch_key ASSIGN LBRACE (value_compare_expr|object_compare_expr|logical_expr|if_else_expr|switch_expr|val|conditional_macroblock)* RBRACE;



//数值右值
val
    : INTEGER
    | BOOLEAN
    | FLOAT
    | STRING
    | ATTRIB
    | ID
    | variable
    | call_script_trigger

    ;
//参数
call_script_trigger: '"'key'"';
//全局变量
variable : '@' key;
logical_operators
    : AND
    | OR
    | NOT
    | NOR
    ;
relational_operators
    : ASSIGN
    | GT
    | LT
    | GE
    | LE
    | NEQ
    ;
if_else_key
    : IF
    | ELSE_IF
    | ELSE
    ;
switch_key
    : SWITCH
    ;

key
    : ID
    | ATTRIB
    ;

ACCESSOR
    : '.'
    | ':'
    ;
VARIABLE_PREFIX
    : '@'
    ;
//以下是token定义
//常量放在最前面
// 运算符常量
ASSIGN    : '=';
GT        : '>';
LT        : '<';
GE        : '>=';
LE        : '<=';
NEQ       : '!=';
PLUS      : '+';
MINUS     : '-';
MULT      : '*';
DIV       : '/';

IF : 'if';
ELSE_IF : 'else_if ';
ELSE : 'else';
SWITCH: 'switch';

OR       : 'OR';
AND      : 'AND';
NOT      : 'NOT';
NOR      : 'NOR';

BOOLEAN: YES | NO;
YES      : 'yes';
NO       : 'no';

// 分隔符
LPAREN    : '(';
RPAREN    : ')';
LBRACE    : '{';
RBRACE    : '}';
SEMI      : ';';
COMMA     : ',';

//宏相关
LBRACKET    : '[';
RBRACKET    : ']';
BANG      : '!';

ATTRIB
    : ID ACCESSOR (ATTRIB | ID)*
    ;


ID
    : IDENTIFIER
    | INTEGER
    ;




INTEGER
    : [+-]? INTEGERFRAG
    ;
FLOAT
    : [+-]? INTEGERFRAG'.'INTEGERFRAG
    ;


fragment INTEGERFRAG
    : [0-9]+
    ;



IDENTIFIER
    : IDENITIFIERHEAD IDENITIFIERBODY*
    ;
fragment IDENITIFIERHEAD
    : [a-zA-Z$|/]
    ;

fragment IDENITIFIERBODY
    : IDENITIFIERHEAD
    | [0-9_]
    ;
STRING
    : '"' ~["\r\n]+ '"'
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