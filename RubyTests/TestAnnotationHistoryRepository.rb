script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

java_import com.nuix.superutilities.annotations.AnnotationHistoryRepository

$current_case = $utilities.getCaseFactory.open('C:\@NUIX\Cases\BigCase')

repo = AnnotationHistoryRepository.new("C:\\Temp\\AnnotationRepository.db")
repo.setSnapshotFirstSync(false)
repo.syncHistory($current_case)

# repo.eachRecordedTagEvent(0) do |event|
# 	puts event
# end

# repo.eachRecordedExclusionEvent(0) do |event|
# 	puts event
# end

# repo.eachRecordedCustomMetadataEvent(0) do |event|
# 	puts event
# end

# repo.eachRecordedItemSetEvent(0) do |event|
# 	puts event
# end

puts repo.buildSummary.toString

repo.close

$current_case.close