script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

java_import com.nuix.superutilities.misc.PdfUtility

input_file = "D:\\temp\\WatermarkTest\\input.pdf"
output_file = "D:\\temp\\WatermarkTest\\output.pdf"
phrase = "Watermark Test"
font_size = 48
opacity = 0.25
rotation = 45.0

PdfUtility.createWaterMarkedPdf(input_file,output_file,"WATERMARK TEST",font_size,opacity,rotation)