package Play.Maps;

import java.util.ArrayList;

import Engine.AssetManager.CharacterSprites;
import Engine.Game;
import Engine.Tools.Function;
import Engine.Tools.Vec2;
import Play.LootTable;
import Play.Entities.Dynamic;
import Play.Entities.Entity;
import Play.Entities.NPC;
import Play.Entities.Teleport;
import Play.Entities.Trigger;
import Play.Entities.Trigger.WillTrigger;
import Play.Maps.MapManager.Maps;
import Play.Quests.QuestManager;
import Play.Quests.QuestManager.Quests;
import Play.TheaterEngine.Commands.TheaterEngine;
import Play.TheaterEngine.Cutscenes.CutsceneManager.Cutscenes;

public class CoolIslandMap extends TileMap {

	private static NPC sparky, squirty, bulby;
	private static Teleport throughLava, toOtherCorner, fromHouse;

	private static Trigger cueKillCutscene;

	public CoolIslandMap(Game game) {
		super(game, Maps.COOL_ISLAND);
		QuestManager.setInitiator(Quests.PIKACHU_CORNER, sparky = new NPC(game, "Sparky", CharacterSprites.PIKACHU, new Vec2(19, 29)));
		QuestManager.setInitiator(Quests.LAVA_MAN, squirty = new NPC(game, "Squirty", CharacterSprites.SQUIRTLE, new Vec2(30, 29)));
		bulby = new NPC(game, "Bulby", CharacterSprites.BULBASAUR, new Vec2(15, 15)).setText(new LootTable<String>().addSet(
				new String[] { "Hi!" , "You're awesome!" , "I feel sad." ,
						"WOW! You had a 1/25 chance to see this message! That's like totally the coolest thing ever dude!" },
				new double[] { 8 , 8 , 8 , 1 }));

		throughLava = (Teleport) new Teleport(game, true, "throughLava", new Vec2(11, 12), Maps.LOL).setShouldBeDrawn(true).setTransform(20, 24, 10, 5);
		toOtherCorner = (Teleport) new Teleport(game, false, "Other Corner", new Vec2(49, 49)).setShouldBeDrawn(true).setTransform(2, 2, 1, 1);
		fromHouse = (Teleport) new Teleport(game, false, "Into House", new Vec2(4.5, 6), Maps.INSIDE_HOUSE).setTransform(24, 9, 1, 1);

		cueKillCutscene = (Trigger) new Trigger(game, "CueKill", true, WillTrigger.FOREVER, new Function() {

			public void run() { TheaterEngine.cueCutscene(Cutscenes.EXAMPLE); }

		}).setShouldBeDrawn(true).setCollisionType(false, false).setTransform(20, 40, 10, 10);
	}

	public void populateDynamics(ArrayList<Dynamic> entities) {
		entities.add(sparky.setText(getDialog(sparky)));
		entities.add(squirty.setText(getDialog(squirty)));
		entities.add(bulby);
		entities.add(throughLava);
		entities.add(toOtherCorner);
		entities.add(fromHouse);
		entities.add(cueKillCutscene);
	}

	public String getDialog(Entity e) {
		if (e == sparky) {
			return !QuestManager.completedQuest(Quests.PIKACHU_CORNER, false) ? "Hey, would you run to the top-left corner for me?"
					: "You helped me! Thank you so much.";
		} else if (e == squirty) {
			return !QuestManager.doingQuest(Quests.LAVA_MAN) && !QuestManager.completedQuest(Quests.LAVA_MAN, false)
					? "I've got a quest for you! Go talk to the man past the lava. He'll explain what you need to do."
					: "Have fun! Give me some apples.";
		} else return super.getDialog(e);
	}

	public boolean onInteract(Entity target) {
		if (target == squirty) {
			QuestManager.addQuest(Quests.LAVA_MAN);
			squirty.setText(getDialog(squirty));
			return true;
		} else if (target == sparky) {
			QuestManager.addQuest(Quests.PIKACHU_CORNER);
			return true;
		} else if (target == throughLava) {
			return true;
		}
		return false;
	}

}
