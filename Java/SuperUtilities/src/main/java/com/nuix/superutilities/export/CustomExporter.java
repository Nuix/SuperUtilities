package com.nuix.superutilities.export;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
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
import com.nuix.superutilities.reporting.SimpleWorksheet;
import com.nuix.superutilities.reporting.SimpleXlsx;

import nuix.BatchExporter;
import nuix.Case;
import nuix.Item;
import nuix.ItemEventCallback;
import nuix.ItemEventInfo;
import nuix.ItemExpression;
import nuix.MetadataItem;
import nuix.MetadataProfile;
import nuix.Utilities;

/***
 * Provides customized exports while still leveraging the performance of <a href="https://download.nuix.com/releases/desktop/stable/docs/en/scripting/api/nuix/BatchExporter.html">BatchExporter</a>.
 * This is accomplished by first performing a BatchExport using the Nuix API.  Once that temporary export is completed, all the exported products (text, natives, images, pdfs) are then
 * restructured based on a series of file naming templates.  While restructuring is occurring, paths in DAT and OPT load files are updated to match new structure.
 * @author Jason Wells
 *
 */
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
	
	private boolean exportXlsx = false;
	private boolean keepOriginalDat = false;
	
	private Map<String,String> headerRenames = new HashMap<String,String>();
	private Set<String> columnRemovals = new HashSet<String>();
	
	private Map<?,?> parallelProcessingSettings = new HashMap<String,Object>();
	private Map<?,?> imagingSettings = new HashMap<String,Object>();
	private Map<?,?> stampingSettings = new HashMap<String,Object>();
	
	private Map<String,BiFunction<Item,String,String>> dynamicPlaceholders = new LinkedHashMap<String,BiFunction<Item,String,String>>();
	
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
	 * @param profile The profile to use when generating metadata loadfiles.
	 */
	public void setProfile(MetadataProfile profile) {
		this.profile = profile;
	}
	
	/***
	 * Assigns a dynamically calculated placeholder to this instance.
	 * @param placeholderName Placeholder name with "{" or "}".  For example "my_value".  Placeholder in templates can then be referred to using "{my_value}". It is
	 * preferred that you use only lower case characters and no whitespace characters.  Letters, numbers and underscores only is recommended.
	 * @param function A function which accepts as arguments an item and String with product type.  Expected to return a String value.  If function yields a null
	 * then placeholder will resolve to "NO_VALUE" when resolving the template.
	 */
	public void setDynamicPlaceholder(String placeholderName, BiFunction<Item,String,String> function) {
		dynamicPlaceholders.put(placeholderName, function);
	}
	
	/***
	 * Removes a previously added dynamic placeholder.
	 * @param placeholderName The name of the dynamic placeholder to remove
	 */
	public void removeDynamicPlaceholder(String placeholderName) {
		dynamicPlaceholders.remove(placeholderName);
	}
	
	/***
	 * Removes all previously added dynamic placeholders.
	 */
	public void clearAllDynamicPlaholders() {
		dynamicPlaceholders.clear();
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
	
	/***
	 * Allows you to provide a Map of headers to rename.  Intended to provide a way to rename headers that Nuix automatically adds
	 * to DAT files it generates:<br>
	 * <code>DOCID</code><br>
	 * <code>PARENT_DOCID</code><br>
	 * <code>ATTACH_DOCID</code><br>
	 * <code>BEGINBATES</code><br>
	 * <code>ENDBATES</code><br>
	 * <code>BEGINGROUP</code><br>
	 * <code>ENDGROUP</code><br>
	 * <code>PAGECOUNT</code><br>
	 * <code>ITEMPATH</code> (when exporting natives)<br>
	 * <code>TEXTPATH</code> (when exporting text)<br>
	 * <code>PDFPATH</code> (when exporting PDFs)<br>
	 * <code>TIFFPATH</code> (when exporting TIFFs)<br>
	 * @param renames A Map with the map key being the header name before and the map value being what you want that header renamed to.  Map key is case sensitive!
	 */
	public void setHeaderRenames(Map<String,String> renames) {
		this.headerRenames = renames;
	}

	/***
	 * Allows you to provide a list of header names to specify columns to remove.  Intended to provide a way to remove columns that Nuix automatically adds
	 * to DAT files it generates:<br>
	 * <code>DOCID</code><br>
	 * <code>PARENT_DOCID</code><br>
	 * <code>ATTACH_DOCID</code><br>
	 * <code>BEGINBATES</code><br>
	 * <code>ENDBATES</code><br>
	 * <code>BEGINGROUP</code><br>
	 * <code>ENDGROUP</code><br>
	 * <code>PAGECOUNT</code><br>
	 * <code>ITEMPATH</code> (when exporting natives)<br>
	 * <code>TEXTPATH</code> (when exporting text)<br>
	 * <code>PDFPATH</code> (when exporting PDFs)<br>
	 * <code>TIFFPATH</code> (when exporting TIFFs)<br>
	 * Case sensitive and takes priority over column renaming, so make sure you provide original pre-rename headers.
	 * @param columnHeaders Collection of column header names for columns to be removed.  Case sensitive and takes priority over column renaming,
	 * so make sure you provide original pre-rename headers.
	 */
	public void setColumnRemovals(Collection<String> columnHeaders) {
		this.columnRemovals = new HashSet<String>();
		this.columnRemovals.addAll(columnHeaders);
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
	
	/***
	 * If a given file path already exists, iteratively adds a suffix until an unused file path is discovered.
	 * @param intendedDest The file which may already exist
	 * @return The original path if file does not already exist, otherwise a suffixed file name path that does not conflict with
	 * any existing file names.
	 */
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
	 * Resolves dynamic place holders provided by user.
	 * @param resolver The resolver to which we will store calculated placeholder values.
	 * @param item The current item this placeholder will be used for.
	 * @param productType The product type this placeholder will be used for.  Should be one of the following values: "TEXT","NATIVE","PDF","TIFF" and "JSON".
	 */
	private void resolveDynamicPlaceholders(PlaceholderResolver resolver, Item item, String productType) {
		for(Map.Entry<String,BiFunction<Item,String,String>> dynamicPlaceholderFunc : dynamicPlaceholders.entrySet()) {
			String resolvedValue = dynamicPlaceholderFunc.getValue().apply(item, productType);
			if(resolvedValue == null) {
				resolvedValue = "NO_VALUE";
			}
			resolver.set(dynamicPlaceholderFunc.getKey(), resolvedValue);
		}
	}
	
	/***
	 * Exports given items in custom structure.  This is accomplished by first performing a temporary "legal export" using
	 * the API object BatchExporter.  Once that export is complete, files are restructured into final format.  Load file paths are
	 * updated to reflect restructuring.
	 * @param nuixCase The relevant Nuix case, needed to resolve GUIDs in temp export DAT file to actual items.
	 * @param exportDirectory Where the final export should reside.
	 * @param items The items to export
	 * @throws Exception If something goes wrong
	 */
	public void exportItems(Case nuixCase, String exportDirectory, List<Item> items) throws Exception {
		exportItems(nuixCase, new File(exportDirectory),items);
	}
	
	/***
	 * Exports given items in custom structure.  This is accomplished by first performing a temporary "legal export" using
	 * the API object BatchExporter.  Once that export is complete, files are restructured into final format.  Load file paths are
	 * updated to reflect restructuring.
	 * @param nuixCase The relevant Nuix case, needed to resolve GUIDs in temp export DAT file to actual items.
	 * @param exportDirectory Where the final export should reside.
	 * @param items The items to export
	 * @throws Exception If something goes wrong
	 */
	public void exportItems(Case nuixCase, File exportDirectory, List<Item> items) throws Exception {
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
			
			// Pass along worker settings if we have some
			if(parallelProcessingSettings != null && parallelProcessingSettings.size() > 0) {
				logInfo("Providing parallel processing settings...");
				exporter.setParallelProcessingSettings(parallelProcessingSettings);
			}
			
			// Pass along imaging settings if we have some
			if(imagingSettings != null && imagingSettings.size() > 0) {
				logInfo("Providing imaging settings...");
				exporter.setImagingOptions(imagingSettings);
			}
			
			// Pass along stamping settings if we have some
			if(stampingSettings != null && stampingSettings.size() > 0) {
				logInfo("Providing stamping settings...");
				exporter.setStampingOptions(stampingSettings);
			}
			
			logInfo("Beginning temp export using BatchExporter...");
			exporter.exportItems(items);
			logInfo("Finished temp export using BatchExporter");
			
			/* BEGIN FIXING UP INTO NEW STRUCTURE */
					
			// Used to resolve naming templates to final path structure
			PlaceholderResolver resolver = new PlaceholderResolver();
			
			// Tracks old relative path and new relative path so that OPT file can be updated
			Map<String,String> tiffRenames = new HashMap<String,String>();
			
			final SimpleXlsx xlsx = new SimpleXlsx(new File(exportDirectory,"loadfile.xlsx"));
			SimpleWorksheet worksheet = xlsx.getSheet("Loadfile");
			
			logInfo("Restructuring export using %s as input, writing to %s as output...",tempDatFile.getAbsolutePath(),finalDatFile.getAbsolutePath());
			try(DatLoadFileWriter datWriter = new DatLoadFileWriter(finalDatFile)){
				DatLoadFileReader.withEachRecord(tempDatFile, new Consumer<LinkedHashMap<String,String>>() {
					boolean headersWrittern = false;
					long restructureStartMillis = System.currentTimeMillis();
					int recordsProcessed = 0;
					
					@Override
					public void accept(LinkedHashMap<String, String> record) {
						// Periodically log progress
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
								// If user specified column should be removed, then we also
								// don't need to output a header/renamed header for it
								if(!columnRemovals.contains(header)) {
									// Perform any header renaming we may need to do
									if(headerRenames.containsKey(header)) {
										outputHeaders.add(headerRenames.get(header));
									} else {
										outputHeaders.add(header);
									}
								}
							}
							
							// If we're exporting JSON files, we need to add our own column to record
							// the path in the DAT since this is a product we are adding and not Nuix
							if(exportJson  && !columnRemovals.contains("JSONPATH")) {
								outputHeaders.add("JSONPATH");
							}
							
							datWriter.writeValues(outputHeaders);
							
							if(exportXlsx) {
								worksheet.appendRow(outputHeaders);
							}
							
							headersWrittern = true;
						}
						
						String guid = record.get("GUID");
						
						try {
							Item currentItem = nuixCase.search("guid:"+guid).get(0);
							resolver.clear();
							resolver.setPath("export_directory", exportDirectory.getAbsolutePath());
							resolver.setFromItem(currentItem);
							
							// Restructure text files if we have them
							if(exportText) {
								File source = new File(exportTempDirectory,record.get("TEXTPATH"));
								resolver.set("extension", FilenameUtils.getExtension(record.get("TEXTPATH")));
								resolveDynamicPlaceholders(resolver, currentItem, "TEXT");
								File dest = new File(resolver.resolveTemplatePath(textFileNameTemplate));
								dest = resolveNameCollisions(dest);
								dest.getParentFile().mkdirs();
								source.renameTo(dest);
								if(!columnRemovals.contains("TEXTPATH")) {
									record.put("TEXTPATH",getRelativePath(exportDirectory,dest));
								}
							}
							
							// Restructure native files if we have them
							if(exportNatives) {
								File source = new File(exportTempDirectory,record.get("ITEMPATH"));
								resolver.set("extension", FilenameUtils.getExtension(record.get("ITEMPATH")));
								resolveDynamicPlaceholders(resolver, currentItem, "NATIVE");
								File dest = new File(resolver.resolveTemplatePath(nativeFileNameTemplate));
								dest = resolveNameCollisions(dest);
								dest.getParentFile().mkdirs();
								source.renameTo(dest);
								if(!columnRemovals.contains("ITEMPATH")) {
									record.put("ITEMPATH",getRelativePath(exportDirectory,dest));
								}
							}
							
							// Restructure PDF files if we have them
							if(exportPdfs) {
								File source = new File(exportTempDirectory,record.get("PDFPATH"));
								resolver.set("extension", FilenameUtils.getExtension(record.get("PDFPATH")));
								resolveDynamicPlaceholders(resolver, currentItem, "PDF");
								File dest = new File(resolver.resolveTemplatePath(pdfFileNameTemplate));
								dest = resolveNameCollisions(dest);
								dest.getParentFile().mkdirs();
								source.renameTo(dest);
								if(!columnRemovals.contains("PDFPATH")) {
									record.put("PDFPATH",getRelativePath(exportDirectory,dest));
								}
							}
							
							// Restructure TIFF file if we have them
							if(exportTiffs) {
								File source = new File(exportTempDirectory,record.get("TIFFPATH"));
								resolver.set("extension", FilenameUtils.getExtension(record.get("TIFFPATH")));
								resolveDynamicPlaceholders(resolver, currentItem, "TIFF");
								File dest = new File(resolver.resolveTemplatePath(tiffFileNameTemplate));
								dest = resolveNameCollisions(dest);
								dest.getParentFile().mkdirs();
								source.renameTo(dest);
								String newTiffRelativePath = getRelativePath(exportDirectory,dest);
								tiffRenames.put(record.get("TIFFPATH"),newTiffRelativePath);
								if(!columnRemovals.contains("TIFFPATH")) {
									record.put("TIFFPATH",newTiffRelativePath);
								}
							}
							
							// Produce JSON file if settings specified to do so
							if(exportJson) {
								resolver.set("extension", "json");
								resolveDynamicPlaceholders(resolver, currentItem, "JSON");
								File dest = new File(resolver.resolveTemplatePath(jsonFileNameTemplate));
								dest = resolveNameCollisions(dest);
								dest.getParentFile().mkdirs();
								String jsonRelativePath = getRelativePath(exportDirectory,dest);
								jsonExporter.exportItemAsJson(currentItem, dest);
								if(!columnRemovals.contains("JSONPATH")) {
									record.put("JSONPATH", jsonRelativePath);
								}
							}
							
							recordsProcessed++;
						} catch (Exception e) {
							logError("Error during export restructuring for item with GUID '%s':\n%s",
									guid,FormatUtility.debugString(e));
						}
						
						// Remove any columns user asked to not have in output
						for(String header : columnRemovals) {
							record.remove(header);
						}
						
						datWriter.writeRecordValues(record);
						if(exportXlsx) {
							List<Object> recordValues = new ArrayList<Object>();
							recordValues.addAll(record.values());
							worksheet.appendRow(recordValues);
						}
					}
				});
				
				if(exportXlsx) {
					worksheet.autoFitColumns();
					xlsx.save();
					xlsx.close();
				}
			}
			
			logInfo("Export Restructure | Completed");
			
			// Fix up OPT file to reflect final TIFF file paths
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
			
			// If we wish to keep a copy of the original DAT from the temporary export then
			// we need to move it over before we delete the temporary export
			if(keepOriginalDat) {
				logInfo("Copying Nuix exported DAT file as nuix_loadfile.dat");
				File finalOriginalDatFile = new File(exportDirectory,"nuix_loadfile.dat");
				FileUtils.moveFile(tempDatFile, finalOriginalDatFile);
			}
			
			// Finally, we can delete our temp export as we have moved everything into the final structure
			logInfo("Deleting temporary export...");
			FileUtils.deleteDirectory(exportTempDirectory);	
			
			logInfo("Custom export completed");
		} catch (Exception e) {
			logError("Error during export:\n%s",FormatUtility.debugString(e));
		} finally {
			if(generalLog != null) generalLog.close();
			if(errorLog != null) errorLog.close();	
		}
	}

	/***
	 * Sets the Map of settings which will be passed to <code>BatchExporter.setParallelProcessingSettings(Map)</code> when performing
	 * the temporary export before restructuring.
	 * @param settings A map of settings, see API documentation for
	 * <a href="https://download.nuix.com/releases/desktop/stable/docs/en/scripting/api/nuix/ParallelProcessingConfigurable.html#setParallelProcessingSettings-java.util.Map-">BatchExporter.setParallelProcessingSettings</a>
	 * for a list of settings accepted.
	 */
	public void setParallelProcessingSettings(Map<?, ?> settings) {
		this.parallelProcessingSettings = settings;
	}
	
	/***
	 * Sets the Map of settings which will be passed to <code>BatchExporter.setImagingOptions(Map)</code> when performing
	 * the temporary export before restructuring.
	 * @param settings A map of settings, see API documentation for
	 * <a href="https://download.nuix.com/releases/desktop/stable/docs/en/scripting/api/nuix/ImagingConfigurable.html#setImagingOptions-java.util.Map-">BatchExporter.setImagingOptions</a>
	 * for a list of settings accepted.
	 */
	public void setImagingOptions(Map<?, ?> settings) {
		this.imagingSettings = settings;
	}
	
	/***
	 * Sets the Map of settings which will be passed to <code>BatchExporter.setStampingOptions(Map)</code> when performing
	 * the temporary export before restructuring.
	 * @param settings A map of settings, see API documentation for
	 * <a href="https://download.nuix.com/releases/desktop/stable/docs/en/scripting/api/nuix/StampingConfigurable.html#setStampingOptions-java.util.Map-">BatchExporter.setStampingOptions</a>
	 * for a list of settings accepted.
	 */
	public void setStampingOptions(Map<?, ?> settings) {
		this.stampingSettings = settings;
	}

	/***
	 * Gets whether DAT contents should additionally be exported as an XLSX spreadsheet.
	 * @return Whether DAT contents should additionally be exported as an XLSX spreadsheet.
	 */
	public boolean getExportXlsx() {
		return exportXlsx;
	}

	/***
	 * Sets whether DAT contents should additionally be exported as an XLSX spreadsheet.
	 * @param exportXlsx Whether DAT contents should additionally be exported as an XLSX spreadsheet.
	 */
	public void setExportXlsx(boolean exportXlsx) {
		this.exportXlsx = exportXlsx;
	}

	/***
	 * Gets whether original DAT will be kept by moving it to final export directory.
	 * @return True if final DAT will be kept
	 */
	public boolean getKeepOriginalDat() {
		return keepOriginalDat;
	}

	/***
	 * Sets whether final DAT will be kept by moving it to final export directory/
	 * @param keepOriginalDat True if final DAT should be kept
	 */
	public void setKeepOriginalDat(boolean keepOriginalDat) {
		this.keepOriginalDat = keepOriginalDat;
	}
}
