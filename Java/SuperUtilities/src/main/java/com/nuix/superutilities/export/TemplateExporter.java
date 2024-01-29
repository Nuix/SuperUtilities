package com.nuix.superutilities.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.google.common.base.Joiner;

import nuix.Item;

/***
 * Experimental class attempting to integrate Ruby ERB templates with Java by using JRuby to
 * compile a Ruby method which is capable of rendering an ERB and injecting it with Java data.
 * @author Jason Wells
 *
 */
public class TemplateExporter {
	private static Logger logger = Logger.getLogger(TemplateExporter.class);
	
	private ScriptingContainer container = null;
	
	public TemplateExporter(File erbTemplateFile) throws Exception {
		container = new ScriptingContainer(LocalContextScope.SINGLETHREAD,LocalVariableBehavior.TRANSIENT);
		String erbTemplateSource = null;
		List<String> lines = Files.readAllLines(Paths.get(erbTemplateFile.getPath()));
		erbTemplateSource = Joiner.on("\n").join(lines);
		String scriptlet = 
				"#coding: UTF-8\n"+
				"require 'erb'\n"+
				"include ERB::Util\n" +
				"@compiled_erb = ERB.new(@template_source,0,'<>')\n"+
				"def render(item,data)\n"+
				"   b = binding\n"+
				"	return @compiled_erb.result(b)\n"+
				"end\n";
		
		container.put("@template_source", erbTemplateSource);
		container.runScriptlet(scriptlet);
	}
	
	public String render(Item item){
		return (String) container.callMethod("", "render", new Object[]{item});
	}
	
	public String render(Item item, Map<String,Object> data){
		Map<String,Object> toSend = new HashMap<String,Object>();
		for (Map.Entry<String, Object> entry : data.entrySet()){
			toSend.put(entry.getKey(), entry.getValue());
		}
		return (String) container.callMethod("", "render", new Object[]{item,toSend});
	}
	
	public void renderToFile(Item item, File outputFile, Map<String,Object> data) throws Exception {
		FileOutputStream fos = null;
		BufferedWriter bw = null;
		
		outputFile.getParentFile().mkdirs();
		fos = new FileOutputStream(outputFile);
		bw = new BufferedWriter(new OutputStreamWriter(fos));
		bw.write(render(item,data));
		bw.close();
	}
	
	public void renderToFile(Item item, String outputFile, Map<String,Object> data) throws Exception{
		renderToFile(item,new File(outputFile),data);
	}
	
	public void renderToPdf(Item item, File outputFile, Map<String,Object> data) throws Exception{
		String resolvedTemplate = render(item,data);
		htmlToPdf(resolvedTemplate,outputFile);
	}
		
	public static void htmlToPdf(String htmlSource, File outputPdfPath)
	{
	    try {
	        OutputStream out = new FileOutputStream(outputPdfPath);

	        ITextRenderer renderer = new ITextRenderer();

	        renderer.setDocumentFromString(htmlSource);
	        renderer.layout();
	        renderer.createPDF(out);

	        out.close();
	    } catch (Exception e) {
	        logger.error("Error while converting HTML to PDF", e);
	    }
	}
	
	public static void htmlToPdf(String htmlSource, String outputPdfPath){
		htmlToPdf(htmlSource,new File(outputPdfPath));
	}
}
