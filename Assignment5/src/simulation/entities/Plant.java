package simulation.entities;

import processing.core.PVector;
import simulation.core.WaterSimulation;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.Random;

public class Plant extends SimItem {
	private final float DESTROY_MALICE_RADIUS = 30.0f;
	private final float MAX_SPEED = 1.0f;
	private final Random random = new Random();
	private float angle;
	private float wobbleAngle = 0;
	private float waveTimer = 0;

	private static final float WAVE_INTERVAL = 15.0f;

	private PVector acceleration;
	private float nextDirectionChange;
	private float forceMagnitude = 0.05f;

	public Plant(PVector position, int size, WaterSimulation waterSim) {
		super(position, size, 1.0f, new Color(0, 150, 0), waterSim);
		this.angle = random.nextFloat() * (float) Math.PI * 2;

		float speed = 0.5f + random.nextFloat() * 0.5f;
		this.velocity = new PVector((float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed);

		this.acceleration = new PVector(0, 0);
		this.nextDirectionChange = random.nextFloat() * 100;
	}

	@Override
	public void update() {
		PVector oldPosition = position.copy();

		wobbleAngle += 0.05f;
		float wobble = (float) Math.sin(wobbleAngle) * 0.1f;

		nextDirectionChange--;
		if (nextDirectionChange <= 0) {

			float forceX = (random.nextFloat() * 2 - 1) * forceMagnitude;
			float forceY = (random.nextFloat() * 2 - 1) * forceMagnitude;
			acceleration.add(new PVector(forceX, forceY));

			nextDirectionChange = 50 + random.nextFloat() * 100;
		}

		velocity.add(acceleration);
		velocity.add(new PVector((float) Math.cos(wobbleAngle) * 0.02f, (float) Math.sin(wobbleAngle) * 0.02f));

		if (velocity.mag() > MAX_SPEED) {
			velocity.normalize().mult(MAX_SPEED);
		}

		velocity.x += (random.nextFloat() * 0.1f - 0.05f);
		velocity.y += (random.nextFloat() * 0.1f - 0.05f);

		position.add(velocity);

		angle = velocity.heading();

		waveTimer++;
		if (waveTimer >= WAVE_INTERVAL) {
			if (waterSim != null) {
				createWaves();
			}
			waveTimer = 0;
		}

		int buffer = 50;
		int maxWidth = waterSim.getCols() * waterSim.getCellSize() - buffer;
		int maxHeight = waterSim.getRows() * waterSim.getCellSize() - buffer;

		boolean bounced = false;

		if (position.x < buffer) {
			position.x = buffer;
			velocity.x *= -0.8f;
			bounced = true;
		} else if (position.x > maxWidth) {
			position.x = maxWidth;
			velocity.x *= -0.8f;
			bounced = true;
		}

		if (position.y < buffer) {
			position.y = buffer;
			velocity.y *= -0.8f;
			bounced = true;
		} else if (position.y > maxHeight) {
			position.y = maxHeight;
			velocity.y *= -0.8f;
			bounced = true;
		}

		if (bounced) {

			velocity.add(new PVector(random.nextFloat() * 0.2f - 0.1f, random.nextFloat() * 0.2f - 0.1f));
		}

		acceleration.mult(0);
	}

	@Override
	protected void createWaves() {
		if (waterSim != null) {
			int x = (int) position.x;
			int y = (int) position.y;

			if (random.nextFloat() < 0.3f) {
				waterSim.setDisturbanceRadius(2);
				waterSim.createDisturbance(x, y);

				waterSim.setDisturbanceRadius(1);
			} else {

				waterSim.createDisturbance(x, y);
			}
		}
	}

	@Override
	public void draw(Graphics2D g) {
		if (!isAlive)
			return;

		AffineTransform at = g.getTransform();
		g.translate(position.x, position.y);
		g.rotate(angle);

		g.setColor(new Color(0, 100, 0));
		g.fillRect(-2, -size / 2, 4, size);

		g.setColor(color);
		g.fillOval(-size / 3, -size / 2, size / 2, size / 4);
		g.fillOval(-size / 4, -size / 3, size / 2, size / 4);
		g.fillOval(-size / 3, -size / 4, size / 2, size / 4);

		g.setTransform(at);

		if (Creature.displayInfo) {

			Area plantArea = getOutline();
			g.setColor(new Color(0, 255, 0, 30));
			g.fill(plantArea);

			g.setColor(new Color(0, 150, 0, 30));
			g.drawOval((int) (position.x - DESTROY_MALICE_RADIUS), (int) (position.y - DESTROY_MALICE_RADIUS),
					(int) (DESTROY_MALICE_RADIUS * 2), (int) (DESTROY_MALICE_RADIUS * 2));

			drawInfo(g);
		}
	}

	public void drawInfo(Graphics2D g) {
		AffineTransform at = g.getTransform();
		g.translate(position.x, position.y);

		String st1 = "simulation.entities.Plant";
		String st2 = String.format("v:(%.1f,%.1f)", velocity.x, velocity.y);

		Font f = new Font("Courier", Font.PLAIN, 12);
		g.setFont(f);
		FontMetrics metrics = g.getFontMetrics(f);

		g.setColor(new Color(0, 100, 0));
		g.drawString(st1, -metrics.stringWidth(st1) / 2, size + 20);
		g.drawString(st2, -metrics.stringWidth(st2) / 2, size + 35);

		g.setTransform(at);
	}

	@Override
	public Area getOutline() {
		Area plantArea = new Area();

		Ellipse2D.Float mainBody = new Ellipse2D.Float(position.x - size / 3, position.y - size / 3, 2 * size / 3,
				2 * size / 3);
		plantArea.add(new Area(mainBody));

		AffineTransform at = new AffineTransform();
		at.translate(position.x, position.y);
		at.rotate(angle);

		Ellipse2D.Float leaf1 = new Ellipse2D.Float(-size / 3, -size / 2, size / 2, size / 4);
		Area leaf1Area = new Area(leaf1);
		leaf1Area.transform(at);
		plantArea.add(leaf1Area);

		Ellipse2D.Float leaf2 = new Ellipse2D.Float(-size / 4, -size / 3, size / 2, size / 4);
		Area leaf2Area = new Area(leaf2);
		leaf2Area.transform(at);
		plantArea.add(leaf2Area);

		Ellipse2D.Float leaf3 = new Ellipse2D.Float(-size / 3, -size / 4, size / 2, size / 4);
		Area leaf3Area = new Area(leaf3);
		leaf3Area.transform(at);
		plantArea.add(leaf3Area);

		return plantArea;
	}

	public boolean canDestroyMalice(PVector nodePosition) {
		float distance = PVector.dist(position, nodePosition);
		return distance < DESTROY_MALICE_RADIUS;
	}
}