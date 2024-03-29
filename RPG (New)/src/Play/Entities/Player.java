package Play.Entities;

import java.awt.Graphics;
import java.awt.event.KeyEvent;

import Engine.Game;
import Engine.AssetManager.CharacterSprites;
import Engine.Tools.Vec2;
import Engine.Tools.fRect;
import Play.PlayState;
import Play.Quests.Quest;
import Play.Quests.QuestManager;
import Play.TheaterEngine.Commands.TheaterEngine;

public class Player extends Creature {

	/**
	 * @param game An instance of the game object
	 * @param x    The initial x coordinate in the world
	 * @param y    The initial y coordinate in the world
	 */
	public Player(Game game, Vec2 pos) {
		super(game, "Player", CharacterSprites.SQUIRTLE, pos);

		// Set player defaults
		size = NPC.SIZE;
		this.pos = new Vec2(pos.x + (1 - size.x) / 2, pos.y + (1 - size.y) / 2);
		relativeHitbox = new fRect(4.0 / 16, 11.0 / 16, 8.0 / 16, 5.0 / 16);

	}

	public void tick(double deltaTime) {

		// Handle input if theater not happening, otherwise zero velocity
		if (!TheaterEngine.hasCommand()) {

			if (game.keyDown('s') && !game.keyDown('w')) v.y = 0.01 * deltaTime;
			else if (game.keyDown('w') && !game.keyDown('s')) v.y = -0.01 * deltaTime;
			else v.y = 0;

			if (game.keyDown('a') && !game.keyDown('d')) v.x = -0.01 * deltaTime;
			else if (game.keyDown('d') && !game.keyDown('a')) v.x = 0.01 * deltaTime;
			else v.x = 0;

		} else if (!TheaterEngine.hasControl(this)) v = new Vec2(0, 0);

		// Handle collisions and animations
		super.tick(deltaTime);

		// Check interactions
		if (game.keyUp(KeyEvent.VK_ENTER) && !TheaterEngine.hasCommand()) {
			for (Dynamic e : PlayState.entities) {
				if (e != this && interactArea().intersects(e.interactableRegion())) {
					// Do whatever to the entity
					e.onInteract(this);

					// Check map, then check quests
					boolean breakLoop = false;
					if (PlayState.map.onInteract(e)) breakLoop = true;

					for (Quest q : QuestManager.currentQuestList) {
						if (q.onInteract(e)) {
							breakLoop = true;
							break;
						}
					}
					if (breakLoop) break;
				}
			}
		}
	}

	public void render(Graphics g, int ox, int oy) {

		super.render(g, ox, oy);
		if (isOnScreen()) {
			// worldToScreen(interactArea()).draw(g, Color.white);
		}

	}

	/**
	 * Returns a Rectangle representing where the Player's interaction zone is, outside of the player body, in world coordinates.
	 */
	public fRect interactArea() {
		fRect r;
		switch (facing) {
			case Up:
				r = new fRect(0, -0.5, 1, 0.5);
				break;
			case Down:
				r = new fRect(0, 1, 1, 0.5);
				break;
			case Left:
				r = new fRect(-0.25, 0, 0.5, 1);
				break;
			case Right:
				r = new fRect(0.75, 0, 0.5, 1);
				break;
			default:
				return interactableRegion();
		}
		return new fRect(pos.x + size.x * r.x, pos.y + size.y * r.y, size.x * r.width, size.y * r.height);
	}

	public void onInteract(Entity e) {}

}
