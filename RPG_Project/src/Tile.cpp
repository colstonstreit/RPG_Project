#include "Tile.h"

#include <glad/glad.h>
#include <GLFW/glfw3.h>

#include <fstream>
#include <iostream>

#include "Game.h"
#include "ResourceManager.h"
#include "Shader.h"
#include "Spritesheet.h"
#include "Texture.h"

/* ======================================== Tile ======================================== */

std::vector<Tile*> Tile::s_Tiles = std::vector<Tile*>(static_cast<int>(ETile::NUM_TILES), nullptr);
bool Tile::s_IsDirty = true;

void Tile::sInitializeTiles(const ResourceManager& resourceManager) {

    // Lambda function to load tiles more easily
    auto loadStaticTile = [](ETile tileType, Sprite* sprite) {
        int index = static_cast<int>(tileType);
        Tile::s_Tiles[index] = new TileStatic(tileType, sprite);
    };

    auto loadAnimatedTile = [](ETile tileType, Sprite* animationFrames, unsigned int numberOfFrames, float secondsPerFrame) {
        int index = static_cast<int>(tileType);
        Tile::s_Tiles[index] = new TileAnimated(tileType, animationFrames, numberOfFrames, secondsPerFrame);
    };

    // Grab needed spritesheets
    const Spritesheet& tileSheet = resourceManager.getSpritesheet(ESpritesheet::TILE_SHEET);

    // Actually load tiles
    loadStaticTile(ETile::GRASS, new Sprite(tileSheet.crop(0, 0)));
    loadStaticTile(ETile::SAND, new Sprite(tileSheet.crop(1, 0)));
    loadStaticTile(ETile::BRICK, new Sprite(tileSheet.crop(2, 0)));
    loadAnimatedTile(ETile::WATER, new Sprite[4] {
        tileSheet.crop(3, 0),
        tileSheet.crop(4, 0),
        tileSheet.crop(5, 0),
        tileSheet.crop(4, 0)
                     }, 4, 0.75f);
    loadAnimatedTile(ETile::LAVA, new Sprite[4] {
        tileSheet.crop(6, 0),
        tileSheet.crop(7, 0),
        tileSheet.crop(8, 0),
        tileSheet.crop(7, 0)
                     }, 4, 0.75f);
    loadStaticTile(ETile::TREE, new Sprite(tileSheet.crop(9, 0)));
    loadStaticTile(ETile::SUN, new Sprite(tileSheet.crop(10, 0)));
    loadStaticTile(ETile::FLOWER, new Sprite(tileSheet.crop(11, 0)));
    loadStaticTile(ETile::HOUSE_DOOR, new Sprite(tileSheet.crop(12, 0)));
    loadStaticTile(ETile::HOUSE_WINDOW, new Sprite(tileSheet.crop(13, 0)));
    loadStaticTile(ETile::HOUSE_WALL, new Sprite(tileSheet.crop(14, 0)));
    loadStaticTile(ETile::BLUE_HOUSE_UPPER_LEFT, new Sprite(tileSheet.crop(12, 1)));
    loadStaticTile(ETile::BLUE_HOUSE_UPPER_MID, new Sprite(tileSheet.crop(13, 1)));
    loadStaticTile(ETile::BLUE_HOUSE_UPPER_RIGHT, new Sprite(tileSheet.crop(14, 1)));
    loadStaticTile(ETile::BLUE_HOUSE_LOWER_LEFT, new Sprite(tileSheet.crop(12, 2)));
    loadStaticTile(ETile::BLUE_HOUSE_LOWER_MID, new Sprite(tileSheet.crop(13, 2)));
    loadStaticTile(ETile::BLUE_HOUSE_LOWER_RIGHT, new Sprite(tileSheet.crop(14, 2)));
    loadStaticTile(ETile::WOOD_FLOORBOARD, new Sprite(tileSheet.crop(15, 0)));
    loadStaticTile(ETile::STONE_BRICK, new Sprite(tileSheet.crop(15, 1)));

    // Empty Tile - use invalid crop indices to generate out-of-bound UVs, which will be discarded in shader
    loadStaticTile(ETile::NULL_EMPTY, new Sprite(tileSheet.crop(-1, -1)));
}

