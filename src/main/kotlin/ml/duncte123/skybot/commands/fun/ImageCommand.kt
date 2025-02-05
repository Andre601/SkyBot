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

package ml.duncte123.skybot.commands.`fun`

import me.duncte123.botcommons.messaging.EmbedUtils
import me.duncte123.botcommons.messaging.MessageUtils.sendEmbed
import me.duncte123.botcommons.web.WebUtils
import ml.duncte123.skybot.Author
import ml.duncte123.skybot.objects.command.Command
import ml.duncte123.skybot.objects.command.CommandCategory
import ml.duncte123.skybot.objects.command.CommandContext
import ml.duncte123.skybot.utils.CommandUtils.isUserOrGuildPatron

@Author(nickname = "duncte123", author = "Duncan Sterken")
class ImageCommand : Command() {

    init {
        this.category = CommandCategory.PATRON
        this.name = "image"
        this.help = "Searches for an image on google"
        this.usage = "<search term>"
    }

    override fun execute(ctx: CommandContext) {
        if (isUserOrGuildPatron(ctx)) {
            if (ctx.args.isEmpty()) {
                this.sendUsageInstructions(ctx)
                return
            }

            val keyword = ctx.argsRaw

            WebUtils.ins.getJSONObject(String.format(ctx.googleBaseUrl, keyword)).async {
                val jsonArray = it["items"]
                val randomItem = jsonArray[ctx.random.nextInt(jsonArray.size())]
                sendEmbed(
                    ctx,
                    EmbedUtils.getDefaultEmbed()
                        .setTitle(
                            randomItem["title"].asText(),
                            randomItem["image"]["contextLink"].asText()
                        )
                        .setImage(randomItem["link"].asText())
                )
            }
        }
    }
}
