script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.annotations.BulkRedactor
java_import com.nuix.superutilities.SuperUtilities

# Initialize super utilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

scope_items = $current_case.searchUnsorted("kind:email")

markup_set_name = "Redaction_Test_#{Time.now.to_i}"
markup_set = $current_case.createMarkupSet(markup_set_name)
puts "Created markup set #{markup_set_name}"

br = BulkRedactor.new

br.whenMessageLogged do |message|
	puts message
end

# terms = [
# 	"fake",
# 	"cat",
# 	"dog",
# 	"mouse",
# 	"monkey",
# ]
# br.findAndRedactTerms($current_case,"D:\\Temp\\BulkRedactorTemp",markup_set,terms,scope_items)

expressions = [
	"\\bname\\b",
]
br.findAndRedactExpressions($current_case,"D:\\Temp\\BulkRedactorTemp",markup_set,expressions,scope_items)