void Tile::sUpdateTiles(double deltaTime) {
    for (Tile* tile : Tile::s_Tiles) {
        if (tile != nullptr) {
            bool frameChanged = tile->update(deltaTime);
            if (frameChanged) {
                Tile::s_IsDirty = true;
            }
        }
    }
}

const Tile* Tile::sGetTile(ETile tileType) {
    return Tile::s_Tiles[static_cast<int>(tileType)];
}

bool Tile::sTileIsDirty() {
    return Tile::s_IsDirty;
}

void Tile::sCleanDirtyTile() {
    Tile::s_IsDirty = false;
}

Tile::Tile(ETile tileType) : tileType(tileType) {}

TileStatic::TileStatic(ETile tileType, Sprite* sprite) : Tile(tileType), sprite(sprite) {}

TileStatic::~TileStatic() {
    delete this->sprite;
}

bool TileStatic::update(double deltaTime) { return false; }

const Sprite& TileStatic::getCurrentSprite() const {
    return *(this->sprite);
}

TileAnimated::TileAnimated(ETile tileType, Sprite* animationFrames, unsigned int numberOfFrames, float secondsPerFrame)
    : Tile(tileType), animationFrames(animationFrames), numberOfFrames(numberOfFrames), secondsPerFrame(secondsPerFrame) {}

TileAnimated::~TileAnimated() {
    delete[] this->animationFrames;
}

bool TileAnimated::update(double deltaTime) {
    this->elapsedTime += deltaTime;
    if (this->elapsedTime >= secondsPerFrame) {
        this->elapsedTime -= secondsPerFrame;
        this->currentFrameIndex = (this->currentFrameIndex + 1) % this->numberOfFrames;
        return true;
    }
    return false;
}

const Sprite& TileAnimated::getCurrentSprite() const {
    return this->animationFrames[this->currentFrameIndex];
}


/* ======================================== TileLayer ======================================== */

TileLayer::TileLayer(Game& game, unsigned int width, unsigned int height, ETile* tileData) : game(game), width(width), height(height), tiles(tileData) {}

TileLayer::~TileLayer() {
    if (this->tiles) {
        delete[] this->tiles;
    }
}

