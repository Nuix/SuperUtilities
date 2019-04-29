# Demonstrates basic usage of placeholder resolver and multi value placeholders.

script_directory = File.dirname(__FILE__)

# Initialize super utilities
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
java_import com.nuix.superutilities.misc.PlaceholderResolver
java_import com.nuix.superutilities.misc.NamedStringList
$su = SuperUtilities.init($utilities,NUIX_VERSION)

pr = PlaceholderResolver.new

animals = ["Cat","Dog","Bird"]
colors = ["Green","Red","Blue","Black","White"]
names = ["Fido","Fluffy","|St. Awesome|","\\/iolet"]

nsl_animals = NamedStringList.new
nsl_animals.setName("animals")
nsl_animals.addAll(animals)

nsl_colors = NamedStringList.new
nsl_colors.setName("colors")
nsl_colors.addAll(colors)

nsl_names = NamedStringList.new
nsl_names.setName("names")
nsl_names.addAll(names)

nsls = [
	nsl_animals,
	nsl_colors,
	nsl_names
]

pr.set("age","5")

template = "The {animals} '{names}' is {age} and has a {colors} name tag."
puts "Template: #{template}"
result = pr.resolveTemplateMultiValues(template,nsls)
result.each_with_index do |value,value_index|
	puts "#{(value_index+1).to_s.rjust(3)}.) #{value}"
end

template = "C:\\{age}\\{animals}\\{colors}NameTag_{names}.txt"
puts "Path Template: #{template}"
result = pr.resolveTemplatePathMultiValues(template,nsls)
result.each_with_index do |value,value_index|
	puts "#{(value_index+1).to_s.rjust(3)}.) #{value}"
end

=begin
OUTPUT:
Template: The {animals} '{names}' is {age} and has a {colors} name tag.
  1.) The Cat '\/iolet' is 5 and has a Blue name tag.
  2.) The Cat '|St. Awesome|' is 5 and has a White name tag.
  3.) The Cat 'Fluffy' is 5 and has a Blue name tag.
  4.) The Dog '|St. Awesome|' is 5 and has a White name tag.
  5.) The Cat '\/iolet' is 5 and has a White name tag.
  6.) The Bird 'Fluffy' is 5 and has a Green name tag.
  7.) The Bird '|St. Awesome|' is 5 and has a Black name tag.
  8.) The Cat '|St. Awesome|' is 5 and has a Blue name tag.
  9.) The Cat 'Fluffy' is 5 and has a White name tag.
 10.) The Dog '\/iolet' is 5 and has a White name tag.
 11.) The Bird '|St. Awesome|' is 5 and has a Green name tag.
 12.) The Bird 'Fido' is 5 and has a White name tag.
 13.) The Bird 'Fido' is 5 and has a Black name tag.
 14.) The Dog 'Fido' is 5 and has a Black name tag.
 15.) The Dog 'Fluffy' is 5 and has a Green name tag.
 16.) The Bird '|St. Awesome|' is 5 and has a White name tag.
 17.) The Cat '|St. Awesome|' is 5 and has a Black name tag.
 18.) The Cat '\/iolet' is 5 and has a Black name tag.
 19.) The Bird '|St. Awesome|' is 5 and has a Blue name tag.
 20.) The Cat '|St. Awesome|' is 5 and has a Green name tag.
 21.) The Cat '|St. Awesome|' is 5 and has a Red name tag.
 22.) The Bird '\/iolet' is 5 and has a Black name tag.
 23.) The Bird 'Fluffy' is 5 and has a Red name tag.
 24.) The Bird 'Fido' is 5 and has a Blue name tag.
 25.) The Dog 'Fluffy' is 5 and has a White name tag.
 26.) The Bird '\/iolet' is 5 and has a Red name tag.
 27.) The Dog 'Fluffy' is 5 and has a Red name tag.
 28.) The Cat 'Fido' is 5 and has a Black name tag.
 29.) The Cat 'Fido' is 5 and has a Blue name tag.
 30.) The Bird 'Fido' is 5 and has a Red name tag.
 31.) The Bird '\/iolet' is 5 and has a Blue name tag.
 32.) The Dog '\/iolet' is 5 and has a Green name tag.
 33.) The Dog '\/iolet' is 5 and has a Blue name tag.
 34.) The Dog '|St. Awesome|' is 5 and has a Green name tag.
 35.) The Cat '\/iolet' is 5 and has a Green name tag.
 36.) The Cat 'Fluffy' is 5 and has a Green name tag.
 37.) The Bird 'Fluffy' is 5 and has a Blue name tag.
 38.) The Bird 'Fluffy' is 5 and has a White name tag.
 39.) The Dog 'Fido' is 5 and has a Red name tag.
 40.) The Bird '|St. Awesome|' is 5 and has a Red name tag.
 41.) The Cat 'Fido' is 5 and has a Green name tag.
 42.) The Bird '\/iolet' is 5 and has a Green name tag.
 43.) The Cat 'Fluffy' is 5 and has a Red name tag.
 44.) The Cat 'Fluffy' is 5 and has a Black name tag.
 45.) The Dog '|St. Awesome|' is 5 and has a Red name tag.
 46.) The Dog 'Fluffy' is 5 and has a Black name tag.
 47.) The Bird 'Fido' is 5 and has a Green name tag.
 48.) The Bird 'Fluffy' is 5 and has a Black name tag.
 49.) The Dog 'Fido' is 5 and has a White name tag.
 50.) The Bird '\/iolet' is 5 and has a White name tag.
 51.) The Dog '\/iolet' is 5 and has a Black name tag.
 52.) The Dog '|St. Awesome|' is 5 and has a Black name tag.
 53.) The Dog 'Fido' is 5 and has a Blue name tag.
 54.) The Cat 'Fido' is 5 and has a Red name tag.
 55.) The Dog '\/iolet' is 5 and has a Red name tag.
 56.) The Dog '|St. Awesome|' is 5 and has a Blue name tag.
 57.) The Cat 'Fido' is 5 and has a White name tag.
 58.) The Dog 'Fido' is 5 and has a Green name tag.
 59.) The Cat '\/iolet' is 5 and has a Red name tag.
 60.) The Dog 'Fluffy' is 5 and has a Blue name tag.
