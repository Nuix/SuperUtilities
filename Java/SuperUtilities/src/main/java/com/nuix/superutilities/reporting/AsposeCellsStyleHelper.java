package com.nuix.superutilities.reporting;

import java.util.ArrayList;
import java.util.List;

import com.aspose.cells.Border;
import com.aspose.cells.BorderCollection;
import com.aspose.cells.BorderType;
import com.aspose.cells.CellBorderType;
import com.aspose.cells.Color;
import com.aspose.cells.Style;

/***
 * A class containing helper methods for working with Aspose Cells Styles.
 * @author Jason Wells
 *
 */
public class AsposeCellsStyleHelper {
	/***
	 * Convenience method for applying a thin black border to all sides of a given cell style.
	 * @param style The style to modify the borders of.
	 */
	public static void enableAllBorders(Style style) {
		BorderCollection bordersCollection = style.getBorders();
		List<Border> borders = new ArrayList<Border>();
		borders.add(bordersCollection.getByBorderType(BorderType.LEFT_BORDER));
		borders.add(bordersCollection.getByBorderType(BorderType.TOP_BORDER));
		borders.add(bordersCollection.getByBorderType(BorderType.RIGHT_BORDER));
		borders.add(bordersCollection.getByBorderType(BorderType.BOTTOM_BORDER));
		for(Border border : borders) {
			border.setColor(Color.getBlack());
			border.setLineStyle(CellBorderType.THIN);
		}
	}
}
