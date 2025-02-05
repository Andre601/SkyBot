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

package ml.duncte123.skybot.commands.music

import me.duncte123.botcommons.messaging.MessageUtils.sendMsg
import ml.duncte123.skybot.Author
import ml.duncte123.skybot.objects.command.CommandContext
import ml.duncte123.skybot.objects.command.MusicCommand
import ml.duncte123.skybot.utils.CommandUtils.isUserOrGuildPatron
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Author(nickname = "Sanduhr32", author = "Maurice R S")
class VolumeCommand : MusicCommand() {

    init {
        this.name = "volume"
        this.help = "Sets the volume on the music player"
        this.usage = "[volume]"
    }

    override fun run(ctx: CommandContext) {
        if (!isUserOrGuildPatron(ctx)) {
            return
        }

        val mng = ctx.audioUtils.getMusicManager(ctx.guild)
        val player = mng.player
        val filters = player.filters
        val args = ctx.args

        if (args.isEmpty()) {
            sendMsg(ctx, "The current volume is **${floor(filters.volume * 100)}**")
            return
        }

        try {
            val userInput = args[0].toFloat() / 100
            val newVolume = max(0f, min(10.0f, userInput))
            val oldVolume = filters.volume

            filters.volume = newVolume

            filters.commit()

            sendMsg(ctx, "Player volume changed from **${floor(oldVolume * 100)}** to **${floor(newVolume * 100)}**")
        } catch (e: NumberFormatException) {
            sendMsg(ctx, "**${args[0]}** is not a valid integer. (0 - 1000)")
        }
    }
}
