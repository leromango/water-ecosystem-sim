package simulation.entities;

import processing.core.PVector;
import simulation.core.WaterSimulation;

import java.awt.*;
import java.awt.geom.Area;

public class SimItem {
	protected PVector position;
	protected PVector velocity;
	protected int size;
	protected float scale;
	protected Color color;
	protected boolean isAlive;

	protected WaterSimulation waterSim;

	public SimItem() {
		this.position = new PVector(0, 0);
		this.velocity = new PVector(0, 0);
		this.size = 10;
		this.scale = 1.0f;
		this.color = Color.WHITE;
		this.isAlive = true;
	}

	public SimItem(PVector position, int size, float scale, Color color, WaterSimulation waterSim) {
		this.position = position;
		this.velocity = new PVector(0, 0);
		this.size = size;
		this.scale = scale;
		this.color = color;
		this.isAlive = true;
		this.waterSim = waterSim;
	}

	public void update() {
		position.add(velocity);

		if (waterSim != null) {
			createWaves();
		}
	}

	protected void createWaves() {

		waterSim.createDisturbance((int) position.x, (int) position.y);
	}

	public void draw(Graphics2D g) {

	}

	public Area getOutline() {

		return null;
	}

	public PVector getPosition() {
		return position.copy();
	}

	public void setPosition(PVector position) {
		this.position = position.copy();
	}

	public PVector getVelocity() {
		return velocity.copy();
	}

	public void setVelocity(PVector velocity) {
		this.velocity = velocity.copy();
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public boolean isAlive() {
		return isAlive;
	}

	public void setAlive(boolean alive) {
		isAlive = alive;
	}

	public void setWaterSim(WaterSimulation waterSim) {
		this.waterSim = waterSim;
	}
}