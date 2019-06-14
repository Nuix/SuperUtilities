package com.nuix.superutilities.annotations;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Precision;
import org.apache.log4j.Logger;

import com.aspose.pdf.Document;
import com.aspose.pdf.Rectangle;
import com.aspose.pdf.TextFragment;
import com.aspose.pdf.TextFragmentAbsorber;
import com.aspose.pdf.TextFragmentCollection;
import com.aspose.pdf.TextSearchOptions;
import com.aspose.pdf.TextSegment;
import com.aspose.pdf.TextSegmentCollection;
import com.nuix.superutilities.export.PdfWorkCache;

import nuix.Case;
import nuix.Item;
import nuix.MarkupSet;

/***
 * Provides bulk redaction functionality, as found in the GUI, but (as of Nuix 7.8.0.10) is not present in the Nuix API.
 * @author Jason Wells
 *
 */
public class BulkRedactor {
	private static Logger logger = Logger.getLogger(BulkRedactor.class);
	
	private Consumer<String> messageLoggedCallback = null;
	
	/***
	 * Allows you to provide a callback which will be invoked when this instance emits a log message.
	 * @param callback The callback to receive logged messages
	 */
	public void whenMessageLogged(Consumer<String> callback) {
		messageLoggedCallback = callback;
	}
	
	private void logMessage(String message) {
		if(messageLoggedCallback != null) {
			messageLoggedCallback.accept(message);
		} else {
			logger.info(message);
		}
	}
	
	private void logMessage(String format, Object... params) {
		logMessage(String.format(format, params));
	}
	
	private Consumer<BulkRedactorProgressInfo> progressUpdatedCallback = null;
	
	/***
	 * Allows you to provide a callback which will be invoked when this instance emits a progress update.
	 * @param callback The callback to invoke when progress is updated
	 */
	public void whenProgressUpdated(Consumer<BulkRedactorProgressInfo> callback) {
		progressUpdatedCallback = callback;
	}
	
	private void fireProgressUpdated(BulkRedactorProgressInfo info) {
		if(progressUpdatedCallback != null) {
			progressUpdatedCallback.accept(info);
		}
	}
	
