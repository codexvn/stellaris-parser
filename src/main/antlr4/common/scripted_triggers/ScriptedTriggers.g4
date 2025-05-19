grammar ScriptedTriggers;
//https://stellaris.paradoxwikis.com/Conditions
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
trigger_body: trigger_body_start logical_block_expr* trigger_body_end;
trigger_body_start
    : LBRACE
    ;
trigger_body_end
    : RBRACE
    ;
logical_block_expr: value_compare_expr|object_compare_expr|logical_expr|if_else_expr|switch_expr|array_compare_expr_val|conditional_macroblock;
block_start
    : LBRACE
    ;
block_end
    : RBRACE
    ;
//条件宏
conditional_macroblock: LBRACKET LBRACKET BANG? key RBRACKET trigger_item RBRACKET;
//a=b
value_compare_expr: value_compare_expr_key relational_operators value_compare_expr_val;
value_compare_expr_key: key;
value_compare_expr_val: val;
//a={b=c}
//a={a b}
object_compare_expr: object_compare_expr_key ASSIGN object_compare_expr_val ;
object_compare_expr_key: key;
object_compare_expr_val: block_start logical_block_expr* block_end;
array_compare_expr_val: val;
//逻辑门运算
// OR { A=C }
logical_expr: logical_expr_key ASSIGN logical_expr_val  ;
logical_expr_key: logical_operators;
logical_expr_val: block_start logical_block_expr* block_end;
//条件运算
// if { A=C }
if_else_expr: if_else_expr_key ASSIGN if_else_expr_val;
if_else_expr_key: IF| ELSE_IF| ELSE;
if_else_expr_val: block_start logical_block_expr* block_end;
switch_expr: switch_expr_key ASSIGN switch_expr_val;
switch_expr_key: SWITCH;
switch_expr_val: block_start logical_block_expr* block_end;




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
    | NAND
    ;
relational_operators
    : ASSIGN
    | GT
    | LT
    | GE
    | LE
    | NEQ
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
NAND     : 'NAND';

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
LOGGING
    : 'log' ~[\r\n]* -> channel(HIDDEN)
    ;

SPACE
    : [ \t\f] -> channel(HIDDEN)
    ;

NL
    : [\r\n] -> channel(HIDDEN)
    ;
WS : [ \t\r\n]+ -> skip;

BOM: '\uFEFF' -> skip;