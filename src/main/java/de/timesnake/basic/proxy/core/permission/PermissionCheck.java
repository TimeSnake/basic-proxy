package de.timesnake.basic.proxy.core.permission;

import de.timesnake.basic.proxy.util.Network;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashSet;
import java.util.Set;

public class PermissionCheck implements Listener {
    public PermissionCheck() {
    }

    @EventHandler
    public void onPermissionCheck(PermissionCheckEvent e) {
        if (e.getSender() instanceof ProxiedPlayer p) {
            String perm = e.getPermission();
            if (Network.getUser(p) != null) {

                if (perm == null) {
                    e.setHasPermission(true);
                    return;
                }

                Set<Permission> perms = new HashSet<>(Network.getUser(p).getPermissions());

                if (perms.isEmpty()) {
                    e.setHasPermission(false);
                    return;
                }

                for (Permission userPerm : perms) {

                    if (userPerm.getPermission().equals("*")) {
                        e.setHasPermission(true);
                        return;
                    }

                    if (userPerm.getPermission().equals(perm)) {
                        e.setHasPermission(true);
                        return;
                    }

                    String[] needPerm = perm.split("\\.");
                    StringBuilder permSum = new StringBuilder();

                    for (String permPart : needPerm) {
                        permSum.append(permPart).append(".");
                        if (userPerm.getPermission().equals(permSum + "*")) {
                            e.setHasPermission(true);
                            return;
                        }
                    }
                }
            }

            e.setHasPermission(false);
        }

    }
}
