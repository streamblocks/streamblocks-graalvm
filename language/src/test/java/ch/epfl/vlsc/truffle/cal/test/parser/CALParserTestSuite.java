package ch.epfl.vlsc.truffle.cal.test.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

public class CALParserTestSuite {

    protected void runTest(String name) {
        System.out.println("========================================================");
        System.out.println("                     Test: " + name);
        System.out.println("========================================================");

        CalLexer lexer = null;
        try {
            lexer = new CalLexer(CharStreams.fromPath(Path.of(System.getProperty("user.dir"), "tests", name + ".cal")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Lexer tokens:");
        for (Token token: lexer.getAllTokens()) {
            System.out.println(token);
        }

        CalParser parser = new CalParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.compilationUnit();

        System.out.println("Parser tree:");
        System.out.println(tree.toStringTree(parser));
        System.out.println();
    }

    @Test
    public void addTest() {
        runTest("add");
    }

    @Test
    public void precedenceTest() throws IOException {
        runTest("precedence");
    }

    @Test
    public void actorStateTest() throws IOException {
        runTest("add-stored-var");
    }

    @Test
    public void actorParameterTest() throws IOException {
        runTest("actor-parameters");
    }

    @Test
    public void lambdaTest() throws IOException {
        runTest("lambda");
    }

    @Test
    public void nestedLambdaTest() throws IOException {
        runTest("nested-lambda");
    }

    @Test
    public void printlnTest() throws IOException {
        runTest("println");
    }

    @Test
    public void printlnVariablesTest() throws IOException {
        runTest("println-var");
    }

    @Test
    public void simpleNetworkTest() throws IOException {
        runTest("simple-network");
    }
    @Test
    public void simpleNestedNetworkTest() throws IOException {
        runTest("network-input");
    }

    @Test
    public void listInitTest() throws IOException {
        runTest("init-list");
    }

    @Test
    public void repeatInputTest() throws IOException {
        runTest("repeat-input");
    }

    @Test
    public void repeatOutputTest() throws IOException {
        runTest("repeat-output");
    }

    @Test
    public void letExprTest() throws IOException {
        runTest("let-expr");
    }

    @Test
    public void foreachTest() throws IOException {
        runTest("for-loop");
    }

    @Test
    public void nestedForLoopsTest() throws IOException {
        runTest("nested-for-loop");
    }

    @Test
    public void forComprehensionTest() throws IOException {
        runTest("for-comprehensions");
    }

    @Test
    public void ifStatementTest() throws IOException {
        runTest("if-statement");
    }

    @Test
    public void ifElseStatementTest() throws IOException {
        runTest("if-else-statement");
    }

    @Test
    public void importsTest() throws IOException {
        runTest("import/Network");
        runTest("import/Sink");
        runTest("import/Source");
    }

    @Test
    public void idctTest() throws IOException {
        runTest("dct/src/TopIDCT");
        runTest("dct/src/idct/Clip");
        runTest("dct/src/idct/idct2d_23002");
        runTest("dct/src/idct/rightshift");
        runTest("dct/src/idct/Scale");
        runTest("dct/src/idct/scaled_1d_idct");
        runTest("dct/src/idct/Transpose");
    }

}