void TileLayer::init() {

    if (tiles == nullptr) {
        tiles = new ETile[width * height];
        for (unsigned int i = 0; i < width * height; i++) {
            int remainder = i % static_cast<int>(ETile::NUM_TILES);
            tiles[i] = static_cast<ETile>(remainder);
        }
    }

    // Generate VAO
    glGenVertexArrays(1, &this->VAO);
    glBindVertexArray(this->VAO);

    // Generate buffer data
    float* staticData = new float[this->width * this->height * 5 * 4];
    float* dynamicData = new float[this->width * this->height * 2 * 4];

    unsigned int staticIndex = 0;
    unsigned int dynamicIndex = 0;
    for (int y = 0; y < this->height; y++) {
        for (int x = 0; x < this->width; x++) {

            ETile type = tiles[y * this->width + x];
            const Tile* tile = Tile::sGetTile(type);
            const Sprite& currentSprite = tile->getCurrentSprite();

            staticData[staticIndex++] = x;
            staticData[staticIndex++] = y;
            staticData[staticIndex++] = 1.0f;
            staticData[staticIndex++] = 1.0f;
            staticData[staticIndex++] = 1.0f;
            dynamicData[dynamicIndex++] = currentSprite.topLeftUV.s;
            dynamicData[dynamicIndex++] = currentSprite.topLeftUV.t;

            staticData[staticIndex++] = x + 1.0f;
            staticData[staticIndex++] = y;
            staticData[staticIndex++] = 1.0f;
            staticData[staticIndex++] = 1.0f;
            staticData[staticIndex++] = 1.0f;
            dynamicData[dynamicIndex++] = currentSprite.topRightUV.s;
            dynamicData[dynamicIndex++] = currentSprite.topRightUV.t;

            staticData[staticIndex++] = x;
            staticData[staticIndex++] = y + 1.0f;
            staticData[staticIndex++] = 1.0f;
            staticData[staticIndex++] = 1.0f;
            staticData[staticIndex++] = 1.0f;
            dynamicData[dynamicIndex++] = currentSprite.bottomLeftUV.s;
            dynamicData[dynamicIndex++] = currentSprite.bottomLeftUV.t;

            staticData[staticIndex++] = x + 1.0f;
            staticData[staticIndex++] = y + 1.0f;
            staticData[staticIndex++] = 1.0f;
            staticData[staticIndex++] = 1.0f;
            staticData[staticIndex++] = 1.0f;
            dynamicData[dynamicIndex++] = currentSprite.bottomRightUV.s;
            dynamicData[dynamicIndex++] = currentSprite.bottomRightUV.t;

        }
    }

    // Construct indices
    unsigned int indicesIndex = 0;
    unsigned int* indices = new unsigned int[6 * this->width * this->height];
    for (int y = 0; y < this->height; y++) {
        for (int x = 0; x < this->width; x++) {
            unsigned int tileBase = 4 * (y * this->width + x);
            indices[indicesIndex++] = tileBase;
            indices[indicesIndex++] = tileBase + 1;
            indices[indicesIndex++] = tileBase + 2;

            indices[indicesIndex++] = tileBase + 1;
            indices[indicesIndex++] = tileBase + 2;
            indices[indicesIndex++] = tileBase + 3;
        }
    }
    glGenBuffers(1, &this->EBO);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this->EBO);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, 6 * this->width * this->height * sizeof(unsigned int), indices, GL_STATIC_DRAW);
    delete[] indices;

    // Build out static attributes (position, color?)
    glGenBuffers(1, &this->VBOStatic);
    glBindBuffer(GL_ARRAY_BUFFER, this->VBOStatic);
    glBufferData(GL_ARRAY_BUFFER, this->width * this->height * 5 * 4 * sizeof(float), staticData, GL_STATIC_DRAW);
    delete[] staticData;

    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 5 * sizeof(float), (void*) 0);
    glEnableVertexAttribArray(0);

    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 5 * sizeof(float), (void*) (sizeof(float) * 2));
    glEnableVertexAttribArray(1);

    // Build out dynamic attributes (UVs, maybe texture ID later?)
    glGenBuffers(1, &this->VBODynamic);
    glBindBuffer(GL_ARRAY_BUFFER, this->VBODynamic);
    glBufferData(GL_ARRAY_BUFFER, this->width * this->height * 2 * 4 * sizeof(float), dynamicData, GL_STATIC_DRAW);
    delete[] dynamicData;

    glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 0, (void*) 0);
    glEnableVertexAttribArray(2);

    // Bind texture
    const Texture& tileTexture = this->game.getResourceManager().getTexture(ETexture::TILE_SHEET);
    glActiveTexture(GL_TEXTURE0);
    tileTexture.bind();

    // Bind shader
    const Shader& shaderProgram = this->game.getResourceManager().getShader(EShader::TEST_2D);
    shaderProgram.use();

}

void TileLayer::update(double deltaTime) {
    bool needsToRefresh = Tile::sTileIsDirty();
    if (needsToRefresh) {
        Tile::sCleanDirtyTile();

        float* dynamicData = new float[this->width * this->height * 2 * 4];
        unsigned int dynamicIndex = 0;
        for (int y = 0; y < this->height; y++) {
            for (int x = 0; x < this->width; x++) {

                ETile type = tiles[y * this->width + x];
                const Tile* tile = Tile::sGetTile(type);
                const Sprite& currentSprite = tile->getCurrentSprite();

                dynamicData[dynamicIndex++] = currentSprite.topLeftUV.s;
                dynamicData[dynamicIndex++] = currentSprite.topLeftUV.t;

                dynamicData[dynamicIndex++] = currentSprite.topRightUV.s;
                dynamicData[dynamicIndex++] = currentSprite.topRightUV.t;

                dynamicData[dynamicIndex++] = currentSprite.bottomLeftUV.s;
                dynamicData[dynamicIndex++] = currentSprite.bottomLeftUV.t;

                dynamicData[dynamicIndex++] = currentSprite.bottomRightUV.s;
                dynamicData[dynamicIndex++] = currentSprite.bottomRightUV.t;

            }
        }

        glBindBuffer(GL_ARRAY_BUFFER, this->VBODynamic);
        glBufferSubData(GL_ARRAY_BUFFER, 0, this->width * this->height * 2 * 4 * sizeof(float), dynamicData);
        delete[] dynamicData;
    }
}

