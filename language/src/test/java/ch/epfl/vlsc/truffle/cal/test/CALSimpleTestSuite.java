package ch.epfl.vlsc.truffle.cal.test;

import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;

public class CALSimpleTestSuite extends CALTestSuite {

    @Test
    public void addTest() throws IOException {
        runTest(TestCase.newBuilder("add").build());
    }

    @Test
    public void actionSelectionGuardTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-guard").setIterations(10).build());
    }

    @Test
    @Ignore("Not implemented yet")
    public void actionSelectionFsmTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-schedulefsm").setIterations(10).build());
    }

    @Test
    @Ignore("Not implemented yet")
    public void actionSelectionRegexTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-scheduleregex").setIterations(10).build());
    }

    @Test
    @Ignore("Not implemented yet")
    public void actionSelectionPriorityTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-priority").setIterations(10).build());
    }

    @Test
    @Ignore("Not implemented yet")
    public void actionSelectionPriorityFsmTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-priorityfsm").setIterations(10).build());
    }

    @Test
    @Ignore("Not implemented yet")
    public void actionSelectionPriorityGuardTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-priorityguard").setIterations(10).build());
    }

    @Test
    public void precedenceTest() throws IOException {
        runTest(TestCase.newBuilder("precedence").build());
    }

    @Test
    public void actorStateTest() throws IOException {
        runTest(TestCase.newBuilder("add-stored-var").setIterations(10).build());
    }

    @Test
    public void actorParameterTest() throws IOException {
        runTest(TestCase.newBuilder("actor-parameters").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void lambdaTest() throws IOException {
        runTest(TestCase.newBuilder("lambda").build());
    }

    @Test
    public void nestedLambdaTest() throws IOException {
        runTest(TestCase.newBuilder("nested-lambda").build());
    }

    @Test
    public void printlnTest() throws IOException {
        runTest(TestCase.newBuilder("println").build());
    }

    @Test
    public void printlnVariablesTest() throws IOException {
        runTest(TestCase.newBuilder("println-var").build());
    }

    @Test
    public void simpleNetworkTest() throws IOException {
        runTest(TestCase.newBuilder("simple-network").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void simpleNestedNetworkTest() throws IOException {
        runTest(TestCase.newBuilder("network-input").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void listInitTest() throws IOException {
        runTest(TestCase.newBuilder("init-list").build());
    }

    @Test
    public void repeatInputTest() throws IOException {
        runTest(TestCase.newBuilder("repeat-input").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void repeatOutputTest() throws IOException {
        runTest(TestCase.newBuilder("repeat-output").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void letExprTest() throws IOException {
        runTest(TestCase.newBuilder("let-expr").build());
    }

    @Test
    public void foreachTest() throws IOException {
        runTest(TestCase.newBuilder("for-loop").build());
    }

    @Test
    public void nestedForLoopsTest() throws IOException {
        runTest(TestCase.newBuilder("nested-for-loop").build());
    }

    @Test
    public void forComprehensionTest() throws IOException {
        runTest(TestCase.newBuilder("for-comprehensions").build());
    }

    @Test
    public void ifStatementTest() throws IOException {
        runTest(TestCase.newBuilder("if-statement").build());
    }

    @Test
    public void ifElseStatementTest() throws IOException {
        runTest(TestCase.newBuilder("if-else-statement").build());
    }

    @Test
    public void importsTest() throws IOException {
        runTest(TestCase.newBuilder("import/Network").setActorName("test.SourceSink").setDirLookup(true).build());
    }

    @Test
    public void idctTest() throws IOException {
        runTest(TestCase.newBuilder("dct/src/TopIDCT").setActorName("RVC.TopIDCT").setDirLookup(true).build());
    }

}
