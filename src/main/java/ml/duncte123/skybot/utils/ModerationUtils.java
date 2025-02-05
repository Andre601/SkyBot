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

import me.duncte123.botcommons.messaging.MessageConfig;
import ml.duncte123.skybot.Author;
import ml.duncte123.skybot.Authors;
import ml.duncte123.skybot.SkyBot;
import ml.duncte123.skybot.Variables;
import ml.duncte123.skybot.adapters.DatabaseAdapter;
import ml.duncte123.skybot.entities.jda.DunctebotGuild;
import ml.duncte123.skybot.objects.user.ConsoleUser;
import ml.duncte123.skybot.objects.user.FakeUser;
import ml.duncte123.skybot.objects.api.Ban;
import ml.duncte123.skybot.objects.api.Mute;
import com.dunctebot.models.settings.GuildSetting;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static me.duncte123.botcommons.messaging.EmbedUtils.embedMessage;
import static me.duncte123.botcommons.messaging.MessageUtils.sendMsg;
import static net.dv8tion.jda.api.exceptions.ErrorResponseException.ignore;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_BAN;

@Authors(authors = {
    @Author(nickname = "Sanduhr32", author = "Maurice R S"),
    @Author(nickname = "duncte123", author = "Duncan Sterken")
})
//@SuppressWarnings("PMD") // TODO: have a good look at this
public class ModerationUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ModerationUtils.class);

    private ModerationUtils() {}

    public static boolean canInteract(Member mod, Member target, String action, TextChannel channel) {

        if (!mod.canInteract(target)) {
            sendMsg(channel, "You cannot " + action + " this member");
            return false;
        }

        final Member self = mod.getGuild().getSelfMember();

        if (!self.canInteract(target)) {
            sendMsg(channel, "I cannot " + action + " this member, are their roles above mine?");
            return false;
        }

        return true;
    }

    public static void modLog(@Nonnull User mod, @Nonnull User punishedUser, @Nonnull String punishment,
                              @Nullable String reason, @Nullable String time, DunctebotGuild guild) {
        if (!isLogEnabled(punishment, guild)) {
            return;
        }

        String length = "";

        if (time != null && !time.isEmpty()) {
            length = " lasting **" + time + "**";
        }

        String reasonLine = "";

        if (reason != null && !reason.isEmpty()) {
            reasonLine = " with reason _\"" + reason + "\"_";
        }

        modLog(String.format("User **%#s** got **%s** by **%#s**%s%s",
            punishedUser,
            punishment,
            mod,
            length,
            reasonLine
        ), guild);
    }

    public static void modLog(String message, DunctebotGuild guild) {
        final long chan = guild.getSettings().getLogChannel();

        if (chan > 0) {
            final TextChannel logChannel = AirUtils.getLogChannel(chan, guild);

            sendMsg(logChannel, message);
        }
    }

    private static boolean isLogEnabled(String type, DunctebotGuild guild) {
        final GuildSetting settings = guild.getSettings();

        switch (type) {
            case "ban":
            case "banned":
            case "softban":
                return settings.isBanLogging();

            case "unban":
            case "unbanned":
                return settings.isUnbanLogging();

            case "mute":
            case "muted":
            case "unmuted":
                return settings.isMuteLogging();

            case "kick":
            case "kicked":
                return settings.isKickLogging();

            case "warn":
            case "warned":
                return settings.isWarnLogging();

            default:
                return true;
        }
    }

    public static int getWarningCountForUser(DatabaseAdapter adapter, @Nonnull User user, @Nonnull Guild guild) throws ExecutionException, InterruptedException {
        final CompletableFuture<Integer> future = new CompletableFuture<>();

        adapter.getWarningCountForUser(user.getIdLong(), guild.getIdLong(), (it) -> {
            future.complete(it);

            return null;
        });

        return future.get();
    }

    public static void handleUnmute(List<Mute> mutes, DatabaseAdapter adapter, Variables variables) {
        LOG.debug("Checking for users to unmute");
        final ShardManager shardManager = SkyBot.getInstance().getShardManager();

        for (final Mute mute : mutes) {
            final Guild guild = shardManager.getGuildById(mute.getGuildId());

            if (guild == null) {
                continue;
            }

            Member target = null;

            try {
                target = guild.retrieveMemberById(mute.getUserId()).complete();
            } catch (ErrorResponseException e) {
                // if it's not unknown member or unknown user we will rethroww the exception
                if (e.getErrorResponse() != ErrorResponse.UNKNOWN_MEMBER
                    && e.getErrorResponse() != ErrorResponse.UNKNOWN_USER) {
                    throw e;
                }
            }

            if (target == null) {
                continue;
            }

            if (!guild.getSelfMember().canInteract(target)) {
                continue;
            }

            final User targetUser = target.getUser();

            LOG.debug("Unmuting " + mute.getUserTag());

            final DunctebotGuild dbGuild = new DunctebotGuild(guild, variables);
            final long muteRoleId = dbGuild.getSettings().getMuteRoleId();

            if (muteRoleId < 1L) {
                continue;
            }

            final Role muteRole = guild.getRoleById(muteRoleId);

            if (muteRole == null) {
                continue;
            }

            if (!guild.getSelfMember().canInteract(muteRole)) {
                continue;
            }

            guild.removeRoleFromMember(target, muteRole).reason("Mute expired").queue();

            modLog(new ConsoleUser(), targetUser, "unmuted", null, null, dbGuild);
        }

        LOG.debug("Checking done, unmuted {} users.", mutes.size());

        final List<Integer> purgeIds = mutes.stream().map(Mute::getId).collect(Collectors.toList());

        if (!purgeIds.isEmpty()) {
            adapter.purgeMutes(purgeIds);
        }
    }

    public static void handleUnban(List<Ban> bans, DatabaseAdapter adapter, Variables variables) {
        LOG.debug("Checking for users to unban");

        // Get the ShardManager from our instance
        // Via the ShardManager we can fetch guilds and unban users
        final ShardManager shardManager = SkyBot.getInstance().getShardManager();

        // Loop over all the bans that came in
        for (final Ban ban : bans) {
            // Get the guild from the ban object
            final Guild guild = shardManager.getGuildById(ban.getGuildId());

            // If we're not in the guild anymore just ignore this iteration
            if (guild == null) {
                continue;
            }

            LOG.debug("Unbanning " + ban.getUserName());

            // Unban the user and set the reason to "Ban expired"
            guild.unban(ban.getUserId())
                .reason("Ban expired")
                // Ignore errors that indicate an unknown ban
                // This may happen some times
                .queue(null, ignore(UNKNOWN_BAN));

            // We're creating a fake user even though we can probably get a real user
            // This is to make sure that we have the data we need when logging the unban
            final User fakeUser = new FakeUser(
                ban.getUserName(),
                Long.parseUnsignedLong(ban.getUserId()),
                Short.parseShort(ban.getDiscriminator())
            );

            // Send the unban to the log channel
            modLog(new ConsoleUser(), fakeUser, "unbanned", null, null, new DunctebotGuild(guild, variables));
        }

        LOG.debug("Checking done, unbanned {} users.", bans.size());

        // Map all the bans to just the id
        final List<Integer> purgeIds = bans.stream().map(Ban::getId).collect(Collectors.toList());

        // If the bans are not empty send a purge request to the databse
        // This will make sure that we don't get them again
        if (!purgeIds.isEmpty()) {
            adapter.purgeBans(purgeIds);
        }
    }

    public static void muteUser(DunctebotGuild guild, Member member, TextChannel channel, String cause, long minuteDuration) {
        muteUser(guild, member, channel, cause, minuteDuration, false);
    }

    public static void muteUser(DunctebotGuild guild, Member member, TextChannel channel, String cause, long minuteDuration, boolean sendMessages) {
        final GuildSetting guildSettings = guild.getSettings();
        final long muteRoleId = guildSettings.getMuteRoleId();

        if (muteRoleId <= 0) {
            if (sendMessages) {
                sendMsg(channel, "The role for the punished people is not configured. Please set it up." +
                    "We disabled your spam filter until you have set up a role.");
            }

            guildSettings.setEnableSpamFilter(false);
            return;
        }

        final Role muteRole = guild.getRoleById(muteRoleId);

        if (muteRole == null) {
            if (sendMessages) {
                sendMsg(channel, "The role for the punished people is inexistent.");
            }
            return;
        }

        final Member self = guild.getSelfMember();

        if (!self.hasPermission(Permission.MANAGE_ROLES)) {
            if (sendMessages) {
                sendMsg(channel, "I don't have permissions for muting a person. Please give me role managing permissions.");
            }
            return;
        }

        if (!self.canInteract(member) || !self.canInteract(muteRole)) {
            if (sendMessages) {
                sendMsg(channel, "I can not access either the member or the role.");
            }
            return;
        }
        final String reason = String.format("The member %#s was muted for %s until %d", member.getUser(), cause, minuteDuration);
        guild.addRoleToMember(member, muteRole).reason(reason).queue(
            (success) ->
                guild.removeRoleFromMember(member, muteRole).reason("Scheduled un-mute")
                    .queueAfter(minuteDuration, TimeUnit.MINUTES)
            ,
            (failure) -> {
                final long chan = guildSettings.getLogChannel();
                if (chan > 0) {
                    final TextChannel logChannel = AirUtils.getLogChannel(chan, guild);

                    final String message = String.format("%#s bypassed the mute.", member.getUser());

                    if (sendMessages) {
                        sendMsg(new MessageConfig.Builder()
                            .setChannel(logChannel)
                            .setEmbed(embedMessage(message))
                            .build());
                    }
                }
            });
    }

    public static void kickUser(Guild guild, Member member, TextChannel channel, String cause) {
        kickUser(guild, member, channel, cause, false);
    }

    public static void kickUser(Guild guild, Member member, TextChannel channel, String cause, boolean sendMessages) {
        final Member self = guild.getSelfMember();

        if (!self.hasPermission(Permission.KICK_MEMBERS)) {
            if (sendMessages) {
                sendMsg(channel, "I don't have permissions for kicking a person. Please give me kick members permissions.");
            }
            return;
        }

        if (!self.canInteract(member)) {
            if (sendMessages) {
                sendMsg(channel, "I can not access the member.");
            }
            return;
        }
        final String reason = String.format("The member %#s was kicked for %s.", member.getUser(), cause);
        guild.kick(member, reason).reason(reason).queue();
    }
}
