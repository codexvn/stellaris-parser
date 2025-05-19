grammar Technology;
//https://stellaris.paradoxwikis.com/Technology_modding
technology
    : (technology_item | variable_item|inline_script_modifier)* EOF
    ;
variable_item
    : key_ref ASSIGN val
    ;
inline_script_modifier: modifier;
technology_item
    : technology_name ASSIGN technology_body;

technology_name
    : IDENTIFIER
    ;
//部分关键字,防止与值冲突
keywork
    : 'technology'
    | 'component'
    | 'ship'
    | 'gateway'
    | 'component_technology'
    | 'ship_technology'
    ;
technology_body_start
    : LBRACE
    ;
technology_body_end
    : RBRACE
    ;

technology_body
    : technology_body_start (
      area
    | tier
    | category
    | potential
    | ai_update_type
    | prerequisites
    | is_reverse_engineerable
    | start_tech
    | is_rare
    | is_dangerous
    | is_insight
    | weight_modifier
    | icon
    | modifier
    | cost
    | cost_by_script
    | cost_per_level
    | weight
    | levels
    | technology_swap
    | gateway
    | repeatable
    | weight_groups
    | mod_weight_if_group_picked
    | feature_flags
    | prereqfor_desc
    | ai_weight
    | starting_potential)+ technology_body_end
    ;

area: 'area' ASSIGN area_val;
area_val: technology_area_enum;
tier: 'tier' ASSIGN tier_val;
tier_val: val;
category: 'category' ASSIGN  category_val ;
//# Physics
//field_manipulation, particles, computing
//
//# Society
//psionics, new_worlds, statecraft, biology, military_theory
//
//# Engineering
//materials, rocketry, voidcraft, industry
category_val: LBRACE val RBRACE;
icon: 'icon' ASSIGN icon_val;
icon_val: val;
cost: 'cost' ASSIGN cost_val;
cost_val: val;
cost_by_script: 'cost' ASSIGN  cost_by_script_val ;
cost_by_script_val: LBRACE (factor| inline_script)* RBRACE;
cost_per_level: 'cost_per_level' ASSIGN cost_per_level_val;
cost_per_level_val: val;
is_rare: 'is_rare' ASSIGN is_rare_val;
is_rare_val: BOOLEAN;
is_dangerous: 'is_dangerous' ASSIGN is_dangerous_val;
is_dangerous_val: BOOLEAN;
weight: 'weight' ASSIGN (weight_val1|weight_val2);
weight_val1: LBRACE factor RBRACE;
weight_val2: val;
levels: 'levels' ASSIGN levels_val;
levels_val: val;
potential: 'potential' ASSIGN potential_val;
potential_val: condition_statement;
gateway: 'gateway' ASSIGN gateway_val;
gateway_val: 'ship'| IDENTIFIER;

repeatable: 'repeatable' ASSIGN repeatable_val;
repeatable_val: val;
weight_groups: 'weight_groups' ASSIGN weight_groups_val;
weight_groups_val: array_val;
mod_weight_if_group_picked: 'mod_weight_if_group_picked' ASSIGN mod_weight_if_group_picked_val;
mod_weight_if_group_picked_val: condition_statement;
start_tech: 'start_tech' ASSIGN start_tech_val;
start_tech_val: BOOLEAN;
is_reverse_engineerable: 'is_reverse_engineerable' ASSIGN is_reverse_engineerable_val;
is_reverse_engineerable_val: BOOLEAN;
ai_update_type: 'ai_update_type' ASSIGN ai_update_type_val;
ai_update_type_val: ai_update_type_enum;
is_insight: 'is_insight' ASSIGN is_insight_val;
is_insight_val: BOOLEAN;
feature_flags: 'feature_flags' ASSIGN feature_flags_val;
feature_flags_val: array_val;
starting_potential: 'starting_potential' ASSIGN starting_potential_val;
starting_potential_val: condition_statement;





prerequisites: 'prerequisites' ASSIGN prerequisites_val;
prerequisites_val: condition_statement;


technology_swap: 'technology_swap' ASSIGN technology_swap_val;
technology_swap_val
    : LBRACE
        (name
         |inherit_icon
         |inherit_name
         |inherit_effects
         |trigger
         |modifier
         |prereqfor_desc
         |area
         |category
         |weight)+
        RBRACE
    ;
name: 'name' ASSIGN name_val;
name_val: val;
inherit_icon: 'inherit_icon' ASSIGN inherit_icon_val;
inherit_icon_val: val;
inherit_name: 'inherit_name' ASSIGN inherit_name_val;
inherit_name_val: val;
inherit_effects: 'inherit_effects' ASSIGN inherit_effects_val;
inherit_effects_val: val;
trigger: 'trigger' ASSIGN trigger_val;
trigger_val: condition_statement;

