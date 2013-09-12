package com.mojang.minecraft.render;

import com.mojang.minecraft.GameSettings;
import com.mojang.minecraft.Minecraft;
import com.mojang.minecraft.level.tile.Block;
import com.mojang.minecraft.render.texture.TextureFX;
import com.mojang.minecraft.render.texture.TextureFireFX;
import com.mojang.minecraft.render.texture.TextureLavaFX;
import com.mojang.minecraft.render.texture.TextureWaterFX;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;

public class TextureManager {
    public static BufferedImage load1(BufferedImage image) {
	int charWidth = image.getWidth() / 16;
	BufferedImage image1 = new BufferedImage(16, image.getHeight()
		* charWidth, BufferedImage.TYPE_INT_ARGB);
	Graphics graphics = image1.getGraphics();

	for (int i = 0; i < charWidth; i++) {
	    graphics.drawImage(image, -i << 4, i * image.getHeight(), null);
	}

	graphics.dispose();

	return image1;
    }

    public boolean Applet;

    public HashMap<String, Integer> textures = new HashMap<String, Integer>();
    public HashMap<Integer, BufferedImage> textureImages = new HashMap<Integer, BufferedImage>();
    public IntBuffer idBuffer = BufferUtils.createIntBuffer(1);
    public ByteBuffer textureBuffer = BufferUtils.createByteBuffer(262144);
    public List<TextureFX> animations = new ArrayList<TextureFX>();
    public GameSettings settings;
    public List<BufferedImage> textureAtlas = new ArrayList();

    public BufferedImage currentTerrainPng = null;
    public BufferedImage customSideBlock = null;
    public BufferedImage customEdgeBlock = null;

    public File minecraftFolder;
    public File texturesFolder;

    public int previousMipmapMode;

    public TextureManager(GameSettings settings, boolean Applet) {
	this.Applet = Applet;
	this.settings = settings;

	minecraftFolder = Minecraft.mcDir;
	texturesFolder = new File(minecraftFolder, "texturepacks");

	if (!texturesFolder.exists()) {
	    texturesFolder.mkdir();
	}
    }

    public List<BufferedImage> Atlas2dInto1d(BufferedImage atlas2d, int tiles,
	    int atlassizezlimit) {

	int tilesize = atlas2d.getWidth() / tiles;

	int atlasescount = Math.max(1, (tiles * tiles * tilesize)
		/ atlassizezlimit);
	List<BufferedImage> atlases = new ArrayList<BufferedImage>();

	// 256 x 1
	BufferedImage atlas1d = null;

	for (int i = 0; i < tiles * tiles; i++) {
	    int x = i % tiles;
	    int y = i / tiles;
	    int tilesinatlas = (tiles * tiles / atlasescount);
	    if (i % tilesinatlas == 0) {
		if (atlas1d != null) {
		    atlases.add(atlas1d);
		}
		atlas1d = new BufferedImage(tilesize, atlassizezlimit,
			BufferedImage.TYPE_INT_ARGB);
	    }
	    for (int xx = 0; xx < tilesize; xx++) {
		for (int yy = 0; yy < tilesize; yy++) {
		    int c = atlas2d
			    .getRGB(x * tilesize + xx, y * tilesize + yy);
		    atlas1d.setRGB(xx, (i % tilesinatlas) * tilesize + yy, c);
		}
	    }
	}
	atlases.add(atlas1d);
	return atlases;
    }

    private int b(int c1, int c2) {
	int a1 = (c1 & 0xFF000000) >> 24 & 0xFF;
	int a2 = (c2 & 0xFF000000) >> 24 & 0xFF;

	int ax = (a1 + a2) / 2;
	if (ax > 255) {
	    ax = 255;
	}
	if (a1 + a2 <= 0) {
	    a1 = 1;
	    a2 = 1;
	    ax = 0;
	}

	int r1 = (c1 >> 16 & 0xFF) * a1;
	int g1 = (c1 >> 8 & 0xFF) * a1;
	int b1 = (c1 & 0xFF) * a1;

	int r2 = (c2 >> 16 & 0xFF) * a2;
	int g2 = (c2 >> 8 & 0xFF) * a2;
	int b2 = (c2 & 0xFF) * a2;

	int rx = (r1 + r2) / (a1 + a2);
	int gx = (g1 + g2) / (a1 + a2);
	int bx = (b1 + b2) / (a1 + a2);
	return ax << 24 | rx << 16 | gx << 8 | bx;
    }

