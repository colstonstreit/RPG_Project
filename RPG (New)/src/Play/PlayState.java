package Play;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Comparator;

import Engine.Game;
import Engine.State;
import Engine.Tools.Vec2;
import Engine.Tools.fRect;
import Play.Entity.Dynamic;

public class PlayState extends State {

	public static TileMap map;
	public static String newMapName;

	public static Camera camera;
	public static Player player;

	public static ArrayList<Dynamic> entities = new ArrayList<Dynamic>();

	public PlayState(Game game) {
		super(game);
		camera = new Camera(game, 0, 0);

		player = new Player(game, new Vec2(12, 17));
		entities.add(player);

		changeMap("Cool Island");

		camera.centerOnEntity(player, false);
	}

	public void tick(double deltaTime) {

		// Update commands
		TheaterEngine.tick(deltaTime);

		// Switch to editor if p is pressed
		if (game.keyUp('p')) game.changeState(Game.States.EDITOR);

		// Switch camera mode if f is pressed
		if (game.keyUp('f')) camera.centerOnEntity(player, !camera.smoothMovement);

		// Test all the commands if t is pressed
		if (game.keyUp('t') && !TheaterEngine.hasCommand()) {
			ArrayList<Command> commands = new ArrayList<Command>();
			commands.add(new Command.FadeOut(game, 1000, 1000, 2000, Color.black));
			commands.add(new Command.ShowDialog(game, "Hi!"));
			commands.add(new Command.Wait(game, 2000));
			commands.add(new Command.Move(game, player, new Vec2(1, 1), 1000, true));
			TheaterEngine.addGroup(commands, true);
		}

		// Zoom out/in if q/e are pressed
		if (game.keyUp('q')) Tile.GAME_SIZE -= 2;
		if (game.keyUp('e')) Tile.GAME_SIZE += 2;

		// Update entities
		for (Dynamic e : entities)
			e.tick(deltaTime);

		// Update map
		map.tick(deltaTime);

		// Change map if necessary!
		if (newMapName != null) {
			changeMap(newMapName);
			newMapName = null;
		}

		// Update camera
		camera.tick(deltaTime);

	}

	public void render(Graphics g) {

		// Render map
		map.render(g, camera.ox, camera.oy);

		// Sort entities by the y-value at their feet
		entities.sort(new Comparator<Dynamic>() {

			public int compare(Dynamic o1, Dynamic o2) {
				return (o1.pos.y + o1.size.y == o2.pos.y + o2.size.y) ? 0 : (o1.pos.y + o1.size.y > o2.pos.y + o2.size.y) ? 1 : -1;
			}

		});

		// Draw entities in the correct order
		for (Dynamic e : entities)
			e.render(g, camera.ox, camera.oy);

		TheaterEngine.render(g, camera.ox, camera.oy);

	}

	/**
	 * Switches the map to the one with the name passed in, or does nothing if the requested map does not exist.
	 * 
	 * @param name The name of the requested map
	 */
	public static void changeMap(String name) {
		if (!Maps.mapList.containsKey(name)) {
			System.out.println("There is no map with the name: " + name + "!");
			return;
		} else if (map != null && name.equals(map.name)) return;

		entities.clear();
		entities.add(player);
		map = Maps.mapList.get(name);
		map.populateDynamics(entities);

	}

	public Vec2 worldToScreen(Vec2 v) { return new Vec2(v.x * Tile.GAME_SIZE + camera.ox, v.y * Tile.GAME_SIZE + camera.oy); }

	public Vec2 screenToWorld(Vec2 v) { return new Vec2((v.x - camera.ox) / Tile.GAME_SIZE, (v.y - camera.oy) / Tile.GAME_SIZE); }

	public fRect worldToScreen(fRect r) {
		Vec2 topCorner = worldToScreen(new Vec2(r.x, r.y));
		Vec2 bottomCorner = worldToScreen(new Vec2(r.x + r.width, r.y + r.height));
		return new fRect(topCorner.x, topCorner.y, bottomCorner.x - topCorner.x, bottomCorner.y - topCorner.y);
	}

	public fRect screenToWorld(fRect r) {
		Vec2 topCorner = screenToWorld(new Vec2(r.x, r.y));
		Vec2 bottomCorner = screenToWorld(new Vec2(r.x + r.width, r.y + r.height));
		return new fRect(topCorner.x, topCorner.y, bottomCorner.x - topCorner.x, bottomCorner.y - topCorner.y);
	}
}