	/***
	 * This method provides the logic to take the individual text segments (think characters in a match) and coalesce them into per line redactions.
	 * @param segments Collection of text segments received from a given TextFragment.
	 * @param pageRect Rectangle representing dimensions of source page.  Needed to convert coordinates to Nuix accepted coordinates.
	 * @param pageNumber The page number the given match comes from.  Recorded in {@link NuixImageAnnotationRegion} so it knows what page to perform markups on.
	 * @return A List of annotation region objects representing the coalesced segments.
	 */
	private List<NuixImageAnnotationRegion> buildRegionsFromSegments(TextSegmentCollection segments, Rectangle pageRect, int pageNumber){
		List<NuixImageAnnotationRegion> result = new ArrayList<NuixImageAnnotationRegion>();
		
		// Group rectangle for each char by line
		Map<Double,List<TextSegment>> groupedByLine = new TreeMap<Double,List<TextSegment>>();
		logger.debug("Fragment Segments:");
		for(TextSegment segment : segments) {
			Rectangle segRect = segment.getRectangle();
			double y = Precision.round(segRect.getLLY(), 2);
			if(!groupedByLine.containsKey(y)) {
				groupedByLine.put(y, new ArrayList<TextSegment>());
			}
			groupedByLine.get(y).add(segment);
			
			logger.debug(String.format("LLX=%s, LLY=%s, URX=%s, URY=%s, T=%s",
					segment.getRectangle().getLLX(), segment.getRectangle().getLLY(),
					segment.getRectangle().getURX(), segment.getRectangle().getURY(),
					segment.getText()
				));
		}
		
		for(Map.Entry<Double, List<TextSegment>> lineGroupedSegments : groupedByLine.entrySet()) {
			List<TextSegment> lineSegments = lineGroupedSegments.getValue(); 
			lineSegments.sort(new Comparator<TextSegment>() {
				@Override
				public int compare(TextSegment o1, TextSegment o2) {
					Rectangle segRect1 = o1.getRectangle();
					Rectangle segRect2 = o2.getRectangle();
					return Double.compare(segRect1.getLLX(), segRect2.getLLX());
				}
			});
			
			logger.debug("Line Segments:");
			for(TextSegment seg : lineSegments) {
				logger.debug(String.format("LLX=%s, LLY=%s, URX=%s, URY=%s, T=%s",
						seg.getRectangle().getLLX(), seg.getRectangle().getLLY(),
						seg.getRectangle().getURX(), seg.getRectangle().getURY(),
						seg.getText()
					));
			}
			
			double lowestX = lineSegments.get(0).getRectangle().getLLX();
			double highestX = lineSegments.get(lineSegments.size()-1).getRectangle().getURX();
			
			logger.debug(String.format("lowestX=%s", lowestX));
			logger.debug(String.format("highestX=%s", highestX));
			
			double x = lowestX / pageRect.getWidth();
			double y = 1.0 - (lineSegments.get(0).getRectangle().getLLY() / pageRect.getHeight());
			double width = (highestX - lowestX) / pageRect.getWidth();
			double height = lineSegments.get(0).getRectangle().getHeight() / pageRect.getHeight();
			
			// Markup in Nuix seems to come out a little taller than it needs to so we are going
			// to make some small tweaks to the converted region to trim a tiny bit off the top
			double heightUnit = height * 0.01; // 1% of region height
			y += heightUnit * 5; // Translate down 5% of height
			height -= heightUnit * 5; // Then remove 5% from height (move bottom edge back up a little)
			
			NuixImageAnnotationRegion region = new NuixImageAnnotationRegion();
			region.setAsposeSourcePageRectangle(pageRect);
			region.setX(x);
			region.setY(y - height);
			region.setHeight(height);
			region.setWidth(width);
			String text = String.join("", lineSegments.stream().map(s -> s.getText()).collect(Collectors.toList()));
			region.setText(text);
			region.setPageNumber(pageNumber);
			
			logger.debug("Resulting Region:");
			logger.debug(region);
			
			result.add(region);
		}
		
		return result;
	}
	
	/***
	 * Generates {@link NuixImageAnnotationRegion} objects for matches found by Aspose in the given PDF based on the provided regular expressions.
	 * @param file The PDF file to search
	 * @param expressions The regular expressions to look for.
	 * @return A List of annotation region objects representing the matches.
	 */
	public List<NuixImageAnnotationRegion> findExpressionsInPdfFile(File file, Collection<String> expressions) {
		Document pdfDocument = new Document(file.getAbsolutePath());
		List<NuixImageAnnotationRegion> result = new ArrayList<NuixImageAnnotationRegion>();
		for(String expression : expressions) {
			TextFragmentAbsorber absorber = new TextFragmentAbsorber(expression);
			
			TextSearchOptions options = new TextSearchOptions(true);
			absorber.setTextSearchOptions(options);
			pdfDocument.getPages().accept(absorber);
			logger.info(String.format("Scanning %s for %s", file.getAbsolutePath(), expression));
			TextFragmentCollection textFragmentCollection = absorber.getTextFragments();
			for(TextFragment fragment : textFragmentCollection) {
				Rectangle pageRect = fragment.getPage().getRect();
				
				logger.debug("Text Fragment:");
				logger.debug(String.format("LLX=%s, LLY=%s, URX=%s, URY=%s, T=%s",
						fragment.getRectangle().getLLX(), fragment.getRectangle().getLLY(),
						fragment.getRectangle().getURX(), fragment.getRectangle().getURY(),
						fragment.getText()
					));
				result.addAll(buildRegionsFromSegments(fragment.getSegments(),pageRect,fragment.getPage().getNumber()));
			}
		}
		pdfDocument.close();
		return result;
	}
	
