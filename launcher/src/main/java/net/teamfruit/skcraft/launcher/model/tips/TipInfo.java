package net.teamfruit.skcraft.launcher.model.tips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipInfo {
	private String desc;
	private String thumb;
}
