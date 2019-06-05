script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.loadfiles.OptLoadFile

test_file = File.join(script_directory,"TestData","LegalExport","loadfile.opt")
opt = OptLoadFile.fromFile(test_file)
puts "First Page Records: #{opt.getFirstPageRecords.size}"
puts "Volume Names: #{opt.getVolumeNames.join("; ")}"
puts "Records: #{opt.getRecords.size}"
opt.getRecords.each_with_index do |opt_record,opt_record_index|
	puts "==== #{opt_record_index+1} / #{opt.getRecords.size} ===="
	puts "ID: #{opt_record.getId}"
	puts "Volume: #{opt_record.getVolume}"
	puts "Path: #{opt_record.getPath}"
	puts "isFirstPage: #{opt_record.isFirstPage}"
	puts "Box: #{opt_record.getBox}"
	puts "Folder: #{opt_record.getFolder}"
	puts "Pages: #{opt_record.getPages}"
end