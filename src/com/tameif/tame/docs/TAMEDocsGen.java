/*******************************************************************************
 * Copyright (c) 2017-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.docs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.blackrook.commons.Common;
import com.blackrook.commons.CommonTokenizer;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.linkedlist.Queue;
import com.tameif.tame.TAMEModule;
import com.tameif.tame.factory.TAMEJSExporter;
import com.tameif.tame.factory.TAMEJSExporterOptions;
import com.tameif.tame.factory.TAMEScriptIncluder;
import com.tameif.tame.factory.TAMEScriptParseException;
import com.tameif.tame.factory.TAMEScriptReader;
import com.tameif.tame.lang.ArithmeticOperator;
import com.tameif.tame.lang.Value;

/**
 * Generates TAME documentation.
 * @author Matthew Tropiano
 */
public final class TAMEDocsGen
{
	/** Shorthand STDOUT. */
	static final PrintStream out = System.out;

	/** Current time. */
	static final String NOW_STRING = (new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

	/** Resource root. */
	static final String RESOURCE_ROOT = "./site-dynamic";
	/** Sidebar content. */
	static final String RESOURCE_SIDEBARHTML = RESOURCE_ROOT + "/sidebar.html";
	/** Pages list file. */
	static final String RESOURCE_PAGESLIST = RESOURCE_ROOT + "/_pages.txt";
	/** Script root. */
	static final String RESOURCE_SCRIPTROOT = RESOURCE_ROOT + "/scripts/";

	/** Pages list file. */
	static final String SOURCE_SIDEASSETS = "./site-assets";

	/** Output directory for generated JS. */
	static final String OUTPATH_JS = "js/generated/";
	/** Output directory for generated JS engine. */
	static final String OUTPATH_JS_TAMEENGINE = OUTPATH_JS + "TAME.js";
	/** Output directory for generated JS engine. */
	static final String OUTPATH_JS_BROWSERHANDLER = OUTPATH_JS + "TAMEBrowserHandler.js";
	/** Output directory for generated JS module. */
	static final String OUTPATH_JS_TAMEMODULE = OUTPATH_JS + "modules/";

	/** Parse command: set variable. */
	static final String COMMAND_SET = "set";
	/** Parse command: clear variable. */
	static final String COMMAND_CLEAR = "clear";
	/** Parse command: print variable. */
	static final String COMMAND_PRINT = "print";
	/** Parse command: include (file) */
	static final String COMMAND_INCLUDE = "include";
	/** Parse command: tamescript (name) (modulevarname) (file) */
	static final String COMMAND_TAMESCRIPT = "tamescript";
	/** Parse command: generate-table (operator name) */
	static final String COMMAND_GENERATE_TABLE = "generate-table";
	/** Variable prefix. */
	static final String VAR_PREFIX = "$";

	/** Values to use for the arithmetic operator tables. */
	static final Value[] TEST_VALUES = 
	{
		Value.create(false),
		Value.create(true),
		Value.create(Double.POSITIVE_INFINITY),
		Value.create(Double.NEGATIVE_INFINITY),
		Value.create(Double.NaN),
		Value.create(0),
		Value.create(0.0),
		Value.create(10),
		Value.create(3),
		Value.create(10.0),
		Value.create(3.0),
		Value.create(10.5),
		Value.create(3.5),
		Value.create(-10),
		Value.create(-3),
		Value.create(-10.0),
		Value.create(-3.0),
		Value.create(-10.5),
		Value.create(-3.5),
		Value.create(""),
		Value.create(" "),
		Value.create("0"),
		Value.create("0.0"),
		Value.create("10"),
		Value.create("3"),
		Value.create("10.0"),
		Value.create("3.0"),
		Value.create("10.5"),
		Value.create("3.5"),
		Value.create("-10"),
		Value.create("-3"),
		Value.create("-10.0"),
		Value.create("-3.0"),
		Value.create("-10.5"),
		Value.create("-3.5"),
		Value.create("apple"),
		Value.create("banana"),
		Value.create("NaN"),
		Value.create("infinity"),
		Value.createEmptyList(),
		Value.createList(Value.create(true), Value.create(3), Value.create(5.0), Value.create("orange"))
	};
	
	/** TAMEScript includer. */
	static final TAMEScriptIncluder TAMESCRIPT_INCLUDER = new TAMEScriptIncluder()
	{
		@Override
		public InputStream getIncludeResource(String path) throws IOException
		{
			return new FileInputStream(new File(path));
		}

		@Override
		public String getNextIncludeResourceName(String streamName, String path) throws IOException
		{
			String streamParent = null;
			int lidx = -1;
			if ((lidx = streamName.lastIndexOf('/')) >= 0)
				streamParent = streamName.substring(0, lidx + 1);

			return (streamParent != null ? streamParent : "") + path;
		}
	};

	/** TAMEScript JS Exporter Options. */
	static final TAMEJSExporterOptions TAMESCRIPT_JSEXPORTER_OPTIONS_ENGINE = new TAMEJSExporterOptions()
	{
		@Override
		public boolean isPathOutputEnabled()
		{
			return false;
		}

		@Override
		public String getWrapperName()
		{
			return TAMEJSExporter.WRAPPER_ENGINE;
		}

		@Override
		public String getModuleVariableName()
		{
			return null;
		}
	};

	// Entry point.
	public static void main(String[] args) throws Exception
	{
		if (args.length < 1)
		{
			out.println("ERROR: Expected output directory path.");
			System.exit(1);
			return;
		}

		// Get outpath root.
		String outPath = Common.removeEndingSequence(args[0], "/");
		File outDir = new File(outPath);
		if (!Common.createPath(outDir.getPath()))
		{
			out.println("ERROR: Could not create path for "+outDir.getPath());
			System.exit(3);
		}

		if (!outDir.isDirectory())
		{
			out.println("ERROR: Provided path is not a directory.");
			System.exit(2);
			return;
		}

		// Copy static pages.
		copyStaticPages(outDir);

		// Export engine.
		exportEngine(outPath);
		// Export browser JS helper.
		exportBrowserHandler(outPath);

		// Process pages.
		processAllPages(outPath);

		out.println("Done!");
	}

	private static void processAllPages(String outPath) throws IOException
	{
		for (String[] page : getPageList())
		{
			boolean error = false;
			File outFile = new File(outPath + "/" + page[0]);
			if (!Common.createPathForFile(outFile))
			{
				out.println("ERROR: Could not create path for "+outFile.getPath());
				continue;
			}

			HashMap<String, String> variables = new HashMap<>();
			variables.put("title", page[1]);
			variables.put("content_page", "content/"+page[0]);
			String template = page[2];

			PrintWriter pw = null;
			try {
				pw = new PrintWriter(outFile, "UTF-8");
				parsePageResource(outPath, outFile, pw, RESOURCE_ROOT + "/" + template, variables);
			} catch (SecurityException e) {
				out.println("ERROR: Could not write file "+outFile.getPath()+". Access denied.");
				error = true;
			} catch (IOException e) {
				out.println("ERROR: Could not write file "+outFile.getPath()+". "+e.getLocalizedMessage());
				error = true;
			} finally {
				Common.close(pw);
				if (error)
					outFile.delete();
			}
		}
	}

	private static boolean exportBrowserHandler(String outPath) throws IOException
	{
		File outFile = new File(outPath + "/" + OUTPATH_JS_BROWSERHANDLER);
		if (!Common.createPathForFile(outFile))
		{
			out.println("ERROR: Could not create path for "+outFile.getPath());
			return false;
		}

		InputStream in = null;
		OutputStream out = null;
		try {
			in = Common.openResource("tamejs/html/TAMEBrowserHandler.js");
			out = new FileOutputStream(outFile);
			Common.relay(in, out);
		} finally {
			Common.close(in);
			Common.close(out);
		}

		return true;
	}

	private static boolean exportEngine(String outPath) throws IOException
	{
		File outFile = new File(outPath + "/" + OUTPATH_JS_TAMEENGINE);
		if (!Common.createPathForFile(outFile))
		{
			out.println("ERROR: Could not create path for "+outFile.getPath());
			return false;
		}

		TAMEJSExporter.export(outFile, null, TAMESCRIPT_JSEXPORTER_OPTIONS_ENGINE);
		return true;
	}

	private static void copyStaticPages(File outDir)
	{
		for (File inFile : Common.explodeFiles(new File(SOURCE_SIDEASSETS)))
		{
			File outFile = new File(outDir.getPath() + "/" + Common.removeStartingSequence(inFile.getPath().replaceAll("\\\\", "/"), SOURCE_SIDEASSETS));
			if (!Common.createPathForFile(outFile))
			{
				out.println("ERROR: Could not create path for "+outFile.getPath());
				continue;
			}

			FileInputStream fis = null;
			FileOutputStream fos = null;
			try {
				fis = new FileInputStream(inFile);
				fos = new FileOutputStream(outFile);
				Common.relay(fis, fos);
			} catch (SecurityException e) {
				out.printf("ERROR: Could not copy \"%s\" to \"%s\". Access denied.\n", inFile.getPath(), outFile.getPath());
			} catch (IOException e) {
				out.printf("ERROR: Could not copy \"%s\" to \"%s\"\n", inFile.getPath(), outFile.getPath());
			} finally {
				Common.close(fis);
				Common.close(fos);
			}
		}
	}

	private static Iterable<String[]> getPageList() throws IOException
	{
		Queue<String[]> out = new Queue<>();
		InputStream in = null;
		try {
			String line = null;
			BufferedReader pageReader = Common.openTextStream(in = new FileInputStream(new File(RESOURCE_PAGESLIST)));
			while ((line = pageReader.readLine()) != null)
			{
				CommonTokenizer ct = new CommonTokenizer(line);
				String[] linedata = new String[3];
				for (int i = 0; i < linedata.length; i++)
					linedata[i] = ct.nextToken();
				out.add(linedata);
			}
		} finally {
			Common.close(in);
		}
		return out;
	}

	/**
	 * Parses a file resource (presumably HTML) looking for <code>&lt;? ... ?&gt;</code> tags to parse and interpret.
	 * @param outFile the output file.
	 * @param writer the output writer.
	 * @param inPath input resource path.
	 */
	public static void parsePageResource(String outPath, File outFile, Writer writer, String inPath, HashMap<String, String> pageContext) throws IOException
	{
		final String TAG_START = "<!--[";
		final String TAG_END = "]-->";
		final int STATE_PAGE = 0;
		final int STATE_START_TAG_MAYBE = 1;
		final int STATE_TAG = 2;
		final int STATE_END_TAG_MAYBE = 3;

		Reader reader = null;
		InputStream in = null;
		try {
			in = new FileInputStream(new File(inPath));
			StringBuilder tagPart = new StringBuilder();
			StringBuilder tagContent = new StringBuilder();
			reader = new InputStreamReader(in, "UTF-8");
			int state = STATE_PAGE;
			int readChar = 0;

			while ((readChar = reader.read()) != -1)
			{
				char c = (char)readChar;
				switch (state)
				{
					case STATE_PAGE:
					{
						if (c == TAG_START.charAt(0))
						{
							tagPart.append(c);
							state = STATE_START_TAG_MAYBE;
						}
						else
							writer.write(c);
					}
					break;

					case STATE_START_TAG_MAYBE:
					{
						if (c != TAG_START.charAt(tagPart.length()))
						{
							writer.write(tagPart.toString());
							tagPart.delete(0, tagPart.length());
							writer.write(c);
							state = STATE_PAGE;
						}
						else
						{
							tagPart.append(c);
							if (tagPart.length() >= TAG_START.length())
							{
								state = STATE_TAG;
								tagPart.delete(0, tagPart.length());
							}
						}
					}
					break;

					case STATE_TAG:
					{
						if (c == TAG_END.charAt(0))
						{
							tagPart.append(c);
							state = STATE_END_TAG_MAYBE;
						}
						else
							tagContent.append(c);
					}
					break;

					case STATE_END_TAG_MAYBE:
					{
						if (c != TAG_END.charAt(tagPart.length()))
						{
							tagContent.append(tagPart.toString());
							tagPart.delete(0, tagPart.length());
							tagContent.append(c);
							state = STATE_TAG;
						}
						else
						{
							tagPart.append(c);
							if (tagPart.length() >= TAG_END.length())
							{
								state = STATE_PAGE;
								String content = tagContent.toString();
								if (!interpretTag(outPath, outFile, content.trim(), inPath, writer, pageContext))
									writer.write(TAG_START + content + TAG_END);
								tagContent.delete(0, tagContent.length());
								tagPart.delete(0, tagPart.length());
							}
						}
					}
					break;
				}

			}

		} finally {
			Common.close(reader);
		}

	}

	/**
	 * Potentially resolves a token variable.
	 * @param inToken the input token.
	 * @param pageContext the context that holds variables.
	 * @return the associated object.
	 */
	private static String resolveVariable(String inToken, HashMap<String, String> pageContext)
	{
		if (inToken.startsWith(VAR_PREFIX))
		{
			String var = inToken.substring(VAR_PREFIX.length());
			if (pageContext.containsKey(var))
				return pageContext.get(var);
			else
				return inToken;
		}
		else
			return inToken;
	}

	/**
	 * Generates a table for some value stuff.
	 * @param writer the writer to write to.
	 * @param name the name of the table to generate.
	 */
	private static void generateTable(Writer writer, String name) throws IOException
	{
		writer.write("<!-- Start generated table for: " + name + " -->\n");
		writer.write("<table class=\"w3-table w3-striped w3-hoverable w3-border\">\n");
		
		ArithmeticOperator operator;
		if ("boolean".equalsIgnoreCase(name))
		{
			writer.write("<thead><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; BOOLEAN &gt; </td><td>"+Value.create(TEST_VALUES[i].asBoolean())+"</td></tr>\n");
			writer.write("</tbody>\n");
		}
		else if ("integer".equalsIgnoreCase(name))
		{
			writer.write("<thead><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; INTEGER &gt; </td><td>"+Value.create(TEST_VALUES[i].asLong())+"</td></tr>\n");
			writer.write("</tbody>\n");
		}
		else if ("float".equalsIgnoreCase(name))
		{
			writer.write("<thead><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; FLOAT &gt; </td><td>"+Value.create(TEST_VALUES[i].asDouble())+"</td></tr>\n");
			writer.write("</tbody>\n");
		}
		else if ("string".equalsIgnoreCase(name))
		{
			writer.write("<thead><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; STRING &gt; </td><td>"+Value.create(TEST_VALUES[i].asString())+"</td></tr>\n");
			writer.write("</tbody>\n");
		}
		else if ("length".equalsIgnoreCase(name))
		{
			writer.write("<thead><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; LENGTH &gt; </td><td>"+TEST_VALUES[i].length()+"</td></tr>\n");
			writer.write("</tbody>\n");
		}
		else if ("empty".equalsIgnoreCase(name))
		{
			writer.write("<thead><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; EMPTY &gt; </td><td>"+Value.create(TEST_VALUES[i].isEmpty())+"</td></tr>\n");
			writer.write("</tbody>\n");
		}
		else if ((operator = Reflect.createForType(name, ArithmeticOperator.class)) != null)
		{
			generateArithmeticTable(writer, operator);
		}
		else
		{
			out.println("PROBLEM!!!! "+name+" is not a table name.");
			writer.write("<tr><th>!!! "+name+" is not a table name !!!</th></tr>");
		}
		writer.write("</table>\n");
	}
	
	/**
	 * Generates an arithmetic table for a specific operator.
	 * @param writer the writer to write to.
	 * @param operator the operator to generate a table for.
	 */
	private static void generateArithmeticTable(Writer writer, ArithmeticOperator operator) throws IOException
	{
		if (operator.isBinary())
		{
			writer.write("<thead><tr><th>Value1</th><th>Operator</th><th>Value2</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			
			for (int i = 0; i < TEST_VALUES.length; i++)
				for (int j = 0; j < TEST_VALUES.length; j++)
					writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td>"+operator.getSymbol()+"</td><td>"+TEST_VALUES[j]+"</td><td>&#61;</td><td>"+operator.doOperation(TEST_VALUES[i], TEST_VALUES[j])+"</td></tr>\n");

			writer.write("</tbody>\n");
		}
		else
		{
			writer.write("<thead><tr><th>Operator</th><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+operator.getSymbol()+"</td><td>"+TEST_VALUES[i]+"</td><td>&#61;</td><td>"+operator.doOperation(TEST_VALUES[i])+"</td></tr>\n");

			writer.write("</tbody>\n");
		}
	}
	
	/**
	 * Interprets the contents of a parsed tag.
	 * @param outFile the base output path.
	 * @param tagContent the content of the tag.
	 * @param inPath the origin resource path.
	 * @param writer the output writer.
	 */
	private static boolean interpretTag(String outPath, File outFile, String tagContent, String inPath, Writer writer, HashMap<String, String> pageContext) throws IOException
	{
		String parentPath = inPath.substring(0, inPath.lastIndexOf('/') + 1);
		CommonTokenizer tokenizer = new CommonTokenizer(tagContent);

		if (tokenizer.isEmpty())
			return false;

		String command = tokenizer.nextToken();

		if (command.equalsIgnoreCase(COMMAND_SET))
		{
			String variableName = tokenizer.nextToken();
			String variableValue = resolveVariable(tokenizer.nextToken(), pageContext);
			pageContext.put(variableName, variableValue);
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_CLEAR))
		{
			String variableName = tokenizer.nextToken();
			pageContext.removeUsingKey(variableName);
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_PRINT))
		{
			String variableName = tokenizer.nextToken();
			writer.write(resolveVariable(variableName, pageContext));
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_INCLUDE))
		{
			String relativePath = resolveVariable(tokenizer.nextToken(), pageContext);
			parsePageResource(outPath, outFile, writer, parentPath + relativePath, pageContext);
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_GENERATE_TABLE))
		{
			String name = resolveVariable(tokenizer.nextToken(), pageContext);
			generateTable(writer, name);
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_TAMESCRIPT))
		{
			String headingName = resolveVariable(tokenizer.nextToken(), pageContext);
			String moduleName = resolveVariable(tokenizer.nextToken(), pageContext);
			String scriptPath = resolveVariable(tokenizer.nextToken(), pageContext);

			InputStream scriptIn = null;
			String scriptFile = RESOURCE_SCRIPTROOT + scriptPath;
			try {

				scriptIn = new FileInputStream(new File(scriptFile));
				String scriptContent = Common.getTextualContents(scriptIn);
				TAMEModule module = TAMEScriptReader.read(scriptFile, scriptContent, TAMESCRIPT_INCLUDER);

				writer.write("<div class=\"w3-example\">\n");
				writer.write("\t<button id=\"tame-"+moduleName+"-trace\" class=\"w3-button w3-red button-launch\" style=\"float:right;\" onclick=\"tameStartExample('"+headingName+" (Debug and Trace)', "+moduleName+", true, true)\"><i class=\"fa fa-bug\"></i></button>\n");
				writer.write("\t<button id=\"tame-"+moduleName+"-debug\" class=\"w3-button w3-yellow button-launch\" style=\"float:right;\" onclick=\"tameStartExample('"+headingName+" (Debug)', "+moduleName+", true)\"><i class=\"fa fa-bug\"></i></button>\n");
				writer.write("\t<button id=\"tame-"+moduleName+"\" class=\"w3-button w3-green button-launch\" style=\"float:right;\" onclick=\"tameStartExample('"+headingName+"', "+moduleName+")\">Play Example</button>\n");
				writer.write("\t<h4>"+headingName+"</h4>\n");
				writer.write("<pre class=\"tame-example\"><code data-language=\"tamescript\">\n");
				writer.write(scriptContent.replaceAll(">", "&gt;").replaceAll("<", "&lt;"));
				writer.write("</code></pre>\n");
				writer.write("</div>\n");

				String filePath = outPath + "/" + OUTPATH_JS_TAMEMODULE + moduleName + ".js";

				File jsFile = new File(filePath);
				if (Common.createPathForFile(jsFile))
					TAMEJSExporter.export(jsFile, module, new ModuleExporterOptions(moduleName));
				else
					out.println("!!! CANNOT EXPORT JS !!!");

				writer.write("<script type=\"text/javascript\" src=\"" + (OUTPATH_JS_TAMEMODULE + moduleName + ".js") + "\"></script>\n");
			} catch (TAMEScriptParseException e) {
				out.println("PROBLEM!!!! "+scriptFile+" not parsed!!");
				out.println(e.getClass().getSimpleName()+": "+e.getLocalizedMessage());
				writer.write("<div class=\"w3-card-4 w3-red\">");
				writer.write("<div class=\"w3-container w3-padding-4\">");
				writer.write("<h3>"+scriptFile+"</h3>");
				writer.write("<h5>"+e.getClass().getSimpleName()+"</h5>");
				writer.write("<pre>"+e.getLocalizedMessage()+"</pre>");
				writer.write("</div>");
				writer.write("</div>");
				return false;
			} catch (IOException e) {
				writer.write("<pre>!!! CAN'T FIND SCRIPT \""+scriptFile+"\" !!!</pre>");
				return false;
			} finally {
				Common.close(scriptIn);
			}

			return true;
		}

		return false;
	}

	/** Module Exporter Options. */
	private static class ModuleExporterOptions implements TAMEJSExporterOptions
	{
		private String moduleName;
		private ModuleExporterOptions(String moduleName)
		{
			this.moduleName = moduleName;
		}

		@Override
		public String getModuleVariableName()
		{
			return moduleName;
		}

		@Override
		public String getWrapperName()
		{
			return TAMEJSExporter.WRAPPER_MODULE;
		}

		@Override
		public boolean isPathOutputEnabled()
		{
			return false;
		}

	}

}