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
