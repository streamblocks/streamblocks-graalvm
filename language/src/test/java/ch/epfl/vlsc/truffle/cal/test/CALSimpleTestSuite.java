package ch.epfl.vlsc.truffle.cal.test;

import java.io.IOException;

import ch.epfl.vlsc.truffle.cal.nodes.InvariantViolationException;
import org.graalvm.polyglot.PolyglotException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class CALSimpleTestSuite extends CALTestSuite {
    @Test
    public void addTest() throws IOException {
        runTest(TestCase.newBuilder("add").build());
    }

    @Test
    @Ignore
    public void blockStatementTest() throws IOException {
        runTest(TestCase.newBuilder("block-statement").build());
    }

    @Test
    public void actionSelectionGuardTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-guard").setIterations(10).build());
    }

    @Test
    public void actionSelectionFsmTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-schedulefsm").setIterations(10).build());
    }

    @Test
    public void actionSelectionRegexTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-scheduleregex").setIterations(10).build());
    }

    @Test
    public void actionSelectionRegexOptionalTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-scheduleregex-optional").setIterations(10).build());
    }

    @Test
    public void actionSelectionPriorityTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-priority").setIterations(10).build());
    }

    @Test
    public void actionSelectionPriorityFsmTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-priorityfsm").setIterations(10).build());
    }

    @Test
    public void actionSelectionPriorityGuardTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-priorityguard").setIterations(10).build());
    }

    @Test
    public void actionSelectionPriorityAntiSymmetry() throws IOException {
        runTestRegexPartial(TestCase.newBuilder("action-selection-priorityantisymmetry").setIterations(10).build());
    }

    @Test
    public void actionSelectionPriorityGuardPrefix() throws IOException {
        runTestRegexPartial(TestCase.newBuilder("action-selection-priorityguardprefix").setIterations(10).build());
    }

    @Test
    public void actionSelectionExtraPriorityRelations() throws IOException {
        runTest(TestCase.newBuilder("action-selection-extrapriorityrelations").setIterations(10).build());
    }

    @Test
    public void actionSelectionPriorityFsmGuardTest() throws IOException {
        runTest(TestCase.newBuilder("action-selection-priorityfsmguard").setIterations(10).build());
    }

    @Test
    public void precedenceTest() throws IOException {
        runTest(TestCase.newBuilder("precedence").build());
    }

    @Test
    public void binopTest() throws IOException {
        runTest(TestCase.newBuilder("binop").build());
    }

    @Test
    public void unaryopTest() throws IOException {
        runTest(TestCase.newBuilder("unary-op").build());
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
    public void initializeTest() throws IOException {
        runTest(TestCase.newBuilder("initialize").setIterations(10).build());
    }

    @Test
    public void initializePriorityTest() throws IOException {
        runTest(TestCase.newBuilder("initialize-priority").setIterations(10).build());
    }

    @Test
    public void initializePriorityAndNotPriorityTest() throws IOException {
        runTest(TestCase.newBuilder("initialize-priorityandnonpriority").setIterations(10).build());
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
    public void networkOldVariableReferenceTest() throws IOException {
        runTest(TestCase.newBuilder("network-old-variable-reference").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void simpleNestedNetworkTest() throws IOException {
        runTest(TestCase.newBuilder("network-input").setActorName("simple.dwf.SourceSink").setIterations(-1).build());
    }

    @Test
    public void multipleInputExpressionsTest() throws IOException {
        runTest(TestCase.newBuilder("network-input-multiple-input-expressions").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void multipleInputExpressionsIgnoreTokenTest() throws IOException {
        runTest(TestCase.newBuilder("network-input-multiple-input-expressions-ignoretoken").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void multipleInputPortsTest() throws IOException {
        runTest(TestCase.newBuilder("network-multiple-input-ports").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    @Ignore
    public void multipleInputPortsWithReuseOfVariableTest() throws IOException {
        runTest(TestCase.newBuilder("network-multiple-input-ports-repeatuseofvariable").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void networkActionselectionGuardInputToken() throws IOException {
        runTest(TestCase.newBuilder("network-actionselection-guard-input-token").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void multipleInputExpressionsRepeatTest() throws IOException {
        runTest(TestCase.newBuilder("network-input-multiple-input-expressions-repeat").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void multipleInputExpressionsRepeatIgnoreTokenTest() throws IOException {
        runTest(TestCase.newBuilder("network-input-multiple-input-expressions-repeat-ignoretoken").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void multipleOutputExpressionsTest() throws IOException {
        runTest(TestCase.newBuilder("network-input-multiple-output-expressions").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void networkLoopConnectionTest() throws IOException {
        runTest(TestCase.newBuilder("network-loop-connection").setIterations(-1).setActorName("test.Sum").build());
    }

    @Test
    public void networkLoopConnectionInvariantTest() throws IOException {
        try {
            runTest(TestCase.newBuilder("network-loop-connection-invariant").setIterations(-1).setActorName("test.Sum").build());
        } catch (PolyglotException e) {
            Assert.assertTrue(e.getMessage().contains("InvariantViolationException"));
            return;
        }
        Assert.fail("Excpected InvariantViolationException, but it was not raised");
    }

    @Test
    public void networkLoopConnectionInvariantInitializeTest() throws IOException {
        runTest(TestCase.newBuilder("network-loop-connection-invariant-initialize").setIterations(-1).setActorName("test.Sum").build());
    }

    @Test
    public void networkLoopConnectionFibsTest() throws IOException {
        runTest(TestCase.newBuilder("network-loop-connection-fibs").setActorName("test.Fibs").build());
    }

    @Test
    @Ignore
    public void networkLanguageFibsTest() throws IOException {
        runTest(TestCase.newBuilder("network-language-fibs").setActorName("test.Fibs").build());
    }

    @Test
    public void networkMultipleConnectionsOrderingTest() throws IOException {
        runTest(TestCase.newBuilder("network-multiple-connections-actor").setActorName("test.NumPrinter").build());
    }

    @Test
    @Ignore
    public void networkReduntantConnectionTest() throws IOException {
        runTest(TestCase.newBuilder("network-redundant-connection").setActorName("test.NumPrinter").build());
    }

    @Test
    public void listInitTest() throws IOException {
        runTest(TestCase.newBuilder("init-list").build());
    }

    @Test
    public void listNestedTest() throws IOException {
        runTest(TestCase.newBuilder("nested-list").build());
    }

    @Test
    public void repeatInputTest() throws IOException {
        runTest(TestCase.newBuilder("repeat-input").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void repeatOutputSingleExprTest() throws IOException {
        runTest(TestCase.newBuilder("repeat-output-single-expr").setActorName("simple.dwf.SourceSink").build());
    }

    @Test
    public void repeatOutputMultipleExprTest() throws IOException {
        runTest(TestCase.newBuilder("repeat-output-multiple-expr").setActorName("simple.dwf.SourceSink").build());
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
    public void whileLoopTest() throws IOException {
        runTest(TestCase.newBuilder("while-loop").setIterations(-1).build());
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
    @Ignore
    public void forComprehensionExtendedTest() throws IOException {
        runTest(TestCase.newBuilder("for-comprehensions-comprehensive").build());
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
    public void typeChecksTest() throws IOException {
        runTest(TestCase.newBuilder("typechecks").build());
    }

    @Test
    public void ProcedureTest() throws IOException {
        runTest(TestCase.newBuilder("procedure").setIterations(10).build());
    }

    @Test
    public void ProcedureArgTest() throws IOException {
        runTest(TestCase.newBuilder("procedure-arg").setIterations(10).build());
    }

    @Test
    public void ProcedureClosureTest() throws IOException {
        runTest(TestCase.newBuilder("procedure-closure").setIterations(10).build());
    }

    @Test
    public void importsTest() throws IOException {
        runTest(TestCase.newBuilder("import/Network").setActorName("test.SourceSink").setDirLookup(true).build());
    }

    @Test
    public void lazyEvaluationEdgecaseTest() throws IOException {
        runTest(TestCase.newBuilder("lazy-evaluation-edgecase").setActorName("test.test").build());
    }

    @Test
    public void idctTest() throws IOException {
        runTest(TestCase.newBuilder("dct/src/TopIDCT").setActorName("RVC.TopIDCT").setDirLookup(true).build());
    }

    @Test
    public void jpegSingleImageTest() throws IOException {
        runTest(TestCase.newBuilder("jpeg/SingleImageTest").setIterations(-1).setActorName("jpeg.SingleImagePrinter").setDirLookup(true).build());
    }

    @Test
    @Ignore
    public void filtersTest() throws IOException {
        runTest(TestCase.newBuilder("filters/fir/src/TopFIRThroughput").setIterations(-1).setActorName("fir.TopFIRThroughput").setDirLookup(true).build());
    }
}
