package top.codexvn.mod;

import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import common.technology.TechnologyBaseVisitor;
import common.technology.TechnologyLexer;
import common.technology.TechnologyParser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.tuple.Pair;
import top.codexvn.ParseLocalisation;
import top.codexvn.ParseScriptedVariables;
import top.codexvn.models.LocalisationEnum;
import top.codexvn.models.Technology;

public class TechnologyTree {
    private static final Map<String, String> SCRIPTED_VARIABLES = new LinkedHashMap<>();
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    public static void main(String[] args) throws Exception {
        //获取全局变量
        SCRIPTED_VARIABLES.putAll(ParseScriptedVariables.run());

        String technologyFiles = ".game_files/common/technology";
        //列出所有文件
        Collection<File> files = FileUtils.listFiles(new File(technologyFiles), new String[]{"txt"}, false);
        for (Map.Entry<LocalisationEnum, ParseLocalisation.Localisation> entry : ParseLocalisation.run().entrySet()) {
            LocalisationEnum localisationEnum = entry.getKey();
            ParseLocalisation.Localisation localisation = entry.getValue();
            List<Technology> technologies = new ArrayList<>();
            for (File file : files) {
                //读取文件内容
                String content = FileUtils.readFileToString(file, "UTF-8");
                if (StringUtils.isNotBlank(content)) {
                    var a = parseTechnology(content,localisationEnum,localisation);
                    technologies.addAll(a.getAllTechnologies());
                }
            }
            //输出mod科技树
            Pair<String, String> i18Replace = i18Replace(technologies,localisationEnum);
            FileUtils.writeByteArrayToFile(new File("gen/localisation/%s/replace/techtree_title_%s.yml".formatted(localisationEnum.getDirName(),localisationEnum.getValue())),
                    ArrayUtils.addAll(UTF8_BOM, i18Replace.getKey().getBytes(StandardCharsets.UTF_8)));
            FileUtils.writeByteArrayToFile(new File("gen/localisation/%s/replace/techtree_desc_%s.yml".formatted(localisationEnum.getDirName(),localisationEnum.getValue())),
                    ArrayUtils.addAll(UTF8_BOM,i18Replace.getValue().getBytes(StandardCharsets.UTF_8)));
            String treeDesc = buildTree(technologies,localisationEnum);
            FileUtils.writeByteArrayToFile(new File("gen/localisation/%s/replace/techtree_%s.yml".formatted(localisationEnum.getDirName(),localisationEnum.getValue())),
                    ArrayUtils.addAll(UTF8_BOM,treeDesc.getBytes(StandardCharsets.UTF_8)));
        }



        System.gc();
    }

    public static String buildTree(List<Technology> technologies,LocalisationEnum localisationEnum) {
        Map<String, Technology> key2Technology = technologies.stream()
                .collect(Collectors.toMap(Technology::getKey, i -> i));
        for (Technology technology : technologies) {
            List<Pair<String, List<String>>> prerequisites = technology.getPrerequisites();
            for (Pair<String, List<String>> prerequisite : prerequisites) {
                for (String key : prerequisite.getRight()) {
                    Technology parent = key2Technology.get(key);
                    if (parent != null) {
                        parent.getChildren().add(technology);
                    }
                }
            }
        }
        StringJoiner allTreeDescSJ = new StringJoiner("\n");
        Map<String,StringJoiner> treeMap = new LinkedHashMap<>();

        //递归树,然后输出文本树
        for (Technology  technology: technologies) {
            StringJoiner treeDescSJ = new StringJoiner("\n");
            dfs(technology,1,treeDescSJ);
            String suffix = "_techtree";
            String key = technology.getKey()+suffix;
            treeMap.put(key,treeDescSJ);
        }
        allTreeDescSJ.add("%s:".formatted(localisationEnum.getName()));
        for (Map.Entry<String, StringJoiner> entry : treeMap.entrySet()) {
            String key = entry.getKey();
            StringJoiner treeDescSJ = entry.getValue();
            allTreeDescSJ.add(" %s: \"%s\"".formatted(key, treeDescSJ.toString().replace("\n","\\n")));
        }
        return allTreeDescSJ.toString();
    }
    public static void dfs(Technology technology, int level,StringJoiner joiner) {
        String prefix = "   |".repeat(level);
        String levelStr = "(级别%s)".formatted(technology.getTier());
        joiner.add("%s--%s".formatted(prefix, technology.getName()+levelStr));
        for (Technology child : technology.getChildren()) {
            dfs(child, level + 1, joiner);
        }
    }


