package com.nuix.superutilities.reporting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aspose.cells.Color;

/***
 * Iterator over a collection of Aspose Cells Color objects.  When the end of the collection is hit it
 * automatically starts at the beginning again, effectively creating an infinite circular collection.
 * @author Jason Wells
 *
 */
public class ColorRing implements Iterator<Color> {

	private List<Color> colors = new ArrayList<Color>();
	int pos = 0;
	
	@Override
	public boolean hasNext() {
		return colors.size() > 0;
	}

	@Override
	public Color next() {
		Color nextColor = colors.get(pos);
		pos++;
		if(pos > colors.size() - 1) {
			pos = 0;
		}
		return nextColor;
	}
	
	/***
	 * Adds the provided base color and series of tinted variations to this instance.
	 * @param baseColor The based color to start with
	 * @param tintSteps The number of additional tinted versions of the base color to add
	 */
	public void addTintSeries(Color baseColor, int tintSteps) {
		for (int i = 0; i <= tintSteps; i++) {
			colors.add(AsposeCellsColorHelper.getTint(baseColor, (float)i));
		}
	}
	
	/***
	 * Clears all colors currently assigned to this instance.
	 */
	public void clearColors() {
		colors.clear();
	}
	
	/***
	 * Moves position of this collection back to the start so that next call to {@link #next()} will return
	 * the first color in the sequence.
	 */
	public void restart() {
		pos = 0;
	}
	
	public void addColor(int red, int green, int blue) {
		colors.add(Color.fromArgb(red, green, blue));
	}
}
