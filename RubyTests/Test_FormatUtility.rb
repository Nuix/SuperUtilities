script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.misc.FormatUtility

puts "===== HEX => BYTES / BYTES => HEX ====="
md5_string = "8b69e50c471a3b73b021d2182c372eca"
md5_bytes = FormatUtility.hexToBytes(md5_string)
converted_md5_string = FormatUtility.bytesToHex(md5_bytes)

puts "md5_string: #{md5_string}"
puts "converted_md5_string: #{converted_md5_string}"

puts "===== ELAPSED STRING ====="
def to_seconds(days=0,hours=0,minutes=0,seconds=0)
	return seconds + (minutes * 60) + (hours * 60 * 60) + (days * 24 * 60 * 60)
end

5.times do
	days = rand(0..3)
	hours = rand(0..23)
	minutes = rand(0..59)
	seconds = rand(0..59)

	elapsed_string = FormatUtility.getInstance.secondsToElapsedString(to_seconds(days,hours,minutes,seconds))
	puts "#{days} days, #{hours} hours, #{minutes} minutes and #{seconds} seconds => #{elapsed_string}"
end

puts "===== RESOLVE PLACEHOLDERS ====="
template_string = "Hello {name}, I heard your favorite color is {color}."
placeholder_values = {
	"name" => "Bob",
	"color" => "Green",
}
puts "template_string: #{template_string}"
puts "placeholder_values:"
placeholder_values.each do |key,value|
	puts "#{key} => #{value}"
end
puts "Resolved: #{FormatUtility.getInstance.resolvePlaceholders(template_string,placeholder_values)}"

puts "===== TOKENIZE TEXT ===="
sample_text =<<SAMPLETEXT
Hello Moses,

Here's everyone who has yet to turn in a TPS report:
1.Kyler Douglas from Games & Tools
2.Jairo Schamberger from Jewelry
3.Douglas from Games & Tools
4.Schamberger from Jewelry
5.Schamberger from Jewelry
6.Ena Baumbach from Health
7.McLaughlin from Music
And with that we move on to: 

Sometimes I feel like:
Supah Beetle: Arsenal
with the power of Energy Resistance which I use to keep the Grocery, Shoes & Tools department at bay!
SAMPLETEXT

puts "sample_text:"
puts sample_text

puts "Tokens:"
puts FormatUtility.tokenizeText(sample_text)

puts "===== GET HTML TEXT ====="
test_html_source =<<HTML
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>Report</title>
	<style>
	html, body {
		margin: 0px;
		padding: 0px;
		height: 100%;
	}
	</style>
</head>
<body>
	<h1>Document Header</h1>
	
	Here is a list of colors
	<ul>
	<li>Red</li>
	<li>Green</li>
	<li>Blue</li>
	</ul>

	Visit us on <a href="https://github.com/nuix">GitHub</a>.
</body>
</html>
HTML

puts "test_html_source:"
puts test_html_source

puts "HTML Text:"
puts FormatUtility.getHtmlText(test_html_source)