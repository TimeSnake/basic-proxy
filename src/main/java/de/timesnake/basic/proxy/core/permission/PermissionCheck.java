/*
 * basic-proxy.main
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

package de.timesnake.basic.proxy.core.permission;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import de.timesnake.basic.proxy.util.Network;
import de.timesnake.library.extension.util.permission.ExPermission;

import java.util.HashSet;
import java.util.Set;

public class PermissionCheck {

    @Subscribe
    public void onPermissionCheck(PermissionsSetupEvent e) {
        if (e.getSubject() instanceof Player) {
            e.setProvider(subject -> new PlayerPermissionChecker(((Player) e.getSubject())));
        }
    }

    public static class PlayerPermissionChecker implements PermissionFunction {

        private final Player player;

        public PlayerPermissionChecker(Player player) {
            this.player = player;
        }

        @Override
        public Tristate getPermissionValue(String permission) {
            if (permission == null) {
                return Tristate.TRUE;
            }

            Set<ExPermission> perms = new HashSet<>(Network.getUser(player).getPermissions());

            if (perms.isEmpty()) {
                return Tristate.FALSE;
            }

            for (ExPermission userPerm : perms) {

                if (userPerm.getPermission().equals("*")) {
                    return Tristate.TRUE;
                }

                if (userPerm.getPermission().equals(permission)) {
                    return Tristate.TRUE;
                }

                String[] needPerm = permission.split("\\.");
                StringBuilder permSum = new StringBuilder();

                for (String permPart : needPerm) {
                    permSum.append(permPart).append(".");
                    if (userPerm.getPermission().equals(permSum + "*")) {
                        return Tristate.TRUE;
                    }
                }
            }

            return Tristate.FALSE;
        }
    }
}
