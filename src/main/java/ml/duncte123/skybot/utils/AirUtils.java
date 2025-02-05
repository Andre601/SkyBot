/*
 * Skybot, a multipurpose discord bot
 *      Copyright (C) 2017 - 2020  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan & Maurice R S "Sanduhr32"
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ml.duncte123.skybot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.natanbc.reliqua.request.PendingRequest;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import fredboat.audio.player.LavalinkManager;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import io.sentry.Sentry;
import me.duncte123.botcommons.StringUtils;
import me.duncte123.botcommons.messaging.MessageConfig;
import me.duncte123.botcommons.web.WebParserUtils;
import me.duncte123.botcommons.web.WebUtils;
import me.duncte123.botcommons.web.requests.JSONRequestBody;
import me.duncte123.durationparser.Duration;
import ml.duncte123.skybot.Author;
import ml.duncte123.skybot.Authors;
import ml.duncte123.skybot.SkyBot;
import ml.duncte123.skybot.adapters.DatabaseAdapter;
import ml.duncte123.skybot.audio.GuildMusicManager;
import ml.duncte123.skybot.entities.jda.FakeMember;
import ml.duncte123.skybot.extensions.Time4JKt;
import ml.duncte123.skybot.objects.FakePendingRequest;
import ml.duncte123.skybot.objects.api.Reminder;
import ml.duncte123.skybot.objects.command.CommandContext;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.JDAImpl;
import net.time4j.format.TextWidth;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static me.duncte123.botcommons.messaging.MessageUtils.sendMsg;
import static me.duncte123.botcommons.web.WebParserUtils.toJSONObject;

@Authors(authors = {
    @Author(nickname = "Sanduhr32", author = "Maurice R S"),
    @Author(nickname = "duncte123", author = "Duncan Sterken"),
    @Author(nickname = "ramidzkh", author = "Ramid Khan")
})
public class AirUtils {

    private AirUtils() {}

    public static boolean isURL(String url) {
        return url.matches("^https?:\\/\\/[-a-zA-Z0-9+&@#\\/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#\\/%=~_|]");
    }

    public static boolean isInt(String integer) {
        return integer.matches("^\\d{1,11}$");
    }


    public static int parseIntSafe(String integer) {
        if (isInt(integer)) {
            return Integer.parseInt(integer);
        }

        return -1;
    }

    private static int longToInt(long input) {
        return (int) input;
    }

    public static String getUptime(long time) {
        return getUptime(time, false);
    }

    public static String getUptime(long time, boolean withTime) {
        /*
        This code has been inspired from JDA-Butler <https://github.com/Almighty-Alpaca/JDA-Butler/>
         */
        //Like it's ever gonna be up for more then a week
        final int years = longToInt(time / 31104000000L);
        final int months = longToInt(time / 2592000000L % 12);
        final int days = longToInt(time / 86400000L % 30);

        final StringBuilder builder = new StringBuilder();

        //Get the years, months and days
        builder.append(formatTimeWord("Year", years, true))
            .append(formatTimeWord("Month", months, true))
            .append(formatTimeWord("Day", days, false));

        //If we want the time added we pass in true
        if (withTime) {
            final int hours = longToInt(time / 3600000L % 24);
            final int minutes = longToInt(time / 60000L % 60);
            final int seconds = longToInt(time / 1000L % 60);

            builder.append(", ")
                .append(formatTimeWord("Hour", hours, true))
                .append(formatTimeWord("Minute", minutes, true))
                .append(formatTimeWord("Second", seconds, false));
        }

        final String uptimeString = builder.toString();

        return uptimeString.startsWith(", ") ? uptimeString.replaceFirst(", ", "") : uptimeString;
    }

    private static String formatTimeWord(String word, int amount, boolean withComma) {
        if (amount == 0) {
            return "";
        }

        final StringBuilder builder = new StringBuilder()
            .append(amount).append(' ').append(word);

        if (amount > 1) {
            builder.append('s');
        }

        if (withComma) {
            builder.append(", ");
        }

        return builder.toString();
    }

    public static void stop(AudioUtils audioUtils, ShardManager manager) {
        stopMusic(audioUtils, manager);

        audioUtils.getPlayerManager().shutdown();
    }

    private static void stopMusic(AudioUtils audioUtils, ShardManager manager) {
        final TLongObjectMap<GuildMusicManager> temp = new TLongObjectHashMap<>(audioUtils.getMusicManagers());

        for (final long key : temp.keys()) {
            final Guild guild = manager.getGuildById(key);

            if (guild != null) {
                stopMusic(guild, audioUtils);
            }
        }
    }

    public static void stopMusic(Guild guild, AudioUtils audioUtils) {
        final GuildMusicManager mng = audioUtils.getMusicManagers().get(guild.getIdLong());

        if (mng == null) {
            return;
        }

        final LavalinkManager lavalinkManager = LavalinkManager.INS;

        mng.stopAndClear();

        if (lavalinkManager.isConnected(guild)) {
            lavalinkManager.closeConnection(guild);
        }
    }

    public static TextChannel getLogChannel(long channel, Guild guild) {
        return getLogChannel(Long.toString(channel), guild);
    }

    private static TextChannel getLogChannel(String channelId, Guild guild) {
        if (channelId == null || channelId.isEmpty()) {
            return GuildUtils.getPublicChannel(guild);
        }

        final List<TextChannel> foundChannels = FinderUtil.findTextChannels(channelId, guild);

        if (foundChannels.isEmpty()) {
            return null;
        }

        return foundChannels.get(0);
    }

    @Nonnull
    public static String colorToHex(int hex) {
        final int red = (hex & 0xFF0000) >> 16;
        final int green = (hex & 0xFF00) >> 8;
        final int blue = hex & 0xFF;

        return String.format("#%02x%02x%02x", red, green, blue);
    }

    public static int colorToInt(String hex) {
        return Integer.parseInt(hex.substring(1), 16);
    }

    public static User getMentionedUser(CommandContext ctx) {
        User target = ctx.getAuthor();

        if (!ctx.getArgs().isEmpty()) {
            final List<User> foundUsers = FinderUtils.searchUsers(ctx.getArgsRaw(), ctx);

            if (!foundUsers.isEmpty()) {
                target = foundUsers.get(0);
            }
        }

        return target;
    }

    public static Member getMentionedMember(String argument, CommandContext ctx) {
        final List<Member> foundMembers = FinderUtils.searchMembers(argument, ctx);

        if (foundMembers.isEmpty()) {
            return new FakeMember(argument);
        }

        return foundMembers.get(0);
    }

    public static String getDatabaseDateFormat(Duration duration) {
        return getDatabaseDateFormat(getDatabaseDate(duration));
    }

    public static String getDatabaseDateFormat(OffsetDateTime date) {
        return date.truncatedTo(ChronoUnit.MILLIS).toString();
    }

    public static OffsetDateTime fromDatabaseFormat(String date) {
        try {
            return OffsetDateTime.parse(date);
        }
        catch (DateTimeParseException e) {
            e.printStackTrace();

            return OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public static String makeDatePretty(TemporalAccessor accessor) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(accessor);
    }

    public static OffsetDateTime getDatabaseDate(Duration duration) {
        return OffsetDateTime.now(ZoneOffset.UTC).plus(duration.getMilis(), ChronoUnit.MILLIS);
    }

    public static void handleExpiredReminders(List<Reminder> reminders, DatabaseAdapter adapter) {
        // Get the shardManager and a list of ints to purge the ids for
        final ShardManager shardManager = SkyBot.getInstance().getShardManager();
        final List<Integer> toPurge = new ArrayList<>();

        for (final Reminder reminder : reminders) {
            // The reminder message template
            final String message = String.format(
                "%s you asked me to remind you about \"%s\"",
                Time4JKt.humanize(reminder.getCreate_date(), TextWidth.ABBREVIATED),
                reminder.getReminder().trim()
            );

            // If we have a channel send the message to that
            if (reminder.getIn_channel()) {
                final long channelId = reminder.getChannel_id();
                final TextChannel channel = shardManager.getTextChannelById(channelId);

                // If we don't have any channel information we will continue at the end
                // skipping the continue statement makes sure that we roll into the dm part of this
                if (channel != null) {
                    // Add the reminder to the list of the reminders to purge
                    toPurge.add(reminder.getId());
                    sendMsg(
                        new MessageConfig.Builder()
                            .setChannel(channel)
                            .setMessage(String.format("<@%s>, %s", reminder.getUser_id(), message))
                            .replyTo(reminder.getMessage_id())
                            .build()
                    );

                    // go to the next one and don't run the user code if a channel was found
                    continue;
                }
            }

            try {
                Objects.requireNonNull(shardManager.getShardById(0))
                    .openPrivateChannelById(reminder.getUser_id())
                    .flatMap(
                        (c) -> c.sendMessage(message + "\n" + reminder.getJumpUrl())
                    )
                    .complete();
                toPurge.add(reminder.getId());
            }
            catch (ErrorResponseException errorResponseEx) {
                final ErrorResponse errorResponse = errorResponseEx.getErrorResponse();

                if (
                    // The account probably got deleted or something
                    errorResponse == ErrorResponse.UNKNOWN_USER ||
                        // we cannot dm this user (has dms blocked?)
                        errorResponse == ErrorResponse.CANNOT_SEND_TO_USER
                ) {
                    toPurge.add(reminder.getId());
                }
            }
            catch (Exception e) {
                Sentry.capture(e);
            }
        }

        // get a date that is 2 days in the future
        final OffsetDateTime plusTwoDays = OffsetDateTime.now(ZoneOffset.UTC).plus(2L, ChronoUnit.DAYS);

        // Remove any reminders that have not been removed after 2 days
        final List<Integer> extraRemoval = reminders.stream()
            .filter((reminder) -> reminder.getReminder_date().isAfter(plusTwoDays))
            .map(Reminder::getId)
            .collect(Collectors.toList());

        toPurge.addAll(extraRemoval);
        adapter.purgeReminders(toPurge);
    }

    public static String parsePerms(Permission[] perms) {
        final String neededPerms = Arrays.stream(perms)
            .map(Permission::getName)
            .collect(Collectors.joining("`, `"));

        return StringUtils.replaceLast(neededPerms, "`, `", "` and `");
    }

    public static void setJDAContext(JDA jda) {
        ((JDAImpl) jda).setContext();
    }

    @Nonnull
    public static PendingRequest<String> shortenUrl(String url, String googleKey, ObjectMapper mapper) {
        final ObjectNode json = mapper.createObjectNode();

        json.set("dynamicLinkInfo",
            mapper.createObjectNode()
                .put("domainUriPrefix", "dunctebot.link")
                .put("link", url)
        );
        json.set("suffix",
            mapper.createObjectNode()
                .put("option", "SHORT") // SHORT or UNGUESSABLE
        );

        try {
            return WebUtils.ins.postRequest(
                "https://firebasedynamiclinks.googleapis.com/v1/shortLinks?key=" + googleKey,
                JSONRequestBody.fromJackson(json)
            )
                .build(
                    (r) -> {
                        final ObjectNode response = toJSONObject(r, mapper);

                        if (response == null) {
                            return "Google did a fucky wucky and send invalid json";
                        }

                        return response.get("shortLink").asText();
                    },
                    WebParserUtils::handleError
                );
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();

            // Return a fake pending request to make sure that things don't break
            return new FakePendingRequest<>("JSON PARSING FAILED: " + e.getMessage());
        }
    }
}
