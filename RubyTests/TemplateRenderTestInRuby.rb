require 'erb'
require 'cgi'
include ERB::Util
$template_source = File.read(File.join(File.dirname(__FILE__),"ItemTemplate1.erb"))
$compiled_erb = ERB.new($template_source,nil,'<>')
def render(item)
	$result = $compiled_erb.result(binding)
end

$current_case = $utilities.getCaseFactory.open("C:\\@Nuix\\Cases\\Ziggy")
item = $current_case.search("Cat").first
render(item)
puts $result
$current_case.close