    public void generateMipMaps(ByteBuffer data, int width, int height,
	    boolean test) {
	ByteBuffer mipData = data;

	for (int level = test ? 0 : 1; level <= 4; level++) {
	    int parWidth = width >> level - 1;
	    int mipWidth = width >> level;
	    int mipHeight = height >> level;

	    if (mipWidth <= 0 || mipHeight <= 0) {
		break;
	    }

	    ByteBuffer mipData1 = BufferUtils.createByteBuffer(data.capacity());

	    mipData1.clear();

	    for (int mipX = 0; mipX < mipWidth; mipX++) {
		for (int mipY = 0; mipY < mipHeight; mipY++) {
		    int p1 = mipData.getInt((mipX * 2 + 0 + (mipY * 2 + 0)
			    * parWidth) * 4);
		    int p2 = mipData.getInt((mipX * 2 + 1 + (mipY * 2 + 0)
			    * parWidth) * 4);
		    int p3 = mipData.getInt((mipX * 2 + 1 + (mipY * 2 + 1)
			    * parWidth) * 4);
		    int p4 = mipData.getInt((mipX * 2 + 0 + (mipY * 2 + 1)
			    * parWidth) * 4);

		    int pixel = b(b(p1, p2), b(p3, p4));

		    mipData1.putInt((mipX + mipY * mipWidth) * 4, pixel);
		}
	    }

	    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, GL11.GL_RGBA,
		    mipWidth, mipHeight, 0, GL11.GL_RGBA,
		    GL11.GL_UNSIGNED_BYTE, mipData1);
	    GL11.glAlphaFunc(GL11.GL_GEQUAL, 0.1F * level); // Create
							    // transparency for
							    // each level.

	    mipData = mipData1;
	}
    }

    public int load(BufferedImage image) {
	idBuffer.clear();

	GL11.glGenTextures(idBuffer);

	int textureID = idBuffer.get(0);

	load(image, textureID);

	textureImages.put(textureID, image);

	return textureID;
    }

    public void load(BufferedImage image, int textureID) {
	if (image == null)
	    return;
	int width = image.getWidth();
	int height = image.getHeight();

	GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
	if (settings.smoothing > 0) {
	    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
		    GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
	    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
		    GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

	    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
		    GL12.GL_TEXTURE_BASE_LEVEL, 0);
	    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL,
		    4);
	} else {
	    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
		    GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
	    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
		    GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
	}

	// GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE,
	// GL11.GL_MODULATE);

	int[] pixels = new int[width * height];
	byte[] color = new byte[(width * height) << 2];

	image.getRGB(0, 0, width, height, pixels, 0, width);

	for (int pixel = 0; pixel < pixels.length; pixel++) {
	    int alpha = pixels[pixel] >>> 24;
	    int red = pixels[pixel] >> 16 & 0xFF;
	    int green = pixels[pixel] >> 8 & 0xFF;
	    int blue = pixels[pixel] & 0xFF;

	    if (settings.anaglyph) {
		int rgba3D = (red * 30 + green * 59 + blue * 11) / 100;

		green = (red * 30 + green * 70) / 100;
		blue = (red * 30 + blue * 70) / 100;
		red = rgba3D;
	    }

	    int i = pixel << 2;
	    color[i] = (byte) red;
	    color[i + 1] = (byte) green;
	    color[i + 2] = (byte) blue;
	    color[i + 3] = (byte) alpha;
	}

	if (this.textureBuffer.capacity() != color.length) {
	    this.textureBuffer = BufferUtils.createByteBuffer(color.length);
	} else {
	    this.textureBuffer.clear();
	}
	textureBuffer.put(color);
	textureBuffer.position(0).limit(color.length);

	GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height,
		0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, textureBuffer);

	if (settings.smoothing > 0) {
	    if (settings.smoothing == 1) {
		ContextCapabilities capabilities = GLContext.getCapabilities();

		if (capabilities.OpenGL30) {
		    if (previousMipmapMode != settings.smoothing) {
			System.out
				.println("Using OpenGL 3.0 for mipmap generation.");
		    }

		    GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
		} else if (capabilities.GL_EXT_framebuffer_object) {
		    if (previousMipmapMode != settings.smoothing) {
			System.out
				.println("Using GL_EXT_framebuffer_object extension for mipmap generation.");
		    }

		    EXTFramebufferObject
			    .glGenerateMipmapEXT(GL11.GL_TEXTURE_2D);
		} else if (capabilities.OpenGL14) {
		    if (previousMipmapMode != settings.smoothing) {
			System.out
				.println("Using OpenGL 1.4 for mipmap generation.");
		    }

		    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
			    GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
		}
	    } else if (settings.smoothing == 2) {
		if (previousMipmapMode != settings.smoothing) {
		    System.out
			    .println("Using custom system for mipmap generation.");
		}

		generateMipMaps(textureBuffer, width, height, false);
	    }
	    if (settings.anisotropic > 0) {
		float max = GL11.glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D,
			GL_TEXTURE_MAX_ANISOTROPY_EXT, max);

	    }
	}

	previousMipmapMode = settings.smoothing;
    }

    public void initAtlas() {
	String textureFile = "/terrain.png";
	BufferedImage image = null;
	if (this.currentTerrainPng != null) {
	    image = currentTerrainPng;
	} else {
	    try {
		image = ImageIO.read(TextureManager.class
			.getResourceAsStream(textureFile));
	    } catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	    }
	}
	textureAtlas.clear();
	textureAtlas = Atlas2dInto1d(image, 16, image.getWidth() / 16);
    }

    public int load(String file) {
	if(this.animations.size() == 0 && this.currentTerrainPng == null){
	    this.registerAnimations();
	}
	
	if (file.contains("terrain") && textures.containsKey("customTerrain")) {
	    return textures.get("customTerrain");
	}
	if (file.contains("terrain") && !textures.containsKey("customTerrain")
		&& currentTerrainPng != null) {
	    int id = load(currentTerrainPng);
	    textures.put("customTerrain", id);
	    if(this.customEdgeBlock == null){
		customEdgeBlock = this.textureAtlas.get(Block.BEDROCK.textureId);
		textures.put("customEdge", load(customEdgeBlock));
	    }
	    if(this.customSideBlock == null){
		customSideBlock = this.textureAtlas.get(Block.WATER.textureId);
		textures.put("customSide", load(customSideBlock));
	    }
	    return id;
	}
	if (file.contains("rock") && textures.containsKey("customEdge")) {
	    return textures.get("customEdge");
	}
	if (file.contains("rock") && !textures.containsKey("customEdge")
		&& customEdgeBlock != null) {
	    int id = load(customEdgeBlock);
	    textures.put("customEdge", id);
	    return id;
	}
	if (file.contains("water") && textures.containsKey("customSide")) {
	    return textures.get("customSide");
	}
	if (file.contains("water") && !textures.containsKey("customSide")
		&& customSideBlock != null) {
	    int id = load(customSideBlock);
	    textures.put("customSide", id);
	    return id;
	}

	if (textures.get(file) != null) {
	    return textures.get(file);
	} else {
	    try {

		idBuffer.clear();

		GL11.glGenTextures(idBuffer);

		int textureID = idBuffer.get(0);

		if (file.endsWith(".png")) {
		    if (file.startsWith("##")) {
			load(load1(ImageIO.read(TextureManager.class
				.getResourceAsStream(file.substring(2)))),
				textureID);
		    } else {
			load(ImageIO.read(TextureManager.class
				.getResourceAsStream(file)), textureID);
		    }

		    textures.put(file, textureID);
		} else if (file.endsWith(".zip")) {
		    ZipFile zip = new ZipFile(new File(minecraftFolder,
			    "texturepacks/" + file));

		    String terrainPNG = "terrain.png";

		    if (zip.getEntry(terrainPNG.startsWith("/") ? terrainPNG
			    .substring(1, terrainPNG.length()) : terrainPNG) != null) {
			load(ImageIO.read(zip.getInputStream(zip.getEntry(terrainPNG
				.startsWith("/") ? terrainPNG.substring(1,
				terrainPNG.length()) : terrainPNG))), textureID);
		    } else {
			load(ImageIO.read(TextureManager.class
				.getResourceAsStream(terrainPNG)), textureID);
		    }

		    zip.close();
		}

		return textureID;
	    } catch (IOException e) {
		throw new RuntimeException("!!", e);
	    }
	}
    }

    public int loadTexturePack(String file) throws IOException {
	int textureID = 0;
	if (file.endsWith(".zip")) {
	    animations.clear();
	    ZipFile zip = new ZipFile(new File(minecraftFolder, "texturepacks/"
		    + file));

	    String terrainPNG = "terrain.png";

	    if (zip.getEntry(terrainPNG.startsWith("/") ? terrainPNG.substring(
		    1, terrainPNG.length()) : terrainPNG) != null) {
		currentTerrainPng = ImageIO
			.read(zip.getInputStream(zip.getEntry(terrainPNG
				.startsWith("/") ? terrainPNG.substring(1,
				terrainPNG.length()) : terrainPNG)));
	    } else {
		try {
		    currentTerrainPng = ImageIO.read(TextureManager.class
			    .getResourceAsStream(terrainPNG));
		} catch (Exception e) {
		    zip.close();
		    return textureID;
		}
	    }

	    zip.close();

	}
	initAtlas();
	return textureID;
    }

    public void registerAnimations() {
	this.animations.add(new TextureWaterFX());
	this.animations.add(new TextureLavaFX());
	this.animations.add(new TextureFireFX());
    }
}