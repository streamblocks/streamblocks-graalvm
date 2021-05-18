package ch.epfl.vlsc.truffle.cal.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import com.oracle.truffle.api.dsl.NodeFactory;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.builtins.CALBuiltinNode;

public class CALSimpleTestSuite extends CALTestSuite {
	
    @Test
    public void addTest() throws IOException {
    	runTest(TestCase.newBuilder("add").build());
    }
    @Test
    public void actorState() throws IOException {
    	runTest(TestCase.newBuilder("add-stored-var").setIterations(10).build());
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
    	runTest(TestCase.newBuilder("simple-network").setActorName("SourceSink").build());
    }

    @Test
    public void listInitTest() throws IOException {
    	runTest(TestCase.newBuilder("init-list").build());
    }
	
    @Test
    public void foreachTest() throws IOException {
    	runTest(TestCase.newBuilder("for-loop").build());
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
}
