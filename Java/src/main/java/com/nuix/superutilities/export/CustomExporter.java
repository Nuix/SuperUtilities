package com.nuix.superutilities.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import com.nuix.superutilities.SuperUtilities;
import com.nuix.superutilities.loadfiles.DatLoadFile;
import com.nuix.superutilities.loadfiles.DatLoadFileReader;
import com.nuix.superutilities.loadfiles.DatLoadFileWriter;
import com.nuix.superutilities.loadfiles.OptLoadFile;
import com.nuix.superutilities.loadfiles.OptRecord;
import com.nuix.superutilities.loadfiles.SimpleTextFileWriter;
import com.nuix.superutilities.misc.FormatUtility;
import com.nuix.superutilities.misc.PlaceholderResolver;

import nuix.BatchExporter;
import nuix.Case;
import nuix.Item;
import nuix.ItemEventCallback;
import nuix.ItemEventInfo;
import nuix.ItemExpression;
import nuix.MetadataItem;
import nuix.MetadataProfile;
import nuix.Utilities;

public class CustomExporter {
	private MetadataProfile profile = null;
	
	private SimpleTextFileWriter generalLog = null;
	private SimpleTextFileWriter errorLog = null;
	
	private boolean exportText = false;
	private Map<String,Object> textExportSettings = new HashMap<String,Object>();
	private String textFileNameTemplate = "{export_directory}\\TEXT\\{guid}.txt";
	
	private boolean exportNatives = false;
	private Map<String,Object> emailExportSettings = new HashMap<String,Object>();
	private String nativeFileNameTemplate = "{export_directory}\\NATIVES\\{guid}.{extension}";
	
	private boolean exportPdfs = false;
	private Map<String,Object> pdfExportSettings = new HashMap<String,Object>();
	private String pdfFileNameTemplate = "{export_directory}\\PDF\\{guid}.pdf";
	
	private boolean exportTiffs = false;
	private Map<String,Object> tiffExportSettings = new HashMap<String,Object>();
	private String tiffFileNameTemplate = "{export_directory}\\IMAGE\\{guid}.{extension}";
	
	private boolean exportJson = false;
	private String jsonFileNameTemplate = "{export_directory}\\JSON\\{guid}.json";
	private JsonExporter jsonExporter = null;
	
	private Map<String,String> headerRenames = new HashMap<String,String>();
	
	public CustomExporter() {}
	
	/***
	 * Gets the metadata profile which will be used during export.  May be null.
	 * @return The metadata profile used during export.
	 */
	public MetadataProfile getProfile() {
		return profile;
	}

	/***
	 * Sets the metadata profile used during export.  May be null, in which case a metadata profile
	 * containing only the column "GUID" will be created.  Note that if a metadata profile is provided
	 * that does not contain the column "GUID", then a copy of the submit metadata profile is made and
	 * the "GUID" column is added to it.  This is done because the GUID is needed during the restructuring
	 * phase to find the relevant item in the case corresponding to a given record in the temporary export's
	 * Concordance DAT file.
	 * @param profile
	 */
	public void setProfile(MetadataProfile profile) {
		this.profile = profile;
	}
	
	public void exportNatives(String fileNameTemplate, Map<String,Object> emailExportSettings) {
		exportNatives = true;
		this.emailExportSettings = emailExportSettings;
		nativeFileNameTemplate = fileNameTemplate;
	}
	
	public void exportText(String fileNameTemplate, Map<String,Object> textExportSettings) {
		exportText = true;
		this.textExportSettings = textExportSettings;
		textFileNameTemplate = fileNameTemplate;
	}
	
	public void exportPdfs(String fileNameTemplate, Map<String,Object> pdfExportSettings) {
		exportPdfs = true;
		this.pdfExportSettings = pdfExportSettings;
		pdfFileNameTemplate = fileNameTemplate;
	}
	
	public void exportTiffs(String fileNameTemplate, Map<String,Object> tiffExportSettings) {
		exportTiffs = true;
		this.tiffExportSettings = tiffExportSettings;
		tiffFileNameTemplate = fileNameTemplate;
	}
	
	public void exportJson(String fileNameTemplate) {
		exportJson = true;
		jsonFileNameTemplate = fileNameTemplate;
		jsonExporter = new JsonExporter();
	}
	
	public void exportJson(String fileNameTemplate, JsonExporter jsonExporter) {
		exportJson = true;
		jsonFileNameTemplate = fileNameTemplate;
		this.jsonExporter = jsonExporter;
	}
	
	public void setHeaderRenames(Map<String,String> renames) {
		this.headerRenames = renames;
	}
	
	private void logInfo(String format, Object... params) {
		String message = String.format(format, params);
		System.out.println(message);
		if(generalLog != null) {
			generalLog.writeLine(message);
		}
	}
	
