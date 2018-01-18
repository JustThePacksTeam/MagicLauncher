package net.teamfruit.skcraft.launcher.model.tips;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipInfoList {

    public static final int MIN_VERSION = 1;

    private int minimumVersion;
	private List<TipInfo> tips;

}
