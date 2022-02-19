package de.timesnake.basic.proxy.core.permission;

import de.timesnake.library.basic.util.Status;

import java.util.Collection;

public class Permission {

    private String permission;
    private Status.Permission mode;
    private Collection<String> server;

    public Permission(String permission, Status.Permission mode, Collection<String> server) {
        this.setPermission(permission);
        this.setMode(mode);
        this.setServer(server);
    }

    public String getPermission() {
        return this.permission;
    }

    public Status.Permission getMode() {
        return this.mode;
    }

    public Collection<String> getServer() {
        return this.server;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setMode(Status.Permission mode) {
        this.mode = mode;
    }

    public void setServer(Collection<String> server) {
        this.server = server;
    }
}
