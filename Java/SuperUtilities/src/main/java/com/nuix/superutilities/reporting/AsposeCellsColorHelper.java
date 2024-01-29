package com.nuix.superutilities.reporting;

import com.aspose.cells.Color;

/***
 * A class containing helper methods for working with Aspose Cells colors.
 * @author Jason Wells
 *
 */
public class AsposeCellsColorHelper {
	/***
	 * Tints a particular color channel value by a certain degree.
	 * @param colorChannelValue Color channel value (0-255) to tint
	 * @param degree The degree in which to tint the color channel.
	 * @return A tinted version of the color channel value
	 */
	public static int tintChannel(int colorChannelValue, float degree){
		if(degree == 0)
			return colorChannelValue;
		
		int tint = (int) (colorChannelValue + (degree * (255f - (float)colorChannelValue)));
		if(tint < 0)
			return 0;
		else if(tint > 255)
			return 255;
		else
			return tint;
	}
	
	/***
	 * Tints a color by the given degree
	 * @param red Red color channel value (0-255)
	 * @param green Green color channel value (0-255)
	 * @param blue Blue color channel value (0-255)
	 * @param degree Degree to which all the color channels will be tinted
	 * @return A new Color object which has been tinted the specified amount
	 */
	public static Color getTint(int red, int green, int blue, float degree){
		return Color.fromArgb(
				tintChannel(red,degree),
				tintChannel(green,degree),
				tintChannel(blue,degree)
			);
	}
	
	/***
	 * Helper method to convert signed byte to unsigned int
	 * @param b The input byte value
	 * @return Unsigned int equivalent
	 */
	private static int byteToUnsignedInt(byte b) {
		return b & 0xFF;
	}
	
	/***
	 * Provides a tinted variation of the provided base color
	 * @param baseColor A base color which will be tinted
	 * @param degree To what extend the base color is tinted
	 * @return Tinted copy of the provided base color
	 */
	public static Color getTint(Color baseColor, float degree) {
		return getTint(
				byteToUnsignedInt(baseColor.getR()),
				byteToUnsignedInt(baseColor.getG()),
				byteToUnsignedInt(baseColor.getB()),
				degree
			);
	}
}
