script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

$current_case = $utilities.getCaseFactory.open("C:\\@Nuix\\Cases\\Ziggy")

export_directory = "C:\\Temp\\JSON\\"
java.io.File.new(export_directory).mkdirs
items = $current_case.search("")
items = $utilities.getItemSorter.sortItemsByPosition(items)
items = items.take(5000)

last_progress = Time.now
exporter = $su.createJsonExporter
items.each_with_index do |item,item_index|
	if (Time.now - last_progress) > 1 || item_index+1 == items.size
		puts "Processing item #{item_index+1}/#{items.size}"
		last_progress = Time.now
	end
	file_name = "#{(item_index+1).to_s.rjust(8,"0")}.json"
	export_file = File.join(export_directory,  file_name)
	exporter.exportItemAsJson(item,java.io.File.new(export_file))
end

$current_case.close

puts "Done"