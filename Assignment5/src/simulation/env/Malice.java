package simulation.env;

import java.util.ArrayList;
import java.util.Random;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import processing.core.PVector;
import simulation.core.WaterSimulation;
import simulation.entities.Plant;
import simulation.entities.Prey;

public class Malice {
	private ArrayList<Node> nodes;
	private ArrayList<Connector> connectors;
	private ArrayList<FractalBranch> fractalBranches;
	private ArrayList<Connection> nodeConnections;
	private WaterSimulation waterSim;
	private Random random;
	private long lastUpdateTime;
	private int cols, rows, cellSize;
	private int maxFractalDepth = 4;

	private final int MAX_DISTANCE_FROM_PARENT = 7;
	private float spreadProbability = 0.7f;

	private class Connection {
		Node node1;
		Node node2;

		public Connection(Node node1, Node node2) {
			this.node1 = node1;
			this.node2 = node2;
		}
	}

	public class Node {
		int x, y;
		int generation;

		public Node(int x, int y, int generation) {
			this.x = x;
			this.y = y;
			this.generation = generation;
		}

		public double distanceTo(Node other) {
			return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
		}

		public double distanceTo(int x, int y) {
			return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2));
		}
	}

	private class Connector {
		Node start;
		int endX, endY;
		float lifespan;
		float maxLifespan;
		boolean active;

		int originalX, originalY;

		public Connector(Node start, int endX, int endY) {
			this.start = start;
			this.endX = endX;
			this.endY = endY;
			this.originalX = endX;
			this.originalY = endY;
			this.active = true;
			this.lifespan = 0;

			this.maxLifespan = 50000 + random.nextInt(5000);
		}

		public boolean update(long deltaTime) {

			long scaledDelta = Math.min(deltaTime, 100);
			lifespan += scaledDelta;

			double distance = Math.sqrt(Math.pow(endX - originalX, 2) + Math.pow(endY - originalY, 2));
			if (distance > MAX_DISTANCE_FROM_PARENT) {
				return false;
			}

			return lifespan < maxLifespan && active;
		}
	}

	private class FractalBranch {
		int x1, y1, x2, y2;
		int depth;
		float angle;

		public FractalBranch(int x1, int y1, int x2, int y2, int depth, float angle) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.depth = depth;
			this.angle = angle;
		}
	}

	public Malice(WaterSimulation waterSim) {
		this.waterSim = waterSim;
		this.cols = waterSim.getCols();
		this.rows = waterSim.getRows();
		this.cellSize = waterSim.getCellSize();
		this.nodes = new ArrayList<>();
		this.connectors = new ArrayList<>();
		this.fractalBranches = new ArrayList<>();
		this.nodeConnections = new ArrayList<>();
		this.random = new Random();
		this.lastUpdateTime = System.currentTimeMillis();

		int startX = cols / 2;
		int startY = cols / 2;
		createInitialNode(startX, startY);
	}

	public void setMaxDistanceFromParent(int distance) {

	}

	public void setSpreadProbability(float probability) {
		if (probability >= 0.0f && probability <= 1.0f) {
			this.spreadProbability = probability;
		}
	}

	private void createInitialNode(int x, int y) {
		x = (x / 5) * 5;
		y = (y / 5) * 5;

		Node initialNode = new Node(x, y, 0);
		nodes.add(initialNode);

		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * 2 * i / 8;
			int distance = 2;
			int endX = x + (int) (Math.cos(angle) * distance);
			int endY = y + (int) (Math.sin(angle) * distance);

			if (isInBounds(endX, endY)) {
				connectors.add(new Connector(initialNode, endX, endY));
			}
		}
	}

	public Node createNodeAtPosition(float x, float y) {
		int gridX = (int) (x / cellSize);
		int gridY = (int) (y / cellSize);

		gridX = (gridX / 5) * 5;
		gridY = (gridY / 5) * 5;

		for (Node existingNode : nodes) {
			double distance = Math.sqrt(Math.pow(existingNode.x - gridX, 2) + Math.pow(existingNode.y - gridY, 2));
			if (distance < 3) {

				return null;
			}
		}

		Node closestNode = findClosestNode(gridX, gridY);

		Node newNode = new Node(gridX, gridY, closestNode != null ? closestNode.generation + 1 : 0);

		nodes.add(newNode);

		if (closestNode != null) {

			Connector connector = new Connector(closestNode, gridX, gridY);
			connectors.add(connector);

			nodeConnections.add(new Connection(closestNode, newNode));
		}

		createConnectorsFromNode(newNode);

		return newNode;
	}

	private Node findClosestNode(int x, int y) {
		if (nodes.isEmpty()) {
			return null;
		}

		Node closest = null;
		double closestDistance = Double.MAX_VALUE;

		for (Node node : nodes) {

			if (node.x == x && node.y == y)
				continue;

			double distance = Math.sqrt(Math.pow(node.x - x, 2) + Math.pow(node.y - y, 2));
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = node;
			}
		}

		if (closest != null && closestDistance > MAX_DISTANCE_FROM_PARENT) {
			return null;
		}

		return closest;
	}

	private void createConnectorsFromNode(Node node) {
		for (int i = 0; i < 3; i++) {
			double angle = Math.PI * 2 * i / 3;
			int distance = 2;
			int endX = node.x + (int) (Math.cos(angle) * distance);
			int endY = node.y + (int) (Math.sin(angle) * distance);

			if (isInBounds(endX, endY)) {
				connectors.add(new Connector(node, endX, endY));
			}
		}
	}

	private boolean isInBounds(int x, int y) {
		return x >= 0 && x < cols && y >= 0 && y < rows;
	}

	public void update() {
		long currentTime = System.currentTimeMillis();
		long deltaTime = currentTime - lastUpdateTime;
		lastUpdateTime = currentTime;

		deltaTime = Math.min(deltaTime, 100);

		ArrayList<Connector> toRemove = new ArrayList<>();
		ArrayList<Connector> toAdd = new ArrayList<>();

		for (Connector c : connectors) {
			if (!c.update(deltaTime)) {
				toRemove.add(c);
				continue;
			}

			if (isInBounds(c.endX, c.endY)) {
				float currentHeight = waterSim.getWaterHeight(c.endX, c.endY);

				if (Math.abs(currentHeight) > 0.2f) {
					int bestDx = 0, bestDy = 0;
					float highestDiff = 0;

					for (int dx = -1; dx <= 1; dx++) {
						for (int dy = -1; dy <= 1; dy++) {
							if (dx == 0 && dy == 0)
								continue;

							int nx = c.endX + dx;
							int ny = c.endY + dy;

							if (isInBounds(nx, ny)) {
								float neighborHeight = waterSim.getWaterHeight(nx, ny);
								float diff = Math.abs(neighborHeight - currentHeight);

								if (diff > highestDiff) {
									highestDiff = diff;
									bestDx = dx;
									bestDy = dy;
								}
							}
						}
					}

					int proposedX = c.endX + bestDx;
					int proposedY = c.endY + bestDy;
					double distanceFromParent = c.start.distanceTo(proposedX, proposedY);

					if (distanceFromParent <= MAX_DISTANCE_FROM_PARENT) {
						c.endX = proposedX;
						c.endY = proposedY;
					}

					boolean onGridOrNearby = isOnGrid(c.endX, c.endY) || (c.endX % 5 <= 1 || c.endX % 5 >= 4)
							|| (c.endY % 5 <= 1 || c.endY % 5 >= 4);

					if (onGridOrNearby && random.nextFloat() < spreadProbability * Math.min(deltaTime / 20.0, 0.4)) {

						createNewNode(c, toAdd);

						if (random.nextFloat() < 0.3) {
							c.active = false;
							toRemove.add(c);
						}
					}
				}
			}
		}

		connectors.removeAll(toRemove);
		connectors.addAll(toAdd);

		updateFractalBranches();

		ensureAllNodesAreConnected();
	}

	private void ensureAllNodesAreConnected() {

		nodeConnections.clear();

		for (int i = 0; i < nodes.size(); i++) {
			Node current = nodes.get(i);
			boolean isConnected = false;

			for (Connector connector : connectors) {
				if (connector.start == current) {
					isConnected = true;
					break;
				}
			}

			if (!isConnected) {

				Node nearestNode = null;
				double nearestDistance = Double.MAX_VALUE;

				for (int j = 0; j < nodes.size(); j++) {
					if (i == j)
						continue;

					Node other = nodes.get(j);
					double distance = current.distanceTo(other);

					if (distance < nearestDistance) {
						nearestDistance = distance;
						nearestNode = other;
					}
				}

				if (nearestNode != null) {
					nodeConnections.add(new Connection(current, nearestNode));
				}
			}
		}
	}

	private void updateFractalBranches() {
		fractalBranches.clear();

		if (nodes.size() >= 3) {
			for (int i = 0; i < nodes.size(); i++) {
				Node current = nodes.get(i);
				for (int j = i + 1; j < Math.min(i + 4, nodes.size()); j++) {
					Node other = nodes.get(j);

					double dx = (other.x - current.x) * cellSize;
					double dy = (other.y - current.y) * cellSize;
					double dist = Math.sqrt(dx * dx + dy * dy);

					if (dist < cellSize * 15) {
						int startX = current.x * cellSize + cellSize / 2;
						int startY = current.y * cellSize + cellSize / 2;
						int endX = other.x * cellSize + cellSize / 2;
						int endY = other.y * cellSize + cellSize / 2;

						float angle = (float) Math.atan2(dy, dx);
						FractalBranch branch = new FractalBranch(startX, startY, endX, endY, 0, angle);
						fractalBranches.add(branch);

						generateFractalBranches(branch, 1);
					}
				}
			}
		}
	}

	private void generateFractalBranches(FractalBranch parent, int depth) {
		if (depth >= maxFractalDepth)
			return;

		int midX = (parent.x1 + parent.x2) / 2;
		int midY = (parent.y1 + parent.y2) / 2;

		double branchLength = Math.sqrt(Math.pow(parent.x2 - parent.x1, 2) + Math.pow(parent.y2 - parent.y1, 2))
				* (0.6 - depth * 0.1);

		for (int i = 0; i < 2; i++) {
			float newAngle = parent.angle + (i == 0 ? 0.6f : -0.6f);

			int endX = midX + (int) (Math.cos(newAngle) * branchLength);
			int endY = midY + (int) (Math.sin(newAngle) * branchLength);

			FractalBranch newBranch = new FractalBranch(midX, midY, endX, endY, depth, newAngle);
			fractalBranches.add(newBranch);

			generateFractalBranches(newBranch, depth + 1);
		}
	}

	private void createNewNode(Connector c, ArrayList<Connector> newConnectors) {
		int parentGeneration = c.start.generation;
		for (Node existingNode : nodes) {
			double distance = Math.sqrt(Math.pow(existingNode.x - c.endX, 2) + Math.pow(existingNode.y - c.endY, 2));
			if (distance < 2) {

				return;
			}
		}

		Node newNode = new Node(c.endX, c.endY, parentGeneration + 1);
		nodes.add(newNode);
		int generation = parentGeneration + 1;

		int baseNumBranches = 6;
		int numBranches = Math.max(baseNumBranches - (generation / 2), 2);

		double angleOffset = Math.PI * 2 * random.nextDouble();

		int actualBranches = (int) Math.ceil(numBranches * spreadProbability);
		actualBranches = Math.max(2, actualBranches);

		for (int i = 0; i < actualBranches; i++) {
			double angle = angleOffset + (i * Math.PI * 0.618033988749895);

			int distance = Math.max(3 - generation / 3, 1);

			int endX = c.endX + (int) (Math.cos(angle) * distance);
			int endY = c.endY + (int) (Math.sin(angle) * distance);

			if (isInBounds(endX, endY)) {
				boolean isTooIsolated = c.start.distanceTo(endX, endY) > MAX_DISTANCE_FROM_PARENT;
				if (!isTooIsolated) {
					newConnectors.add(new Connector(newNode, endX, endY));
				}
			}
		}
	}

	public void render(Graphics2D g2d) {
		g2d.setStroke(new BasicStroke(0.5f));
		for (Connection conn : nodeConnections) {
			int startX = conn.node1.x * cellSize + cellSize / 2;
			int startY = conn.node1.y * cellSize + cellSize / 2;
			int endX = conn.node2.x * cellSize + cellSize / 2;
			int endY = conn.node2.y * cellSize + cellSize / 2;

			g2d.setColor(new Color(100, 0, 100, 100));
			g2d.drawLine(startX, startY, endX, endY);
		}

		for (FractalBranch branch : fractalBranches) {
			float strokeWidth = Math.max(2.5f - (branch.depth * 0.5f), 0.5f);
			g2d.setStroke(new BasicStroke(strokeWidth));

			int hue = (branch.depth * 30) % 360;
			Color branchColor = Color.getHSBColor(hue / 360f, 0.8f, 0.7f);
			g2d.setColor(new Color(branchColor.getRed(), branchColor.getGreen(), branchColor.getBlue(), 120));

			g2d.drawLine(branch.x1, branch.y1, branch.x2, branch.y2);
		}

		g2d.setStroke(new BasicStroke(1.0f));
		for (Connector c : connectors) {
			int startX = c.start.x * cellSize + cellSize / 2;
			int startY = c.start.y * cellSize + cellSize / 2;
			int endX = c.endX * cellSize + cellSize / 2;
			int endY = c.endY * cellSize + cellSize / 2;

			int generation = c.start.generation;
			int blueVal = Math.max(0, Math.min(180 + generation * 20, 255));
			g2d.setColor(new Color(180, 0, blueVal, 200));
			g2d.drawLine(startX, startY, endX, endY);
		}

		for (Node node : nodes) {
			int x = node.x * cellSize + cellSize / 4;
			int y = node.y * cellSize + cellSize / 4;

			int generation = node.generation;
			int red = Math.max(0, Math.min(180 + generation * 15, 255));
			int blue = Math.max(0, Math.min(180 + generation * 15, 255));

			g2d.setColor(new Color(red, 0, blue));
			int nodeSize = Math.max(cellSize - generation, cellSize / 2);
			g2d.fillOval(x, y, nodeSize, nodeSize);
		}
	}

	private boolean isOnGrid(int x, int y) {
		return x % 5 == 0 && y % 5 == 0;
	}

	public ArrayList<PVector> getNodePositions() {
		ArrayList<PVector> positions = new ArrayList<>();
		for (Node node : nodes) {
			positions.add(new PVector(node.x * cellSize + cellSize / 2, node.y * cellSize + cellSize / 2));
		}
		return positions;
	}

	public Node getNodeAt(int gridX, int gridY) {
		for (Node node : nodes) {
			if (node.x == gridX && node.y == gridY) {
				return node;
			}
		}
		return null;
	}

	public void removeNode(Node node) {
		if (node != null && nodes.contains(node)) {
			nodes.remove(node);

			ArrayList<Connector> toRemove = new ArrayList<>();
			for (Connector c : connectors) {
				if (c.start == node) {
					toRemove.add(c);
				}
			}
			connectors.removeAll(toRemove);

			ArrayList<Connection> connectionsToRemove = new ArrayList<>();
			for (Connection conn : nodeConnections) {
				if (conn.node1 == node || conn.node2 == node) {
					connectionsToRemove.add(conn);
				}
			}
			nodeConnections.removeAll(connectionsToRemove);
		}
	}

	public void checkPlantCollisions(Plant plant) {
		ArrayList<Node> nodesToRemove = new ArrayList<>();
		for (Node node : nodes) {
			PVector nodePos = new PVector(node.x * cellSize + cellSize / 2, node.y * cellSize + cellSize / 2);
			if (plant.canDestroyMalice(nodePos)) {
				nodesToRemove.add(node);
			}
		}

		for (Node node : nodesToRemove) {
			removeNode(node);
		}
	}

	public void checkPreyCollisions(Prey prey) {
		for (Node node : nodes) {
			PVector nodePos = new PVector(node.x * cellSize + cellSize / 2, node.y * cellSize + cellSize / 2);
			prey.checkMaliceContact(nodePos);
		}
	}
}