    public static Pair<String,String> i18Replace(List<Technology> technologies,LocalisationEnum localisationEnum){
        StringJoiner titleSJ = new StringJoiner("\n");
        titleSJ.add("%s:".formatted(localisationEnum.getName()));
        String techtreeSuffix = "_techtree";
        String descSuffix = "_desc";
        StringJoiner descSJ = new StringJoiner("\n");
        descSJ.add("%s:".formatted(localisationEnum.getName()));
        for (Technology technology : technologies) {
            String level = "(级别%s)".formatted(technology.getTier());
            String key = technology.getKey();
            titleSJ.add(" %s: \"%s\"".formatted(key, technology.getName()+level));
            descSJ.add(" %s: \"%s\"".formatted(key + descSuffix, technology.getDescription()+"\\n科技树:\\n$%s$\\n".formatted(key+techtreeSuffix)));
        }
        return Pair.of(titleSJ.toString(),descSJ.toString());
    }

    public static TechnologyVisitor parseTechnology(String content,LocalisationEnum localisationEnum,ParseLocalisation.Localisation localisation) throws Exception {
        TechnologyLexer lexer = new TechnologyLexer(CharStreams.fromString(content));
        TechnologyParser parser = new TechnologyParser(new CommonTokenStream(lexer));
        List<? extends ANTLRErrorListener> errorListeners = parser.getErrorListeners();
        TechnologyVisitor visitor = new TechnologyVisitor(localisationEnum,localisation);
        visitor.visit(parser.root());
        return visitor;
    }


    public static String getMaybeScriptedVariable(String key) {
        return SCRIPTED_VARIABLES.getOrDefault(key, key);
    }

    @Slf4j
    @Data
    @EqualsAndHashCode(callSuper = true)
    @RequiredArgsConstructor
    public static class TechnologyVisitor extends TechnologyBaseVisitor<Void> {
        private Technology thisDto = new Technology();
        private List<Technology> allTechnologies = new ArrayList<>();
        private final LocalisationEnum localisationEnum;
        private final ParseLocalisation.Localisation localisation;


