/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.basic.proxy.core.group;

public class GroupNotInDatabaseException extends Exception {

    private String name;

    public GroupNotInDatabaseException(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String getMessage() {
        return "[BasicProxySystem] WARNING group " + this.name + " is not loaded";
    }
}
