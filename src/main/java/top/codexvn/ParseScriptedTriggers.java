package top.codexvn;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import common.scripted_triggers.ScriptedTriggersBaseVisitor;
import common.scripted_triggers.ScriptedTriggersLexer;
import common.scripted_triggers.ScriptedTriggersParser;
import common.scripted_triggers.ScriptedTriggersVisitor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import top.codexvn.models.LocalisationEnum;

@Slf4j
public class ParseScriptedTriggers {
    @Data
    public static class CsvDate {
        private String name;
        private String desc;
    }
    public static void main(String[] args) throws Exception {
        Map<String, String> run = run();
        log.info(run.toString());
    }
    public static Map<String,String> run() throws Exception {
        Map<String,String> result = new LinkedHashMap<>();
        String filePath = ".game_files/common/scripted_triggers";
        //列出所有文件
        Collection<File> files = FileUtils.listFiles(new File(filePath), new String[]{"txt"}, false);
        ParseLocalisation.Localisation localisation = ParseLocalisation.run().get(LocalisationEnum.SIMP_CHINESE);
        Map<String, String> variables = ParseScriptedVariables.run();
        Map<String, String> customI18 = new LinkedHashMap<>(){
            {
                put("value","值");
                put("exists","存在");
            }
        };
        EasyExcel.read(ClassLoader.getSystemResourceAsStream("trigger_name2en.csv"))
                .excelType(ExcelTypeEnum.CSV)
                .head(CsvDate.class)
                .autoCloseStream(true)
                .doReadAllSync()
                .stream().map(CsvDate.class::cast).forEach(i->customI18.putIfAbsent(i.getName(),i.getDesc()));
        for (File file : files) {
            //读取文件内容
            String content = FileUtils.readFileToString(file, "UTF-8");
            Map<String, String> key2valMap = doParse(content,localisation,variables,customI18);
            result.putAll(key2valMap);
        }
        return result;
    }
    public static Map<String,String> doParse(String content,
                                             ParseLocalisation.Localisation localisation,
                                             Map<String, String> variables,
                                             Map<String, String> customI18) throws Exception {
        Map<String,String> result = new LinkedHashMap<>();
        ScriptedTriggersLexer lexer = new ScriptedTriggersLexer(CharStreams.fromString(content));
        ScriptedTriggersParser parser = new ScriptedTriggersParser(new CommonTokenStream(lexer));
        ScriptedTriggersVisitor<Void> visitor = new ScriptedTriggersBaseVisitor<Void>(){
            private StringBuilder sb = new StringBuilder();
            private int level = 0;
            private final String levelPrefix = "  ";
            public String getI18(String text){
                return StringUtils.firstNonEmpty(
                        localisation.getTitle().get(text),
                        variables.get(text),
                        localisation.getTitle().get(text.toLowerCase()),
                        localisation.getTitle().get(text.toUpperCase()),
                        localisation.getTitle().get("HAS_NUM_"+text.toUpperCase()),
                        localisation.getTitle().get("TRIGGER_"+text.toLowerCase()),
                        customI18.get(text),
                        text);
            }
            @Override
            public Void visitTrigger_name(ScriptedTriggersParser.Trigger_nameContext ctx) {
                sb.append(getI18(ctx.getText())).append("：\n");
                return super.visitTrigger_name(ctx);
            }
            @Override
            public Void visitTrigger_body_end(ScriptedTriggersParser.Trigger_body_endContext ctx) {
                sb = new StringBuilder();
                return super.visitTrigger_body_end(ctx);
            }

            @Override
            public Void visitBlock_start(ScriptedTriggersParser.Block_startContext ctx) {
                sb.append(" ").append(getI18(ctx.getText())).append("\n");
                level++;
                return super.visitBlock_start(ctx);
            }

            @Override
            public Void visitBlock_end(ScriptedTriggersParser.Block_endContext ctx) {
                level--;
                sb.append(levelPrefix.repeat(level)).append(getI18(ctx.getText())).append("\n");
                return super.visitBlock_end(ctx);
            }

            @Override
            public Void visitValue_compare_expr_key(ScriptedTriggersParser.Value_compare_expr_keyContext ctx) {
                sb.append(levelPrefix.repeat(level)).append(getI18(ctx.getText()));
                return super.visitValue_compare_expr_key(ctx);
            }
            //    : ASSIGN
            //    | GT
            //    | LT
            //    | GE
            //    | LE
            //    | NEQ
            @Override
            public Void visitRelational_operators(ScriptedTriggersParser.Relational_operatorsContext ctx) {
                String op = getI18(ctx.getText());
                sb.append(" ");
                switch (op){
                    case "=" -> {
                        sb.append("等于");
                    }
                    case ">" -> {
                        sb.append("大于");
                    }
                    case "<" -> {
                        sb.append("小于");
                    }
                    case ">=" -> {
                        sb.append("大于等于");
                    }
                    case "<=" -> {
                        sb.append("小于等于");
                    }
                    case "!=" -> {
                        sb.append("不等于");
                    }
                }
                sb.append(" ");
                return super.visitRelational_operators(ctx);
            }

            @Override
            public Void visitValue_compare_expr_val(ScriptedTriggersParser.Value_compare_expr_valContext ctx) {
                sb.append("%s".formatted(getI18(ctx.getText()))).append("\n");
                return super.visitValue_compare_expr_val(ctx);
            }

            @Override
            public Void visitObject_compare_expr_key(ScriptedTriggersParser.Object_compare_expr_keyContext ctx) {
                sb.append("%s%s".formatted(levelPrefix.repeat(level),getI18(ctx.getText())));
                return super.visitObject_compare_expr_key(ctx);
            }

            @Override
            public Void visitObject_compare_expr_val(ScriptedTriggersParser.Object_compare_expr_valContext ctx) {
                return super.visitObject_compare_expr_val(ctx);
            }
            @Override
            public Void visitLogical_expr_key(ScriptedTriggersParser.Logical_expr_keyContext ctx) {
                String op = getI18(ctx.getText());
                sb.append(levelPrefix.repeat(level));
                switch (op) {
                    case "OR" -> {
                        sb.append("满足任意条件：");
                    }
                    case "AND" -> {
                        sb.append("满足所有条件：");
                    }
                    case "NOT" -> {
                        sb.append("不满足所有条件：");
                    }
                    case "NOR" -> {
                        sb.append("不满足任意条件：");
                    }
                    case "NAND" -> {
                        sb.append("只要其中有一个条件不满足：");
                    }
                }
                return super.visitLogical_expr_key(ctx);
            }
            //IF : 'if';
            //ELSE_IF : 'else_if ';
            //ELSE : 'else';
            @Override
            public Void visitIf_else_expr_key(ScriptedTriggersParser.If_else_expr_keyContext ctx) {
                sb.append(levelPrefix.repeat(level));
                switch (ctx.getText()) {
                    case "if" -> {
                        sb.append("如果满足条件：");
                    }
                    case "else_if" -> {
                        sb.append("如果满足其他条件：");
                    }
                    case "else" -> {
                        sb.append("否则：");
                    }
                }
                return super.visitIf_else_expr_key(ctx);
            }
        };
        visitor.visit(parser.root());
        return result;
    }
}
