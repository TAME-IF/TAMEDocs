/*******************************************************************************
 * Copyright (c) 2017-2019 Matt Tropiano
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.blackrook.json.JSONObject;
import com.blackrook.json.JSONWriter;
import com.tameif.tame.TAMEModule;
import com.tameif.tame.factory.TAMEJSExporter;
import com.tameif.tame.factory.TAMEJSExporterOptions;
import com.tameif.tame.factory.TAMEScriptIncluder;
import com.tameif.tame.factory.TAMEScriptParseException;
import com.tameif.tame.factory.TAMEScriptReader;
import com.tameif.tame.lang.ArithmeticOperator;
import com.tameif.tame.lang.Value;
import com.tameif.tame.struct.CaseInsensitiveStringSet;
import com.tameif.tame.struct.FileUtils;
import com.tameif.tame.struct.IOUtils;

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
	/** Output directory for generated JS modules. */
	static final String OUTPATH_JS_TAMEMODULE = OUTPATH_JS + "modules/";
	/** Output directory for generated search index data. */
	static final String OUTPATH_JS_SEARCHINDEX = OUTPATH_JS + "searchindex.js";
	/** Output directory for missing page data. */
	static final String OUTPATH_JS_MISSINGPAGE = OUTPATH_JS + "missingpages.js";

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
	/** Environment Variable prefix. */
	static final String ENVVAR_PREFIX = "%";
	/** System Property Variable prefix. */
	static final String PROPVAR_PREFIX = "&";
	/** NOINDEX tag. */
	static final String TAG_NOINDEX = "<!--:NOINDEX:-->";

	/** Common English tokens. */
	static final CaseInsensitiveStringSet INDEX_COMMON_TOKENS = new CaseInsensitiveStringSet() {{
		BufferedReader br = null;
		try {
			br = IOUtils.openTextStream(IOUtils.openResource("com/tameif/tame/docs/resources/index-common.txt"));
			String s;
			while ((s = br.readLine()) != null)
				put(s.trim());
		} catch (IOException e) {
			throw new RuntimeException("Could not load common index token list!", e);
		} finally {
			IOUtils.close(br);
		}
	}};
	
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
		public String getIncludeResourcePath(String streamName, String path) throws IOException
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
		public String getModuleVariableName()
		{
			return null;
		}

		@Override
		public String getStartingPath() 
		{
			return "resource:tamejs/Engine.js";
		}

		@Override
		public PrintStream getVerboseStream()
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
		String outPath = removeEndingSequence(args[0], "/");
		File outDir = new File(outPath);
		if (!FileUtils.createPath(outDir.getPath()))
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

		Iterable<String[]> pageList = getPageList();
		
		// Scan existing pages.
		scanExistingPages(outPath, pageList);
		
		// Process pages.
		processAllPages(outPath, pageList);

		// Build local index.
		out.println("Indexing...");
		generateIndex(outPath, pageList);

		out.println("Done!");
	}
	
	private static void scanExistingPages(String outPath, Iterable<String[]> pageList)
	{
		JSONObject index = JSONObject.createEmptyArray();

		for (String[] page : pageList)
		{
			String parent = new File(RESOURCE_ROOT + "/" + page[2]).getParent();
			if (!(new File(parent + "/content/" + page[0]).exists()))
				index.append(JSONObject.create(page[0]));
		}
		
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileOutputStream(new File(outPath + "/" + OUTPATH_JS_MISSINGPAGE)), true);
			pw.println("var TAMEDOCS_MISSINGPAGES = ");
			JSONWriter.writeJSON(index, pw);
			pw.println(";");
		} catch (FileNotFoundException e) {
			out.println("Cannot open "+OUTPATH_JS_SEARCHINDEX+" for writing a search index!");
			e.printStackTrace(out);
		} catch (IOException e) {
			out.println("Cannot write to "+OUTPATH_JS_SEARCHINDEX+" for the search index!");
			e.printStackTrace(out);
		} finally {
			IOUtils.close(pw);
		}

	}

	private static void processAllPages(String outPath, Iterable<String[]> pageList) throws IOException
	{
		for (String[] page : pageList)
		{
			boolean error = false;
			File outFile = new File(outPath + "/" + page[0]);
			if (!FileUtils.createPathForFile(outFile))
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
				pw = new PrintWriter(outFile);
				parsePageResource(outPath, outFile, pw, RESOURCE_ROOT + "/" + template, variables);
			} catch (SecurityException e) {
				out.println("ERROR: Could not write file "+outFile.getPath()+". Access denied.");
				error = true;
			} catch (IOException e) {
				out.println("ERROR: Could not write file "+outFile.getPath()+". "+e.getLocalizedMessage());
				error = true;
			} finally {
				IOUtils.close(pw);
				if (error)
					outFile.delete();
			}
		}
	}

	private static boolean generateIndex(String outPath, Iterable<String[]> pageList)
	{
		CountMap<String> tokenToAppearances = new CountMap<>();
		HashMap<String, CountMap<String>> tokenToFileAppearances = new HashMap<>();
		HashMap<String, Set<String>> partialToTokens = new HashMap<>();
		
		// scan HTML files in output folder.
		File outputDirectory = new File(outPath);
		for (File file : outputDirectory.listFiles((f)->{
			String fn = f.getName(); 
			return fn.length() > 5 && fn.substring(fn.length() - 5, fn.length()).equalsIgnoreCase(".html");
		}))
		{
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				generateIndexScanForFile(file.getName(), reader, tokenToAppearances, tokenToFileAppearances, partialToTokens);
			} catch (IOException e) {
				out.println("Cannot open "+file.getPath()+" for indexing!");
				e.printStackTrace(out);
			} finally {
				IOUtils.close(reader);
			}
		}

		HashMap<String, HashMap<String, Float>> tokenToFileDensity = new HashMap<>();
		for (Map.Entry<String, CountMap<String>> pair : tokenToFileAppearances.entrySet())
		{
			String token = pair.getKey();
			
			HashMap<String, Float> map;
			if ((map = tokenToFileDensity.get(token)) == null)
				tokenToFileDensity.put(token, map = new HashMap<>());
			
			for (Map.Entry<String, Integer> c : pair.getValue().entrySet())
			{
				String file = c.getKey();
				float appearances = (float)c.getValue();
				map.put(file, appearances / tokenToAppearances.amount(token));
			}
		}
		
		HashMap<String, String> fileToTitle = new HashMap<>();
		for (String[] page : pageList)
			fileToTitle.put(page[0], page[1]);
		
		JSONObject index = JSONObject.createEmptyObject();
		index.addMember("partials", partialToTokens);
		index.addMember("pages", fileToTitle);
		index.addMember("tokenDensity", tokenToFileDensity);
		
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileOutputStream(new File(outPath + "/" + OUTPATH_JS_SEARCHINDEX)), true);
			pw.println("var TAMEDOCS_SEARCH_INDEX = ");
			JSONWriter.writeJSON(index, pw);
			pw.println(";");
		} catch (FileNotFoundException e) {
			out.println("Cannot open "+OUTPATH_JS_SEARCHINDEX+" for writing a search index!");
			e.printStackTrace(out);
		} catch (IOException e) {
			out.println("Cannot write to "+OUTPATH_JS_SEARCHINDEX+" for the search index!");
			e.printStackTrace(out);
		} finally {
			IOUtils.close(pw);
		}
		
		return true;
	}

	// This relies on HTML being well-formed!
	private static void generateIndexScanForFile(
		String fileName, 
		Reader reader, 
		CountMap<String> tokenToAppearances, 
		HashMap<String, CountMap<String>> tokenToFile, 
		HashMap<String, Set<String>> partialToTokens
	) throws IOException 
	{
		StringBuilder token = new StringBuilder();
		StringBuilder tag = new StringBuilder();
		
		final int STATE_INIT = 0;
		final int STATE_WHITESPACE = 1;
		final int STATE_TOKEN = 2;
		final int STATE_MAYBE_NOINDEX = 3;
		final int STATE_MAYBE_REINDEX = 4;
		final int STATE_HTML = 5;
		final int STATE_ENTITY = 6;
		final int STATE_NOINDEX = 7;
		int state = STATE_INIT;
		
		int r;
		while ((r = reader.read()) != -1)
		{
			char c = (char)r;
			
			switch (state)
			{
				case STATE_INIT:
				{
					if (Character.isWhitespace(c))
						state = STATE_WHITESPACE;
					else if (c == '<')
					{
						state = STATE_MAYBE_NOINDEX;
						tag.delete(0, tag.length());
						tag.append('<');
					}
					else if (c == '&')
						state = STATE_ENTITY;
					else if (Character.isAlphabetic(c) || Character.isIdeographic(c))
					{
						token.append(c);
						state = STATE_TOKEN;
					}
				}
				break;
				
				case STATE_WHITESPACE:
				{
					if (Character.isAlphabetic(c) || Character.isIdeographic(c))
					{
						token.append(c);
						state = STATE_TOKEN;
					}
					else if (c == '<')
					{
						state = STATE_MAYBE_NOINDEX;
						tag.delete(0, tag.length());
						tag.append('<');
					}
					else if (c == '&')
						state = STATE_ENTITY;
				}
				break;

				case STATE_TOKEN:
				{
					if (c != '-' && !Character.isAlphabetic(c) && !Character.isIdeographic(c))
					{
						// Flush token.
						generateIndexScanForToken(fileName, token.toString(), tokenToAppearances, tokenToFile, partialToTokens);
						token.delete(0, token.length());

						if (Character.isWhitespace(c))
							state = STATE_WHITESPACE;
						else if (c == '<')
						{
							state = STATE_MAYBE_NOINDEX;
							tag.delete(0, tag.length());
							tag.append('<');
						}
						else if (c == '&')
							state = STATE_ENTITY;
						else
							state = STATE_INIT;
					}
					else
						token.append(c);
				}
				break;
				
				case STATE_MAYBE_NOINDEX:
				{
					if (c == TAG_NOINDEX.charAt(tag.length()))
					{
						tag.append(c);
						if (tag.length() == TAG_NOINDEX.length())
							state = STATE_NOINDEX;
					}
					else if (c == '>')
						state = STATE_INIT;
					else
						state = STATE_HTML;
				}
				break;

				case STATE_NOINDEX:
				{
					if (c == '<')
					{
						state = STATE_MAYBE_REINDEX;
						tag.delete(0, tag.length());
						tag.append('<');
					}
				}
				break;

				case STATE_MAYBE_REINDEX:
				{
					if (c == TAG_NOINDEX.charAt(tag.length()))
					{
						tag.append(c);
						if (tag.length() == TAG_NOINDEX.length())
							state = STATE_INIT;
					}
					else
						state = STATE_NOINDEX;
				}
				break;

				case STATE_HTML:
				{
					if (c == '>')
						state = STATE_INIT;
				}
				break;

				case STATE_ENTITY:
				{
					if (c == ';')
						state = STATE_INIT;
				}
				break;
			}
			
		}
		
		// Flush token.
		if (token.length() > 0)
		{
			generateIndexScanForToken(fileName, token.toString(), tokenToAppearances, tokenToFile, partialToTokens);
			token.delete(0, token.length());
		}
	}

	private static void generateIndexScanForToken(
		String fileName, 
		String token, 
		CountMap<String> tokenToAppearances, 
		HashMap<String, CountMap<String>> tokenToFile, 
		HashMap<String, Set<String>> partialToTokens
	)
	{
		if (token.length() < 3)
			return;
		if (INDEX_COMMON_TOKENS.contains(token))
			return;
		
		token = token.toLowerCase();
		
		boolean makePartials = tokenToAppearances.amount(token) <= 0;
		tokenToAppearances.give(token, 1);
		
		CountMap<String> map;
		if ((map = tokenToFile.get(token)) == null)
			tokenToFile.put(token, (map = new CountMap<>()));
		map.give(fileName, 1);
	
		// partials, min 3 characters.
		if (makePartials)
		{
			for (int x = 3; x < token.length(); x++)
			{
				/*
				for (int i = 0; i + x <= token.length(); i++)
				{
					String partial = token.substring(i, i + x);
					partialToTokens.add(partial, token);
				}
				*/
				
				String partial = token.substring(0, x);
				Set<String> tokenSet;
				if ((tokenSet = partialToTokens.get(partial)) == null)
					partialToTokens.put(partial, tokenSet = new HashSet<String>());
				tokenSet.add(token);
			}
		}
	}

	private static boolean exportEngine(String outPath) throws IOException
	{
		File outFile = new File(outPath + "/" + OUTPATH_JS_TAMEENGINE);
		if (!FileUtils.createPathForFile(outFile))
		{
			out.println("ERROR: Could not create path for "+outFile.getPath());
			return false;
		}

		TAMEJSExporter.export(outFile, null, TAMESCRIPT_JSEXPORTER_OPTIONS_ENGINE);
		return true;
	}

	private static void copyStaticPages(File outDir)
	{
		for (File inFile : explodeFiles(new File(SOURCE_SIDEASSETS)))
		{
			File outFile = new File(outDir.getPath() + "/" + removeStartingSequence(inFile.getPath().replaceAll("\\\\", "/"), SOURCE_SIDEASSETS));
			if (!FileUtils.createPathForFile(outFile))
			{
				out.println("ERROR: Could not create path for "+outFile.getPath());
				continue;
			}

			FileInputStream fis = null;
			FileOutputStream fos = null;
			try {
				fis = new FileInputStream(inFile);
				fos = new FileOutputStream(outFile);
				IOUtils.relay(fis, fos, 8192);
			} catch (SecurityException e) {
				out.printf("ERROR: Could not copy \"%s\" to \"%s\". Access denied.\n", inFile.getPath(), outFile.getPath());
			} catch (IOException e) {
				out.printf("ERROR: Could not copy \"%s\" to \"%s\"\n", inFile.getPath(), outFile.getPath());
			} finally {
				IOUtils.close(fis);
				IOUtils.close(fos);
			}
		}
	}

	private static Iterable<String[]> getPageList() throws IOException
	{
		Queue<String[]> out = new LinkedList<>();
		InputStream in = null;
		try {
			String line = null;
			BufferedReader pageReader = IOUtils.openTextStream(in = new FileInputStream(new File(RESOURCE_PAGESLIST)));
			while ((line = pageReader.readLine()) != null)
			{
				CommonTokenizer ct = new CommonTokenizer(line);
				String[] linedata = new String[3];
				for (int i = 0; i < linedata.length; i++)
					linedata[i] = ct.nextToken();
				out.add(linedata);
			}
		} finally {
			IOUtils.close(in);
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
			reader = new InputStreamReader(in);
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
			IOUtils.close(reader);
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
		else if (inToken.startsWith(ENVVAR_PREFIX))
		{
			String var = inToken.substring(ENVVAR_PREFIX.length());
			return System.getenv(var);
		}
		else if (inToken.startsWith(PROPVAR_PREFIX))
		{
			String var = inToken.substring(PROPVAR_PREFIX.length());
			return System.getProperty(var, inToken);
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
		writer.write(TAG_NOINDEX+"\n");
		writer.write("<div class=\"tamedocs-table-overflow\">\n");

		ArithmeticOperator operator;
		if ("boolean".equalsIgnoreCase(name))
		{
			writer.write("<table class=\"tamedocs-table-values w3-table w3-striped w3-hoverable\">\n");
			writer.write("<thead class=\"w3-teal\"><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; BOOLEAN &gt; </td><td>"+Value.create(TEST_VALUES[i].asBoolean())+"</td></tr>\n");
			writer.write("</tbody>\n");
			writer.write("</table>\n");
		}
		else if ("integer".equalsIgnoreCase(name))
		{
			writer.write("<table class=\"tamedocs-table-values w3-table w3-striped w3-hoverable\">\n");
			writer.write("<thead class=\"w3-teal\"><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; INTEGER &gt; </td><td>"+Value.create(TEST_VALUES[i].asLong())+"</td></tr>\n");
			writer.write("</tbody>\n");
			writer.write("</table>\n");
		}
		else if ("float".equalsIgnoreCase(name))
		{
			writer.write("<table class=\"tamedocs-table-values w3-table w3-striped w3-hoverable\">\n");
			writer.write("<thead class=\"w3-teal\"><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; FLOAT &gt; </td><td>"+Value.create(TEST_VALUES[i].asDouble())+"</td></tr>\n");
			writer.write("</tbody>\n");
			writer.write("</table>\n");
		}
		else if ("string".equalsIgnoreCase(name))
		{
			writer.write("<table class=\"tamedocs-table-values w3-table w3-striped w3-hoverable\">\n");
			writer.write("<thead class=\"w3-teal\"><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; STRING &gt; </td><td>"+Value.create(TEST_VALUES[i].asString())+"</td></tr>\n");
			writer.write("</tbody>\n");
			writer.write("</table>\n");
		}
		else if ("length".equalsIgnoreCase(name))
		{
			writer.write("<table class=\"tamedocs-table-values w3-table w3-striped w3-hoverable\">\n");
			writer.write("<thead class=\"w3-teal\"><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; LENGTH &gt; </td><td>"+Value.create(TEST_VALUES[i].length())+"</td></tr>\n");
			writer.write("</tbody>\n");
			writer.write("</table>\n");
		}
		else if ("empty".equalsIgnoreCase(name))
		{
			writer.write("<table class=\"tamedocs-table-values w3-table w3-striped w3-hoverable\">\n");
			writer.write("<thead class=\"w3-teal\"><tr><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td> &gt; EMPTY &gt; </td><td>"+Value.create(TEST_VALUES[i].isEmpty())+"</td></tr>\n");
			writer.write("</tbody>\n");
			writer.write("</table>\n");
		}
		else if ((operator = Enum.valueOf(ArithmeticOperator.class, name.toUpperCase())) != null)
		{
			generateArithmeticTable(writer, operator);
		}
		else
		{
			writer.write("<table class=\"w3-table w3-striped w3-hoverable\">\n");
			out.println("PROBLEM!!!! "+name+" is not a table name.");
			writer.write("<tr><th>!!! "+name+" is not a table name !!!</th></tr>");
			writer.write("</table>\n");
		}
		writer.write("</table>\n");
		writer.write("</div>\n");
		writer.write(TAG_NOINDEX+"\n");
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
			writer.write("<table class=\"tamedocs-table-binary w3-table w3-striped w3-hoverable\">\n");
			writer.write("<thead class=\"w3-teal\"><tr><th>Value1</th><th>Operator</th><th>Value2</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			
			for (int i = 0; i < TEST_VALUES.length; i++)
				for (int j = 0; j < TEST_VALUES.length; j++)
					writer.write("<tr><td>"+TEST_VALUES[i]+"</td><td>"+operator.getSymbol()+"</td><td>"+TEST_VALUES[j]+"</td><td>&#61;</td><td>"+operator.doOperation(TEST_VALUES[i], TEST_VALUES[j])+"</td></tr>\n");

			writer.write("</tbody>\n");
			writer.write("</table>\n");
		}
		else
		{
			writer.write("<table class=\"tamedocs-table-unary w3-table w3-striped w3-hoverable\">\n");
			writer.write("<thead class=\"w3-teal\"><tr><th>Operator</th><th>Value</th><th>&nbsp;</th><th>Result</th></tr></thead>\n");
			writer.write("<tbody>\n");
			
			for (int i = 0; i < TEST_VALUES.length; i++)
				writer.write("<tr><td>"+operator.getSymbol()+"</td><td>"+TEST_VALUES[i]+"</td><td>&#61;</td><td>"+operator.doOperation(TEST_VALUES[i])+"</td></tr>\n");

			writer.write("</tbody>\n");
			writer.write("</table>\n");
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
			pageContext.remove(variableName);
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
			writer.write("\n"+TAG_NOINDEX+"\n");			
			try {

				scriptIn = new FileInputStream(new File(scriptFile));
				String scriptContent = getTextualContents(scriptIn);
				TAMEModule module = TAMEScriptReader.read(scriptFile, scriptContent, TAMESCRIPT_INCLUDER);
				writer.write("<div class=\"w3-example\">\n");
				
				writer.write("\t<div class=\"w3-right\" style=\"margin-top:12px\">");
				writer.write("<button class=\"w3-hide-small w3-button w3-green button-launch\" onclick=\"tameStartExample('"+headingName+"', "+moduleName+")\">Play Example</button>");
				writer.write("<button class=\"w3-hide-medium w3-hide-large w3-hide-xlarge w3-button w3-green button-launch\" onclick=\"tameStartExample('"+headingName+"', "+moduleName+")\">&#9654;</button>");
				writer.write("<button class=\"w3-button w3-yellow button-launch\" onclick=\"tameStartExample('"+headingName+" (Debug)', "+moduleName+", true)\"><i class=\"fa fa-bug\"></i></button>");
				writer.write("<button class=\"w3-button w3-red button-launch\" onclick=\"tameStartExample('"+headingName+" (Debug and Trace)', "+moduleName+", true, true)\"><i class=\"fa fa-bug\"></i></button>");
				writer.write("<button class=\"w3-button w3-blue button-launch\" onclick=\"tamedocsClipboard('code-"+moduleName+"')\"><i class=\"fa fa-clipboard\"></i></button>");
				writer.write("</div>\n");

				writer.write("\t<h3>"+headingName+"</h3>\n");
				writer.write("<pre class=\"tame-example example-scroll\"><code data-language=\"tamescript\" id=\"code-"+moduleName+"\">\n");
				writer.write(scriptContent.replaceAll(">", "&gt;").replaceAll("<", "&lt;"));
				writer.write("</code></pre>\n");
				writer.write("</div>\n");

				String filePath = outPath + "/" + OUTPATH_JS_TAMEMODULE + moduleName + ".js";

				File jsFile = new File(filePath);
				if (FileUtils.createPathForFile(jsFile))
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
				writer.write("<pre style=\"white-space: pre-line;\">"+e.getLocalizedMessage()+"</pre>");
				writer.write("</div>");
				writer.write("</div>");
				return false;
			} catch (IOException e) {
				writer.write("<pre style=\"white-space: pre-line;\">!!! CAN'T FIND SCRIPT \""+scriptFile+"\" !!!</pre>\n");
				return false;
			} finally {
				IOUtils.close(scriptIn);
			}
			writer.write("\n"+TAG_NOINDEX+"\n");			
			return true;
		}

		return false;
	}

	/**
	 * Retrieves the textual contents of a stream in the system's current encoding.
	 * @param in	the input stream to use.
	 * @return		a contiguous string (including newline characters) of the stream's contents.
	 * @throws IOException	if the read cannot be done.
	 */
	private static String getTextualContents(InputStream in) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = br.readLine()) != null)
		{
			sb.append(line);
			sb.append('\n');
		}
		br.close();
		return sb.toString();
	}

	/**
	 * Removes a character sequence from the start of a string if it starts with the sequence.
	 * @param input the input string to change.
	 * @param sequence the sequence to potentially remove.
	 * @return a modified string or the input string if no modification.
	 */
	private static String removeStartingSequence(String input, String sequence)
	{
		if (input.startsWith(sequence))
			return input.substring(sequence.length());
		else
			return input;
	}

	/**
	 * Removes a character sequence from the end of a string if it exists.
	 * @param input the input string to change.
	 * @param sequence the sequence to potentially remove.
	 * @return a modified string or the input string if no modification.
	 */
	private static String removeEndingSequence(String input, String sequence)
	{
		if (input.endsWith(sequence))
			return input.substring(0, input.length() - sequence.length());
		else
			return input;
	}

	/**
	 * Explodes a list of files into a larger list of files,
	 * such that all of the files in the resultant list are not
	 * directories, by traversing directory paths.
	 *
	 * The returned list is not guaranteed to be in any order
	 * related to the input list, and may contain files that are
	 * in the input list if they are not directories.
	 *
	 * @param files	the list of files to expand.
	 * @return	a list of all files found in the subdirectory search.
	 * @throws	NullPointerException if files is null.
	 */
	private static File[] explodeFiles(File ... files)
	{
		Queue<File> fileQueue = new LinkedList<File>();
		List<File> fileList = new ArrayList<File>();
	
		for (File f : files)
			fileQueue.add(f);
	
		while (!fileQueue.isEmpty())
		{
			File dequeuedFile = fileQueue.poll();
			if (dequeuedFile.isDirectory())
			{
				for (File f : dequeuedFile.listFiles())
					fileQueue.add(f);
			}
			else
			{
				fileList.add(dequeuedFile);
			}
		}
	
		File[] out = new File[fileList.size()];
		fileList.toArray(out);
		return out;
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
		public String getStartingPath()
		{
			return "resource:tamejs/ModuleData.js";
		}

		@Override
		public PrintStream getVerboseStream()
		{
			return null;
		}
		
	}
	
}