factor: 'factor' ASSIGN factor_val;
factor_val: normal_val;
add: 'add' ASSIGN add_val;
add_val: FLOAT|normal_val;
inline_script
    : 'inline_script' ASSIGN (inline_script_val_1|inline_script_val_2);
inline_script_val_1: LBRACE (key ASSIGN val) + RBRACE;
inline_script_val_2: key;

modifier
    : 'modifier' ASSIGN modifier_val
    ;
modifier_val
    : LBRACE
        (factor
        | add
        | compare_condition_expr
        | op_condition_expr
        | inline_script
        )* RBRACE
    ;
weight_modifier: 'weight_modifier' ASSIGN weight_modifier_val;
weight_modifier_val: LBRACE(factor| add| modifier| inline_script)* RBRACE;
ai_weight: 'ai_weight' ASSIGN ai_weight_val ;
ai_weight_val: LBRACE( base|weight|factor| add| modifier| inline_script)* RBRACE;
base: 'base' ASSIGN base_val;
base_val: val;
hide_prereq_for_desc: 'hide_prereq_for_desc' ASSIGN hide_prereq_for_desc_val;
hide_prereq_for_desc_val : prereq_for_category_enum;

prereqfor_desc : 'prereqfor_desc' ASSIGN prereqfor_desc_val;
prereqfor_desc_val: LBRACE(hide_prereq_for_desc|prereq_for_category_enum ASSIGN i18_val)*RBRACE;
i18_val: LBRACE (i18_title| i18_desc)+ RBRACE;
i18_title : 'title' ASSIGN i18_title_val;
i18_title_val : val;
i18_desc : 'desc' ASSIGN i18_desc_val;
i18_desc_val : val;


prereq_for_category_enum:
		'ship'
		|'custom'
		|'component'
		|'diplo_action'
		|'feature'
		|'resource';
technology_area_enum:
        'engineering'
        |'society'
        |'physics';
ai_update_type_enum:
        'all' |'military';
//逻辑门条件表达式
op_condition_expr: LOGICAL_OPERATORS ASSIGN condition_statement;

// 条件判断表达式
condition_expr
    : compare_condition_expr
    | op_condition_expr
    | in_condition_expr
    ;
// 条件判断块
// A=B
// OR { A=C }
condition_statement
    : LBRACE condition_expr* RBRACE
    ;

//数据比较条件表达式
compare_condition_expr: value_compare_condition_expr| object_compare_condition_expr;
//在列表中
in_condition_expr:  id_+;
//用于数据的比较
value_compare_condition_expr: condition_key value_compare val;
//条件块条件表达式
object_compare_condition_expr: condition_key ASSIGN condition_statement;
//是否在数组中
condition_key: key ;

array_val
    : LBRACE val* RBRACE
    ;

val
    : normal_val |keywork;

normal_val
    :
    STRING
    | INTEGER
    | BOOLEAN
    | FLOAT
    | key
    | key_ref
    ;
key
    : id_
    | attrib
    | tag
    | variable
    ;
variable: '$'id_'$';
key_ref
    : '@' id_
    ;
//因为有些tag是以#开头的,所以需要全部枚举出来
tag : '#repeatable'| 'repeatable';
attrib
    : id_ accessor (attrib | id_)
    ;

accessor
    : '.'
    | '@'
    | ':'
    ;

id_
    : IDENTIFIER
    | STRING
    | INTEGER
    ;

BOOLEAN
	: YES
	| NO
	;

LOGICAL_OPERATORS
    : AND
    | OR
    | NOT
    | NOR
    ;
IDENTIFIER
    : IDENITIFIERHEAD IDENITIFIERBODY*
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

fragment IDENITIFIERHEAD
    : [a-zA-Z/]
    ;

fragment IDENITIFIERBODY
    : IDENITIFIERHEAD
    | [0-9_]
    ;

value_compare
    : ASSIGN
    |  GT
   |  LT
     | GE
     | LE
    |  NEQ
    ;

// 运算符
EQUALS    : '==';
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

OR       : 'OR';
AND      : 'AND';
NOT      : 'NOT';
NOR      : 'NOR';
YES      : 'yes';
NO       : 'no';

// 分隔符
LPAREN    : '(';
RPAREN    : ')';
LBRACE    : '{';
RBRACE    : '}';
SEMI      : ';';
COMMA     : ',';
COLON    : ':';

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