script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.export.ChunkedDATExporter

$current_case = $utilities.getCaseFactory.open("C:\\@Nuix\\Cases\\Ziggy")

items = $current_case.search("")
profile = $utilities.getMetadataProfileStore.getMetadataProfile("Audited Review")
export_directory = "C:\\Temp\\Chunked"

exporter = ChunkedDATExporter.new
exporter.setExportDirectory(export_directory)
exporter.setProfile(profile)

exporter.export(items)

$current_case.close