	/***
	 * Finds text in PDFs of given items.  Then generates redactions based on the matches.
	 * @param nuixCase The source Nuix case.  Needed to obtain items (if none were given) and/or obtain the appropriate markup set.
	 * @param settings The settings used to find and generate the redactions.
	 * @param scopeItems Items to find and redact.
	 * @throws Exception If something goes wrong
	 * @return Returns a list of all match region objects (so they can be reported, inspected, etc)
	 */
	public List<NuixImageAnnotationRegion> findAndMarkup(Case nuixCase, BulkRedactorSettings settings, Collection<Item> scopeItems) throws Exception {
		if(scopeItems == null || scopeItems.size() < 1) {
			logger.info("No scopeItems were provided, using all items in case");
			scopeItems = nuixCase.search("");
		}
		
		List<NuixImageAnnotationRegion> allFoundRegions = new ArrayList<NuixImageAnnotationRegion>();
		
		// This is very important!  If Aspose is not initialized, it will be working in evaluation mode
		// which means it will give partial results!
		com.nuix.data.util.aspose.AsposePdf.ensureInitialised();

		PdfWorkCache pdfCache = new PdfWorkCache(settings.getTempDirectory());
		MarkupSet markupSet = null;
		if (settings.getApplyRedactions() ||  settings.getApplyRedactions()) {
			markupSet = settings.getMarkupSet(nuixCase);	
		}
		int currentIteration = 0;
		int matches = 0;
		
		logMessage("Regular Expressions:");
		for(String expression : settings.getExpressions()) {
			logMessage(expression);
		}
		
		logMessage("Named Entities:");
		for(String namedEntity : settings.getNamedEntityTypes()) {
			logMessage(namedEntity);
		}
		
		for(Item item : scopeItems) {
			currentIteration += 1;
			File tempPdf = pdfCache.getPdfPath(item);
			
			List<NuixImageAnnotationRegion> regions = findExpressionsInPdfFile(tempPdf, settings.getExpressions());
			if(regions.size() > 0) {
				for(NuixImageAnnotationRegion region : regions) {
					region.setItem(item);
				}
				allFoundRegions.addAll(regions);
				logMessage("Item with GUID %s had %s matches",item.getGuid(),regions.size());
				for(NuixImageAnnotationRegion region : regions) {
					matches++;
					if(settings.getApplyRedactions()) { region.applyRedaction(markupSet); }
					if(settings.getApplyHighLights()) { region.applyHighlight(markupSet); }
				}	
			}
			
			//Named entities require that we get matched values, convert those to expressions and then do another pass
			if (settings.getNamedEntityTypes().size() > 0) {
				Set<String> entityValues = new HashSet<String>();
				for(String entityType : settings.getNamedEntityTypes()) {
					entityValues.addAll(item.getEntities(entityType));
				}
				
				Set<String> entityExpressions = entityValues.stream().map(v -> BulkRedactorSettings.phraseToExpression(v)).collect(Collectors.toSet());
				List<NuixImageAnnotationRegion> entityRegions = findExpressionsInPdfFile(tempPdf, entityExpressions);
				if(entityRegions.size() > 0) {
					for(NuixImageAnnotationRegion region : entityRegions) {
						region.setItem(item);
					}
					allFoundRegions.addAll(regions);
					logMessage("Item with GUID %s had %s named entity matches",item.getGuid(),entityRegions.size());
					for(NuixImageAnnotationRegion region : entityRegions) {
						matches++;
						if(settings.getApplyRedactions()) { region.applyRedaction(markupSet); }
						if(settings.getApplyHighLights()) { region.applyHighlight(markupSet); }
					}	
				}
			}
			
			// Report progress
			BulkRedactorProgressInfo progressInfo = new BulkRedactorProgressInfo();
			progressInfo.setCurrent(currentIteration);
			progressInfo.setTotal(scopeItems.size());
			progressInfo.setMatches(matches);
			fireProgressUpdated(progressInfo);
		}
		
		logMessage("Cleaning up temp directory %s",settings.getTempDirectory());
		pdfCache.cleanupTemporaryPdfs();
		logMessage("Temp directory deleted");
		
		return allFoundRegions;
	}
}
