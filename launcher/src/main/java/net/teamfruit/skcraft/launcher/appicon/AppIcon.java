package net.teamfruit.skcraft.launcher.appicon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import com.skcraft.launcher.swing.SwingHelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class AppIcon {
	public static void setFrameIconSet(JFrame frame, IconSet iconSet) {
		frame.setIconImages(iconSet.getIcons());
	}

	public static IconSet getTaskIcon(IconSet iconSet, Color color) {
		final List<BufferedImage> icons = new ArrayList<BufferedImage>();
		for (BufferedImage base: iconSet.getIcons()) {
			int image_width = base.getWidth();
			int image_height = base.getHeight();
			int icon_size = Math.min(image_width, image_height)/2;
			BufferedImage image = new BufferedImage(image_width, image_height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		    g.drawImage(base, 0, 0, null);
		    g.setColor(color);
		    g.fillOval(image_width-icon_size, image_height-icon_size, icon_size, icon_size);
		    g.dispose();
		    icons.add(image);
		}
		return new AppIconSet(icons);
	}

	public interface IconSet {
		BufferedImage getIcon();

		List<BufferedImage> getIcons();
	}

	@RequiredArgsConstructor
	private static class AppIconSet implements IconSet {
		private final @Getter List<BufferedImage> icons;

		@Override
		public BufferedImage getIcon() {
			if (!icons.isEmpty())
				return icons.get(0);
			return null;
		}
	}

	public static IconSet getIconSet(Class<?> clazz, String... paths) {
		final List<BufferedImage> icons = new ArrayList<BufferedImage>();
		for (String path : paths) {
			BufferedImage image = SwingHelper.readBufferedImage(clazz, path);
			if (image!=null)
				icons.add(image);
		}
		return new AppIconSet(icons);
	}

	private static final @Getter(lazy = true) IconSet appIconSet = createAppIconSet();

	private static IconSet createAppIconSet() {
		final Class<?> clazz = AppIcon.class;
		final String path_format = "icon_%s.png";
		final String[] icon_paths = {
				String.format(path_format, "16x16"),
				String.format(path_format, "32x32"),
				String.format(path_format, "48x48"),
				String.format(path_format, "64x64"),
				String.format(path_format, "128x128"),
				String.format(path_format, "256x256"),
		};
		return getIconSet(clazz, icon_paths);
	}
}
