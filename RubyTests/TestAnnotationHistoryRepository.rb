case_directory = 'C:\@NUIX\Cases\Ziggy'
history_db_file = "C:\\Temp\\AnnotationRepository_#{Time.now.to_i}.db"

script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

java_import com.nuix.superutilities.annotations.AnnotationHistoryRepository
java_import com.nuix.superutilities.annotations.AnnotationSyncSettings

$current_case = $utilities.getCaseFactory.open(case_directory)

repo = AnnotationHistoryRepository.new(history_db_file)
repo.setSnapshotFirstSync(true)

settings = AnnotationSyncSettings.new
settings.setSyncCustomMetadataEvents(true)
settings.setSyncTagEvents(true)
settings.setSyncItemSetEvents(true)
settings.setSyncExclusionEvents(true)
settings.setSyncCustodianEvents(true)
settings.setSyncProductionSetEvents(true)

puts "Begin history sync..."
repo.syncHistory($current_case,settings)

puts repo.buildSummary.toString

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

repo.close

$current_case.close