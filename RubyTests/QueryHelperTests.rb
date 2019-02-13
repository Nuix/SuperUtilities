script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.query.QueryHelper

$current_case = $utilities.getCaseFactory.open("D:\\cases\\Ziggy")

# Try a handful of years
error_count = 0
(1980..2050).each do |year|
	query = QueryHelper.yearRangeQuery(year)
	begin
		$current_case.search(query,{:limit => 0})
		success = true
	rescue Exception => exc
		success = false
	end
	puts "[#{year}]: #{query}, Success: #{success}"
	error_count += 1 if success == false
end

# Try combination of years and months
(1980..2050).each do |year|
	(1..12).each do |month|
		query = QueryHelper.yearMonthRangeQuery(year,month)
		begin
			$current_case.search(query,{:limit => 0})
			success = true
		rescue Exception => exc
			success = false
		end
		puts "[#{year}]: #{query}, Success: #{success}"
		error_count += 1 if success == false
	end
end

puts "Errors: #{error_count}"

# Test joining methods
query_pieces = [
	nil,
	"",
	"cat",
	"dog",
	"(kind:(email OR spreadsheet))"
]

puts QueryHelper.joinByAnd(query_pieces)
puts QueryHelper.parenThenJoinByAnd(query_pieces)
puts QueryHelper.joinByOr(query_pieces)
puts QueryHelper.parenThenJoinByOr(query_pieces)

$current_case.close