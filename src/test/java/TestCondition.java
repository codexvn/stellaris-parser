import java.nio.file.Path;
import java.util.List;

import common.technology.TechnologyLexer;
import common.technology.TechnologyParser;
import lombok.SneakyThrows;
import org.antlr.v4.gui.Trees;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import top.codexvn.ParseTechnology;

public class TestCondition {
    @SneakyThrows
    public static void main(String[] args) {
        TechnologyLexer lexer = new TechnologyLexer(CharStreams.fromPath(Path.of("F:\\IdeaProjects\\stellaris-parser\\src\\test\\java\\input.txt")));
        Token token;
        while ((token = lexer.nextToken()).getType() != Token.EOF) {
            System.out.printf("Token: %-15s Text: '%s'%n", lexer.getVocabulary().getSymbolicName(token.getType()), token.getText());
        }
        lexer = new TechnologyLexer(CharStreams.fromPath(Path.of("F:\\IdeaProjects\\stellaris-parser\\src\\test\\java\\input.txt")));
        TechnologyParser parser = new TechnologyParser(new CommonTokenStream(lexer));
        List<? extends ANTLRErrorListener> errorListeners = parser.getErrorListeners();
        ParseTechnology.TechnologyVisitor visitor = new ParseTechnology.TechnologyVisitor();
        Trees.inspect(parser.root(), parser);
        visitor.visit(parser.root());
        System.gc();
    }
}
