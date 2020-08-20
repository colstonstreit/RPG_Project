package Play;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;

import Engine.Game;
import Engine.Tools.Vec2;
import Engine.Tools.fRect;
import Play.Entity.Dynamic;

public class TheaterEngine {

	private static LinkedList<ArrayList<Command>> commandGroups = new LinkedList<ArrayList<Command>>(); // The current queue of commands
	private static boolean justCompleted = false; // whether or not a command has been completed in the last tick() cycle

	/**
	 * Returns true if there is a command currently in progress or if one has just completed.
	 */
	public static boolean hasCommand() { return justCompleted || !commandGroups.isEmpty(); }

	/**
	 * Adds a new command to the queue.
	 * 
	 * @param c The command to be added to the queue.
	 */
	public static void add(Command c) {
		ArrayList<Command> t = new ArrayList<Command>();
		t.add(c);
		commandGroups.add(t);
	}

	/**
	 * Adds a new group of commands to the queue.
	 * 
	 * @param commandGroup A list of commands to be added to the queue.
	 * @param separate     True if each command in the commandGroup should be done in sequential order, False if should be done concurrently
	 */
	public static void addGroup(ArrayList<Command> commandGroup, boolean separate) {
		if (separate) {
			for (int i = 0, n = commandGroup.size(); i < n; i++)
				add(commandGroup.get(i));
		} else commandGroups.add(commandGroup);
	}

	/**
	 * Updates the command at the front of the queue, and removes it if it has been completed.
	 */
	public static void tick(double deltaTime) {
		// Reset just completed flag every frame
		justCompleted = false;

		// Update the first group of commands if it exists, and pop it out if it's finished.
		if (!commandGroups.isEmpty()) {
			ArrayList<Command> list = commandGroups.get(0);

			// Update each command in the first command group, and remove them as they are finished
			for (int i = 0, n = list.size(); i < n; i++) {
				Command c = list.get(i);
				if (c.hasCompleted) {
					list.remove(i);
					i--;
					n--;
					justCompleted = true;
				} else c.tick(deltaTime);
			}

			// If all commands in this group have been completed, remove this empty group from the command list
			if (list.isEmpty()) commandGroups.pop();
		}
	}

	public static void render(Graphics g, int ox, int oy) {
		// Render each of the commands in the first group
		if (!commandGroups.isEmpty()) {
			ArrayList<Command> list = commandGroups.get(0);
			for (int i = 0, n = list.size(); i < n; i++)
				list.get(i).render(g, ox, oy);
		}

	}

	/**
	 * Returns true if a current command is a Move command AND the entity controlled by that move command is the Dynamic passed in.
	 * 
	 * @param e The entity to be checked whether or not is under control by a Move command.
	 */
	public static boolean hasControl(Dynamic e) {
		if (commandGroups.isEmpty() || commandGroups.get(0).isEmpty()) return false;

		for (int i = 0, n = commandGroups.get(0).size(); i < n; i++) {
			Command c = commandGroups.get(0).get(i);
			if (c instanceof Command.Move && ((Command.Move) c).e == e) return true;
		}

		return false;
	}
}

//////////////////////////////////////////////////////////////////////////////////////////

abstract class Command {

	protected Game game; // an instance of the Game object
	public boolean hasStarted = false, hasCompleted = false; // Whether or not the command has been started and completed

	public Command(Game game) { this.game = game; }

	/**
	 * Performs tasks that can only be done once at the start of the command.
	 */
	public void start() {}

	/**
	 * Calls the start method if hasn't already, then updates the command.
	 */
	public void tick(double deltaTime) {
		if (!hasStarted) {
			start();
			hasStarted = true;
		}
	}

	/**
	 * Renders the command if applicable.
	 */
	public void render(Graphics g, int ox, int oy) {}

	/**
	 * Completes the command.
	 */
	public void complete() { hasCompleted = true; }

	//////////////////////////////////////////////////////////////////////////////////////////

	public static class ShowDialog extends Command {

		private fRect dialogBox; // The box where dialog should be displayed
		private String dialog; // The dialog to be shown

		/**
		 * @param game   An instance of the game object
		 * @param dialog The dialog that should be shown on screen
		 */
		public ShowDialog(Game game, String dialog) {
			super(game);
			this.dialog = dialog;
		}

		public void tick(double deltaTime) {
			super.tick(deltaTime);
			// Close dialog upon pressing correct key
			if (game.keyUp(KeyEvent.VK_ENTER)) complete();
		}

		public void render(Graphics g, int ox, int oy) {
			// Create dialog box if haven't yet
			if (dialogBox == null) dialogBox = new fRect(0, 0, game.getWidth(), game.getHeight()).getSubRect(0.05, 0.7, 0.9, 0.25);

			// Fill and draw before drawing text on top
			dialogBox.fill(g, new Color(0, 0, 0, 50));
			dialogBox.draw(g, Color.white);
			g.setFont(new Font("Times New Roman", Font.BOLD, 24));
			Game.drawCenteredString(g, Color.white, dialog, dialogBox, true);
		}

	}

	//////////////////////////////////////////////////////////////////////////////////////////

	public static class Wait extends Command {

		private long timer; // A timer to measure how much time has passed
		private int delay; // The delay in milliseconds to wait

