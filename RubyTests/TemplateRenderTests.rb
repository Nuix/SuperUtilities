script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

$current_case = $utilities.getCaseFactory.open("D:\\Cases\\Ziggy")
items = $current_case.search("cat").take(5)

t = $su.createTemplateExporter(File.join(script_directory,"ItemTemplate1.erb"))

items.each do |item|
	t.renderToFile(item,"C:\\temp\\su\\template\\#{item.getGuid}.txt",{})
end

t = $su.createTemplateExporter(File.join(script_directory,"ItemTemplate2.erb"))
data = {}


items.each do |item|
	t.renderToFile(item,java.io.File.new("C:\\temp\\su\\template\\#{item.getGuid}.html"),data)
	t.renderToPdf(item,java.io.File.new("C:\\temp\\su\\template\\#{item.getGuid}.pdf"),data)
end

itesm = $current_case.search("")
start = Time.now
items.each do |item|
	t.renderToPdf(item,java.io.File.new("C:\\temp\\su\\template\\test.pdf"),data)
end
finish = Time.now
puts finish - start

$current_case.close