package xyz.mahasamut.DiscordServerCloner;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import xyz.mahasamut.DiscordServerCloner.utils.HttpUtils;

/**
 * 
 * @author M4h45amu7x
 *
 */
public class Main {

	private static String TOKEN = "";
	private static String SERVER_ID = "";
	private static String CUR_SERVER_ID = "";

	private static long DELAY = 100L;

	private static List<Role> ROLES = new ArrayList<Role>();
	private static List<Role> ROLES_NEW = new ArrayList<Role>();
	private static List<Channel> CHANNELS = new ArrayList<Channel>();
	private static List<Emoji> EMOJIS = new ArrayList<Emoji>();

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		System.out.println("");
		System.out.println("==============================================");
		System.out.println("");
		System.out.println("   Discord Server Cloner made by M4h45amu7x");
		System.out.println("");
		System.out.println("==============================================");
		System.out.println("");

		System.out.println("Your token: ");
		TOKEN = scanner.nextLine();

		System.out.println("Server ID: ");
		SERVER_ID = scanner.nextLine();

		System.out.println("Delay in ms (Default 100): ");
		try {
			DELAY = Long.parseLong(scanner.nextLine());
		} catch (NumberFormatException e) {
			System.out.println("You entered an invalid delay. Set the delay to 100ms");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
			}
		}

		scanner.close();

		try {
			System.out.println("Logged in as " + getUsername());
			Thread.sleep(1000);
		} catch (Exception e) {
			System.out.println("Can't login");
			System.exit(0);
		}

		try {
			ROLES = getRoles();
		} catch (Exception e) {
			System.out.println("Can't get any role data");
			System.exit(0);
		}

		try {
			CUR_SERVER_ID = createServer();
			System.out.println("Created server");
		} catch (Exception e) {
			System.out.println("Can't get any server data");
			System.exit(0);
		}

		try {
			CHANNELS = getChannels(SERVER_ID);
		} catch (Exception e) {
			System.out.println("Can't get any channel data");
		}

		try {
			EMOJIS = getEmojis();
		} catch (Exception e) {
			System.out.println("Can't get any emoji data");
		}

		try {
			deleteDefaultChannels();
			System.out.println("Deleted default channels!");
		} catch (Exception e) {
			System.out.println("Can't delete default channels");
		}

		try {
			createChannels();
			System.out.println("All categories and channels have been created!");
		} catch (Exception e) {
			System.out.println("Can't create any categories and channels");
		}

		try {
			createEmojis();
			System.out.println("All emojis have been created!");
		} catch (Exception e) {
			System.out.println("Can't create any emojis");
		}

		System.out.println("");
		System.out.println("=====================================");
		System.out.println("");
		System.out.println("   Clone the server successfully!");
		System.out.println("");
		System.out.println("=====================================");
		System.out.println("");
	}

	public static String getUsername() throws Exception {
		String url = "https://discord.com/api/v9/users/@me";
		Map<String, String> params = new HashMap<String, String>();
		params.put("Authorization", TOKEN);
		params.put("Content-Type", "application/json");

		JsonElement element = new JsonParser().parse(HttpUtils.get(url, params));
		JsonObject object = element.getAsJsonObject();

		return object.get("username").getAsString() + "#" + object.get("discriminator").getAsString();
	}

	public static String createServer() throws Exception {
		JsonElement infoElement = new JsonParser().parse(getInfo());
		JsonObject infoObject = infoElement.getAsJsonObject();
		String url = "https://discord.com/api/v9/guilds";

		Map<String, String> params = new HashMap<String, String>();
		params.put("Authorization", TOKEN);
		params.put("Content-Type", "application/json");

		JsonObject object = new JsonObject();
		object.addProperty("name", infoObject.get("name").getAsString());
		object.addProperty("icon",
				"data:image/png;base64," + getByteArrayFromImageURL(
						"https://cdn.discordapp.com/icons/" + infoObject.get("id").getAsString() + "/"
								+ infoObject.get("icon").getAsString() + ".png?size=240"));

		JsonArray roleArray = new JsonArray();
		for (Role role : ROLES) {
			JsonObject roleObject = new JsonObject();

			roleObject.addProperty("id", role.getId());
			roleObject.addProperty("name", role.getName());
			roleObject.addProperty("permissions", role.getPermissions());
			roleObject.addProperty("color", role.getColor());
			roleObject.addProperty("hoist", role.isHoist());
			roleObject.addProperty("mentionable", role.isMentionable());
			roleObject.addProperty("position", role.getPosition());

			roleArray.add(roleObject);
		}
		object.add("roles", roleArray);

		JsonObject createdObject = new JsonParser().parse(HttpUtils.post(url, params, object.toString()))
				.getAsJsonObject();

		for (JsonElement element : createdObject.get("roles").getAsJsonArray()) {
			JsonObject rolesObject = element.getAsJsonObject();

			ROLES_NEW.add(new Role(rolesObject.get("id").getAsString(), rolesObject.get("name").getAsString(),
					rolesObject.get("permissions").getAsString(), rolesObject.get("color").getAsInt(),
					rolesObject.get("hoist").getAsBoolean(), rolesObject.get("mentionable").getAsBoolean(),
					rolesObject.get("position").getAsInt()));
		}

		ROLES_NEW.sort(Comparator.comparing(Role::getPosition));

		return createdObject.get("id").getAsString();
	}

	public static String getInfo() throws Exception {
		String url = "https://discord.com/api/v9/guilds/" + SERVER_ID;
		Map<String, String> params = new HashMap<String, String>();
		params.put("Authorization", TOKEN);
		params.put("Content-Type", "application/json");

		return HttpUtils.get(url, params);
	}

	public static void createChannels() throws Exception {
		List<Channel> categories = CHANNELS.stream().filter(c -> c.getType() == 4).collect(Collectors.toList());
		categories.sort(Comparator.comparing(Channel::getPosition));

		for (Channel category : categories) {
			try {
				String url = "https://discord.com/api/v9/guilds/" + CUR_SERVER_ID + "/channels";

				Map<String, String> categoryParams = new HashMap<String, String>();
				categoryParams.put("Authorization", TOKEN);
				categoryParams.put("Content-Type", "application/json");

				JsonObject categoryObject = new JsonObject();

				categoryObject.addProperty("type", category.getType());
				categoryObject.addProperty("name", category.getName());
				categoryObject.add("permission_overwrites", category.getPermission_overwrites());
				categoryObject.add("parent_id", null);
				categoryObject.addProperty("nsfw", category.isNsfw());

				String parent_id = new JsonParser()
						.parse(HttpUtils.post(url, categoryParams, categoryObject.toString())).getAsJsonObject()
						.get("id").getAsString();
				System.out.println("Created category: " + category.getName());

				List<Channel> channels = Main.CHANNELS.stream().filter(
						c -> c.getType() != 4 && c.getParent_id() != null && c.getParent_id().equals(category.getId()))
						.collect(Collectors.toList());
				channels.sort(Comparator.comparing(Channel::getPosition));

				for (Channel channel : channels) {
					try {
						Map<String, String> channelParams = new HashMap<String, String>();
						channelParams.put("Authorization", TOKEN);
						channelParams.put("Content-Type", "application/json");

						JsonObject channelObject = new JsonObject();

						channelObject.addProperty("type", channel.getType());
						channelObject.addProperty("name", channel.getName());
						channelObject.add("permission_overwrites", channel.getPermission_overwrites());
						channelObject.addProperty("parent_id", parent_id);
						channelObject.addProperty("nsfw", channel.isNsfw());

						HttpUtils.post(url, channelParams, channelObject.toString());
						System.out.println("Created channel: " + channel.getName());
					} catch (Exception e) {
						System.out.println("Can't created: " + channel.getName());
					}
					Thread.sleep(DELAY);
				}

			} catch (Exception e) {
				System.out.println("Can't create category: " + category.getName());
			}
			Thread.sleep(DELAY);
		}
	}

	public static void deleteDefaultChannels() throws Exception {
		List<Channel> channels = getChannels(CUR_SERVER_ID);

		for (Channel channel : channels) {
			String url = "https://discord.com/api/v9/channels/" + channel.getId();
			Map<String, String> params = new HashMap<String, String>();
			params.put("Authorization", TOKEN);
			params.put("Content-Type", "application/json");

			HttpUtils.delete(url, params);
		}
	}

	public static List<Channel> getChannels(String serverID) throws Exception {
		String url = "https://discord.com/api/v9/guilds/" + serverID + "/channels";
		Map<String, String> params = new HashMap<String, String>();
		params.put("Authorization", TOKEN);
		params.put("Content-Type", "application/json");
		List<Channel> channels = new ArrayList<Channel>();
		List<Role> cachedRoles = ROLES;

		JsonElement jsonElement = new JsonParser().parse(HttpUtils.get(url, params));

		for (JsonElement element : jsonElement.getAsJsonArray()) {
			JsonObject object = element.getAsJsonObject();
			JsonArray newPermissionArray = new JsonArray();

			for (JsonElement permissionElement : object.get("permission_overwrites").getAsJsonArray()) {
				JsonObject permissionObject = permissionElement.getAsJsonObject();
				JsonObject permissionNewObject = new JsonObject();

				for (int i = 0; i < cachedRoles.size(); i++) {
					Role role = cachedRoles.get(i);

					if (role.getId().equalsIgnoreCase(permissionObject.get("id").getAsString())) {
						permissionNewObject.addProperty("id", ROLES_NEW.get(i).getId());
					}
				}
				permissionNewObject.addProperty("type", permissionObject.get("type").getAsInt());
				permissionNewObject.addProperty("allow", permissionObject.get("allow").getAsString());
				permissionNewObject.addProperty("deny", permissionObject.get("deny").getAsString());

				newPermissionArray.add(permissionNewObject);
			}

			channels.add(new Channel(object.get("id").getAsString(), object.get("type").getAsInt(),
					object.get("name").getAsString(),
					object.get("parent_id").isJsonNull() ? null : object.get("parent_id").getAsString(),
					newPermissionArray, object.get("position").getAsInt(), object.get("nsfw").getAsBoolean()));
		}

		return channels;
	}

	public static List<Role> getRoles() throws Exception {
		String url = "https://discord.com/api/v9/guilds/" + SERVER_ID + "/roles";
		Map<String, String> params = new HashMap<String, String>();
		params.put("Authorization", TOKEN);
		params.put("Content-Type", "application/json");
		List<Role> roles = new ArrayList<Role>();

		JsonElement jsonElement = new JsonParser().parse(HttpUtils.get(url, params));

		for (JsonElement element : jsonElement.getAsJsonArray()) {
			JsonObject object = element.getAsJsonObject();

			roles.add(new Role(object.get("id").getAsString(), object.get("name").getAsString(),
					object.get("permissions").getAsString(), object.get("color").getAsInt(),
					object.get("hoist").getAsBoolean(), object.get("mentionable").getAsBoolean(),
					object.get("position").getAsInt()));
		}

		roles.sort(Comparator.comparing(Role::getPosition));

		return roles;
	}

	public static void createEmojis() throws Exception {
		String url = "https://discord.com/api/v9/guilds/" + CUR_SERVER_ID + "/emojis";

		Map<String, String> params = new HashMap<String, String>();
		params.put("Authorization", TOKEN);
		params.put("Content-Type", "application/json");

		for (Emoji emoji : EMOJIS) {
			try {
				JsonObject object = new JsonObject();

				object.addProperty("name", emoji.getName());
				object.addProperty("image", "data:image/png;base64," + getByteArrayFromImageURL(
						"https://cdn.discordapp.com/emojis/" + emoji.getId() + ".webp?size=128&quality=lossless"));
				object.add("roles", emoji.getRoles());

				HttpUtils.post(url, params, object.toString());
				System.out.println("Created emoji: " + emoji.getName());
			} catch (Exception e) {
				System.out.println("Can't create emoji: " + emoji.getName());
			}
			Thread.sleep(DELAY);
		}
	}

	public static List<Emoji> getEmojis() throws Exception {
		String url = "https://discord.com/api/v9/guilds/" + SERVER_ID + "/emojis";
		Map<String, String> params = new HashMap<String, String>();
		params.put("Authorization", TOKEN);
		params.put("Content-Type", "application/json");
		List<Emoji> emojis = new ArrayList<Emoji>();

		JsonElement jsonElement = new JsonParser().parse(HttpUtils.get(url, params));

		for (JsonElement element : jsonElement.getAsJsonArray()) {
			JsonObject object = element.getAsJsonObject();

			emojis.add(new Emoji(object.get("id").getAsString(), object.get("name").getAsString(),
					object.get("roles").getAsJsonArray()));
		}

		return emojis;
	}

	public static String getByteArrayFromImageURL(String url) throws Exception {
		HttpURLConnection connection = HttpUtils.request(url, "GET");
		InputStream input = connection.getInputStream();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read = 0;
		while ((read = input.read(buffer, 0, buffer.length)) != -1) {
			output.write(buffer, 0, read);
		}
		output.flush();
		return Base64.getEncoder().encodeToString(output.toByteArray());
	}

	public static class Role {

		private String id;
		private String name;
		private String permissions;
		private int color;
		private boolean hoist;
		private boolean mentionable;
		private int position;

		public Role(String id, String name, String permissions, int color, boolean hoist, boolean mentionable,
				int position) {
			this.id = id;
			this.name = name;
			this.permissions = permissions;
			this.position = position;
			this.color = color;
			this.hoist = hoist;
			this.mentionable = mentionable;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getPermissions() {
			return permissions;
		}

		public int getColor() {
			return color;
		}

		public boolean isHoist() {
			return hoist;
		}

		public boolean isMentionable() {
			return mentionable;
		}

		public int getPosition() {
			return position;
		}

	}

	public static class Channel {

		private String id;
		private int type;
		private String name;
		private String parent_id;
		private JsonArray permission_overwrites;
		private int position;
		private boolean nsfw;

		public Channel(String id, int type, String name, String parent_id, JsonArray permission_overwrites,
				int position, boolean nsfw) {
			this.id = id;
			this.type = type;
			this.name = name;
			this.parent_id = parent_id;
			this.permission_overwrites = permission_overwrites;
			this.position = position;
			this.nsfw = nsfw;
		}

		public String getId() {
			return id;
		}

		public int getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public String getParent_id() {
			return parent_id;
		}

		public JsonArray getPermission_overwrites() {
			return permission_overwrites;
		}

		public int getPosition() {
			return position;
		}

		public boolean isNsfw() {
			return nsfw;
		}

	}

	public static class Emoji {

		private String id;
		private String name;
		private JsonArray roles;

		public Emoji(String id, String name, JsonArray roles) {
			this.id = id;
			this.name = name;
			this.roles = roles;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public JsonArray getRoles() {
			return roles;
		}

	}

}
