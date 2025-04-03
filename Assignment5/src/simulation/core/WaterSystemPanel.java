package simulation.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import processing.core.PVector;
import simulation.entities.Creature;
import simulation.entities.Plant;
import simulation.entities.Prey;
import simulation.env.Malice;
import simulation.ui.SidebarPanel;

public class WaterSystemPanel extends JPanel implements ActionListener, KeyListener {
	private final WaterSimulation waterSimulation;
	private Timer WaterAnimationTimer;
	private SidebarPanel sidebarPanel;
	private int simulationWidth;

	private Malice malice;
	private ArrayList<Plant> plants;
	private ArrayList<Prey> preys;
	private Random random;

	private ArrayList<Plant> plantsToRemove;

	private ArrayList<Plant> userPlants;
	private ArrayList<Prey> userPreys;

	private static final int DEFAULT_PLANT_COUNT = 6;
	private static final int DEFAULT_PREY_COUNT = 3;

	private Point mousePosition = new Point(0, 0);

	public WaterSystemPanel(Dimension size) {
		super();
		setSize(size);
		setPreferredSize(size);
		random = new Random();

		setLayout(new BorderLayout());

		JPanel simulationPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				renderSimulation((Graphics2D) g);
			}
		};
		simulationPanel.setPreferredSize(new Dimension(size.width - 200, size.height));

		simulationPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					waterSimulation.createDisturbance(e.getX(), e.getY());
				} else if (e.getButton() == MouseEvent.BUTTON3) {

					handleRightClick(e.getX(), e.getY());
				}
			}
		});

		simulationPanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				mousePosition = e.getPoint();
				repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				mousePosition = e.getPoint();

				if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
					waterSimulation.createDisturbance(e.getX(), e.getY());
				}
				repaint();
			}
		});

		sidebarPanel = new SidebarPanel() {
			private boolean isDragging = false;
			private int dragItemType = NONE;

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				sidebarDragging = isDragging;
				sidebarDragType = dragItemType;
			}

			@Override
			public void addNotify() {
				super.addNotify();

				this.addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						Point p = e.getPoint();

						if (plantRect.contains(p)) {
							isDragging = true;
							dragItemType = PLANT;
							repaint();
						} else if (preyRect.contains(p)) {
							isDragging = true;
							dragItemType = PREY;
							repaint();
						} else if (maliceRect.contains(p)) {
							isDragging = true;
							dragItemType = MALICE;
							repaint();
						} else {

							handleConfigButtons(e);
						}
					}

					@Override
					public void mouseReleased(MouseEvent e) {
						if (isDragging) {

							Point sidebarLoc = sidebarPanel.getLocationOnScreen();
							Point simLoc = simulationPanel.getLocationOnScreen();
							int simX = e.getX() + sidebarLoc.x - simLoc.x;
							int simY = e.getY() + sidebarLoc.y - simLoc.y;

							dropItemAt(dragItemType, simX, simY);

							isDragging = false;
							dragItemType = NONE;
							repaint();
							WaterSystemPanel.this.repaint();
						}
					}
				});

				this.addMouseMotionListener(new MouseMotionAdapter() {
					@Override
					public void mouseDragged(MouseEvent e) {
						if (isDragging) {

							Point screenPoint = new Point(e.getX(), e.getY());
							SwingUtilities.convertPointToScreen(screenPoint, sidebarPanel);
							SwingUtilities.convertPointFromScreen(screenPoint, simulationPanel);

							Point sidebarLoc = sidebarPanel.getLocationOnScreen();
							Point simLoc = simulationPanel.getLocationOnScreen();
							int simX = e.getX() + sidebarLoc.x - simLoc.x;
							int simY = e.getY() + sidebarLoc.y - simLoc.y;

							mousePosition = new Point(simX, simY);

							if (screenPoint.x >= 0 && screenPoint.x < simulationWidth && screenPoint.y >= 0
									&& screenPoint.y < getHeight()) {
								if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) {

									dropItemAt(dragItemType, screenPoint.x, screenPoint.y);
									isDragging = false;
									dragItemType = NONE;
								}
							}

							repaint();
							WaterSystemPanel.this.repaint();
						}
					}
				});
			}

			private void handleConfigButtons(MouseEvent e) {
				Point p = e.getPoint();

				if (decreasePlantsRect.contains(p)) {
					if (maxPlants > 1)
						maxPlants--;
					repaint();
				} else if (increasePlantsRect.contains(p)) {
					if (maxPlants < 20)
						maxPlants++;
					repaint();
				} else if (decreasePreyRect.contains(p)) {
					if (maxPrey > 1)
						maxPrey--;
					repaint();
				} else if (increasePreyRect.contains(p)) {
					if (maxPrey < 20)
						maxPrey++;
					repaint();
				}
			}
		};

		sidebarDragging = false;
		sidebarDragType = SidebarPanel.NONE;

		add(simulationPanel, BorderLayout.CENTER);
		add(sidebarPanel, BorderLayout.EAST);

		simulationWidth = size.width - 200;

		waterSimulation = new WaterSimulation(simulationWidth, size.height, 7);
		malice = new Malice(waterSimulation);

		Creature.initializeEnvironment(new Dimension(simulationWidth, size.height), 50);

		plants = new ArrayList<>();
		preys = new ArrayList<>();
		plantsToRemove = new ArrayList<>();
		userPlants = new ArrayList<>();
		userPreys = new ArrayList<>();

		initializeEntities();

		addKeyListener(this);
		setFocusable(true);

		WaterAnimationTimer = new Timer(30, this);
		WaterAnimationTimer.start();
	}

	private boolean sidebarDragging;
	private int sidebarDragType;

	private void initializeEntities() {
		for (int i = 0; i < DEFAULT_PLANT_COUNT; i++) {
			createRandomPlant(false);
		}

		for (int i = 0; i < DEFAULT_PREY_COUNT; i++) {
			createRandomPrey(false);
		}

		updatePreyReferences();

		for (Prey prey : preys) {
			prey.setMalice(malice);
		}
	}

	private void renderSimulation(Graphics2D g2d) {
		waterSimulation.render(g2d);

		malice.render(g2d);

		for (Plant plant : plants) {
			if (plant.isAlive()) {
				plant.draw(g2d);
			}
		}

		for (Prey prey : preys) {
			if (prey.isAlive()) {
				prey.draw(g2d);
			}
		}

		if (sidebarDragging) {
			drawDragPreview(g2d, sidebarDragType, mousePosition);
		}
	}

	private void drawDragPreview(Graphics2D g2d, int itemType, Point pos) {
		if (pos.x >= simulationWidth)
			return;

		switch (itemType) {
		case SidebarPanel.PLANT:
			g2d.setColor(new Color(0, 150, 0, 150));
			g2d.fillOval(pos.x - 15, pos.y - 15, 30, 30);
			break;
		case SidebarPanel.PREY:
			g2d.setColor(new Color(255, 165, 0, 150));
			g2d.fillOval(pos.x - 20, pos.y - 10, 40, 20);
			break;
		case SidebarPanel.MALICE:
			g2d.setColor(new Color(100, 0, 100, 150));
			g2d.fillOval(pos.x - 10, pos.y - 10, 20, 20);
			break;
		}
	}

	private void dropItemAt(int itemType, int x, int y) {
		if (x >= 0 && x < simulationWidth && y >= 0 && y < getHeight()) {
			switch (itemType) {
			case SidebarPanel.PLANT:
				createPlantAt(x, y);
				break;
			case SidebarPanel.PREY:
				createPreyAt(x, y);
				break;
			case SidebarPanel.MALICE:
				malice.createNodeAtPosition(x, y);
				break;
			}
		}
	}

	private void updatePreyReferences() {
		for (Prey prey : preys) {
			prey.setOtherPrey(preys);
		}
	}

	private void createRandomPlant(boolean isUserCreated) {
		int x = random.nextInt(simulationWidth - 100) + 50;
		int y = random.nextInt(getHeight() - 100) + 50;
		PVector pos = new PVector(x, y);
		int size = random.nextInt(10) + 20;

		Plant plant = new Plant(pos, size, waterSimulation);
		plants.add(plant);

		if (isUserCreated) {
			userPlants.add(plant);
		}
	}

	private Plant createPlantAt(int x, int y) {
		if (x >= simulationWidth)
			return null;

		PVector pos = new PVector(x, y);
		int size = random.nextInt(10) + 20;

		Plant plant = new Plant(pos, size, waterSimulation);
		plants.add(plant);
		userPlants.add(plant);
		return plant;
	}

	private void createRandomPrey(boolean isUserCreated) {
		int x = random.nextInt(simulationWidth - 100) + 50;
		int y = random.nextInt(getHeight() - 100) + 50;
		PVector pos = new PVector(x, y);
		int size = random.nextInt(15) + 25;

		Prey prey = new Prey(pos, size, waterSimulation);
		prey.setSpeed(2.0f + random.nextFloat());

		prey.setMalice(malice);

		preys.add(prey);

		if (isUserCreated) {
			userPreys.add(prey);
		}

		updatePreyReferences();
	}

	private Prey createPreyAt(int x, int y) {
		if (x >= simulationWidth)
			return null;

		PVector pos = new PVector(x, y);
		int size = random.nextInt(15) + 25;

		Prey prey = new Prey(pos, size, waterSimulation);
		prey.setSpeed(2.0f + random.nextFloat());
		prey.setMalice(malice);

		preys.add(prey);
		userPreys.add(prey);

		updatePreyReferences();

		return prey;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		waterSimulation.update();

		malice.update();

		plantsToRemove.clear();

		for (Plant plant : plants) {
			if (plant.isAlive()) {
				plant.update();
				malice.checkPlantCollisions(plant);
			} else {
				plantsToRemove.add(plant);
			}
		}

		for (Plant plant : plantsToRemove) {
			plants.remove(plant);

			userPlants.remove(plant);

			int maxPlants = sidebarPanel.getMaxPlants();
			int nonUserPlantCount = plants.size() - userPlants.size();

			if (nonUserPlantCount < maxPlants) {
				createRandomPlant(false);
			}
		}

		ArrayList<Prey> preysToRemove = new ArrayList<>();

		ArrayList<Plant> plantsEaten = new ArrayList<>();

		for (Prey prey : preys) {
			if (prey.isAlive()) {
				prey.update();

				for (Plant plant : plants) {
					if (plant.isAlive() && !plantsEaten.contains(plant)) {

						Area preyArea = prey.getOutline();
						Area plantArea = plant.getOutline();

						Area intersection = new Area(preyArea);
						intersection.intersect(plantArea);

						if (!intersection.isEmpty()) {
							prey.eatPlant(plant);
							plantsEaten.add(plant);
							break;
						}
					}
				}

				malice.checkPreyCollisions(prey);

				ArrayList<Plant> livePlants = new ArrayList<>();
				for (Plant plant : plants) {
					if (plant.isAlive() && !plantsEaten.contains(plant)) {
						livePlants.add(plant);
					}
				}
				prey.huntForPlant(livePlants);
			} else {
				preysToRemove.add(prey);
			}
		}

		for (Plant plant : plantsEaten) {
			plant.setAlive(false);
			plantsToRemove.add(plant);
		}

		for (Plant plant : plantsToRemove) {
			plants.remove(plant);

			userPlants.remove(plant);

			int maxPlants = sidebarPanel.getMaxPlants();
			int nonUserPlantCount = plants.size() - userPlants.size();

			if (nonUserPlantCount < maxPlants) {
				createRandomPlant(false);
			}
		}

		for (Prey prey : preysToRemove) {
			preys.remove(prey);
			userPreys.remove(prey);

			int maxPrey = sidebarPanel.getMaxPrey();
			int nonUserPreyCount = preys.size() - userPreys.size();

			if (nonUserPreyCount < maxPrey) {
				createRandomPrey(false);
			}
		}

		maintainEntityCounts();

		updatePreyReferences();

		repaint();
	}

	private void maintainEntityCounts() {
		int maxPlants = sidebarPanel.getMaxPlants();
		int maxPrey = sidebarPanel.getMaxPrey();

		int nonUserPlantCount = plants.size() - userPlants.size();
		while (nonUserPlantCount < maxPlants) {
			createRandomPlant(false);
			nonUserPlantCount++;
		}

		int nonUserPreyCount = preys.size() - userPreys.size();
		while (nonUserPreyCount < maxPrey) {
			createRandomPrey(false);
			nonUserPreyCount++;
		}
	}

	private void handleRightClick(int x, int y) {
		for (Iterator<Plant> it = plants.iterator(); it.hasNext();) {
			Plant plant = it.next();
			if (plant.isAlive()) {

				Area plantArea = plant.getOutline();
				if (plantArea.contains(x, y)) {
					plant.setAlive(false);

					userPlants.remove(plant);

					it.remove();
					return;
				}
			}
		}

		for (Iterator<Prey> it = preys.iterator(); it.hasNext();) {
			Prey prey = it.next();
			if (prey.isAlive()) {

				Area preyArea = prey.getOutline();
				if (preyArea.contains(x, y)) {
					prey.setAlive(false);

					userPreys.remove(prey);

					it.remove();
					return;
				}
			}
		}

		ArrayList<PVector> nodePositions = malice.getNodePositions();
		for (PVector nodePos : nodePositions) {
			if (Math.abs(nodePos.x - x) < 15 && Math.abs(nodePos.y - y) < 15) {
				int gridX = (int) (nodePos.x / waterSimulation.getCellSize());
				int gridY = (int) (nodePos.y / waterSimulation.getCellSize());
				Malice.Node node = malice.getNodeAt(gridX, gridY);
				if (node != null) {
					malice.removeNode(node);
					return;
				}
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			Creature.displayInfo = !Creature.displayInfo;
			repaint();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
}