package simulation.entities;

import processing.core.PVector;
import simulation.core.WaterSimulation;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;

public abstract class Creature extends SimItem {
	protected float angle, speed, baseSpeed;
	protected boolean hasFeelers = true;
	protected float feelerLength = 100, feelerAngle = (float) Math.PI / 4;
	protected static Dimension environmentSize;
	protected static int margin;

	protected float energy;
	protected static final float MAX_ENERGY = 100f;
	protected static final float MIN_ENERGY = 20f;
	protected int state;
	public static final int STATE_NORMAL = 0;
	public static final int STATE_SICK = 1;
	public static final int STATE_DEAD = 2;
	protected int sickTimer = 0;
	protected int deathThreshold = 100;
	protected boolean isInfected = false;

	public static boolean displayInfo = true;

	public static void initializeEnvironment(Dimension envSize, int m) {
		environmentSize = envSize;
		margin = m;
	}

	public Creature(PVector startPos, int size, float scale, WaterSimulation waterSim) {

		super(startPos, size, scale, Color.WHITE, waterSim);

		this.angle = 0;
		this.energy = MAX_ENERGY;
		this.state = STATE_NORMAL;
		this.baseSpeed = 0;
	}

	@Override
	public void update() {
		super.update();

		angle = velocity.heading();
		updateEnergy();
		avoidBorders();
	}

	protected void updateEnergy() {
		float cost = speed * size * 0.001f;
		energy -= cost;
		if (energy < MIN_ENERGY && state == STATE_NORMAL) {
			state = STATE_SICK;
			sickTimer = deathThreshold;
			baseSpeed = speed;
			speed = baseSpeed / 2;
		}
		if (state == STATE_SICK) {
			sickTimer--;
			if (sickTimer <= 0) {
				state = STATE_DEAD;
				isAlive = false;
			}
		}
		if (energy > MAX_ENERGY) {
			float extra = energy - MAX_ENERGY;
			size += (int) (extra * 0.1);
			energy = MAX_ENERGY;
		}
	}

	protected void avoidBorders() {
		if (environmentSize == null)
			return;
		PVector force = new PVector(0, 0);
		float threshold = 200;
		float coef = 3000;

		float dLeft = position.x - margin;
		if (dLeft < threshold) {
			if (dLeft < 1)
				dLeft = 1;
			force.x += coef / (dLeft * dLeft);
		}
		float dRight = (environmentSize.width - margin) - position.x;
		if (dRight < threshold) {
			if (dRight < 1)
				dRight = 1;
			force.x -= coef / (dRight * dRight);
		}
		float dTop = position.y - margin;
		if (dTop < threshold) {
			if (dTop < 1)
				dTop = 1;
			force.y += coef / (dTop * dTop);
		}
		float dBottom = (environmentSize.height - margin) - position.y;
		if (dBottom < threshold) {
			if (dBottom < 1)
				dBottom = 1;
			force.y -= coef / (dBottom * dBottom);
		}
		velocity.add(force);
		velocity.setMag(speed);
	}

	protected void drawFeelers(Graphics2D g) {
		if (!hasFeelers)
			return;
		PVector forward = velocity.copy().normalize().mult(feelerLength);
		PVector forwardEnd = PVector.add(position, forward);
		PVector left = velocity.copy().normalize().rotate(-feelerAngle).mult(feelerLength);
		PVector leftEnd = PVector.add(position, left);
		PVector right = velocity.copy().normalize().rotate(feelerAngle).mult(feelerLength);
		PVector rightEnd = PVector.add(position, right);
		g.drawLine((int) position.x, (int) position.y, (int) forwardEnd.x, (int) forwardEnd.y);
		g.drawLine((int) position.x, (int) position.y, (int) leftEnd.x, (int) leftEnd.y);
		g.drawLine((int) position.x, (int) position.y, (int) rightEnd.x, (int) rightEnd.y);
	}

