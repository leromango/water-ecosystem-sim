package simulation.entities;

import processing.core.PVector;
import simulation.core.WaterSimulation;
import simulation.env.Malice;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Random;

public class Prey extends Creature {
	private final float PLANT_DETECTION_RADIUS = 150.0f;
	private final float MALICE_DETECTION_RADIUS = 3.0f;
	private final float PREY_AVOIDANCE_FORCE = 1.5f;
	private final Random random = new Random();

	private static final int STATE_WANDERING = 0;
	private static final int STATE_HUNTING = 1;
	private static final int STATE_FLEEING = 2;
	private static final int STATE_AVOIDING = 3;

	private int infectionTimer = 0;
	private ArrayList<BlackDot> blackDots = new ArrayList<>();
	private static final int MAX_DOTS = 20;

	private Malice malice;
	private int nodeCreationTimer = 0;
	private static final int NODE_CREATION_INTERVAL = 10;

	private class BlackDot {
		float relativeX, relativeY;
		float size;
		float angle;

		public BlackDot() {
			float r = random.nextFloat() * 0.9f;
			float theta = random.nextFloat() * (float) (Math.PI * 2);

			this.relativeX = r * (float) Math.cos(theta);
			this.relativeY = r * (float) Math.sin(theta);

			this.size = 2 + random.nextFloat() * 4;

			this.angle = 0;
		}
	}

	private int behaviorState = STATE_WANDERING;
	private Plant targetPlant = null;
	private float wanderAngle = 0;

	private float targetAngle = 0;
	private float rotationSpeed = 0.05f;

	private int waveTimer = 0;
	private static final int WAVE_INTERVAL = 10;

	private ArrayList<Prey> otherPrey;

	public Prey(PVector startPos, int size, WaterSimulation waterSim) {
		super(startPos, size, 1.0f, waterSim);
		this.speed = 2.0f;
		this.baseSpeed = 2.0f;
		this.color = new Color(255, 165, 0);

		this.hasFeelers = true;
		this.feelerLength = 120.0f;
		this.feelerAngle = (float) Math.PI / 4;

		this.otherPrey = new ArrayList<>();
	}

	public void setOtherPrey(ArrayList<Prey> otherPrey) {

		this.otherPrey = new ArrayList<>(otherPrey);

		this.otherPrey.remove(this);
	}

	public void setMalice(Malice malice) {
		this.malice = malice;
	}

	@Override
	public void update() {
		if (!isAlive)
			return;

		if (isInfected) {
			updateInfectionEffect();

			if (malice != null) {
				nodeCreationTimer++;
				if (nodeCreationTimer >= NODE_CREATION_INTERVAL) {

					malice.createNodeAtPosition(position.x, position.y);
					nodeCreationTimer = 0;
				}
			}
		}

		updateBehavior();

		avoidOtherPrey();

		smoothRotation();

		updateWaves();

		super.update();
	}

	private void updateInfectionEffect() {
		infectionTimer++;

		if (infectionTimer % 10 == 0 && blackDots.size() < MAX_DOTS) {
			blackDots.add(new BlackDot());
		}

		if (blackDots.size() >= MAX_DOTS) {
			sickTimer = 0;
		}
	}

	private void avoidOtherPrey() {
		if (otherPrey == null || otherPrey.isEmpty())
			return;

		PVector avoidanceForce = new PVector(0, 0);
		boolean collision = false;

		for (Prey other : otherPrey) {
			if (other == this || !other.isAlive())
				continue;

			if (detectWithFeelers(other)) {
				PVector awayFromOther = PVector.sub(position, other.getPosition());

				float distance = awayFromOther.mag();
				if (distance < 0.1f)
					distance = 0.1f;

				awayFromOther.normalize();
				awayFromOther.mult(PREY_AVOIDANCE_FORCE * (1.0f / distance));

				avoidanceForce.add(awayFromOther);
				collision = true;
			}
		}

		if (collision) {
			velocity.add(avoidanceForce);
			velocity.limit(speed);

			targetAngle = velocity.heading();

			speed = baseSpeed * 1.2f;
		} else {

			speed = baseSpeed;
		}
	}

	private void smoothRotation() {

		float angleDiff = targetAngle - angle;

		while (angleDiff > Math.PI)
			angleDiff -= 2 * Math.PI;
		while (angleDiff < -Math.PI)
			angleDiff += 2 * Math.PI;

		if (Math.abs(angleDiff) > 0.01f) {
			angle += angleDiff * rotationSpeed;
		}
	}

	private void updateWaves() {
		waveTimer++;
		if (waveTimer >= WAVE_INTERVAL) {
			createStrongerWaves();
			waveTimer = 0;
		}
	}

	private void createStrongerWaves() {
		if (waterSim != null) {

			int x = (int) position.x;
			int y = (int) position.y;

			waterSim.setDisturbanceRadius(2);

			waterSim.createDisturbance(x, y);

			waterSim.setDisturbanceRadius(1);
		}
	}

