local found_zero = false

local function print_playback_time()
	local playback_time = mp.get_property_native("playback-time")
	if not (playback_time == 0) then
		mp.msg.info(string.format("%.06f,%f,%f,%f", playback_time, mp.get_property("width"), mp.get_property("height"), mp.get_property("container-fps")))
		mp.commandv("quit")
	else
		if found_zero then
			mp.msg.info(string.format("%.06f,%f,%f,%f", playback_time, mp.get_property("width"), mp.get_property("height"), mp.get_property("container-fps")))
			mp.commandv("quit")
		else
			found_zero = true
		end
	end
end

mp.register_event("playback-restart", print_playback_time)

