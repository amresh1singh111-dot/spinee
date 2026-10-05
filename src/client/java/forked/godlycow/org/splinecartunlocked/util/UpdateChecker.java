package forked.godlycow.org.splinecartunlocked.util;

import com.google.gson.JsonParser;
import forked.godlycow.org.splinecartunlocked.SplinecartUnlocked;
import forked.godlycow.org.splinecartunlocked.SplinecartUnlockedClient;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;

public final class UpdateChecker {
	private static final String MODRINTH_PROJECT = "BFEpFp45";
	private static final String MODRINTH_URL = "https://modrinth.com/mod/splinecartunlocked";

	private UpdateChecker() {
	}

	public static void checkOnJoin() {
		startCheck(message -> {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null && message != null) {
				player.sendSystemMessage(message);
			}
		}, !SplinecartUnlockedClient.CFG_NOTIFY_UPDATES.get(), true);
	}

	public static void checkNow(FabricClientCommandSource source) {
		startCheck(message -> {
			if (message != null) {
				source.sendFeedback(message);
			}
		}, false, false);
	}

	private static void startCheck(MessageSink sink, boolean silentIfUpToDate, boolean silentOnFailure) {
		Thread.startVirtualThread(() -> {
			try {
				String remote = fetchLatestVersion();
				Component message = buildMessage(remote, silentIfUpToDate, silentOnFailure);
				if (message != null) {
					Minecraft.getInstance().execute(() -> sink.accept(message));
				}
			} catch (Exception e) {
				SplinecartUnlocked.LOGGER.error("Update check failed", e);
				if (!silentOnFailure) {
					Minecraft.getInstance().execute(() -> sink.accept(ChatUtil.prefixed(ChatUtil.styled(
							Component.translatable("splinecartunlocked.update.error"), ChatUtil.DIM))));
				}
			}
		});
	}

	private interface MessageSink {
		void accept(Component message);
	}

	private static String fetchLatestVersion() throws Exception {
		String mcVersion = FabricLoader.getInstance().getModContainer("minecraft")
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse(Minecraft.getInstance().getLaunchedVersion());
		String encodedLoaders = URLEncoder.encode("[\"fabric\"]", "UTF-8");
		String encodedVersions = URLEncoder.encode("[\"" + mcVersion + "\"]", "UTF-8");
		String urlStr = "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT
				+ "/version?loaders=" + encodedLoaders + "&game_versions=" + encodedVersions;

		SplinecartUnlocked.LOGGER.info("Checking updates: {}", urlStr);

		HttpURLConnection con = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
		con.setRequestProperty("User-Agent", "SplinecartUnlocked/" + currentVersion());
		con.setConnectTimeout(5000);
		con.setReadTimeout(10000);

		try (InputStream in = con.getInputStream()) {
			String body = new String(in.readAllBytes());
			var arr = JsonParser.parseString(body).getAsJsonArray();
			if (arr.isEmpty()) {
				SplinecartUnlocked.LOGGER.info("No versions found for MC {}", mcVersion);
				return null;
			}
			String latest = arr.get(0).getAsJsonObject().get("version_number").getAsString();
			SplinecartUnlocked.LOGGER.info("Latest version: {}", latest);
			return latest;
		} finally {
			con.disconnect();
		}
	}

	private static Component buildMessage(String remote, boolean silentIfUpToDate, boolean silentOnFailure) {
		String local = currentVersion();

		if (remote == null) {
			if (silentIfUpToDate || silentOnFailure) {
				return null;
			}
			return ChatUtil.prefixed(ChatUtil.styled(
					Component.translatable("splinecartunlocked.update.error"), ChatUtil.DIM));
		}

		int compared = VersionCompare.compare(remote, local);
		if (compared <= 0) {
			if (silentIfUpToDate) {
				return null;
			}
			return ChatUtil.prefixed(ChatUtil.styled(
					Component.translatable("splinecartunlocked.update.up_to_date", local), ChatUtil.GOOD));
		}

		return ChatUtil.prefixed(ChatUtil.styled(
				Component.translatable("splinecartunlocked.update.outdated", remote, local), ChatUtil.WARN)
				.append(Component.literal(" "))
				.append(Component.literal("[Modrinth]")
						.withStyle(Style.EMPTY
								.withColor(ChatUtil.BRAND)
								.withUnderlined(true)
								.withClickEvent(new ClickEvent.OpenUrl(URI.create(MODRINTH_URL))))));
	}

	public static String currentVersion() {
		return FabricLoader.getInstance().getModContainer(SplinecartUnlocked.MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}
}
