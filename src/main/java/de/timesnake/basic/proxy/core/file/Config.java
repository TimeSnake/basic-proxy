/*
 * de.timesnake.workspace.basic-proxy.main
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

package de.timesnake.basic.proxy.core.file;

import de.timesnake.basic.proxy.util.file.ExFile;

public class Config extends ExFile {

	public Config() {
		super("basic-proxy", "config.toml");
		this.load();
	}

	public String getGuestGroupName() {
		return config.getString("group.guest");
	}

	public String getNetworkPath() {
		return config.getString("network.path");
	}

	public String getVelocitySecret() {
		return config.getString("network.velocity-secret");
	}

	public Boolean isTmuxEnabled() {
		return config.getBoolean("network.tmux");
	}

	public Integer getMaxPlayersLobby() {
		return super.getLong("max_players.lobby").intValue();
	}

	public Integer getMaxPlayersBuild() {
		return super.getLong("max_players.build").intValue();
	}

}
