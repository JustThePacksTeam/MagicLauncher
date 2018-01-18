package net.teamfruit.skcraft.launcher.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.swing.SwingHelper;

import net.teamfruit.skcraft.launcher.Tip;
import net.teamfruit.skcraft.launcher.util.ImageSizes;
import net.teamfruit.skcraft.launcher.util.SizeData;

public class TipsPanel extends JPanel {
	public static class DefaultIcons {
		public static final Image tipsBar = SwingHelper.createImage(Launcher.class, "tips_bar.png");
	}

	public static final TipsPanel instance = new TipsPanel();

	private Timer tm;
	private int x = -1;
	private Image thumb;
	private String title;

	//Images Path In Array
	private final List<Tip> list = new ArrayList<Tip>();
	private final MediaTracker mediaTracker;

	public void updateTipList(final List<Tip> tips) {
		this.list.clear();
		this.list.addAll(tips);
		Collections.shuffle(list);
		for (ListIterator<Tip> itr = list.listIterator(); itr.hasNext();) {
			int id = itr.nextIndex();
			Tip tip = itr.next();
			mediaTracker.addImage(tip.getThumbImage(), id);
			mediaTracker.checkID(id, true);
		}
	}

	private TipsPanel() {
		mediaTracker = new MediaTracker(this);

		//set a timer
		this.tm = new Timer(5*1000, new ActionListener() {
			{
				next();
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				next();
			}

			private void next() {
				int x = TipsPanel.this.x+1;

				if (TipsPanel.this.list.isEmpty()||!mediaTracker.checkID(x, true)) {
					setVisible(false);
					return;
				}

				if (x<0||x>=TipsPanel.this.list.size())
					x = 0;
				TipsPanel.this.x = x;

				final Tip tip = TipsPanel.this.list.get(x);
				TipsPanel.this.title = tip.getDesc();
				TipsPanel.this.thumb = tip.getThumbImage();
				setVisible(true);
				repaint();
			}
		});

		setPreferredSize(new Dimension(400, 200));
		setVisible(false);

		this.tm.start();
	}

	@Override
	protected void paintComponent(final Graphics g) {
		final Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		final int panel_width = getWidth();
		final int panel_height = getHeight();
		if (this.thumb!=null) {
			final int img_width = this.thumb.getWidth(this);
			final int img_height = this.thumb.getHeight(this);
			final SizeData img_size = ImageSizes.OUTER.size(img_width, img_height, panel_width, panel_height);
			g2d.drawImage(this.thumb, (int) ((panel_width-img_size.getWidth())/2), (int) ((panel_height-img_size.getHeight())/2), (int) img_size.getWidth(), (int) img_size.getHeight(), this);
		}
		if (this.title!=null) {
			final Font font = new Font(Font.DIALOG, Font.BOLD, 14);
			g2d.setFont(font);
			final FontMetrics fontmatrics = g2d.getFontMetrics();
			g2d.translate(0, -5);
			final int height_padding = 5;
			final int width_padding_right = 34;
			final int width_padding_left = 24;
			final int pol_w = fontmatrics.stringWidth(this.title)+width_padding_right;
			final int pol_h = fontmatrics.getHeight()+height_padding;

			final Image titleicon = DefaultIcons.tipsBar;
			final int title_width = titleicon.getWidth(this);
			final int title_height = titleicon.getHeight(this);

			final int title_newwidth = pol_h*title_width/title_height;
			final int title_newheight = pol_h;
			g2d.drawImage(titleicon, panel_width-pol_w, panel_height-title_newheight, title_newwidth, title_newheight, this);

			g2d.setColor(Color.WHITE);
			g2d.drawString(this.title, panel_width-pol_w+width_padding_left, panel_height-fontmatrics.getDescent()-height_padding/2);
			g2d.translate(0, 5);
		}
	}
}