	private void updateBehavior() {
		if (isInfected && behaviorState != STATE_HUNTING && targetPlant == null) {
			behaviorState = STATE_HUNTING;
		}

		switch (behaviorState) {
		case STATE_WANDERING:
			wander();
			break;
		case STATE_HUNTING:
			huntForPlant();
			break;
		case STATE_FLEEING:

			behaviorState = STATE_WANDERING;
			wander();
			break;
		case STATE_AVOIDING:

			behaviorState = STATE_WANDERING;
			break;
		}
	}

	private void wander() {

		wanderAngle += (random.nextFloat() - 0.5f) * 0.5f;
		PVector wanderForce = new PVector((float) Math.cos(wanderAngle), (float) Math.sin(wanderAngle));
		wanderForce.mult(0.5f);

		velocity.add(wanderForce);
		velocity.limit(speed);

		targetAngle = velocity.heading();

		if (random.nextFloat() < 0.05 || energy < MAX_ENERGY * 0.3) {
			behaviorState = STATE_HUNTING;
		}
	}

	public void huntForPlant(ArrayList<Plant> plants) {
		if (plants == null || plants.isEmpty()) {
			behaviorState = STATE_WANDERING;
			return;
		}

		if (targetPlant == null || !targetPlant.isAlive() || !plants.contains(targetPlant)) {
			float closestDist = Float.MAX_VALUE;
			targetPlant = null;

			for (Plant plant : plants) {
				if (plant.isAlive()) {

					if (detectWithFeelers(plant)) {

						targetPlant = plant;
						break;
					}

					float dist = PVector.dist(position, plant.getPosition());
					if (dist < closestDist && dist < PLANT_DETECTION_RADIUS) {
						closestDist = dist;
						targetPlant = plant;
					}
				}
			}
		}

		if (targetPlant != null && targetPlant.isAlive() && plants.contains(targetPlant)) {
			PVector direction = PVector.sub(targetPlant.getPosition(), position);

			if (checkPlantOverlap(targetPlant)) {
				eatPlant(targetPlant);
				targetPlant = null;
				behaviorState = STATE_WANDERING;
			} else {
				direction.normalize();
				direction.mult(speed);
				velocity = direction;

				targetAngle = direction.heading();
			}
		} else {
			targetPlant = null;
			behaviorState = STATE_WANDERING;
		}
	}

	private boolean checkPlantOverlap(Plant plant) {
		Area preyArea = getOutline();
		Area plantArea = plant.getOutline();

		Area intersection = new Area(preyArea);

		intersection.intersect(plantArea);

		return !intersection.isEmpty();
	}

	private void huntForPlant() {
		behaviorState = STATE_WANDERING;
	}

	public void eatPlant(Plant plant) {
		if (plant != null && plant.isAlive()) {
			plant.setAlive(false);
			if (isInfected) {
				heal();
			}
			energy = Math.min(energy + MAX_ENERGY * 0.2f, MAX_ENERGY);
		}
	}

	public void checkMaliceContact(PVector nodePosition) {
		if (!isInfected) {
			float distance = PVector.dist(position, nodePosition);
			if (distance < MALICE_DETECTION_RADIUS) {
				infectCreature();
			}
		}
	}

	@Override
	public void infectCreature() {
		if (!isInfected) {
			isInfected = true;
			state = STATE_SICK;
			sickTimer = deathThreshold;

			infectionTimer = 0;
			blackDots.clear();

			blackDots.add(new BlackDot());
		}
	}

	@Override
	public void heal() {
		isInfected = false;
		state = STATE_NORMAL;
		energy = Math.min(energy + MAX_ENERGY * 0.3f, MAX_ENERGY);

		infectionTimer = 0;
		blackDots.clear();
	}

	@Override
	public String animalType() {
		return "simulation.entities.Prey";
	}

	@Override
	public void draw(Graphics2D g) {
		if (!isAlive)
			return;

		AffineTransform at = g.getTransform();
		g.translate(position.x, position.y);
		g.rotate(angle + (float) Math.PI / 2);

		if (isInfected) {
			g.setColor(new Color(200, 0, 0));
		} else {
			g.setColor(color);
		}
		g.fillOval(-size / 2, -size / 2, size, size);

		g.setColor(Color.BLACK);
		g.fillOval(size / 4, -size / 4, size / 6, size / 6);
		g.fillOval(-size / 4 - size / 6, -size / 4, size / 6, size / 6);

		g.setColor(color.darker());
		g.fillOval(-size / 4, size / 3, size / 2, size / 3);

		if (isInfected) {
			g.setColor(Color.BLACK);
			for (BlackDot dot : blackDots) {

				float dotX = dot.relativeX * size / 2;
				float dotY = dot.relativeY * size / 2;

				g.fillOval((int) (dotX - dot.size / 2), (int) (dotY - dot.size / 2), (int) dot.size, (int) dot.size);
			}
		}

		g.setTransform(at);

		if (displayInfo) {

			if (isInfected) {
				g.setColor(new Color(200, 0, 0, 100));
			} else {
				g.setColor(new Color(200, 200, 200, 100));
			}
			drawFeelers(g);
		}

		if (displayInfo) {
			drawInfo(g);
		}
	}

	@Override
	public Area getOutline() {
		Ellipse2D.Float circle = new Ellipse2D.Float(position.x - size / 2, position.y - size / 2, size, size);
		return new Area(circle);
	}
}