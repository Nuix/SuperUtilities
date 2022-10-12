script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.annotations.BulkRedactor
java_import com.nuix.superutilities.annotations.BulkRedactorSettings
java_import com.nuix.superutilities.SuperUtilities

# Initialize super utilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

case_directory = "D:\\cases\\FakeData_1662662781"
temp_directory = "D:\\Temp\\BulkRedactorTemp"
markup_set_name = "Redaction_Test_#{Time.now.to_i}"
scope_query = "kind:email"
expressions = [
	"\\bname\\b",
]
entity_types = [
	"email",
	"company",
	"person",
]

# Open a case to test against
$current_case = $utilities.getCaseFactory.open(case_directory)
scope_items = $current_case.search(scope_query,{"limit"=>10})

# Create settings object and then configure settings
brs = BulkRedactorSettings.new
brs.setMarkupSetName("BulkRedactorTest_#{Time.now.to_i}")
brs.setTempDirectory(temp_directory)
brs.setExpressions(expressions)
brs.setNamedEntityTypes(entity_types)
brs.setApplyRedactions(false)
brs.setApplyHighLights(false)

# How many concurrent threads should be ran.  Note that more is not always better!
concurrency = 4

# Create bulk redactor
br = BulkRedactor.new

# Add callback for messages it logs
br.whenMessageLogged do |message|
	puts message
end

# Find and markup expressions based on settings, this then returns
# NuixImageAnnotationRegion objects for each match found
regions = br.findAndMarkup($current_case,brs,scope_items,concurrency)

# Iterate each found region and print summary about it
regions.each do |region|
	puts region.toString
end

# Close the case we opened
$current_case.close