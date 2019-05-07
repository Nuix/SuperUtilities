script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.loadfiles.OptLoadFile

test_file = File.join(script_directory,"TestData","LegalExport","loadfile.opt")
opt = OptLoadFile.fromFile(test_file)
puts "Records: #{opt.getRecords.size}"
puts "First Page Records: #{opt.getFirstPageRecords.size}"
puts "Volume Names: #{opt.getVolumeNames.join("; ")}"