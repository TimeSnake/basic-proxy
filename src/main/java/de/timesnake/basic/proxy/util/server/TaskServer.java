package de.timesnake.basic.proxy.util.server;

import de.timesnake.database.util.server.DbTaskServer;

import java.nio.file.Path;

public abstract class TaskServer extends Server {

    protected String task;

    protected TaskServer(DbTaskServer database, Path folderPath) {
        super(database, folderPath);
        this.task = database.getTask();
    }

    public String getTask() {
        return this.task;
    }

    public void setTask(String task) {
        this.task = task;
        ((DbTaskServer) super.database).setTask(task);
    }

    @Override
    public String getServerTask() {
        return this.getTask();
    }

    public void setTaskSynchronized(String task) {
        this.task = task;
        ((DbTaskServer) super.database).setTaskSynchronized(task);
    }
}
