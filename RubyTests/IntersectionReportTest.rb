report_file = "D:\\Temp\\IR_#{Time.now.to_i}.xlsx"

terms = [
	"file", "control", "list", "access", "dos",
	"synchronize", "com", "nuix", "well", "read",
	"system", "known", "builtin", "authority",
	"group", "users", "full", "message", "net",
	"hauck", "from", "fake", "org", "name",
	"archive", "this", "hidden", "execute",
	"version", "created", "administrators",
	"authenticated", "only", "part", "modified",
	"type", "accessed", "date", "content",
]

opened_case = false
if $current_case.nil?
	puts "Opening case..."
	$current_case = $utilities.getCaseFactory.open('D:\cases\FakeDataCompound')
	opened_case = true
end

scope_query = ""

custodians = $current_case.getAllCustodians

script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
java_import com.nuix.superutilities.reporting.IntersectionReport
java_import com.nuix.superutilities.reporting.IntersectionReportSheetConfiguration
$su = SuperUtilities.init($utilities,NUIX_VERSION)

report = IntersectionReport.new(report_file)
sheet_config = IntersectionReportSheetConfiguration.new

sheet_config.setScopeQuery(scope_query)

terms.each do |term|
	puts "Adding Term: #{term}"
	sheet_config.addRowCriterion(term.capitalize,term)
end

custodians.each do |custodian|
	puts "Adding Custodian: #{custodian}"
	sheet_config.addColCriterion(custodian, "custodian:\"#{custodian}\"")
end

sheet_config.addScriptedValueGenerator("Emails") do |nuixCase,query|
	extended_query = "(#{query}) AND kind:email AND has-exclusion:0"
	next nuixCase.count(extended_query)
end

sheet_config.addScriptedValueGenerator("Email Families") do |nuixCase,query|
	extended_query = "(#{query}) AND kind:email AND has-exclusion:0"
	items = nuixCase.search(extended_query)
	items = $utilities.getItemUtility.findFamilies(items)
	items = items.reject{|i|i.isExcluded}
	next items.size
end

sheet_config.setRowCategoryLabel("Terms")
sheet_config.setColPrimaryCategoryLabel("Custodians")
report.generate($current_case,"TestSheet1",sheet_config)

if opened_case
	puts "Closing case opened by script..."
	$current_case.close
end