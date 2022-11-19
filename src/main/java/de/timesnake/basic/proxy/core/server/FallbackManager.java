/*
 * workspace.basic-proxy.main
 * Copyright (C) 2022 timesnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.timesnake.basic.proxy.core.server;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.basic.proxy.util.user.User;
import de.timesnake.database.util.object.Type;

public class FallbackManager {

    @Subscribe(order = PostOrder.FIRST)
    public void onServerKickEvent(KickedFromServerEvent e) {
        if (!(e.getResult() instanceof KickedFromServerEvent.DisconnectPlayer)) {
            return;
        }

        if (Network.getServer(e.getServer().getServerInfo().getName()).getType().equals(Type.Server.LOBBY)) {
            return;
        }

        User user = Network.getUser(e.getPlayer());

        if (user == null || user.getLobby() == null) {
            return;
        }

        e.setResult(KickedFromServerEvent.RedirectPlayer.create(user.getLobby().getBungeeInfo()));
    }
}
