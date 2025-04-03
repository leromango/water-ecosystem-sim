package simulation.core;

import processing.core.PApplet;

import java.awt.*;

public class WaterSimulation {
	private int cellSize;
	private int cols, rows;
	private float[][] current, previous;
	private float damping = 0.95f;
	private PApplet noiseGenerator;
	private int disturbanceRadius = 1;
	private int frameCounter = 0;

	public WaterSimulation(int width, int height, int cellSize) {
		this.cellSize = cellSize;
		noiseGenerator = new PApplet();
		cols = width / cellSize;
		rows = height / cellSize;
		current = new float[cols][rows];
		previous = new float[cols][rows];

	}

	private void initializeWaves() {
		for (int i = 0; i < cols; i++) {
			for (int j = 0; j < rows; j++) {
				float n = noiseGenerator.noise(i * 0.1f, j * 0.1f);
				float value = PApplet.map(n, 0, 1, -10, 10);
				current[i][j] = value;
				previous[i][j] = value;
			}
		}
	}

	public void render(Graphics2D g2d) {
		int cols = getCols();
		int rows = getRows();
		int cellSize = getCellSize();

		for (int i = 0; i < cols; i++) {
			for (int j = 0; j < rows; j++) {
				int x = i * cellSize;
				int y = j * cellSize;
				float h = getWaterHeight(i, j);
				int blue = (int) (200 + h * 10);
				blue = Math.max(0, Math.min(blue, 255));
				g2d.setColor(new Color(0, 0, blue));
				g2d.fillRect(x, y, cellSize, cellSize);
			}
		}
	}

	public void update() {
		frameCounter++;
		for (int i = 1; i < cols - 1; i++) {
			for (int j = 1; j < rows - 1; j++) {
				current[i][j] = ((previous[i - 1][j] + previous[i + 1][j] + previous[i][j - 1] + previous[i][j + 1])
						/ 2) - current[i][j];
				current[i][j] *= damping;

			}
		}
		float[][] temp = previous;
		previous = current;
		current = temp;
	}

	public void createDisturbance(int mouseX, int mouseY) {
		int i = mouseX / cellSize;
		int j = mouseY / cellSize;
		for (int di = -disturbanceRadius; di <= disturbanceRadius; di++) {
			for (int dj = -disturbanceRadius; dj <= disturbanceRadius; dj++) {
				int ii = i + di;
				int jj = j + dj;
				if (ii > 0 && ii < cols - 1 && jj > 0 && jj < rows - 1) {
					previous[ii][jj] = -10;
				}
			}
		}
	}

	private float generateSmallNoise(int i, int j) {
		float n = noiseGenerator.noise(i * 0.1f, j * 0.1f, frameCounter * 0.01f);
		return PApplet.map(n, 0, 1, -0.5f, 0.5f);
	}

	public int getCols() {
		return cols;
	}

	public int getRows() {
		return rows;
	}

	public int getCellSize() {
		return cellSize;
	}

	public float getWaterHeight(int i, int j) {
		return current[i][j];
	}

	public void setDisturbanceRadius(int radius) {
		this.disturbanceRadius = radius;
	}

	public void setDamping(float damping) {
		this.damping = damping;
	}
}