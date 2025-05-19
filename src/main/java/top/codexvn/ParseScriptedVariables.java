package top.codexvn;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import common.scripted_variables.ScriptedVariablesBaseVisitor;
import common.scripted_variables.ScriptedVariablesLexer;
import common.scripted_variables.ScriptedVariablesParser;
import common.scripted_variables.ScriptedVariablesVisitor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.io.FileUtils;

@Slf4j
public class ParseScriptedVariables {

    public static void main(String[] args) throws Exception {
        Map<String, String> run = run();
        log.info(run.toString());
    }
    public static Map<String,String> run() throws Exception {
        Map<String,String> result = new LinkedHashMap<>();
        String filePath = ".game_files/common/scripted_variables";
        //列出所有文件
        Collection<File> files = FileUtils.listFiles(new File(filePath), new String[]{"txt"}, false);
        for (File file : files) {
            //读取文件内容
            String content = FileUtils.readFileToString(file, "UTF-8");
            Map<String, String> key2valMap = doParse(content);
            result.putAll(key2valMap);
        }
        return result;
    }
    public static Map<String,String> doParse(String content) throws Exception {
        Map<String,String> result = new LinkedHashMap<>();
        ScriptedVariablesLexer lexer = new ScriptedVariablesLexer(CharStreams.fromString(content));
        ScriptedVariablesParser parser = new ScriptedVariablesParser(new CommonTokenStream(lexer));
        ScriptedVariablesVisitor<Void> visitor = new ScriptedVariablesBaseVisitor<>(){
            private String key;
            @Override
            public Void visitVal(ScriptedVariablesParser.ValContext ctx) {
                String text = ctx.getText();
                result.put(Objects.requireNonNull(key), text);
                return super.visitVal(ctx);
            }

            @Override
            public Void visitVariable_key(ScriptedVariablesParser.Variable_keyContext ctx) {
                key = ctx.getText();
                return super.visitVariable_key(ctx);
            }
        };
        visitor.visit(parser.root());
        return result;
    }
}
