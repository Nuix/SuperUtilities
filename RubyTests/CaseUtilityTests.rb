script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

case_utility = $su.getCaseUtility
case_infos = case_utility.findCaseInformation("C:\\@Nuix\\Cases")
case_infos.each do |case_info|
	puts "="*20
	puts case_info
	case_info.withCase(true) do |nuix_case|
		puts "Items: #{nuix_case.count("")}"
	end
end