source_case_directory = 'C:\@NUIX\Cases\Ziggy_Annotated'
destination_case_directory = 'C:\@NUIX\Cases\Ziggy'
history_db_file = 'C:\Temp\AnnotationRepository.db'

# Load up super utilities
script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.annotations.AnnotationHistoryRepository
java_import com.nuix.superutilities.annotations.AnnotationSyncSettings
require 'csv'

class Logger
	class << self
		attr_accessor :log_file
		def log(obj)
			message = "#{Time.now.strftime("%Y%m%d %H:%M:%S")}: #{obj}"
			puts message
			File.open(@log_file,"a"){|f|f.puts message}
		end

		def log_file_only(obj)
			message = "#{Time.now.strftime("%Y%m%d %H:%M:%S")}: #{obj}"
			File.open(@log_file,"a"){|f|f.puts message}
		end
	end
end

time_stamp = Time.now.strftime("%Y%m%d_%H-%M-%S")
Logger.log_file = File.join(File.dirname(__FILE__),"#{time_stamp}_Log.txt")

def export_case_comparison(directory_a,directory_b,output_csv)
	a = $utilities.getCaseFactory.open(directory_a)
	b = $utilities.getCaseFactory.open(directory_b)

	Logger.log("Exporting case comparison CSV to: #{output_csv}")

	CSV.open(output_csv,"w:utf-8") do |csv|
		csv << ["** Custodian **","** SOURCE **","** DEST **","Matches"]
		a.getAllCustodians.each do |custodian|
			a_count = a.count("custodian:\"#{custodian}\"")
			b_count = b.count("custodian:\"#{custodian}\"")
			csv << [custodian,a_count,b_count,a_count == b_count]
		end

		csv << ["** Tag **","** SOURCE **","** DEST **","Matches"]
		a.getAllTags.each do |tag|
			a_count = a.count("tag:\"#{tag}\"")
			b_count = b.count("tag:\"#{tag}\"")
			csv << [tag,a_count,b_count,a_count == b_count]
		end

		csv << ["** Item Set **","** SOURCE **","** DEST **","Matches"]
		a.getAllItemSets.each do |item_set|
			item_set_name = item_set.getName
			a_count = item_set.getItems.size
			b_item_set = b.findItemSetByName(item_set_name)
			b_count = 0
			if b_item_set.nil? == false
				b_count = b_item_set.getItems.size
			end
			csv << [item_set_name,a_count,b_count,a_count == b_count]
		end

		csv << ["** Production Set **","** SOURCE **","** DEST **","Matches"]
		a.getProductionSets.each do |production_set|
			production_set_name = production_set.getName
			a_count = production_set.getItems.size
			b_production_set = b.findProductionSetByName(production_set_name)
			b_count = 0
			if b_production_set.nil? == false
				b_count = b_production_set.getItems.size
			end
			csv << [production_set_name,a_count,b_count,a_count == b_count]
		end

		csv << ["** Exclusion **","** SOURCE **","** DEST **","Matches"]
		a.getAllExclusions.each do |exclusion|
			a_count = a.count("exclusion:\"#{exclusion}\"")
			b_count = a.count("exclusion:\"#{exclusion}\"")
			csv << [exclusion,a_count,b_count,a_count == b_count]
		end
	end

	a.close
	b.close
end

export_case_comparison(source_case_directory,destination_case_directory,File.join(File.dirname(__FILE__),"#{time_stamp}_CaseComparison_BEFORE.csv"))

#==========================================#
# Record/sync annotations from source case #
#==========================================#

# Open the case
$current_case = $utilities.getCaseFactory.open(source_case_directory)

# Build repo instance
repo = AnnotationHistoryRepository.new(history_db_file)

# When true, first sync will attempt to snapshop state of certain annotations
# rather than just recorded this information from history events, may make things faster
# although it seems later skipping those history events captures by the sync is slow enough this
# may not really help anything
repo.setSnapshotFirstSync(false)

# Configure what we are going to sync into the repo
settings = AnnotationSyncSettings.new
settings.setSyncCustomMetadataEvents(true)
settings.setSyncTagEvents(true)
settings.setSyncItemSetEvents(true)
settings.setSyncExclusionEvents(true)
settings.setSyncCustodianEvents(true)
settings.setSyncProductionSetEvents(true)

# Get to syncing...
Logger.log "====================="
Logger.log "Begin history sync..."
Logger.log "====================="
repo.syncHistory($current_case,settings)

# Show a little report of how things went
Logger.log "\n\n\n"
Logger.log "======="
Logger.log "Summary"
Logger.log "======="

Logger.log repo.buildSummary.toString

# When true we will do a pass where we loop through the recorded events
# and show brief summary of what was recorded for each event, this pass
# is actually querying from the database
echo_back_recorded_events = true

if echo_back_recorded_events
	Logger.log "\n\n\n"
	Logger.log "============================"
	Logger.log "Echoing back recorded events"
	Logger.log "============================"

	repo.eachRecordedTagEvent(0) do |event|
		Logger.log event
	end

	repo.eachRecordedExclusionEvent(0) do |event|
		Logger.log event
	end

	repo.eachRecordedCustomMetadataEvent(0) do |event|
		Logger.log event
	end

	repo.eachRecordedItemSetEvent(0) do |event|
		Logger.log event
	end

	repo.eachRecordedCustodianEvent(0) do |event|
		Logger.log event
	end

	repo.eachRecordedProductionSetEvent(0) do |event|
		Logger.log event
	end
end

# Make sure we close the repo when we are done to make sure its SQLite connection
# against the DB file is closed
repo.close

# Good citizens always close the case when they are done
$current_case.close

#========================================#
# Replay annotations to destination case #
#========================================#

# Open the destination case
$current_case = $utilities.getCaseFactory.open(destination_case_directory)

# Build repo instance
repo = AnnotationHistoryRepository.new(history_db_file)

Logger.log "============================"
Logger.log "Playing back recorded events"
Logger.log "============================"

repo.eachRecordedTagEvent(0) do |event|
	Logger.log event
	event.replay($current_case)
end

repo.eachRecordedExclusionEvent(0) do |event|
	Logger.log event
	event.replay($current_case)
end

repo.eachRecordedCustomMetadataEvent(0) do |event|
	Logger.log event
	event.replay($current_case)
end

repo.eachRecordedItemSetEvent(0) do |event|
	Logger.log event
	event.replay($current_case)
end

repo.eachRecordedCustodianEvent(0) do |event|
	Logger.log event
	event.replay($current_case)
end

repo.eachRecordedProductionSetEvent(0) do |event|
	Logger.log event
	event.replay($current_case)
end

$current_case.close
repo.close

export_case_comparison(source_case_directory,destination_case_directory,File.join(File.dirname(__FILE__),"#{time_stamp}_CaseComparison_AFTER.csv"))