	protected boolean detectWithFeelers(SimItem other) {
		if (!hasFeelers)
			return false;
		PVector otherPos = other.getPosition();
		PVector forward = velocity.copy().normalize().mult(feelerLength);
		PVector forwardEnd = PVector.add(position, forward);
		PVector left = velocity.copy().normalize().rotate(-feelerAngle).mult(feelerLength);
		PVector leftEnd = PVector.add(position, left);
		PVector right = velocity.copy().normalize().rotate(feelerAngle).mult(feelerLength);
		PVector rightEnd = PVector.add(position, right);
		float forwardDist = pointToLineDistance(position, forwardEnd, otherPos);
		float leftDist = pointToLineDistance(position, leftEnd, otherPos);
		float rightDist = pointToLineDistance(position, rightEnd, otherPos);
		float threshold = other.getSize() / 2;
		return forwardDist < threshold || leftDist < threshold || rightDist < threshold;
	}

	private float pointToLineDistance(PVector lineStart, PVector lineEnd, PVector point) {
		PVector line = PVector.sub(lineEnd, lineStart);
		PVector pv = PVector.sub(point, lineStart);
		float projLength = pv.dot(line) / line.mag();
		if (projLength < 0) {
			return PVector.dist(lineStart, point);
		} else if (projLength > line.mag()) {
			return PVector.dist(lineEnd, point);
		}
		PVector projVector = line.copy().normalize().mult(projLength);
		PVector perpendicular = PVector.sub(pv, projVector);
		return perpendicular.mag();
	}

	public float getEnergy() {
		return energy;
	}

	public void setEnergy(float energy) {
		this.energy = energy;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public void drawInfo(Graphics2D g) {
		AffineTransform at = g.getTransform();
		g.translate(position.x, position.y);

		String st1 = "Size     : " + String.format("%.2f", (float) size);
		String st2 = "Speed  : " + String.format("%.2f", velocity.mag());
		String st3 = "Energy : " + String.format("%.2f", energy);
		String st4 = isInfected ? "Infected" : "Healthy";

		Font f = new Font("Courier", Font.PLAIN, 12);
		g.setFont(f);
		FontMetrics metrics = g.getFontMetrics(f);

		float textWidth = Math.max(Math.max(metrics.stringWidth(st1), metrics.stringWidth(st2)),
				Math.max(metrics.stringWidth(st3), metrics.stringWidth(st4)));
		float textHeight = metrics.getHeight();
		float margin = 12, spacing = 6;

		float offsetY = -(size + margin + textHeight * 4 + spacing * 3);

		g.setColor(new Color(255, 255, 255, 60));
		g.fillRect((int) (-textWidth / 2 - margin), (int) (offsetY), (int) (textWidth + margin * 2),
				(int) (textHeight * 7 + spacing * 3 + margin * 2));

		String typeStr = animalType();
		g.setColor(Color.blue.darker());
		g.drawString(typeStr, -metrics.stringWidth(typeStr) / 2, (int) (offsetY + margin + textHeight));

		g.setColor(Color.black);
		g.drawString(st1, -metrics.stringWidth(st1) / 2, (int) (offsetY + margin + textHeight * 2 + spacing));
		g.drawString(st2, -metrics.stringWidth(st2) / 2, (int) (offsetY + margin + textHeight * 3 + spacing * 2));

		if (state == STATE_SICK)
			g.setColor(Color.red);
		g.drawString(st3, -metrics.stringWidth(st3) / 2, (int) (offsetY + margin + textHeight * 4 + spacing * 3));

		if (isInfected)
			g.setColor(Color.red);
		else
			g.setColor(Color.green);
		g.drawString(st4, -metrics.stringWidth(st4) / 2, (int) (offsetY + margin + textHeight * 5 + spacing * 4));

		g.setTransform(at);
	}

	public void infectCreature() {
		if (!isInfected) {
			isInfected = true;
			state = STATE_SICK;
			sickTimer = deathThreshold;
		}
	}

	public void heal() {
		isInfected = false;
		state = STATE_NORMAL;
		energy = Math.min(energy + MAX_ENERGY * 0.3f, MAX_ENERGY);
	}

	public boolean isInfected() {
		return isInfected;
	}

	public abstract String animalType();

	@Override
	public abstract void draw(Graphics2D g);

	@Override
	public abstract Area getOutline();
}