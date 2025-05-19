package top.codexvn;

import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import common.technology.TechnologyBaseVisitor;
import common.technology.TechnologyLexer;
import common.technology.TechnologyParser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.tuple.Pair;
import top.codexvn.models.Technology;

@Slf4j
public class ParseTechnology {
    private static final Map<String, String> SCRIPTED_VARIABLES = new LinkedHashMap<>();
    private static final List<Technology> TECHNOLOGYS = new ArrayList<>();
    private static final ParseLocalisation.Localisation LOCALISATION = new ParseLocalisation.Localisation();
    private static final Color dangerous = Color.decode("#741e1b");
    private static final Color rare = Color.decode("#542d6b");
    private static final Color physics = Color.decode("#316c8f");
    private static final Color society = Color.decode("#276445");
    private static final Color engineering = Color.decode("#6c411b");
    private static final  Map<String, Color> colorMap = new LinkedHashMap<>() {
        {
            put("dangerous", dangerous);
            put("rare", rare);
            put("archaeostudies", rare);

            put("field_manipulation", physics);
            put("computing", physics);
            put("particles", physics);
            put("physics", physics);

            put("biology", society);
            put("statecraft", society);
            put("new_worlds", society);
            put("psionics", society);
            put("military_theory", society);
            put("society", society);

            put("industry", engineering);
            put("voidcraft", engineering);
            put("propulsion", engineering);
            put("materials", engineering);
            put("engineering", engineering);
        }
    };

