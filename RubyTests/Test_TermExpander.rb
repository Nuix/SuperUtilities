script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.misc.TermExpander
java_import com.nuix.superutilities.misc.SimilarityCalculation
java_import com.nuix.superutilities.query.QueryHelper

$current_case = $utilities.getCaseFactory.open('D:\cases\FakeData_1552095540_Hare\Pearlie Morar')

inputs = [
	"ca*",
	"cat*",
	"ale*",
	"*ale",
	"*ale*",
	"ale~",
	"ale~0.5",
	"ale~0.6",
	"ale~0.65",
	"ale~0.7",
	"*ale",
	"ca?",
	"ca??",
	"ca5~",
	"jason~",
	"jason~0.1",
	"jason~0.2",
	"jason~0.3",
	"jason~0.4",
	"jason~0.5",
	"jason~0.6",
	"jason~0.7",
	"jason~0.8",
	"jason~0.9",
	"jason~1.0",
]

# Whether content terms should be included
content = true

# Whether properties terms should be included
properties = true

# Scope query restricting which items' terms are included
scope_query = ""

term_expander = TermExpander.new
term_expander.setFuzzyResolutionAlgorithm(SimilarityCalculation::Nuix)

pass = 0
fail = 0

inputs.each do |input|
	puts "[[ INPUT: #{input} ]]"
	expanded_terms = term_expander.expandTerm($current_case,content,properties,input,scope_query)
	puts "Expanded Terms: #{expanded_terms.size}"
	direct_hit_count = $current_case.count(input)
	term_query = QueryHelper.joinByOr(expanded_terms.map{|et|et.getMatchedTerm})
	term_hit_count = $current_case.count(term_query)
	puts "Nuix Hits on Input: #{direct_hit_count}"
	puts "Nuix Hits OR'ed Terms: #{term_hit_count}"
	counts_match = term_hit_count == direct_hit_count
	puts "Match: #{counts_match}"
	if counts_match
		pass += 1
	else
		fail += 1
	end

	puts "\n\n"
end

puts "Pass: #{pass}"
puts "Fail: #{fail}"

$current_case.close