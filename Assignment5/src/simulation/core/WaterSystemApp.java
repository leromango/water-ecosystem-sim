package simulation.core;

import javax.swing.*;

public class WaterSystemApp extends JFrame {
	public WaterSystemApp(String title) {
		super(title);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(1400, 700);

		WaterSystemPanel panel = new WaterSystemPanel(this.getSize());
		add(panel);
		setVisible(true);
	}

	public static void main(String[] args) {
		new WaterSystemApp("Water Ecosystem Simulation");
	}
}