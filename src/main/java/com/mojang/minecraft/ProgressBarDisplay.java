package com.mojang.minecraft;

import com.mojang.minecraft.gui.HUDScreen;
import com.mojang.minecraft.net.PacketType;
import com.mojang.minecraft.render.ShapeRenderer;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public final class ProgressBarDisplay {

	private String text = "";
	private Minecraft minecraft;
	private String title = "";
	private long start = System.currentTimeMillis();

	public ProgressBarDisplay(Minecraft var1) {
		this.minecraft = var1;
	}

	public final void setTitle(String var1) {
		if (!this.minecraft.running) {
			throw new StopGameException();
		} else {
			this.title = var1;
			int var3 = this.minecraft.width * 240 / this.minecraft.height;
			int var2 = this.minecraft.height * 240 / this.minecraft.height;
			GL11.glClear(256);
			GL11.glMatrixMode(5889);
			GL11.glLoadIdentity();
			GL11.glOrtho(0.0D, (double) var3, (double) var2, 0.0D, 100.0D,
					300.0D);
			GL11.glMatrixMode(5888);
			GL11.glLoadIdentity();
			GL11.glTranslatef(0.0F, 0.0F, -200.0F);
		}
	}

	public static String terrainId = "";
	public static String sideId = "";
	public static String edgeId = "";

	public static HashMap<String, String> serverConfig = new HashMap<String, String>();

	@SuppressWarnings("deprecation")
	private boolean passServerCommand(String lineText) {
		if (lineText == null)
			return false;
		if (lineText.contains("cfg=")) {
			int i = lineText.indexOf("cfg=");
			if (i > -1) {
				String splitlineText = lineText.substring(i + 4).split(" ")[0];
				String Url = "http://"
						+ splitlineText.replace("$U",
								this.minecraft.session.username);

				System.out.println("Fetching config from: " + Url);
				serverConfig = fetchConfig(Url);
				if (serverConfig.containsKey("server.detail")) {
					try {
						String str = serverConfig.get("server.detail");
						this.text = str;
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
				terrainId = (serverConfig.get("environment.terrain"));
				edgeId = (serverConfig.get("environment.edge"));
				sideId = (serverConfig.get("environment.side"));
			}
			if ((terrainId != "") || (edgeId != "") || (sideId != "")
					|| (serverConfig.containsKey("environment.level"))
					|| (serverConfig.containsKey("environment.fog"))
					|| (serverConfig.containsKey("environment.sky"))
					|| (serverConfig.containsKey("environment.cloud"))) {
				//downloadSkin(minecraft);
				//minecraft.textureManager.textures.clear();
			}
		} else
			return false; // return false if no "cfg=" was found

		if (serverConfig.containsKey("server.sendwomid")) {
			byte[] b = new byte[66];
			int i = 0;
			byte[] tempB = b;
			tempB[i] = ((byte) (tempB[i] | 0xD));
			int tempI = 1;
			byte[] tempArr = b;
			tempArr[tempI] = ((byte) (tempArr[tempI] | 0xFF));
			String Command = "/womid " + this.minecraft.session.username;
			Command.getBytes(0, Command.length(), b, 2);
			this.minecraft.networkManager.netHandler.send(
					PacketType.CHAT_MESSAGE, new Object[] {
							Integer.valueOf(-1), Command });

		}
		if (serverConfig.containsKey("server.name")) {
			HUDScreen.ServerName = serverConfig.get("server.name");
		}
		if (serverConfig.containsKey("user.detail")) {
			HUDScreen.UserDetail = serverConfig.get("user.detail");
		}

		return true;
	}

	public static void InitEnv(Minecraft minecraft) {
		if (serverConfig == null)
			return;
		int i1;
		if (serverConfig.containsKey("environment.level")) {
			i1 = Integer.parseInt(serverConfig.get("environment.level"));
			if (i1 >= 0) {
				minecraft.level.waterLevel = i1;
				System.out.println("Changing water level to " + i1);
			}
		}

		if (serverConfig.containsKey("environment.fog")) {
			i1 = Integer.parseInt(serverConfig.get("environment.fog"));
			if (i1 >= 0) {
				minecraft.level.fogColor = i1;
				System.out.println("Changing fog colour to " + i1);
			}
		}

		if (serverConfig.containsKey("environment.sky")) {
			i1 = Integer.parseInt(serverConfig.get("environment.sky"));
			if (i1 >= 0) {
				minecraft.level.skyColor = i1;
				System.out.println("Changing sky colour to " + i1);
			}
		}

		if (serverConfig.containsKey("environment.cloud")) {
			i1 = Integer.parseInt(serverConfig.get("environment.cloud"));
			if (i1 >= 0) {
				minecraft.level.cloudColor = i1;
				System.out.println("Changing cloud colour to " + i1);
			}
		}
		
	}

	public static HashMap<String, String> fetchConfig(String location) {
		HashMap<String, String> localHashMap = new HashMap<String, String>();
		try {
			URLConnection urlConnection = makeConnection(location, "");
			InputStream localInputStream = getInputStream(urlConnection);

			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(localInputStream));
			String str;
			while ((str = bufferedReader.readLine()) != null) {
				System.out.println(new StringBuilder().append("Read line: ")
						.append(str).toString());
				String[] arrayOfString = str.split("=", 2);
				if (arrayOfString.length > 1) {
					localHashMap.put(arrayOfString[0].trim(),
							arrayOfString[1].trim());
					System.out.println(new StringBuilder()
							.append("Adding config ")
							.append(arrayOfString[0].trim()).append(" = ")
							.append(arrayOfString[1].trim()).toString());
				}
			}
			bufferedReader.close();
		} catch (IOException e) {
			System.out.println(new StringBuilder().append("Caught exception: ")
					.append(e).toString());
		}

		return localHashMap;
	}

	public void downloadSkin(Minecraft minecraft) {
		try {
			File File1 = minecraft.mcDir; // my folder
			File File2 = new File(File1, "/resources/Skins"); // skins
																// folder
			if (!File2.exists() && !File2.mkdirs()) {
				// dunno yet
			}
			File File3;
			File File4;
			if (terrainId != null)
				if (terrainId != "") {
					terrainId = terrainId.replaceAll("[^0-9a-fA-F]+", "");
					File3 = new File(File2, "terrain.png");
					File4 = new File(File2, "terrain-" + terrainId);

					if (!File4.exists()) {
						System.out.println("Fetching file to " + File4);
						fetchUrl(File4,
								"http://files.worldofminecraft.com/skin.php?type=terrain&id="
										+ URLEncoder.encode(terrainId), "");
						System.out.println("Fetched file to " + File4);
					}
					System.out.println("Copying " + File4 + " to " + File3);
					copyFile(File4, File3);
					System.out.println("Copied " + File4 + " to " + File3);
					this.minecraft.levelRenderer.refresh();
				}
			if (edgeId != null) {
				edgeId = edgeId.replaceAll("[^0-9a-fA-F]+", "");
				File3 = new File(File2, "water.png");
				File4 = new File(File2, "edge-" + edgeId);
				if (!File4.exists()) {
					fetchUrl(File4,
							"http://files.worldofminecraft.com/skin.php?type=edge&id="
									+ URLEncoder.encode(edgeId), "");
				}
				copyFile(File4, File3);
			}
			if (sideId != null) {
				sideId = sideId.replaceAll("[^0-9a-fA-F]+", "");
				File3 = new File(File2, "rock.png");
				File4 = new File(File2, "side-" + sideId);
				if (!File4.exists()) {
					fetchUrl(File4,
							"http://files.worldofminecraft.com/skin.php?type=side&id="
									+ URLEncoder.encode(sideId), "");
				}
				copyFile(File4, File3);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public static void copyFile(File paramFile1, File paramFile2) {
		FileChannel fileChannel1 = null;
		FileChannel fileChannel2 = null;

		System.out.println("Copy " + paramFile1 + " to " + paramFile2);
		try {
			if (!paramFile2.exists()) {
				paramFile2.createNewFile();
			}

			fileChannel1 = new FileInputStream(paramFile1).getChannel();
			fileChannel2 = new FileOutputStream(paramFile2).getChannel();
			fileChannel2.transferFrom(fileChannel1, 0L, fileChannel1.size());
		} catch (IOException ex) {
			paramFile2.delete();
			System.out.println("IO Error copying file: " + ex);
		} finally {
			try {
				if (fileChannel1 != null)
					fileChannel1.close();
			} catch (IOException ex) {
			}
			try {
				if (fileChannel2 != null)
					fileChannel2.close();
			} catch (IOException ex) {
			}
		}
	}

	private static URLConnection makeConnection(String url, String s1,
			String s2, boolean AddWomProperty) throws IOException {
		System.out.println(new StringBuilder().append("Making connection to ")
				.append(url).toString());

		URLConnection localURLConnection = new URL(url).openConnection();
		localURLConnection.addRequestProperty("Referer", s2);

		localURLConnection.setReadTimeout(40000);
		localURLConnection.setConnectTimeout(15000);
		localURLConnection.setDoInput(true);

		if (AddWomProperty) {
			localURLConnection.addRequestProperty("X-Wom-Version",
					"WoMClient-2.0.8");
			localURLConnection.addRequestProperty("X-Wom-Username", "Greg0001");
			localURLConnection.addRequestProperty("User-Agent",
					new StringBuilder().append("WoM/")
							.append("WoMClient-2.0.8").toString());
		} else {
			localURLConnection
					.addRequestProperty(
							"User-Agent",
							"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:6.0) Gecko/20100101 Firefox/6.0 FirePHP/0.5");
		}

		localURLConnection
				.addRequestProperty("Accept",
						"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		localURLConnection.addRequestProperty("Accept-Language",
				"en-us,en;q=0.5");
		localURLConnection.addRequestProperty("Accept-Encoding",
				"gzip, deflate, compress");
		localURLConnection.addRequestProperty("Connection", "keep-alive");

		if (s1.length() > 0) {
			localURLConnection.addRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			localURLConnection.addRequestProperty("Content-Length",
					Integer.toString(s1.length()));
			localURLConnection.setDoOutput(true);

			OutputStreamWriter localOutputStreamWriter = new OutputStreamWriter(
					localURLConnection.getOutputStream());
			localOutputStreamWriter.write(s1);
			localOutputStreamWriter.flush();
			localOutputStreamWriter.close();
		}

		localURLConnection.connect();

		return localURLConnection;
	}

	private static URLConnection makeConnection(String paramString1,
			String paramString2) throws IOException {
		return makeConnection(paramString1, paramString2, paramString1, true);
	}

	private static InputStream getInputStream(URLConnection paramURLConnection)
			throws IOException {
		Object localObject = paramURLConnection.getInputStream();
		String str = paramURLConnection.getContentEncoding();
		if (str != null) {
			str = str.toLowerCase();

			if (str.contains("gzip")) {
				localObject = new GZIPInputStream((InputStream) localObject);
			} else if (str.contains("deflate")) {
				localObject = new InflaterInputStream((InputStream) localObject);
			}
		}

		return (InputStream) localObject;
	}

	public static int fetchUrl(File paramFile, String paramString1,
			String paramString2) {
		try {
			URLConnection localURLConnection = makeConnection(paramString1,
					paramString2);
			InputStream localInputStream = getInputStream(localURLConnection);

			FileOutputStream localFileOutputStream = new FileOutputStream(
					paramFile);
			byte[] arrayOfByte = new byte[10240];
			int i = 0;
			int j = 0;
			while ((j = localInputStream.read(arrayOfByte, 0, 10240)) >= 0) {
				if (j > 0) {
					localFileOutputStream.write(arrayOfByte, 0, j);
					i += j;
				}
			}
			localFileOutputStream.close();
			localInputStream.close();

			return i;
		} catch (IOException localIOException) {
			System.out.println(new StringBuilder().append("Error fetching ")
					.append(paramString1).append(" to file: ")
					.append(paramFile).append(": ").append(localIOException)
					.toString());

			paramFile.delete();
		}
		return 0;
	}

	public final void setText(String var1) {
		if (!this.minecraft.running) {
			throw new StopGameException();
		} else {
			if (!passServerCommand(var1)) {
				this.text = var1;
			}
			// check here for hacks
			if (minecraft.HackState == null) { //change only once per session
				if(this.minecraft.session == null){
					//presume singleplayer
					minecraft.HackState = ClientHacksState.HacksTagEnabled;
					return;
				}
				if (this.text.toLowerCase().contains("+hax")) {
					minecraft.HackState = ClientHacksState.HacksTagEnabled;
				} else if (this.text.toLowerCase().contains("-hax")) {
					minecraft.HackState = ClientHacksState.HacksTagDisabled;
					minecraft.settings.CanSpeed = false;
				} else if (this.text.toLowerCase().contains("+ophacks") || 
						this.text.toLowerCase().contains("+ophax")) {
					minecraft.HackState = ClientHacksState.OpHacks;
					if(this.minecraft.player.userType < 100){
						minecraft.settings.CanSpeed = false;
					}
				} else {
					minecraft.HackState = ClientHacksState.NoHacksTagShown;
				}
			}
			this.setProgress(-1);
		}
	}

	public final void setProgress(int var1) {
		if (!this.minecraft.running) {
			throw new StopGameException();
		} else {
			long var2;
			if ((var2 = System.currentTimeMillis()) - this.start < 0L
					|| var2 - this.start >= 20L) {
				this.start = var2;
				int var4 = this.minecraft.width * 240 / this.minecraft.height;
				int var5 = this.minecraft.height * 240 / this.minecraft.height;
				GL11.glClear(16640);
				ShapeRenderer var6 = ShapeRenderer.instance;
				int var7 = this.minecraft.textureManager.load("/dirt.png");
				GL11.glBindTexture(3553, var7);
				float var10 = 32.0F;
				var6.begin();
				var6.color(4210752);
				var6.vertexUV(0.0F, (float) var5, 0.0F, 0.0F, (float) var5
						/ var10);
				var6.vertexUV((float) var4, (float) var5, 0.0F, (float) var4
						/ var10, (float) var5 / var10);
				var6.vertexUV((float) var4, 0.0F, 0.0F, (float) var4 / var10,
						0.0F);
				var6.vertexUV(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
				var6.end();
				if (var1 >= 0) {
					var7 = var4 / 2 - 50;
					int var8 = var5 / 2 + 16;
					GL11.glDisable(3553);
					var6.begin();
					var6.color(8421504);
					var6.vertex((float) var7, (float) var8, 0.0F);
					var6.vertex((float) var7, (float) (var8 + 2), 0.0F);
					var6.vertex((float) (var7 + 100), (float) (var8 + 2), 0.0F);
					var6.vertex((float) (var7 + 100), (float) var8, 0.0F);
					var6.color(8454016);
					var6.vertex((float) var7, (float) var8, 0.0F);
					var6.vertex((float) var7, (float) (var8 + 2), 0.0F);
					var6.vertex((float) (var7 + var1), (float) (var8 + 2), 0.0F);
					var6.vertex((float) (var7 + var1), (float) var8, 0.0F);
					var6.end();
					GL11.glEnable(3553);
				}

				this.minecraft.fontRenderer.render(this.title,
						(var4 - this.minecraft.fontRenderer
								.getWidth(this.title)) / 2, var5 / 2 - 4 - 16,
						16777215);
				this.minecraft.fontRenderer
						.render(this.text, (var4 - this.minecraft.fontRenderer
								.getWidth(this.text)) / 2, var5 / 2 - 4 + 8,
								16777215);
				Display.update();

				try {
					Thread.yield();
				} catch (Exception var9) {
					;
				}
			}
		}
	}
}