void TileLayer::render(const glm::mat4& projectionMatrix) {
    const ResourceManager& resourceManager = this->game.getResourceManager();
    const Shader& shaderProgram = resourceManager.getShader(EShader::TEST_2D);

    shaderProgram.setUniformMat4("projection", projectionMatrix);

    glBindVertexArray(this->VAO);
    glDrawElements(GL_TRIANGLES, 6 * this->width * this->height, GL_UNSIGNED_INT, 0);
}

void TileLayer::teardown() {
    // Free resources
    glDeleteVertexArrays(1, &this->VAO);
    glDeleteBuffers(1, &this->EBO);
    glDeleteBuffers(1, &this->VBOStatic);
    glDeleteBuffers(1, &this->VBODynamic);
}

/* ======================================== TileMap ======================================== */

TileMap::TileMap(Game& game, unsigned int width, unsigned int height, unsigned int numLayers) : game(game), width(width), height(height) {
    this->tileLayers.reserve(numLayers);
    for (unsigned int i = 0; i < numLayers; i++) {
        this->tileLayers.emplace_back(game, width, height);
    }
    this->collisions = new bool[width * height] { false };
}

TileMap::TileMap(Game& game, const std::string& path) : game(game) {
    std::fstream fileStream = std::fstream(path);
    unsigned int numLayers;
    std::string breakToIgnore;

    // Read in width, height, and number of layers
    fileStream >> width >> height >> numLayers;
    std::getline(fileStream, breakToIgnore);
    std::getline(fileStream, breakToIgnore);

    // Read in data for each layer
    this->tileLayers.reserve(numLayers);
    for (unsigned int i = 0; i < numLayers; i++) {
        // Read in actual data
        int tileID;
        ETile* tileLayerData = new ETile[width * height];
        for (unsigned int tileDataIndex = 0; tileDataIndex < width * height; tileDataIndex++) {
            fileStream >> tileID;
            if (tileID < 0) {
                tileLayerData[tileDataIndex] = ETile::NULL_EMPTY;
            } else {
                tileLayerData[tileDataIndex] = static_cast<ETile>(tileID);
            }
        }

        // Push new tile layer
        this->tileLayers.emplace_back(game, width, height, tileLayerData);

        // Ignore break
        std::getline(fileStream, breakToIgnore);
        std::getline(fileStream, breakToIgnore);

    }

    // Read in collision data (TODO: could make this more space-efficient by packing in 8 bools per byte)
    this->collisions = new bool[width * height];
    int collisionValue;
    for (unsigned int tileDataIndex = 0; tileDataIndex < width * height; tileDataIndex++) {
        fileStream >> collisionValue;
        collisions[tileDataIndex] = (collisionValue > 0);
    }
}

TileMap::~TileMap() {
    if (this->collisions) {
        delete[] this->collisions;
    }
}

void TileMap::init() {
    for (auto& layer : this->tileLayers) {
        layer.init();
    }
}

void TileMap::update(double deltaTime) {
    for (auto& layer : this->tileLayers) {
        layer.update(deltaTime);
    }
}

void TileMap::render(const glm::mat4& projectionMatrix) {
    for (auto& layer : this->tileLayers) {
        layer.render(projectionMatrix);
    }
}

void TileMap::teardown() {
    for (auto& layer : this->tileLayers) {
        layer.teardown();
    }
}