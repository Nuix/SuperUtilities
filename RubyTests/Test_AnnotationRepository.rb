script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.annotations.AnnotationRepository

$current_case = $utilities.getCaseFactory.open('D:\cases\FakeData_7.8')

db_file = "D:\\Temp\\Annotations_#{Time.now.to_i}.db"
puts "DB File: #{db_file}"
repo = AnnotationRepository.new(db_file)
repo.storeAllMarkupSets($current_case)
repo.applyMarkupsFromDatabaseToCase($current_case,false)
repo.close
$current_case.close