		/**
		 * @param game  The instance of the Game object
		 * @param delay The delay in milliseconds that should be waited
		 */
		public Wait(Game game, int delay) {
			super(game);
			this.delay = delay;
		}

		public void start() { timer = System.currentTimeMillis(); }

		public void tick(double deltaTime) {
			super.tick(deltaTime);
			// Complete command if the wait period has passed
			if (System.currentTimeMillis() - timer >= delay) complete();
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////

	public static class Move extends Command {

		protected Dynamic e; // The entity to be moved
		private Vec2 p; // The position the entity should be moved to
		private Vec2 v; // The velocity vector the entity must follow
		private double time; // The time it should take to move there.
		private double timeElapsed; // The amount of time in milliseconds that have passed by so far.

		private boolean moveThroughThings; // whether or not the entity should ignore collisions.
		private boolean wasSolidVsStatic; // whether or not the entity was originally solid vs static entities
		private boolean wasSolidVsDynamic; // whether or not the entity was originally solid vs dynamic entities.

		/**
		 * @param game              An instance of the game
		 * @param e                 The entity to be moved
		 * @param p                 The position the entity should be moved to
		 * @param time              The amount of time in milliseconds that this movement should take
		 * @param moveThroughThings True if collision should be disregarded during this movement, False if not
		 */
		public Move(Game game, Dynamic e, Vec2 p, double time, boolean moveThroughThings) {
			super(game);
			this.e = e;
			this.p = p;
			this.time = time;
			this.moveThroughThings = moveThroughThings;
			wasSolidVsStatic = e.solidVsStatic;
			wasSolidVsDynamic = e.solidVsDynamic;
		}

		public void start() {
			// Get initial velocity (change in position) and set collision flags to false if so desired
			v = p.subtract(e.pos);
			if (moveThroughThings) e.setCollisionType(false, false);
		}

		public void tick(double deltaTime) {
			super.tick(deltaTime);

			// Add the time that has passed
			timeElapsed += deltaTime;

			// If enough time has passed or the entity is very close to its target, end the movement.
			if (timeElapsed >= time || p.subtract(e.pos).getMagnitude() <= 0.05) {
				complete();
				return;
			}

			// If the unit vector of the displacement changes from initial value, object collided: calculate new path and reset timer
			if (!moveThroughThings) {
				Vec2 newV = p.subtract(e.pos);
				if (!newV.norm().equals(v.norm())) {
					v = newV;
					time -= timeElapsed;
					timeElapsed = 0;
				}
			}

			// Set entity's velocity to the correct proportion
			e.v = v.scale(deltaTime / time);
		}

		public void complete() {
			super.complete();
			// Restore initial flags to their old states
			e.pos = p;
			e.v = new Vec2(0, 0);
			e.setCollisionType(wasSolidVsStatic, wasSolidVsDynamic);
		}

	}

	//////////////////////////////////////////////////////////////////////////////////////////

	public static class FadeOut extends Command {

		private int fadeOutLength, holdLength, fadeInLength; // The length of time spent in the fade in/out and hold phase
		private final Color color; // The color to faded in and out
		private int stage; // The number of the phase it's in (fade out (1), hold (2), fade in (3))

		private double timeElapsed; // The amount of time that has elapsed so far
		private int alpha; // The current transparency of the color to be drawn

		/**
		 * @param game          The instance of the Game object
		 * @param fadeOutLength The number of milliseconds spent in the fading out phase
		 * @param holdLength    The number of millseconds spent in the holding phase (holds the same opaque color)
		 * @param fadeInLength  The number of milliseconds spent in the fading back in phase
		 * @param color         The color that should be faded in
		 */
		public FadeOut(Game game, int fadeOutLength, int holdLength, int fadeInLength, Color color) {
			super(game);
			this.fadeOutLength = fadeOutLength;
			this.holdLength = holdLength;
			this.fadeInLength = fadeInLength;
			this.color = color;
		}

		public void tick(double deltaTime) {
			super.tick(deltaTime);
			// Update how much time has gone by
			timeElapsed += deltaTime;
			if (stage == 0) { // If fading out, calculate alpha based on linear interpolation
				alpha = (int) Math.min(255, (timeElapsed / fadeOutLength * 255.0));
				if (timeElapsed >= fadeOutLength) {
					// Move on to the next stage if enough time has passed, and reset how much time has passed for new timer
					stage++;
					timeElapsed = 0;
				}
			} else if (stage == 1) { // If holding, keep alpha at 255 (full opacity)
				alpha = 255;
				if (timeElapsed >= holdLength) {
					stage++;
					timeElapsed = 0;
				}
			} else if (stage == 2) { // If fading back in, calculate alpha based on linear interpolation
				alpha = (int) Math.max(0, (255.0 - timeElapsed / fadeInLength * 255.0));
				if (timeElapsed >= fadeInLength) {
					complete();
				}
			}
		}

		public void render(Graphics g, int ox, int oy) {
			g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
			g.fillRect(0, 0, game.getWidth(), game.getHeight());
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unused")
	private static class Template extends Command {

		public Template(Game game) { super(game); }

		public void start() {}

		public void tick(double deltaTime) {
			super.tick(deltaTime);
			// Do stuff below

		}

		public void render(Graphics g, int ox, int oy) {}

		public void complete() {

			// Do stuff above
			super.complete();
		}

	}

}
