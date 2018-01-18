package net.teamfruit.skcraft.launcher.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.skcraft.launcher.Instance;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.swing.SwingHelper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import net.teamfruit.skcraft.launcher.util.ImageSizes;
import net.teamfruit.skcraft.launcher.util.SizeData;

@Log
public class InstanceTableCellPanel extends JPanel {
	public static class DefaultIcons {
		public static final Image instanceMissingThumbBackground = SwingHelper.createImage(Launcher.class, "instance_missing_thumb_background.png");
		public static final Image instanceLoadingBackground = SwingHelper.createImage(Launcher.class, "instance_loading_background.png");
		public static final Image instanceNoThumbInstanceIcon = SwingHelper.createImage(Launcher.class, "instance_no_thumb_background.png");
		public static final Image instanceTitleBar = SwingHelper.createImage(Launcher.class, "instance_title_bar.png");
		public static final Image instanceDownloadIcon = SwingHelper.createImage(Launcher.class, "instance_download_icon.png");
		public static final Image instancePlayIcon = SwingHelper.createImage(Launcher.class, "instance_play_icon.png");
		public static final Image instanceOnlineIcon = SwingHelper.createImage(Launcher.class, "instance_online_icon.png");
	}

	private final JComponent parent;
	private @Getter @Setter String title;
	private @Getter @Setter boolean showPlayIcon;
	private @Getter @Setter boolean showSelected;
	private @Getter @Setter boolean notdownloaded;
	private @Getter @Setter boolean online;
	private @Getter Image thumb;
	private @Getter Instance instance;

	public void setThumb(final Image thumb) {
		this.thumb = thumb;
		this.parent.repaint();
	}

	public InstanceTableCellPanel(JComponent parent) {
		SwingHelper.removeOpaqueness(this);
		if (parent==null)
			parent = this;
		this.parent = parent;
		setPreferredSize(new Dimension(250, 64));
	}

	public void setInstance(final Instance instance) {
		this.instance = instance;

		this.notdownloaded = !instance.isLocal();
		this.online = instance.getManifestURL()!=null;

		if (instance.getThumb()==null)
			setThumb(DefaultIcons.instanceNoThumbInstanceIcon);
		else
			if (instance.getIconCache()!=null)
				setThumb(instance.getIconCache());
			else
				try {
					final Image thumb = SwingHelper.createImage(instance.getThumb());
					setThumb(thumb);
					instance.setIconCache(thumb);
				} catch (final Exception e) {
					setThumb(DefaultIcons.instanceMissingThumbBackground);
				}
	}

	@Override
	protected void paintComponent(final Graphics g) {
		final Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		final int panel_width = getWidth();
		final int panel_height = getHeight();
		{
			Image thumb = DefaultIcons.instanceLoadingBackground;
			if (this.thumb!=null&&this.thumb.getWidth(parent)>0)
				thumb = this.thumb;
			final int img_width = thumb.getWidth(this.parent);
			final int img_height = thumb.getHeight(this.parent);
			final SizeData img_size = ImageSizes.OUTER.size(img_width, img_height, panel_width, panel_height);
			g2d.drawImage(thumb, (int)((panel_width-img_size.getWidth())/2), (int)((panel_height-img_size.getHeight())/2), (int)img_size.getWidth(), (int)img_size.getHeight(), this.parent);
		}
		if (this.title!=null) {
			final Font font = new Font(Font.DIALOG, Font.BOLD, 13);
			g2d.setFont(font);
			final FontMetrics fontmatrics = g2d.getFontMetrics();
			g2d.translate(0, -5);
			final int height_padding = 5;
			final int pol_w = fontmatrics.stringWidth(this.title)+30;
			final int pol_h = fontmatrics.getHeight()+height_padding;

			final Image titleicon = DefaultIcons.instanceTitleBar;
			final int title_width = titleicon.getWidth(this.parent);
			final int title_height = titleicon.getHeight(this.parent);

			final int title_newwidth = pol_h*title_width/title_height;
			final int title_newheight = pol_h;
			g2d.drawImage(titleicon, panel_width-pol_w, panel_height-title_newheight, title_newwidth, title_newheight, this.parent);

			g2d.setColor(Color.WHITE);
			g2d.drawString(this.title, panel_width-pol_w+20, panel_height-fontmatrics.getDescent()-height_padding/2);
			g2d.translate(0, 5);
		}
		if (this.online)
			g2d.drawImage(DefaultIcons.instanceOnlineIcon, panel_width-20, 0, 20, 20, this.parent);
		if (this.showSelected) {
			g2d.setColor(new Color(0f, 0f, 1f, 0.75f));
			final int inset = 2;
			g2d.drawRect(0+inset, 0+inset, panel_width-1-inset*2, panel_height-1-inset*2);
		}
		if (this.notdownloaded)
			g2d.drawImage(DefaultIcons.instanceDownloadIcon, 0, 0, 40, 40, this.parent);
		else if (this.showPlayIcon)
			g2d.drawImage(DefaultIcons.instancePlayIcon, 0, 0, 40, 40, this.parent);
	}
}
