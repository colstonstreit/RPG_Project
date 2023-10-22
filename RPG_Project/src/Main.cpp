#include "Dialogue.h"
#include "Game.h"
#include "Scenes/Scene3DTest.h"

#include <iostream>
#include <string>

int oldMain(int argc, char** argv) {

    DialogueManager::parse("res/dialogue/Dialogue_Test.dlog");
    DialogueController* dc = DialogueManager::get("introNpc");
    const std::unordered_map<std::string, bool> gameFlags = {
        { "worldSaved", true },
        { "dogMissing", false }
    };
    dc->chooseDialogue(gameFlags);

    return 0;
}

int main(int argc, char** argv) {
    Game game = Game(800, 600, "RPG Version 2");
    game.init();
    game.changeScene(new Scene3DTest(game));
    game.run();
    return 0;
}