	private void logError(String format, Object... params) {
		String message = String.format(format, params);
		System.out.println(message);
		if(generalLog != null) {
			generalLog.writeLine(message);
		}
		
		if(errorLog != null) {
			errorLog.writeLine(message);
		}
	}
	
	/***
	 * Ensures we have a metadata profile suitable for our needs.  Will either add GUID to provided metadata profile
	 * if it does not already contain that field or it will create a new metadata profile containing only the column GUID
	 * if the profile is null.
	 * @param profile The profile currently specified to be used for export.
	 * @return The provided metadata profile or a copy of the provided metadata profile with GUID field added or a new profile with only GUID field.
	 */
	private MetadataProfile ensureProfileWithGuid(MetadataProfile profile) {
		Utilities util = SuperUtilities.getInstance().getNuixUtilities();
		MetadataProfile exportProfile = profile;
		if(exportProfile == null) {
			exportProfile =  util.getMetadataProfileStore().createMetadataProfile();
			exportProfile = exportProfile.addMetadata("GUID", new ItemExpression<String>() {
				@Override
				public String evaluate(Item item) { return item.getGuid();	}
			});
		} else {
			boolean hasGuid = false;
			for(MetadataItem m : exportProfile.getMetadata()) {
				if(m.getName().contentEquals("GUID")) {
					hasGuid = true;
					break;
				}
			}
			if(!hasGuid) {
				exportProfile = exportProfile.addMetadata("GUID", new ItemExpression<String>() {
					@Override
					public String evaluate(Item item) { return item.getGuid();	}
				});
			}
		}
		return exportProfile;
	}
	
	/***
	 * Creates a relative path.
	 * @param base The base directory to remove.  Ex: C:\Exports\Eport001
	 * @param absolute The full path to make relative to the provided base path.  Ex: C:\Exports\Eport001\TEXT\FILE001.TXT 
	 * @return The absolute path made relative.  Ex: TEXT\FILE001.TXT
	 */
	private String getRelativePath(File base, File absolute) {
		Pattern basePattern = Pattern.compile("\\Q"+base.getAbsolutePath()+"\\\\E");
		return basePattern.matcher(absolute.getAbsolutePath()).replaceFirst("");
	}
	
	private File resolveNameCollisions(File intendedDest) {
		File result = intendedDest;
		int suffix = 0;
		File directory = intendedDest.getParentFile();
		while(result.exists()) {
			suffix++;
			String namePart = FilenameUtils.getBaseName(intendedDest.getAbsolutePath());
			String extension = FilenameUtils.getExtension(intendedDest.getAbsolutePath());
			result = new File(directory,namePart+"_"+suffix+"."+extension);
		}
		return result;
	}
	
	/***
	 * Exports given items in custom structure.  This is accomplished by first performing a temporary "legal export" using
	 * the API object BatchExporter.  Once that export is complete, files are restructured into final format.  Load file paths are
	 * updated to reflect restructuring.
	 * @param nuixCase The relevant Nuix case, needed to resolve GUIDs in temp export DAT file to actual items.
	 * @param exportDirectory Where the final export should reside.
	 * @param items
	 * @throws IOException
	 */
	public void exportItems(Case nuixCase, String exportDirectory, List<Item> items) throws IOException {
		exportItems(nuixCase, new File(exportDirectory),items);
	}
	