        @Override
        public Void visitTechnology_body_start(TechnologyParser.Technology_body_startContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitTechnology_body_end(TechnologyParser.Technology_body_endContext ctx) {
            allTechnologies.add(thisDto);
            thisDto = new Technology();
            return visitChildren(ctx);
        }

        @Override
        public Void visitTechnology_name(TechnologyParser.Technology_nameContext ctx) {
            thisDto.setKey(getMaybeScriptedVariable(ctx.getText()));
            thisDto.setName(localisation.getTitle().getOrDefault(thisDto.getKey(), thisDto.getKey()));
            thisDto.setDescription(localisation.getDesc().getOrDefault(thisDto.getKey() + "_desc", thisDto.getKey() + "_desc"));
            return visitChildren(ctx);
        }

        @Override
        public Void visitTier_val(TechnologyParser.Tier_valContext ctx) {
            String val = getMaybeScriptedVariable(ctx.getText());
            thisDto.setTier(Integer.parseInt(val));
            return visitChildren(ctx);
        }

        @Override
        public Void visitArea_val(TechnologyParser.Area_valContext ctx) {
            String key = getMaybeScriptedVariable(ctx.technology_area_enum().getText());
            thisDto.setArea(key);
            return super.visitArea_val(ctx);
        }

        @Override
        public Void visitCategory_val(TechnologyParser.Category_valContext ctx) {
            String key = getMaybeScriptedVariable(ctx.val().getText());
            thisDto.setCategoryName(localisation.getTitle().getOrDefault(key, key));
            thisDto.setCategory(key);
            return visitChildren(ctx);
        }

        @Override
        public Void visitIcon_val(TechnologyParser.Icon_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitModifier_val(TechnologyParser.Modifier_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitCost_val(TechnologyParser.Cost_valContext ctx) {
            thisDto.setCost(Integer.parseInt(getMaybeScriptedVariable(getMaybeScriptedVariable(ctx.getText()))));
            return visitChildren(ctx);
        }

        @Override
        public Void visitCost_by_script_val(TechnologyParser.Cost_by_script_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitCost_per_level_val(TechnologyParser.Cost_per_level_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitIs_rare_val(TechnologyParser.Is_rare_valContext ctx) {
            thisDto.set_rare(getMaybeScriptedVariable(ctx.getText()).equals("yes"));
            return visitChildren(ctx);
        }

        @Override
        public Void visitIs_dangerous_val(TechnologyParser.Is_dangerous_valContext ctx) {
            thisDto.set_dangerous(getMaybeScriptedVariable(ctx.getText()).equals("yes"));
            return visitChildren(ctx);
        }

        @Override
        public Void visitWeight_val1(TechnologyParser.Weight_val1Context ctx) {
            thisDto.setBase_weight(getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitWeight_val2(TechnologyParser.Weight_val2Context ctx) {
            thisDto.setBase_weight(getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }



        @Override
        public Void visitLevels_val(TechnologyParser.Levels_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitPrerequisites_val(TechnologyParser.Prerequisites_valContext ctx) {
            List<Pair<String, List<String>>> preRequisites = new ArrayList<>();
            List<Pair<String, List<String>>> preRequisitesCn = new ArrayList<>();
            for (TechnologyParser.Condition_exprContext firstCondition : ctx.condition_statement().condition_expr()) {
                if (firstCondition.in_condition_expr() != null) {
                    //数组
                    Pair<String, List<String>> requisites = Pair.of("", firstCondition.in_condition_expr().id_().stream()
                            .map(id -> StringUtils.strip(id.getText(), "\"")).toList());
                    preRequisites.add(requisites);
                    Pair<String, List<String>> requisitesCn = Pair.of("", firstCondition.in_condition_expr().id_().stream()
                            .map(id -> StringUtils.strip(id.getText(), "\""))
                            .map(i ->
                                    localisation.getTitle().get(i)
                            ).toList());
                    preRequisitesCn.add(requisitesCn);
                } else if (firstCondition.op_condition_expr() != null) {
                    //走条件代码块
                    String opCn = switch (firstCondition.op_condition_expr().LOGICAL_OPERATORS().getText()){
                        case "AND" -> "同时满足所有条件";
                        case "OR" -> "满足任意一个条件";
                        case "NOT" -> "不满足所有条件";
                        case "NOR" -> "不满足以下任何一个条件";
                        default -> throw new IllegalStateException("Unexpected value: " + firstCondition.op_condition_expr().LOGICAL_OPERATORS().getText());
                    };
                    Pair<String, List<String>> requisites = Pair.of(opCn, new ArrayList<>());
                    Pair<String, List<String>> requisitesCn = Pair.of(opCn, new ArrayList<>());
                    for (TechnologyParser.Condition_exprContext secondCondition : firstCondition.op_condition_expr()
                            .condition_statement().condition_expr()) {
                        if (secondCondition.in_condition_expr() != null) {
                            //数组
                            requisites.getRight().addAll(secondCondition.in_condition_expr().id_().stream()
                                    .map(id -> StringUtils.strip(id.getText(), "\""))
                                    .toList());
                            requisitesCn.getRight().addAll((secondCondition.in_condition_expr().id_().stream()
                                    .map(id -> StringUtils.strip(id.getText(), "\""))
                                    .map(i ->
                                            localisation.getTitle().get(i)
                                    ).toList()));
                        } else if (secondCondition.compare_condition_expr() != null) {
                            throw new UnsupportedOperationException();
//                            String conditionKey = secondCondition.compare_condition_expr().condition_key().getText();
//                            String val = secondCondition.compare_condition_expr().val().getText();
//                            String op = secondCondition.compare_condition_expr().value_compare().getText();
//                            if (op.equals("NOR")) {
//
//                            }
//                            //格式化数据
//                            String conditionValue = getMaybeScriptedVariable(val);
//                            requisites.add("%s %s %s".formatted(conditionKey, op, conditionValue));
//                            requisitesCn.add("%s %s %s".formatted(conditionKey, op, LOCALISATION.getTitle().getOrDefault(conditionValue, conditionValue)));
                        }
                    }
                    preRequisites.add(requisites);
                    preRequisitesCn.add(requisitesCn);
                }
            }
            thisDto.setPrerequisites(preRequisites);
            thisDto.setPrerequisites_names(preRequisitesCn);
            return visitChildren(ctx);
        }

        @Override
        public Void visitTechnology_swap_val(TechnologyParser.Technology_swap_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitPotential_val(TechnologyParser.Potential_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitGateway_val(TechnologyParser.Gateway_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitRepeatable_val(TechnologyParser.Repeatable_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitWeight_groups_val(TechnologyParser.Weight_groups_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitMod_weight_if_group_picked_val(TechnologyParser.Mod_weight_if_group_picked_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitStart_tech_val(TechnologyParser.Start_tech_valContext ctx) {
            thisDto.set_start_tech(getMaybeScriptedVariable(ctx.getText()).equals("yes"));
            return visitChildren(ctx);
        }

        @Override
        public Void visitIs_reverse_engineerable_val(TechnologyParser.Is_reverse_engineerable_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitAi_update_type_val(TechnologyParser.Ai_update_type_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitIs_insight_val(TechnologyParser.Is_insight_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitFeature_flags_val(TechnologyParser.Feature_flags_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitPrereqfor_desc_val(TechnologyParser.Prereqfor_desc_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitWeight_modifier_val(TechnologyParser.Weight_modifier_valContext ctx) {
            if (ctx.modifier() != null) {
                for (TechnologyParser.ModifierContext modifierContext: ctx.modifier()) {
                    var modifier = modifierContext.modifier_val();
                    List<TechnologyParser.AddContext> adds = modifier.add();
                    List<TechnologyParser.FactorContext> factors = modifier.factor();
                    for (TechnologyParser.Compare_condition_exprContext compareConditionExprContext : modifier.compare_condition_expr()) {

                    }
                }
            }
            return visitChildren(ctx);
        }

        @Override
        public Void visitAi_weight_val(TechnologyParser.Ai_weight_valContext ctx) {
            return visitChildren(ctx);
        }

        @Override
        public Void visitStarting_potential_val(TechnologyParser.Starting_potential_valContext ctx) {
            return visitChildren(ctx);
        }
    }
}
