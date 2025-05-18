grammar Technology;

technology
    : (technology_item | variable_item)* EOF
    ;
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
    | icon
    | modifier
    | cost
    | cost_by_script
    | cost_per_level
    | is_rare
    | is_dangerous
    | weight
    | levels
    | prerequisites
    | technology_swap
    | potential
    | gateway
    | repeatable
    | weight_groups
    | mod_weight_if_group_picked
    | start_tech
    | is_reverse_engineerable
    | ai_update_type
    | is_insight
    | feature_flags
    | prereqfor_desc
    | weight_modifier
    | ai_weight
    | starting_potential)+ technology_body_end
    ;

area: 'area' ASSIGN area_val;
area_val: val;
tier: 'tier' ASSIGN tier_val;
tier_val: val;
category: 'category' ASSIGN  category_val ;
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
is_rare_val: val;
is_dangerous: 'is_dangerous' ASSIGN is_dangerous_val;
is_dangerous_val: val;
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
weight_groups_val: val;
mod_weight_if_group_picked: 'mod_weight_if_group_picked' ASSIGN mod_weight_if_group_picked_val;
mod_weight_if_group_picked_val: condition_statement;
start_tech: 'start_tech' ASSIGN start_tech_val;
start_tech_val: val;
is_reverse_engineerable: 'is_reverse_engineerable' ASSIGN is_reverse_engineerable_val;
is_reverse_engineerable_val: val;
ai_update_type: 'ai_update_type' ASSIGN ai_update_type_val;
ai_update_type_val: val;
is_insight: 'is_insight' ASSIGN is_insight_val;
is_insight_val: val;
feature_flags: 'feature_flags' ASSIGN feature_flags_val;
feature_flags_val: val;
starting_potential: 'starting_potential' ASSIGN starting_potential_val;
starting_potential_val: condition_statement;
ai_weight: 'ai_weight' ASSIGN ai_weight_val ;
ai_weight_val: LBRACE
                   (weight
                    |factor
                    | condition_expr
                    | modifier
                    | inline_script)*
                   RBRACE;





prerequisites
    : 'prerequisites' ASSIGN prerequisites_val
    ;
prerequisites_val
    : condition_statement
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
technology_swap
    : 'technology_swap' ASSIGN technology_swap_val
        ;
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

factor: 'factor' ASSIGN factor_val;
factor_val: val;
inline_script
    : 'inline_script' ASSIGN (inline_script_val_1|inline_script_val_2);
inline_script_val_1: LBRACE keyval + RBRACE;
inline_script_val_2: key;

modifier
    : 'modifier' ASSIGN modifier_val
    ;
modifier_val
    : LBRACE
        (factor
        | condition_expr
        | inline_script
        )* RBRACE
    ;
weight_modifier
    : 'weight_modifier' ASSIGN weight_modifier_val
    ;
weight_modifier_val
    : LBRACE
        (factor
        | condition_expr
        | modifier
        | inline_script
        )* RBRACE
    ;
hide_prereq_for_desc: 'hide_prereq_for_desc' ASSIGN hide_prereq_for_desc_val;
hide_prereq_for_desc_val : val;
i18_val: LBRACE (i18_title| i18_desc)+ RBRACE;
i18_title : 'title' ASSIGN i18_title_val;
i18_title_val : val;
i18_desc : 'desc' ASSIGN i18_desc_val;
i18_desc_val : val;

custom: 'custom' ASSIGN i18_val;
prereq_for_category: 'prereq_for_category' ASSIGN i18_val;
component: 'component' ASSIGN i18_val;
ship: 'ship' ASSIGN i18_val;
diplo_action: 'diplo_action' ASSIGN i18_val;
prereqfor_desc : 'prereqfor_desc' ASSIGN prereqfor_desc_val;
prereqfor_desc_val
    : LBRACE
        (hide_prereq_for_desc
        | custom
        | ship
        | prereq_for_category
        | diplo_action
        | component)*
    RBRACE
    ;
variable_item
    : key_ref ASSIGN val
    ;

// 条件判断表达式
condition_expr
    : compare_condition_expr
    | statement_condition_expr
    | op_condition_expr
    | in_array
    | has_trait_in_council
    | has_modifier
    | num_buildings
    | has_ancrel
    | has_seen_any_bypass
    ;
//数据比较条件表达式
compare_condition_expr: condition_key value_compare val;
//条件块条件表达式
statement_condition_expr: condition_key ASSIGN condition_statement;
// 连续条件表达式,如 AND = {xxx}
op_condition_expr: LOGICAL_OPERATORS ASSIGN condition_statement;
//是否在数组中
in_array:  id_+;

has_modifier: 'has_modifier' ASSIGN has_modifier_val;
has_modifier_val: val;
has_trait_in_council: 'has_trait_in_council' ASSIGN has_trait_in_council_val;
has_trait_in_council_val: LBRACE keyval + RBRACE;
num_buildings: 'num_buildings' ASSIGN num_buildings_val;
num_buildings_val: condition_statement;
has_ancrel: 'has_ancrel' ASSIGN has_ancrel_val;
has_ancrel_val: val;
has_seen_any_bypass: 'has_seen_any_bypass' ASSIGN has_seen_any_bypass_val;
has_seen_any_bypass_val: val;

// 条件判断块
// A=B
// OR { A=C }
condition_statement
    : LBRACE condition_expr* RBRACE
    ;

keyval
    : key ASSIGN val
    ;
key
    : id_
    | attrib
    | tag
    ;
//用于判断的token,如以has或者is开头的
condition_key
    :'has_' IDENTIFIER
    | 'is_' IDENTIFIER
    | key
    ;
val
    :
    normal_val | array_val | keywork
    ;
normal_val
    :
    STRING
    | INTEGER
    | BOOLEAN
    | FLOAT
    | id_
    | attrib
    | key_ref
    | tag
    ;
array_val
    : LBRACE key* RBRACE
    ;

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