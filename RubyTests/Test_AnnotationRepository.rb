script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.annotations.AnnotationRepository
java_import com.nuix.superutilities.annotations.AnnotationMatchingMethod

$current_case = $utilities.getCaseFactory.open('D:\cases\FakeData_7.8')

db_file = "D:\\Temp\\Annotations_#{Time.now.to_i}.db"
puts "DB File: #{db_file}"

last_progress = Time.now
repo = AnnotationRepository.new(db_file)
repo.whenMessageLogged do |message|
	puts message
end
repo.whenProgressUpdated do |current,total|
	if (Time.now - last_progress > 1) || current == total
		puts "#{current}/#{total}"
		last_progress = Time.now
	end
end
repo.storeAllMarkupSets($current_case)
repo.applyMarkupsFromDatabaseToCase($current_case,false,AnnotationMatchingMethod::GUID)
repo.close

$current_case.close