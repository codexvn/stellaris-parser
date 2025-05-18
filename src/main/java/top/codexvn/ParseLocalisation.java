package top.codexvn;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import localisation.LocalisationBaseVisitor;
import localisation.LocalisationLexer;
import localisation.LocalisationParser;
import localisation.LocalisationVisitor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ParseLocalisation {
    @Data
    public static class Localisation {
        private Map<String, String> title = new LinkedHashMap<>();
        private Map<String, String> desc = new LinkedHashMap<>();

        private Set<String> titleNeedUpdate = new HashSet<>();
        private Set<String> descNeedUpdate = new HashSet<>();
    }
    private static final Pattern REF_PATTERN = Pattern.compile("\\$[a-zA-Z0-9_\\-.\"]+\\$");

    public static void main(String[] args) throws Exception {
        var run = run();
        log.info(run.toString());
    }

    public static Localisation run() throws Exception {
        Localisation result = new Localisation();
        String filePath = ".game_files/localisation/simp_chinese";
        //列出所有文件
        Collection<File> files = FileUtils.listFiles(new File(filePath), new String[]{"yml"}, false);
        for (File file : files) {
            //读取文件内容
            String content = FileUtils.readFileToString(file, "UTF-8");
            var thisLocalisation = doParse(content, result);
            //合并所有的值
            result.getTitle().putAll(thisLocalisation.getTitle());
            result.getDesc().putAll(thisLocalisation.getDesc());
            result.getTitleNeedUpdate().addAll(thisLocalisation.getTitleNeedUpdate());
            result.getDescNeedUpdate().addAll(thisLocalisation.getDescNeedUpdate());
        }
        //更新引用
        Set<String> updatedTitle = new HashSet<>();
        Set<String> updatedDesc = new HashSet<>();
        boolean titleUpdated = false;
        boolean descUpdated = false;
        while (!(result.getDescNeedUpdate().isEmpty() && result.getTitleNeedUpdate().isEmpty())) {
            for (String key : result.getTitleNeedUpdate()) {
                String value = result.getTitle().get(key);
                //找到引用的key
                Matcher matcher = REF_PATTERN.matcher(value);
                while (matcher.find()) {
                    String refKey = matcher.group();
                    String cleanedRefKey = refKey.substring(1, refKey.length() - 1);
                    //如果引用的key在title中存在，则替换
                    if (result.getTitle().containsKey(cleanedRefKey)) {
                        value = value.replace(refKey, result.getTitle().get(cleanedRefKey));
                    }
                    if (result.getDesc().containsKey(cleanedRefKey)) {
                        value = value.replace(refKey, result.getDesc().get(cleanedRefKey));
                    }
                }
                result.getTitle().put(key, value);
                if (!REF_PATTERN.matcher(value).find()) {
                    updatedTitle.add(key);
                }
            }
            titleUpdated = result.getTitleNeedUpdate().removeAll(updatedTitle);

            for (String key : result.getDescNeedUpdate()) {
                String value = result.getDesc().get(key);
                //找到引用的key
                Matcher matcher = REF_PATTERN.matcher(value);
                while (matcher.find()) {
                    String refKey = matcher.group();
                    String cleanedRefKey = refKey.substring(1, refKey.length() - 1);
                    //如果引用的key在title中存在，则替换
                    if (result.getTitle().containsKey(cleanedRefKey)) {
                        value = value.replace(refKey, result.getTitle().get(cleanedRefKey));
                    }
                    if (result.getDesc().containsKey(cleanedRefKey)) {
                        value = value.replace(refKey, result.getDesc().get(cleanedRefKey));
                    }
                }
                result.getDesc().put(key, value);
                if (!REF_PATTERN.matcher(value).find()) {
                    updatedDesc.add(key);
                }
            }
          descUpdated =   result.getDescNeedUpdate().removeAll(updatedDesc);
            if (!descUpdated & !titleUpdated) {
                break;
            }
        }


        return result;
    }

    public static Localisation doParse(String content, Localisation globalLocalisation) throws Exception {
        Localisation result = new Localisation();
        LocalisationLexer lexer = new LocalisationLexer(CharStreams.fromString(content));
        LocalisationParser parser = new LocalisationParser(new CommonTokenStream(lexer));
        LocalisationVisitor<Void> visitor = new LocalisationBaseVisitor<>() {
            private String titleKey;
            private String titleDescKey;

            @Override
            public Void visitTitle(LocalisationParser.TitleContext ctx) {
                titleKey = ctx.getText();
                return super.visitTitle(ctx);
            }

            @Override
            public Void visitTitle_val(LocalisationParser.Title_valContext ctx) {
                String value = ctx.getText();
                value = StringUtils.strip(value, "\"");
                result.getTitle().put(titleKey, value);
                if (value.contains("$")) {
                    result.getTitleNeedUpdate().add(titleKey);
                }
                titleKey = null; // Reset after use
                return super.visitTitle_val(ctx);
            }

            @Override
            public Void visitTitle_desc(LocalisationParser.Title_descContext ctx) {
                titleDescKey = ctx.getText();
                return super.visitTitle_desc(ctx);
            }

            @Override
            public Void visitTitle_desc_val(LocalisationParser.Title_desc_valContext ctx) {
                String value = ctx.getText();
                value = StringUtils.strip(value, "\"");
                if (value.contains("$")) {
                    result.getDescNeedUpdate().add(titleDescKey);
                }
                result.getDesc().put(titleDescKey, value);
                titleDescKey = null; // Reset after use
                return super.visitTitle_desc_val(ctx);
            }
        };
        visitor.visit(parser.localisation());
        return result;
    }
}