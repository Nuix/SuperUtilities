script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

class Timer
	class << self
		def time_block(&block)
			timer = new
			timer.start
			block.call(timer)
			timer.stop
			return timer
		end
	end

	attr_accessor :start
	attr_accessor :stop

	def initialize
	end

	def start
		@start = Time.now
		@stop = nil
	end

	def stop
		@stop = Time.now
	end

	def elapsed_seconds
		return (@stop || Time.now) - @start
	end

	def to_s
		return Time.at(elapsed_seconds).gmtime.strftime("%H:%M:%S")
	end
end

$current_case = $utilities.getCaseFactory.open("C:\\@Nuix\\Cases\\Ziggy")

super_iutil = $su.getSuperItemUtility
items = $current_case.search("")

t = Timer.time_block do
	1.times do
		result = super_iutil.findContainerAncestors(items)
		puts "Container Ancestors: #{result.size}"
	end
end
puts t

$current_case.close