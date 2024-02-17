#include "ResourceManager.h"

#include "Shader.h"
#include "Texture.h"
#include "Spritesheet.h"

void ResourceManager::loadResources() {
    this->loadShaders();
    this->loadTextures();
}

ResourceManager::ResourceManager() {
    // Allocate large byte arrays to store each resource type
    shaderArray = (Shader*) new unsigned char[sizeof(Shader) * NUM_SHADERS];
    textureArray = (Texture*) new unsigned char[sizeof(Texture) * NUM_TEXTURES];
    spritesheetArray = (Spritesheet*) new unsigned char[sizeof(Spritesheet) * NUM_SPRITESHEETS];
}

ResourceManager::~ResourceManager() {
    // Must call destructors manually due to use of placement new() operator
    for (unsigned int i = 0; i < NUM_SHADERS; i++)
        this->shaderArray[i].~Shader();
    for (unsigned int i = 0; i < NUM_TEXTURES; i++)
        this->textureArray[i].~Texture();
    for (unsigned int i = 0; i < NUM_SPRITESHEETS; i++)
        this->spritesheetArray[i].~Spritesheet();

    // Delete overall arrays that were allocated
    delete[]((unsigned char*) shaderArray);
    delete[]((unsigned char*) textureArray);
    delete[]((unsigned char*) spritesheetArray);
}

const Shader& ResourceManager::getShader(EShader eshader) const {
    return this->shaderArray[static_cast<int>(eshader)];
}

const Texture& ResourceManager::getTexture(ETexture etexture) const {
    return this->textureArray[static_cast<int>(etexture)];
}

const Spritesheet& ResourceManager::getSpritesheet(ESpritesheet espritesheet) const {
    return this->spritesheetArray[static_cast<int>(espritesheet)];
}

void ResourceManager::loadShaders() {
    // Lambda function to load a shader with placement new operator
    auto loadShader = [this](EShader eshader, const char* vertexPath, const char* fragmentPath) {
        int index = static_cast<int>(eshader);
        new (&(this->shaderArray[index])) Shader(vertexPath, fragmentPath);
    };

    // Go through and load all shaders
    loadShader(EShader::DEFAULT, "res/shaders/VertexShader.vert", "res/shaders/FragmentShader.frag");
    loadShader(EShader::TEST_2D, "res/shaders/2DVertexShader.vert", "res/shaders/2DFragmentShader.frag");
}

void ResourceManager::loadTextures() {
    // Lambda function to load a texture with placement new operator
    auto loadTexture = [this](ETexture etexture, const char* texturePath) {
        int index = static_cast<int>(etexture);
        new (&(this->textureArray[index])) Texture(texturePath);
    };

    // Go through and load all textures
    loadTexture(ETexture::FACE, "res/img/awesomeface.png");
    loadTexture(ETexture::BOX, "res/img/container.jpg");
    loadTexture(ETexture::TILE_SHEET, "res/spritesheets/tileSheet.png");
    loadTexture(ETexture::ITEM_SHEET, "res/spritesheets/itemSheet.png");
    loadTexture(ETexture::CHARACTER_SHEET, "res/spritesheets/characterSheet.png");
}

void ResourceManager::loadSpritesheets() {
    // Lambda function to load a spritesheet with placement new operator
    auto loadSpritesheet = [this](ESpritesheet espritesheet, const Texture& texture, unsigned int tileWidth, unsigned int tileHeight) {
        int index = static_cast<int>(espritesheet);
        new (&(this->spritesheetArray[index])) Spritesheet(texture, tileWidth, tileHeight);
    };

    // Go through and load all spritesheets
    loadSpritesheet(ESpritesheet::TILE_SHEET, this->getTexture(ETexture::TILE_SHEET), 16, 16);
    loadSpritesheet(ESpritesheet::ITEM_SHEET, this->getTexture(ETexture::ITEM_SHEET), 16, 16);
    loadSpritesheet(ESpritesheet::CHARACTER_SHEET, this->getTexture(ETexture::CHARACTER_SHEET), 16, 16);
}
