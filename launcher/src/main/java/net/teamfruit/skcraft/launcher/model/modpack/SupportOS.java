package net.teamfruit.skcraft.launcher.model.modpack;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.skcraft.launcher.model.minecraft.PlatformDeserializer;
import com.skcraft.launcher.model.minecraft.PlatformSerializer;
import com.skcraft.launcher.util.Platform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class SupportOS {
	@JsonDeserialize(contentUsing = PlatformDeserializer.class)
	@JsonSerialize(contentUsing = PlatformSerializer.class)
	private List<Platform> allow;

	@JsonDeserialize(contentUsing = PlatformDeserializer.class)
	@JsonSerialize(contentUsing = PlatformSerializer.class)
	private List<Platform> deny;
}
