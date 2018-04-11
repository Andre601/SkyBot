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

package ml.duncte123.skybot.commands.fun;

import ml.duncte123.skybot.objects.command.Command;
import ml.duncte123.skybot.utils.MessageUtils;
import ml.duncte123.skybot.utils.WebUtils;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IsNowIllegalCommand extends Command {

    private final ScheduledExecutorService illegalService = Executors.newScheduledThreadPool(1,
            r -> new Thread(r, "illegalService"));

    @Override
    public void executeCommand(@NotNull String invoke, @NotNull String[] args, @NotNull GuildMessageReceivedEvent event) {
        String input = StringUtils.join(args, " ")
                .replaceAll("([^a-zA-Z0-9 ]+)", "").toUpperCase();
        if (input.length() < 1) {
            MessageUtils.sendMsg(event, "This command requires a text argument.");
            return;
        }
        if (input.length() > 10)
            input = input.substring(0, 9);
        JSONObject jsonData = new JSONObject().put("task", "gif").put("word", input.replaceAll(" ", "%20"));
        MessageUtils.sendMsg(event, "Checking if \"" + input + "\" is illegal....... (might take up to 1 minute)", success ->
                WebUtils.ins.postJSON("https://is-now-illegal.firebaseio.com/queue/tasks.json", jsonData).async(txt ->
                        illegalService.schedule(() -> {

                            String rawJson = getFileJSON(jsonData.getString("word"));

                            if (rawJson.equals("null")) {
                                success.editMessage(jsonData.getString("word") + " is legal").queue();
                            }
                            JSONObject j = new JSONObject(rawJson);
                            success.editMessage(j.getString("url").replaceAll(" ", "%20")).queue();

                        }, 10L, TimeUnit.SECONDS)
                )
        );
    }

    @Override
    public String help() {
        return "Makes sure that things are illegal.\n" +
                "Usage: `" + PREFIX + getName() + " <words>`";
    }

    @Override
    public String getName() {
        return "isnowillegal";
    }

    private String getFileJSON(String word) {
        return WebUtils.ins.getText("https://is-now-illegal.firebaseio.com/gifs/" + word + ".json").execute();
    }
}
