script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.regex.RegexScanner

output_csv = "C:\\Temp\\RegexScannerTest.csv"

require 'csv'
$current_case = $utilities.getCaseFactory.open("C:\\@Nuix\\Cases\\Ziggy")

items = $current_case.search("")
scanner = RegexScanner.new
scanner.setScanProperties(true)
scanner.setScanContent(true)
scanner.setCaseSensitive(false)
scanner.setCaptureContextualText(true)
scanner.setContextSize(30)

scanner.addPattern("Jason Matches","jason")

last_progress = Time.now

CSV.open(output_csv,"w:utf-8") do |csv|

	csv << [
		"GUID",
		"Item Name",
		"ItemKind",
		"Expression",
		"Location",
		"Value",
		"ValueContext",
		"Match Start",
		"Match End",
	]

	match_index = 0
	scanner.scanItemsParallel(items) do |item_match_collection|
		item = item_match_collection.getItem
		item_match_collection.getMatches.each do |match|
			csv << [
				item.getGuid,
				item.getLocalisedName,
				item.getType.getKind.getName,
				match.getExpression,
				match.getLocation,
				match.getValue,
				match.getValueContext,
				match.getMatchStart,
				match.getMatchEnd,
			]

			match_index += 1
			if (Time.now - last_progress) > 1
				puts "Found #{match_index} matches so far"
				last_progress = Time.now
			end
		end
	end
	puts "Found #{match_index} matches"
end

$current_case.close