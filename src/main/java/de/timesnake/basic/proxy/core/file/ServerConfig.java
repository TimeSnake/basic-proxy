/*
 * Copyright (C) 2022 timesnake
 */

package de.timesnake.basic.proxy.core.file;

import de.timesnake.basic.proxy.util.file.ExFile;

public class ServerConfig extends ExFile {

	public ServerConfig() {
		super("basic-proxy", "server_config.toml");
	}

	public void loadServers() {
		this.load();

	}

	public Integer getMaxPlayersLobby() {
		return super.getLong("max_players.lobby").intValue();
	}

	public Integer getMaxPlayersBuild() {
		return super.getLong("max_players.build").intValue();
	}
}