    public static void main(String[] args) throws Exception {
        //获取全局变量
        byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        SCRIPTED_VARIABLES.putAll(ParseScriptedVariables.run());
        ParseLocalisation.Localisation localisation = ParseLocalisation.run();
        LOCALISATION.getTitle().putAll(localisation.getTitle());
        LOCALISATION.getDesc().putAll(localisation.getDesc());
        String technologyFiles = ".game_files/common/technology";
        //列出所有文件
        Collection<File> files = FileUtils.listFiles(new File(technologyFiles), new String[]{"txt"}, false);
        for (File file : files) {
            //读取文件内容
            String content = FileUtils.readFileToString(file, "UTF-8");
            if (StringUtils.isNotBlank(content)) {
                var a = parseTechnology(content);
                TECHNOLOGYS.addAll(a.getAllTechnologies());
            }
        }
        String treeDesc = buildTree();
        String s = outputMermaid();
        Pair<String, String> i18Replace = i18Replace();
        FileUtils.writeStringToFile(new File("gen/technology_tree_4.x.html"), s, "UTF-8");
        FileUtils.writeStringToFile(new File("gen/technology_tree_4.x.html"), s, "UTF-8");
//        FileUtils.writeByteArrayToFile(new File("gen/localisation/simp_chinese/techtree_title_l_simp_chinese.yml"),
//                ArrayUtils.addAll(UTF8_BOM, i18Replace.getKey().getBytes(StandardCharsets.UTF_8)));
        FileUtils.writeByteArrayToFile(new File("gen/localisation/simp_chinese/replace/techtree_title_l_simp_chinese.yml"),
                ArrayUtils.addAll(UTF8_BOM, i18Replace.getKey().getBytes(StandardCharsets.UTF_8)));
//        FileUtils.writeByteArrayToFile(new File("gen/localisation/simp_chinese/techtree_desc_l_simp_chinese.yml"),
//                ArrayUtils.addAll(UTF8_BOM,i18Replace.getValue().getBytes(StandardCharsets.UTF_8)));
        FileUtils.writeByteArrayToFile(new File("gen/localisation/simp_chinese/replace/techtree_desc_l_simp_chinese.yml"),
                ArrayUtils.addAll(UTF8_BOM,i18Replace.getValue().getBytes(StandardCharsets.UTF_8)));
//        FileUtils.writeByteArrayToFile(new File("gen/localisation/simp_chinese/techtree_l_simp_chinese.yml"),
//                ArrayUtils.addAll(UTF8_BOM,treeDesc.getBytes(StandardCharsets.UTF_8)));
        FileUtils.writeByteArrayToFile(new File("gen/localisation/simp_chinese/replace/techtree_l_simp_chinese.yml"),
                ArrayUtils.addAll(UTF8_BOM,treeDesc.getBytes(StandardCharsets.UTF_8)));
        System.gc();
    }
    public static String buildTree() {
        Map<String, Technology> key2Technology = TECHNOLOGYS.stream()
                .collect(Collectors.toMap(Technology::getKey, i -> i));
        for (Technology technology : TECHNOLOGYS) {
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
        for (Technology  technology: TECHNOLOGYS) {
            StringJoiner treeDescSJ = new StringJoiner("\n");
            dfs(technology,1,treeDescSJ);
            String suffix = "_techtree";
            String key = technology.getKey()+suffix;
            treeMap.put(key,treeDescSJ);
        }
        allTreeDescSJ.add("l_simp_chinese:");
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


    public static Pair<String,String> i18Replace(){
        StringJoiner titleSJ = new StringJoiner("\n");
        titleSJ.add("l_simp_chinese:");
        String techtreeSuffix = "_techtree";
        String descSuffix = "_desc";
        StringJoiner descSJ = new StringJoiner("\n");
        descSJ.add("l_simp_chinese:");
        for (Technology technology : TECHNOLOGYS) {
            String level = "(级别%s)".formatted(technology.getTier());
            String key = technology.getKey();
            titleSJ.add(" %s: \"%s\"".formatted(key, technology.getName()+level));
            descSJ.add(" %s: \"%s\"".formatted(key + descSuffix, technology.getDescription()+"\\n科技树:\\n$%s$\\n".formatted(key+techtreeSuffix)));
        }
        return Pair.of(titleSJ.toString(),descSJ.toString());
    }
    public static String outputMermaid() {
        StringJoiner joiner = new StringJoiner("\n");
        Map<String, String> name2NameWithLevel = new LinkedHashMap<>();
        for (Technology technology : TECHNOLOGYS) {
            String key = technology.getKey();
            String name = technology.getName();
            String level = String.valueOf(technology.getTier());
            String content = "%s_%s".formatted(name, level);
            name2NameWithLevel.put(technology.getKey(), content);
            StrBuilder strBuilder = new StrBuilder();
            strBuilder.append("class %s[\"%s\"] {\n", key, name);
            strBuilder.append("  类别: %s\n", technology.getCategoryName());
            strBuilder.append("  等级: %s\n", technology.getTier());
            strBuilder.append("  是否为稀有科技: %s\n", technology.is_rare() ? "是" : "否");
            strBuilder.append("  是否为危险科技: %s\n", technology.is_dangerous() ? "是" : "否");
            strBuilder.append("  是否为起始科技: %s\n", technology.is_start_tech() ? "是" : "否");
            for (int i = 0; i < technology.getPrerequisites().size(); i++) {
                Pair<String, List<String>> prerequisitesCn = technology.getPrerequisites_names().get(i);
                strBuilder.append("  必要条件%s: %s(%s)\n", i + 1,prerequisitesCn.getLeft() ,String.join(",", prerequisitesCn.getRight()));
            }
            strBuilder.append("}\n");
            if (technology.is_dangerous()) {
                strBuilder.append("  style %s stroke:%s,stroke-width:4px\n".formatted(key, toColorHex(dangerous)));
            } else if (technology.is_rare()) {
                strBuilder.append("  style %s stroke:%s,stroke-width:4px\n".formatted(key, toColorHex(rare)));
            } else {
                String category = technology.getCategory();
                strBuilder.append("  style %s stroke:%s,stroke-width:4px\n".formatted(key, toColorHex(Objects.requireNonNull(colorMap.get(category)))));
            }
            joiner.add(strBuilder.toString());
        }
        for (Technology technology : TECHNOLOGYS) {
            String key = technology.getKey();
            List<Pair<String, List<String>>> prerequisitesNames = technology.getPrerequisites();
            for (Pair<String, List<String>> item : prerequisitesNames) {
                for (String prerequisitesKey : item.getRight()) {
                    String content = "  %s ..> %s".formatted(prerequisitesKey, key);
                    joiner.add(content);
                }
            }
        }
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <title>Mermaid Graph - Left to Right with Zoom</title>
                  <style>
                    body {
                      margin: 0;
                      overflow: hidden;
                    }
                    #container {
                      width: 100vw;
                      height: 100vh;
                      overflow: hidden;
                      background: #f9f9f9;
                      position: relative;
                    }
                    #zoom-area {
                      width: 100%;
                      height: 100%;
                      transform-origin: 0 0;
                      cursor: grab;
                    }
                    .mermaid {
                      font-family: 'Segoe UI', sans-serif;
                    }
                  </style>
                </head>
                <body>
                  <div id="container">
                    <div id="zoom-area" class="mermaid">
                      classDiagram
                        direction LR
                        <mermaid_content>
                    </div>
                  </div>
                
                  <script type="module">
                    import mermaid from "https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs";
                    mermaid.initialize({
                      startOnLoad: true,
                      maxEdges: 5000,  // 支持大图
                      maxTextSize: 1000000
                    });
                  </script>
                
                  <script>
                    // 缩放和平移功能
                    const zoomArea = document.getElementById("zoom-area");
                    let scale = 1;
                    let originX = 0, originY = 0;
                    let isDragging = false;
                    let startX, startY;
                
                    document.addEventListener("wheel", function (e) {
                      e.preventDefault();
                      const zoomFactor = 0.1;
                      if (e.deltaY < 0) {
                        scale *= (1 + zoomFactor);
                      } else {
                        scale *= (1 - zoomFactor);
                      }
                      zoomArea.style.transform = `translate(${originX}px, ${originY}px) scale(${scale})`;
                    }, { passive: false });
                
                    zoomArea.addEventListener("mousedown", (e) => {
                      isDragging = true;
                      startX = e.clientX;
                      startY = e.clientY;
                      zoomArea.style.cursor = "grabbing";
                    });
                
                    document.addEventListener("mouseup", () => {
                      isDragging = false;
                      zoomArea.style.cursor = "grab";
                    });
                
                    document.addEventListener("mousemove", (e) => {
                      if (!isDragging) return;
                      originX += e.clientX - startX;
                      originY += e.clientY - startY;
                      startX = e.clientX;
                      startY = e.clientY;
                      zoomArea.style.transform = `translate(${originX}px, ${originY}px) scale(${scale})`;
                    });
                  </script>
                </body>
                </html>
                
                """.replace("<mermaid_content>", joiner.toString());
    }

    public static TechnologyVisitor parseTechnology(String content) throws Exception {
        TechnologyLexer lexer = new TechnologyLexer(CharStreams.fromString(content));
        TechnologyParser parser = new TechnologyParser(new CommonTokenStream(lexer));
        List<? extends ANTLRErrorListener> errorListeners = parser.getErrorListeners();
        TechnologyVisitor visitor = new TechnologyVisitor();
        visitor.visit(parser.root());
        return visitor;
    }

    public static String toColorHex(Color color) {
        return String.format("#%02x%02x%02x",
                color.getRed(),
                color.getGreen(),
                color.getBlue());
    }

    public static String getMaybeScriptedVariable(String key) {
        return SCRIPTED_VARIABLES.getOrDefault(key, key);
    }

    @Slf4j
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TechnologyVisitor extends TechnologyBaseVisitor<Void> {
        private Technology thisDto = new Technology();
        private List<Technology> allTechnologies = new ArrayList<>();


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
            thisDto.setName(LOCALISATION.getTitle().getOrDefault(thisDto.getKey(), thisDto.getKey()));
            thisDto.setDescription(LOCALISATION.getDesc().getOrDefault(thisDto.getKey() + "_desc", thisDto.getKey() + "_desc"));
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
            thisDto.setCategoryName(LOCALISATION.getTitle().getOrDefault(key, key));
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
                                    LOCALISATION.getTitle().get(i)
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
                                            LOCALISATION.getTitle().get(i)
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
