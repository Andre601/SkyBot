/*
 * Skybot, a multipurpose discord bot
 *      Copyright (C) 2017 - 2018  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan & Maurice R S "Sanduhr32"
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

package ml.duncte123.skybot.commands.image;

import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import static ml.duncte123.skybot.utils.Variables.BLARG_BOT;

public class TheSearchCommand extends ImageCommandBase {

    @Override
    public void executeCommand(@NotNull String invoke, @NotNull String[] args, @NotNull GuildMessageReceivedEvent event) {

        if (!doAllChecks(event, args)) {
            return;
        }

        String text = String.join(" ", args);

        for (User user : event.getMessage().getMentionedUsers()) {
            text = text.replaceAll(user.getAsMention(), String.format("%#s", user));
        }

        for (TextChannel channel : event.getMessage().getMentionedChannels()) {
            text = text.replaceAll(channel.getAsMention(), String.format("%#s", channel));
        }

        for (Role role : event.getMessage().getMentionedRoles()) {
            text = text.replaceAll(role.getAsMention(), String.format("@%s", role.getName()));
        }

        BLARG_BOT.getTheSearch(text).async((image) -> handleBasicImage(event, image));
    }

    @Override
    public String help() {
        return "Go on a journey to find intelligent life.\n" +
                "Usage: `db!thesearch <message>`";
    }

    @Override
    public String getName() {
        return "thesearch";
    }
}