	/***
	 * Exports given items in custom structure.  This is accomplished by first performing a temporary "legal export" using
	 * the API object BatchExporter.  Once that export is complete, files are restructured into final format.  Load file paths are
	 * updated to reflect restructuring.
	 * @param nuixCase The relevant Nuix case, needed to resolve GUIDs in temp export DAT file to actual items.
	 * @param exportDirectory Where the final export should reside.
	 * @param items
	 * @throws IOException
	 */
	public void exportItems(Case nuixCase, File exportDirectory, List<Item> items) throws IOException {
		Utilities util = SuperUtilities.getInstance().getNuixUtilities();
		File exportTempDirectory = new File(exportDirectory,"_TEMP_");
		BatchExporter exporter = util.createBatchExporter(exportTempDirectory);
		MetadataProfile exportProfile = ensureProfileWithGuid(profile);
		
		Map<String,Object> loadfileSettings = new HashMap<String,Object>();
		loadfileSettings.put("metadataProfile", exportProfile);
		exporter.addLoadFile("concordance", loadfileSettings);
		
		if(exportText) {
			Map<String,Object> productSettings = new HashMap<String,Object>();
			productSettings.putAll(textExportSettings);
			productSettings.put("naming", "guid");
			productSettings.put("path", "TEXT");
			logInfo("Configuring BatchExporter for Text Export:\n%s",FormatUtility.debugString(productSettings));
			exporter.addProduct("text", productSettings);
		}
		
		if(exportNatives) {
			Map<String,Object> productSettings = new HashMap<String,Object>();
			productSettings.putAll(emailExportSettings);
			productSettings.put("naming", "guid");
			productSettings.put("path", "NATIVE");
			logInfo("Configuring BatchExporter for Natives Export:\n%s",FormatUtility.debugString(productSettings));
			exporter.addProduct("native", productSettings);
		}
		
		if(exportPdfs) {
			Map<String,Object> productSettings = new HashMap<String,Object>();
			productSettings.putAll(pdfExportSettings);
			productSettings.put("naming", "guid");
			productSettings.put("path", "PDF");
			logInfo("Configuring BatchExporter for PDF Export:\n%s",FormatUtility.debugString(productSettings));
			exporter.addProduct("pdf", productSettings);
		}
		
		if(exportTiffs) {
			Map<String,Object> productSettings = new HashMap<String,Object>();
			productSettings.putAll(tiffExportSettings);
			productSettings.put("naming", "guid");
			productSettings.put("path", "IMAGE");
			logInfo("Configuring BatchExporter for TIFF Export:\n%s",FormatUtility.debugString(productSettings));
			exporter.addProduct("tiff", productSettings);
		}
		
		exportDirectory.mkdirs();
		File tempDatFile = new File(exportTempDirectory,"loadfile.dat");
		File finalDatFile = new File(exportDirectory,"loadfile.dat");
		File generalLogFile = new File(exportDirectory,"CustomExporter.log");
		File errorLogFile = new File(exportDirectory,"CustomExporterErrors.log");
		
		try {
			generalLog = new SimpleTextFileWriter(generalLogFile);
			errorLog = new SimpleTextFileWriter(errorLogFile);
			
			exporter.whenItemEventOccurs(new ItemEventCallback() {
				long batchExportStartMillis = System.currentTimeMillis();
				@Override
				public void itemProcessed(ItemEventInfo info) {
					long diffMillis = System.currentTimeMillis() - batchExportStartMillis;
					if(diffMillis > 2 * 1000) {
						logInfo("Batch Exporter | %s: %s",info.getStage(),info.getStageCount());
						batchExportStartMillis = System.currentTimeMillis();
					}
					
					// Log error reported by BatchExporter
					if(info.getFailure() != null) {
						logError("BatchExporter reports error while exporting item with GUID '%s':\n%s",
								info.getItem().getGuid(), FormatUtility.debugString(info.getFailure()));
					}
				}
			});
			
			logInfo("Beginning temp export using BatchExporter...");
			exporter.exportItems(items);
			logInfo("Finished temp export using BatchExporter");
			
			/* BEGIN FIXING UP INTO NEW STRUCTURE */
					
			// Used to resolve naming templates to final pathing structure
			PlaceholderResolver resolver = new PlaceholderResolver();
			
			// Tracks old relative path and new relative path so that OPT file can be updated
			Map<String,String> tiffRenames = new HashMap<String,String>();
			
			logInfo("Restructuring export using %s as input, writing to %s as output...",tempDatFile.getAbsolutePath(),finalDatFile.getAbsolutePath());
			try(DatLoadFileWriter datWriter = new DatLoadFileWriter(finalDatFile)){
				DatLoadFileReader.withEachRecord(tempDatFile, new Consumer<LinkedHashMap<String,String>>() {
					boolean headersWrittern = false;
					long restructureStartMillis = System.currentTimeMillis();
					int recordsProcessed = 0;
					
					@Override
					public void accept(LinkedHashMap<String, String> record) {
						// Periodically logs progress
						long diffMillis = System.currentTimeMillis() - restructureStartMillis;
						if(diffMillis > 2 * 1000 || recordsProcessed % 100 == 0) {
							logInfo("Export Restructure | %s",recordsProcessed);
							restructureStartMillis = System.currentTimeMillis();
						}
						
						// The first record that comes through, we use the keys of to
						// write out the headers to the destination DAT
						if(headersWrittern == false) {
							List<String> headers = DatLoadFile.getHeadersFromRecord(record);
							List<String> outputHeaders = new ArrayList<String>();
							for(String header : headers) {
								if(headerRenames.containsKey(header)) {
									outputHeaders.add(headerRenames.get(header));
								} else {
									outputHeaders.add(header);
								}
							}
							
							if(exportJson) {
								outputHeaders.add("JSONPATH");
							}
							
							datWriter.writeValues(outputHeaders);
							headersWrittern = true;
						}
						
						String guid = record.get("GUID");
						// System.out.println("Processing GUID: "+guid);
						// System.out.println(FormatUtility.debugString(record));
						
						try {
							Item currentItem = nuixCase.search("guid:"+guid).get(0);
							resolver.clear();
							resolver.setPath("export_directory", exportDirectory.getAbsolutePath());
							resolver.setFromItem(currentItem);
							
							if(exportText) {
								File source = new File(exportTempDirectory,record.get("TEXTPATH"));
								resolver.set("extension", FilenameUtils.getExtension(record.get("TEXTPATH")));
								File dest = new File(resolver.resolveTemplatePath(textFileNameTemplate));
								dest = resolveNameCollisions(dest);
								dest.getParentFile().mkdirs();
								source.renameTo(dest);
								record.put("TEXTPATH",getRelativePath(exportDirectory,dest));
							}
							
							if(exportNatives) {
								File source = new File(exportTempDirectory,record.get("ITEMPATH"));
								resolver.set("extension", FilenameUtils.getExtension(record.get("ITEMPATH")));
								File dest = new File(resolver.resolveTemplatePath(nativeFileNameTemplate));
								dest = resolveNameCollisions(dest);
								dest.getParentFile().mkdirs();
								source.renameTo(dest);
								record.put("ITEMPATH",getRelativePath(exportDirectory,dest));
							}
							
							if(exportPdfs) {
								File source = new File(exportTempDirectory,record.get("PDFPATH"));
								resolver.set("extension", FilenameUtils.getExtension(record.get("PDFPATH")));
								File dest = new File(resolver.resolveTemplatePath(pdfFileNameTemplate));
								dest = resolveNameCollisions(dest);
								dest.getParentFile().mkdirs();
								source.renameTo(dest);
								record.put("PDFPATH",getRelativePath(exportDirectory,dest));
							}
							
							if(exportTiffs) {
								File source = new File(exportTempDirectory,record.get("TIFFPATH"));
								resolver.set("extension", FilenameUtils.getExtension(record.get("TIFFPATH")));
								File dest = new File(resolver.resolveTemplatePath(tiffFileNameTemplate));
								dest = resolveNameCollisions(dest);
								dest.getParentFile().mkdirs();
								source.renameTo(dest);
								String newTiffRelativePath = getRelativePath(exportDirectory,dest);
								tiffRenames.put(record.get("TIFFPATH"),newTiffRelativePath);
								record.put("TIFFPATH",newTiffRelativePath);
							}
							
							if(exportJson) {
								resolver.set("extension", "json");
								File dest = new File(resolver.resolveTemplatePath(jsonFileNameTemplate));
								dest = resolveNameCollisions(dest);
								dest.getParentFile().mkdirs();
								String jsonRelativePath = getRelativePath(exportDirectory,dest);
								jsonExporter.exportItemAsJson(currentItem, dest);
								record.put("JSONPATH", jsonRelativePath);
							}
							
							recordsProcessed++;
						} catch (Exception e) {
							logError("Error during export restructuring for item with GUID '%s':\n%s",
									guid,FormatUtility.debugString(e));
						}
						
						datWriter.writeRecordValues(record);
					}
				});
			}
			
			logInfo("Export Restructure | Completed");
			
			// Fix up OPT file
			if(exportTiffs) {
				logInfo("Fixing pathing in OPT file...");
				File source = new File(exportTempDirectory,"loadfile.opt");
				File dest = new File(exportDirectory,"loadfile.opt");
				OptLoadFile.transpose(source, dest, new Consumer<OptRecord>() {
					@Override
					public void accept(OptRecord record) {
						record.setPath(tiffRenames.get(record.getPath()));
					}
				});
			}
			
			logInfo("Copying BatchExporter summaries...");
			// Copy other export products produced by BatchExporter
			File summaryReportTxt = new File(exportTempDirectory,"summary-report.txt");
			File summaryReportXml = new File(exportTempDirectory,"summary-report.xml");
			File tlMd5DigestTxt = new File(exportTempDirectory,"top-level-MD5-digests.txt");
			
			File summaryReportTxtDest = new File(exportDirectory,"summary-report.txt");
			File summaryReportXmlDest = new File(exportDirectory,"summary-report.xml");
			File tlMd5DigestTxtDest = new File(exportDirectory,"top-level-MD5-digests.txt");
			
			summaryReportTxt.renameTo(summaryReportTxtDest);
			summaryReportXml.renameTo(summaryReportXmlDest);
			tlMd5DigestTxt.renameTo(tlMd5DigestTxtDest);
			
			logInfo("Custom export completed");
		} catch (Exception e) {
			logError("Error during export:\n%s",FormatUtility.debugString(e));
		} finally {
			if(generalLog != null) generalLog.close();
			if(errorLog != null) errorLog.close();	
		}
	}
}
