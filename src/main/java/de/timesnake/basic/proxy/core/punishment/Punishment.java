/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.basic.proxy.core.punishment;

import java.util.Date;
import java.util.UUID;

public class Punishment {
    private UUID uuid;
    private String type;
    private Date date;
    private String byName;
    private String reason;

    public Punishment(UUID uuid, String type, Date date, String byName, String reason) {
        this.setUuid(uuid);
        this.setDate(date);
        this.setByName(byName);
        this.setReason(reason);
        this.setType(type);
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getByName() {
        return this.byName;
    }

    public void setByName(String byName) {
        this.byName = byName;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
