case_directory = 'C:\@NUIX\Cases\Ziggy_Annotated'
history_db_file = 'C:\Temp\AnnotationRepository.db'
echo_back_recorded_events = true

# Load up super utilities
script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.annotations.AnnotationHistoryRepository
java_import com.nuix.superutilities.annotations.AnnotationSyncSettings

# Open the case
$current_case = $utilities.getCaseFactory.open(case_directory)

# Build repo instance
repo = AnnotationHistoryRepository.new(history_db_file)

# Show a little report of how things went
puts "\n\n\n"
puts "======="
puts "Summary"
puts "======="

puts repo.buildSummary.toString

if echo_back_recorded_events
	puts "\n\n\n"
	puts "========================="
	puts "Replaying recorded events"
	puts "========================="

	repo.eachRecordedTagEvent(0) do |event|
		puts event
	end

	repo.eachRecordedExclusionEvent(0) do |event|
		puts event
	end

	repo.eachRecordedCustomMetadataEvent(0) do |event|
		puts event
	end

	repo.eachRecordedItemSetEvent(0) do |event|
		puts event
	end

	repo.eachRecordedCustodianEvent(0) do |event|
		puts event
	end

	repo.eachRecordedProductionSetEvent(0) do |event|
		puts event
	end
end

# Make sure we close the repo when we are done to make sure its SQLite connection
# against the DB file is closed
repo.close

# Good citizens always close the case when they are done
$current_case.close



case_directory = 'C:\@NUIX\Cases\Ziggy'

# Open the destination case
$current_case = $utilities.getCaseFactory.open(case_directory)

# Build repo instance
repo = AnnotationHistoryRepository.new(history_db_file)

puts "============================"
puts "Playing back recorded events"
puts "============================"

repo.eachRecordedTagEvent(0) do |event|
	puts event
	event.replay($current_case)
end

repo.eachRecordedExclusionEvent(0) do |event|
	puts event
	event.replay($current_case)
end

repo.eachRecordedCustomMetadataEvent(0) do |event|
	puts event
	event.replay($current_case)
end

repo.eachRecordedItemSetEvent(0) do |event|
	puts event
	event.replay($current_case)
end

repo.eachRecordedCustodianEvent(0) do |event|
	puts event
	event.replay($current_case)
end

repo.eachRecordedProductionSetEvent(0) do |event|
	puts event
	event.replay($current_case)
end

$current_case.close
repo.close