Path Template: C:\{age}\{animals}\{colors}NameTag_{names}.txt
  1.) C:\5\Dog\BlueNameTag_Fido.txt
  2.) C:\5\Cat\BlueNameTag_Fido.txt
  3.) C:\5\Dog\RedNameTag__St. Awesome_.txt
  4.) C:\5\Bird\RedNameTag____iolet.txt
  5.) C:\5\Dog\WhiteNameTag_Fluffy.txt
  6.) C:\5\Dog\GreenNameTag____iolet.txt
  7.) C:\5\Dog\BlackNameTag__St. Awesome_.txt
  8.) C:\5\Cat\GreenNameTag_Fido.txt
  9.) C:\5\Bird\BlackNameTag_Fluffy.txt
 10.) C:\5\Cat\BlackNameTag__St. Awesome_.txt
 11.) C:\5\Cat\RedNameTag_Fido.txt
 12.) C:\5\Bird\RedNameTag_Fluffy.txt
 13.) C:\5\Cat\GreenNameTag_Fluffy.txt
 14.) C:\5\Bird\GreenNameTag____iolet.txt
 15.) C:\5\Cat\BlueNameTag_Fluffy.txt
 16.) C:\5\Dog\WhiteNameTag_Fido.txt
 17.) C:\5\Bird\BlueNameTag__St. Awesome_.txt
 18.) C:\5\Dog\BlueNameTag_Fluffy.txt
 19.) C:\5\Cat\WhiteNameTag_Fluffy.txt
 20.) C:\5\Bird\GreenNameTag__St. Awesome_.txt
 21.) C:\5\Bird\BlackNameTag____iolet.txt
 22.) C:\5\Cat\BlueNameTag____iolet.txt
 23.) C:\5\Bird\BlueNameTag_Fluffy.txt
 24.) C:\5\Cat\RedNameTag__St. Awesome_.txt
 25.) C:\5\Dog\GreenNameTag_Fido.txt
 26.) C:\5\Cat\GreenNameTag__St. Awesome_.txt
 27.) C:\5\Bird\RedNameTag_Fido.txt
 28.) C:\5\Bird\WhiteNameTag____iolet.txt
 29.) C:\5\Dog\BlackNameTag_Fluffy.txt
 30.) C:\5\Dog\RedNameTag_Fluffy.txt
 31.) C:\5\Dog\WhiteNameTag__St. Awesome_.txt
 32.) C:\5\Dog\RedNameTag_Fido.txt
 33.) C:\5\Cat\WhiteNameTag____iolet.txt
 34.) C:\5\Dog\BlackNameTag_Fido.txt
 35.) C:\5\Bird\BlackNameTag_Fido.txt
 36.) C:\5\Cat\WhiteNameTag_Fido.txt
 37.) C:\5\Cat\RedNameTag____iolet.txt
 38.) C:\5\Bird\GreenNameTag_Fluffy.txt
 39.) C:\5\Bird\WhiteNameTag__St. Awesome_.txt
 40.) C:\5\Cat\GreenNameTag____iolet.txt
 41.) C:\5\Bird\BlueNameTag____iolet.txt
 42.) C:\5\Bird\GreenNameTag_Fido.txt
 43.) C:\5\Bird\BlackNameTag__St. Awesome_.txt
 44.) C:\5\Dog\GreenNameTag__St. Awesome_.txt
 45.) C:\5\Cat\BlackNameTag_Fluffy.txt
 46.) C:\5\Dog\BlueNameTag____iolet.txt
 47.) C:\5\Bird\BlueNameTag_Fido.txt
 48.) C:\5\Cat\BlueNameTag__St. Awesome_.txt
 49.) C:\5\Cat\BlackNameTag____iolet.txt
 50.) C:\5\Cat\BlackNameTag_Fido.txt
 51.) C:\5\Dog\RedNameTag____iolet.txt
 52.) C:\5\Cat\RedNameTag_Fluffy.txt
 53.) C:\5\Dog\GreenNameTag_Fluffy.txt
 54.) C:\5\Dog\BlueNameTag__St. Awesome_.txt
 55.) C:\5\Cat\WhiteNameTag__St. Awesome_.txt
 56.) C:\5\Bird\RedNameTag__St. Awesome_.txt
 57.) C:\5\Dog\BlackNameTag____iolet.txt
 58.) C:\5\Bird\WhiteNameTag_Fluffy.txt
 59.) C:\5\Dog\WhiteNameTag____iolet.txt
 60.) C:\5\Bird\WhiteNameTag_Fido.txt
=end