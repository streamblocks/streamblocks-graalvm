package ch.epfl.vlsc.truffle.cal.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
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
import org.junit.runner.Description;

import ch.epfl.vlsc.truffle.cal.CALLanguage;

abstract class CALTestSuite {
	private static final String SOURCE_SUFFIX = ".cal";
	private static final String INPUT_SUFFIX = ".input";
	private static final String OUTPUT_SUFFIX = ".output";

	private static final String LF = System.getProperty("line.separator");

	static class TestCase {
		protected Description name;
		protected Path path;
		protected String sourceName;
		protected String testInput;
		protected String expectedOutput;
		protected Map<String, String> options;
		protected String actualOutput;

		private static final TestCase EMPTY = new TestCase();

		private TestCase() {
		}

		protected TestCase(Class<?> testClass, String baseName, String sourceName, Path path, String testInput,
				String expectedOutput, Map<String, String> options) {
			this.name = Description.createTestDescription(testClass, baseName);
			this.sourceName = sourceName;
			this.path = path;
			this.testInput = testInput;
			this.expectedOutput = expectedOutput;
			this.options = options;
		}

		public static Builder newBuilder(String name) {
			return EMPTY.new Builder(name);
		}

		class Builder {
			String name;
			String input;
			String actorName;
			Integer iterations;
            Boolean dirLookup;
            Boolean showWarnings;

			private Builder(String name) {
				this.name = name;
				this.input = "";
				this.actorName = "test.test";
				this.iterations = 1;
                this.dirLookup = false;
				this.showWarnings = false;
			}

			public Builder setInput(String input) {
				this.input = input;
				return this;
			}

			public Builder useInputFile() throws FileNotFoundException, IOException {
				setInputFile(name + INPUT_SUFFIX);
				return this;
			}

			public Builder setInputFile(String inputFilename) throws FileNotFoundException, IOException {
				this.input = readAllLines(getTestPath(inputFilename + INPUT_SUFFIX));
				return this;
			}

			public Builder setActorName(String actorName) {
				this.actorName = actorName;
				return this;
			}

			public Builder setIterations(Integer iterations) {
				this.iterations = iterations;
				return this;
			}

			public Builder setDirLookup(Boolean dirLookup) {
				this.dirLookup = dirLookup;
				return this;
			}

			public Builder setShowWarnings(Boolean showWarnings) {
				this.showWarnings = showWarnings;
				return this;
			}

			public TestCase build() throws FileNotFoundException, IOException {
				Map<String, String> options = new HashMap<>();
				options.put("cal.actor", actorName);
				options.put("cal.iterations", iterations.toString());
				options.put("cal.directory-lookup", dirLookup.toString());
				options.put("cal.show-warnings", showWarnings.toString());
				return new TestCase(this.getClass(), name, name, getTestPath(name + SOURCE_SUFFIX), input,
						readAllLines(getTestPath(name + OUTPUT_SUFFIX)), options);
			}
		}
	}

	private static void run(Context context, Path path, PrintWriter out) throws IOException {
		try {
			/* Parse the CAL source file. */
			// Mark the source as interactive, otherwise it prints the return value of the
			// actor
			// which is always NULL
			Source source = Source.newBuilder(CALLanguage.ID, path.toFile()).interactive(false).build();

			/* Call the main entry point, without any arguments. */
			context.eval(source);
		} catch (PolyglotException ex) {
			if (!ex.isInternalError()) {
				out.println(ex.getMessage());
			} else {
				throw ex;
			}
		}
	}

	protected void runTest(TestCase testCase) throws IOException {
		Context context = null;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			/*
			 * for (NodeFactory<? extends CALBuiltinNode> builtin : builtins) {
			 * CALLanguage.installBuiltin(builtin); }
			 */

			Context.Builder builder = Context.newBuilder().allowExperimentalOptions(true)
					.in(new ByteArrayInputStream(testCase.testInput.getBytes("UTF-8"))).out(out);
			for (Map.Entry<String, String> e : testCase.options.entrySet()) {
				builder.option(e.getKey(), e.getValue());
			}
			context = builder.build();
			PrintWriter printer = new PrintWriter(out);
			run(context, testCase.path, printer);
			printer.flush();

			String actualOutput = new String(out.toByteArray());
			Assert.assertEquals(testCase.name.toString(), testCase.expectedOutput, actualOutput);
		} finally {
			if (context != null) {
				context.close();
			}
		}
	}

	/**
	 * Recursively deletes a file that may represent a directory.
	 */
	private static void delete(File f) {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				delete(c);
			}
		}
		if (!f.delete()) {
			PrintStream err = System.err;
			err.println("Failed to delete file: " + f);
		}
	}

	/**
	 * Unpacks a jar file to a temporary directory that will be removed when the VM
	 * exits.
	 *
	 * @param jarfilePath the path of the jar to unpack
	 * @return the path of the temporary directory
	 */
	private static String explodeJarToTempDir(File jarfilePath) {
		try {
			final Path jarfileDir = Files.createTempDirectory(jarfilePath.getName());
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					delete(jarfileDir.toFile());
				}
			});
			jarfileDir.toFile().deleteOnExit();
			JarFile jarfile = new JarFile(jarfilePath);
			Enumeration<JarEntry> entries = jarfile.entries();
			while (entries.hasMoreElements()) {
				JarEntry e = entries.nextElement();
				if (!e.isDirectory()) {
					File path = new File(jarfileDir.toFile(), e.getName().replace('/', File.separatorChar));
					File dir = path.getParentFile();
					dir.mkdirs();
					assert dir.exists();
					Files.copy(jarfile.getInputStream(e), path.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			return jarfileDir.toFile().getAbsolutePath();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	public static Path getRootViaResourceURL(final Class<?> c, String path) {
		URL url = c.getResource(c.getSimpleName() + ".class");
		if (url != null) {
			char sep = File.separatorChar;
			String externalForm = url.toExternalForm();
			String classPart = sep + c.getName().replace('.', sep) + ".class";
			String prefix = null;
			String base;
			if (externalForm.startsWith("jar:file:")) {
				prefix = "jar:file:";
				int bang = externalForm.indexOf('!', prefix.length());
				Assume.assumeTrue(bang != -1);
				File jarfilePath = new File(externalForm.substring(prefix.length(), bang));
				Assume.assumeTrue(jarfilePath.exists());
				base = explodeJarToTempDir(jarfilePath);
			} else if (externalForm.startsWith("file:")) {
				prefix = "file:";
				base = externalForm.substring(prefix.length(), externalForm.length() - classPart.length());
			} else {
				return null;
			}

			String candidate = base + sep + path;
			if (new File(candidate).exists()) {
				return FileSystems.getDefault().getPath(candidate);
			}
		}
		return null;
	}

	public static Path getTestPath(String filename) throws FileNotFoundException {
		String path = "tests";
		Path root = getRootViaResourceURL(CALTestSuite.class, path);

		if (root == null) {
			Path candidate = FileSystems.getDefault().getPath(path);
			if (Files.exists(candidate)) {
				root = candidate;

			}

		}
		if (root == null) {
			throw new FileNotFoundException(path);
		}
		return root.resolve(filename);
	}

	protected static String readAllLines(Path file) throws IOException {
		// fix line feeds for non unix os
		StringBuilder outFile = new StringBuilder();
		for (String line : Files.readAllLines(file, Charset.defaultCharset())) {
			outFile.append(line).append(LF);
		}
		return outFile.toString();
	}
}
