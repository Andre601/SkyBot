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

package ml.duncte123.skybot.audio;

import fredboat.audio.player.LavalinkManager;
import lavalink.client.player.LavalinkPlayer;
import ml.duncte123.skybot.Author;
import ml.duncte123.skybot.SkyBot;
import ml.duncte123.skybot.Variables;
import ml.duncte123.skybot.utils.GuildSettingsUtils;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Author(nickname = "duncte123", author = "Duncan Sterken")
public class GuildMusicManager {
    public final LavalinkPlayer player;
    private final TrackScheduler scheduler;
    private final AtomicLong lastChannel = new AtomicLong(-1);
    private final Supplier<Boolean> announceTracksSupplier;

    public GuildMusicManager(long guildId, Variables variables) {
        this.player = LavalinkManager.INS.createPlayer(guildId);
        this.scheduler = new TrackScheduler(this.player, this);
        this.player.addListener(this.scheduler);
        this.announceTracksSupplier = () -> GuildSettingsUtils.getGuild(guildId, variables).isAnnounceTracks();
    }

    /* package */ boolean isAnnounceTracks() {
        return this.announceTracksSupplier.get();
    }

    // Has to be public because of kotlin
    public long getLastChannel() {
        return this.lastChannel.get();
    }

    public void setLastChannel(long lastChannel) {
        this.lastChannel.set(lastChannel);
    }

    public TrackScheduler getScheduler() {
        return this.scheduler;
    }

    public void stopAndClear() {
        final TrackScheduler scheduler = this.getScheduler();
        this.player.removeListener(scheduler);
        this.player.setPaused(false);

        if (this.player.getPlayingTrack() != null) {
            this.player.stopTrack();
        }

        scheduler.queue.clear();
    }

    @Nullable
    /* package */ TextChannel getLatestChannel() {
        final long last = this.getLastChannel();

        if (last == -1 || last == 0) {
            return null;
        }

        return SkyBot.getInstance().getShardManager().getTextChannelById(last);
    }
}
