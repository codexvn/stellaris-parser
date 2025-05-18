package top.codexvn;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import top.codexvn.models.Technology;

@Slf4j
public class ParseTechnology {
    private static final Map<String, String> SCRIPTED_VARIABLES = new LinkedHashMap<>();
    private static final List<Technology> TECHNOLOGYS = new ArrayList<>();
    private static final ParseLocalisation.Localisation LOCALISATION = new ParseLocalisation.Localisation();

    public static void main(String[] args) throws Exception {
        //获取全局变量
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
        String s = outputMermaid();
        FileUtils.writeStringToFile(new File("preview.html"), s, "UTF-8");
        System.gc();
    }

    public static String outputMermaid() {
        StringJoiner joiner = new StringJoiner("\n");
        Map<String, String> name2NameWithLevel = new LinkedHashMap<>();
        for (Technology technology : TECHNOLOGYS) {
            String name = technology.getName();
            String level = String.valueOf(technology.getTier());
            String content = "%s_%s".formatted(name, level);
            name2NameWithLevel.put(technology.getKey(), content);
        }
        for (Technology technology : TECHNOLOGYS) {
            String name = technology.getName();
            String key = technology.getKey();
            List<Pair<String, List<String>>> prerequisitesNames = technology.getPrerequisites();
            for (Pair<String, List<String>> item : prerequisitesNames) {
                for (String prerequisitesKey : item.getRight()) {
                    String content = "  %s[%s] --> %s[%s]".formatted(prerequisitesKey, name2NameWithLevel.get(prerequisitesKey),key,name2NameWithLevel.get(key));
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
                      graph LR
                        <mermaid_content>
                    </div>
                  </div>
                
                  <script type="module">
                    import mermaid from "https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs";
                    mermaid.initialize({
                      startOnLoad: true,
                      maxEdges: 5000  // 支持大图
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
                
                """.replace("<mermaid_content>",joiner.toString());
    }

    public static TechnologyVisitor parseTechnology(String content) throws Exception {
        TechnologyLexer lexer = new TechnologyLexer(CharStreams.fromString(content));
        TechnologyParser parser = new TechnologyParser(new CommonTokenStream(lexer));
        List<? extends ANTLRErrorListener> errorListeners = parser.getErrorListeners();
        TechnologyVisitor visitor = new TechnologyVisitor();
        visitor.visit(parser.technology());
        return visitor;
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
        public Void visitCategory_val(TechnologyParser.Category_valContext ctx) {
            String key = getMaybeScriptedVariable(ctx.val().getText());
            thisDto.setCategory(LOCALISATION.getTitle().getOrDefault(key, key));
            return visitChildren(ctx);
        }

        @Override
        public Void visitIcon_val(TechnologyParser.Icon_valContext ctx) {
            log.warn("visitIcon: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitModifier_val(TechnologyParser.Modifier_valContext ctx) {
            log.warn("visitModifier: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitCost_val(TechnologyParser.Cost_valContext ctx) {
            thisDto.setCost(Integer.parseInt(getMaybeScriptedVariable(getMaybeScriptedVariable(ctx.getText()))));
            return visitChildren(ctx);
        }

        @Override
        public Void visitCost_by_script_val(TechnologyParser.Cost_by_script_valContext ctx) {
            log.warn("visitCost_by_script: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitCost_per_level_val(TechnologyParser.Cost_per_level_valContext ctx) {
            log.warn("visitCost_per_level: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitIs_rare_val(TechnologyParser.Is_rare_valContext ctx) {
            thisDto.set_rare(Boolean.parseBoolean(getMaybeScriptedVariable(ctx.getText())));
            return visitChildren(ctx);
        }

        @Override
        public Void visitIs_dangerous_val(TechnologyParser.Is_dangerous_valContext ctx) {
            log.info("visitIs_dangerous: " + getMaybeScriptedVariable(ctx.getText()));
            thisDto.set_dangerous(Boolean.parseBoolean(getMaybeScriptedVariable(ctx.getText())));
            return visitChildren(ctx);
        }

        @Override
        public Void visitWeight_val1(TechnologyParser.Weight_val1Context ctx) {
            log.info("visitWeight: " + getMaybeScriptedVariable(ctx.getText()));
            thisDto.setBase_weight(getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitWeight_val2(TechnologyParser.Weight_val2Context ctx) {
            log.info("visitWeight: " + getMaybeScriptedVariable(ctx.getText()));
            thisDto.setBase_weight(getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitLevels_val(TechnologyParser.Levels_valContext ctx) {
            log.info("visitLevels: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitPrerequisites_val(TechnologyParser.Prerequisites_valContext ctx) {
            List<Pair<String, List<String>>> preRequisites = new ArrayList<>();
            List<Pair<String, List<String>>> preRequisitesCn = new ArrayList<>();
            for (TechnologyParser.Condition_exprContext firstCondition : ctx.condition_statement().condition_expr()) {
                if (firstCondition.in_array() != null) {
                    //数组
                    Pair<String, List<String>> requisites = Pair.of("", firstCondition.in_array().id_().stream()
                            .map(id -> StringUtils.strip(id.getText(), "\"")).toList());
                    preRequisites.add(requisites);
                    Pair<String, List<String>> requisitesCn = Pair.of("", firstCondition.in_array().id_().stream()
                            .map(id -> StringUtils.strip(id.getText(), "\""))
                            .map(i ->
                                    LOCALISATION.getTitle().get(i)
                            ).toList());
                    preRequisitesCn.add(requisitesCn);
                } else if (firstCondition.op_condition_expr() != null) {
                    //走条件代码块
                    Pair<String, List<String>> requisites = Pair.of(firstCondition.op_condition_expr().LOGICAL_OPERATORS().getText(), new ArrayList<>());
                    Pair<String, List<String>> requisitesCn = Pair.of(firstCondition.op_condition_expr().LOGICAL_OPERATORS().getText(), new ArrayList<>());
                    for (TechnologyParser.Condition_exprContext secondCondition : firstCondition.op_condition_expr()
                            .condition_statement().condition_expr()) {
                        if (secondCondition.in_array() != null) {
                            //数组
                            requisites.getRight().addAll(secondCondition.in_array().id_().stream()
                                    .map(id -> StringUtils.strip(id.getText(), "\""))
                                    .toList());
                            requisitesCn.getRight().addAll((secondCondition.in_array().id_().stream()
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
            log.info("visitTechnology_swap: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitPotential_val(TechnologyParser.Potential_valContext ctx) {
            log.info("visitPotential: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitGateway_val(TechnologyParser.Gateway_valContext ctx) {
            log.info("visitGateway: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitRepeatable_val(TechnologyParser.Repeatable_valContext ctx) {
            log.info("visitRepeatable: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitWeight_groups_val(TechnologyParser.Weight_groups_valContext ctx) {
            log.info("visitWeight_groups: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitMod_weight_if_group_picked_val(TechnologyParser.Mod_weight_if_group_picked_valContext ctx) {
            log.info("visitMod_weight_if_group_picked: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitStart_tech_val(TechnologyParser.Start_tech_valContext ctx) {
            log.info("visitStart_tech: " + getMaybeScriptedVariable(ctx.getText()));
            thisDto.set_start_tech(Boolean.parseBoolean(getMaybeScriptedVariable(ctx.getText())));
            return visitChildren(ctx);
        }

        @Override
        public Void visitIs_reverse_engineerable_val(TechnologyParser.Is_reverse_engineerable_valContext ctx) {
            log.info("visitIs_reverse_engineerable: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitAi_update_type_val(TechnologyParser.Ai_update_type_valContext ctx) {
            log.info("visitAi_update_type: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitIs_insight_val(TechnologyParser.Is_insight_valContext ctx) {
            log.info("visitIs_insight: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitFeature_flags_val(TechnologyParser.Feature_flags_valContext ctx) {
            log.info("visitFeature_flags: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitPrereqfor_desc_val(TechnologyParser.Prereqfor_desc_valContext ctx) {
            log.info("visitPrereqfor_desc: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitWeight_modifier_val(TechnologyParser.Weight_modifier_valContext ctx) {
            log.info("visitWeight_modifier: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitAi_weight_val(TechnologyParser.Ai_weight_valContext ctx) {
            log.info("visitAi_weight: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }

        @Override
        public Void visitStarting_potential_val(TechnologyParser.Starting_potential_valContext ctx) {
            log.info("visitStarting_potential: " + getMaybeScriptedVariable(ctx.getText()));
            return visitChildren(ctx);
        }
    }
}
