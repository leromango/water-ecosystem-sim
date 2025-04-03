package simulation.ui;

import javax.swing.*;
import java.awt.*;

public class SidebarPanel extends JPanel {
	private static final int WIDTH = 200;

	public static final int NONE = 0;
	public static final int PLANT = 1;
	public static final int PREY = 2;
	public static final int MALICE = 3;

	private int selectedItem = NONE;
	protected int maxPlants = 6;
	protected int maxPrey = 3;

	protected Rectangle plantRect;
	protected Rectangle preyRect;
	protected Rectangle maliceRect;

	protected Rectangle decreasePlantsRect;
	protected Rectangle increasePlantsRect;
	protected Rectangle decreasePreyRect;
	protected Rectangle increasePreyRect;

	public SidebarPanel() {
		setPreferredSize(new Dimension(WIDTH, 700));
		setBackground(new Color(240, 240, 240));
		setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY));

		plantRect = new Rectangle(20, 70, 160, 60);
		preyRect = new Rectangle(20, 160, 160, 60);
		maliceRect = new Rectangle(20, 250, 160, 60);

		decreasePlantsRect = new Rectangle(80, 350, 30, 30);
		increasePlantsRect = new Rectangle(140, 350, 30, 30);
		decreasePreyRect = new Rectangle(80, 400, 30, 30);
		increasePreyRect = new Rectangle(140, 400, 30, 30);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2d.setFont(new Font("Arial", Font.BOLD, 18));
		g2d.drawString("Spawn Bar", 60, 30);

		drawButton(g2d, plantRect, "Plant", new Color(0, 150, 0), selectedItem == PLANT);
		drawButton(g2d, preyRect, "Prey", new Color(255, 165, 0), selectedItem == PREY);
		drawButton(g2d, maliceRect, "Malice Node", new Color(100, 0, 100), selectedItem == MALICE);

		g2d.setFont(new Font("Arial", Font.BOLD, 16));
		g2d.drawString("Configuration", 50, 330);

		g2d.setFont(new Font("Arial", Font.PLAIN, 14));
		g2d.drawString("Max Plants:", 5, 370);
		drawControlButton(g2d, decreasePlantsRect, "-");
		g2d.drawString(String.valueOf(maxPlants), 120, 370);
		drawControlButton(g2d, increasePlantsRect, "+");

		g2d.drawString("Max Preys:", 5, 420);
		drawControlButton(g2d, decreasePreyRect, "-");
		g2d.drawString(String.valueOf(maxPrey), 120, 420);
		drawControlButton(g2d, increasePreyRect, "+");

		g2d.setFont(new Font("Arial", Font.ITALIC, 10));
		g2d.drawString("Drag from buttons to place items", 10, 480);
		g2d.drawString("Right-click to delete items", 10, 500);
		g2d.drawString("Press 'Spacebar' to hide/reveal stats", 10, 520);
	}

	private void drawButton(Graphics2D g2d, Rectangle rect, String text, Color color, boolean selected) {

		if (selected) {
			g2d.setColor(new Color(200, 200, 200));
		} else {
			g2d.setColor(Color.WHITE);
		}
		g2d.fillRect(rect.x, rect.y, rect.width, rect.height);

		g2d.setColor(Color.BLACK);
		g2d.drawRect(rect.x, rect.y, rect.width, rect.height);

		g2d.setColor(color);
		g2d.fillOval(rect.x + 15, rect.y + 15, 30, 30);

		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("Arial", Font.PLAIN, 14));
		g2d.drawString(text, rect.x + (rect.width - 20) / 2, rect.y + rect.height / 2 + 5);
	}

	private void drawControlButton(Graphics2D g2d, Rectangle rect, String text) {
		g2d.setColor(Color.WHITE);
		g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
		g2d.setColor(Color.BLACK);
		g2d.drawRect(rect.x, rect.y, rect.width, rect.height);

		FontMetrics fm = g2d.getFontMetrics();
		int textWidth = fm.stringWidth(text);
		int textHeight = fm.getAscent();
		g2d.drawString(text, rect.x + (rect.width - textWidth) / 2, rect.y + (rect.height + textHeight) / 2 - 2);
	}

	public void clearSelection() {
		selectedItem = NONE;
		repaint();
	}

	public int getSelectedItem() {
		return selectedItem;
	}

	public int getMaxPlants() {
		return maxPlants;
	}

	public int getMaxPrey() {
		return maxPrey;
	}

}