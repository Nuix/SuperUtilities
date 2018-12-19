report_file = "D:\\Temp\\IR.xlsx"

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

custodians = $current_case.getAllCustodians

script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
java_import com.nuix.superutilities.reporting.IntersectionReport
$su = SuperUtilities.init($utilities,NUIX_VERSION)

report = IntersectionReport.new(report_file)

terms.each do |term|
	report.addRowCriterion(term.capitalize,term)
end

custodians.each do |custodian|
	report.addColCriterion(custodian, "custodian:\"#{custodian}\"")
end

report.addScriptedValueGenerator("Emails") do |nuixCase,query|
	extended_query = "(#{query}) AND kind:email AND has-exclusion:0"
	next nuixCase.count(extended_query)
end

report.addScriptedValueGenerator("Email Families") do |nuixCase,query|
	extended_query = "(#{query}) AND kind:email AND has-exclusion:0"
	items = nuixCase.search(extended_query)
	items = $utilities.getItemUtility.findFamilies(items)
	items = items.reject{|i|i.isExcluded}
	next items.size
end

report.setRowCategoryLabel("Terms")
report.setColPrimaryCategoryLabel("Custodians")
report.generate($current_case,"TestSheet1")