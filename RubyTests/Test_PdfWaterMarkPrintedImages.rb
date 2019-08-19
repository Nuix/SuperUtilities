script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

java_import com.nuix.superutilities.misc.PdfUtility

$current_case = $utilities.getCaseFactory.open("D:\\cases\\FakeData_8.0")

items = $current_case.searchUnsorted("flag:audited").take(10_000)
puts "Found #{items.size} items"

temp_directory = "D:\\temp\\WatermarkTest"
phrase = "Watermark Test"
font_size = 48
opacity = 0.25
rotation = 45.0

puts "Beginning watermark generation..."
PdfUtility.waterMarkPrintedImages(temp_directory,items,phrase,font_size,opacity,rotation) do |current,total|
	puts "#{current}/#{total}"
end

$utilities.getBulkAnnotater.addTag("WaterMarkedPrintedImage",items)

$current_case.close

puts "Completed"