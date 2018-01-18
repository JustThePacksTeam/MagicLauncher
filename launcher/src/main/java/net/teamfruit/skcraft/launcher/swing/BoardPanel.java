package net.teamfruit.skcraft.launcher.swing;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;

public class BoardPanel<T extends Component> extends JPanel {
	public BoardPanel() {
		super(new BorderLayout());
	}

	private T component;

	public void set(T component) {
		this.component = component;
		removeAll();
		add(component, BorderLayout.CENTER);
		revalidate();
		repaint();
	}

	public T get() {
		return this.component;
	}
}
