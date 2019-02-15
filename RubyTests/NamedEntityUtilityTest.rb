script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.namedentities.NamedEntityUtility
java_import com.nuix.superutilities.namedentities.NamedEntityRedactionSettings
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

$current_case = $utilities.getCaseFactory.open("D:\\cases\\FakeData_7.8")

rs = NamedEntityRedactionSettings.new
rs.addAllBuiltInEntities

neu = NamedEntityUtility.new
last_progress = Time.now

neu.whenProgressUpdated do |current,total|
	if (Time.now - last_progress) > 1
		puts "#{current}/#{total}"
		last_progress = Time.now
	end
end

neu.whenMessageGenerated do |message|
	puts message
end

result = neu.recordRedactedCopies($current_case,rs)
puts result.toString

NamedEntityUtility.saveRedactionProfile("D:\\temp\\RedactionProfile_#{Time.now.to_i}.profile",result,rs)

$current_case.close