package net.teamfruit.skcraft.launcher;

import java.awt.Image;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.teamfruit.skcraft.launcher.model.tips.TipInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public class Tip extends TipInfo {
	private Image thumbImage;
}
