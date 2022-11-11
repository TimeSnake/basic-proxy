/*
 * timesnake.basic-proxy.main
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

package de.timesnake.basic.proxy.util.chat;

import de.timesnake.library.basic.util.LogHelper;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Plugin extends de.timesnake.library.extension.util.chat.Plugin {

    public static final Plugin GAME = new Plugin("Game", "PSG", LogHelper.getLogger("Game", Level.INFO));
    public static final Plugin SUPPORT = new Plugin("Support", "PSS", LogHelper.getLogger("Support", Level.INFO));
    public static final Plugin PUNISH = new Plugin("Punish", "PSP", LogHelper.getLogger("Punish", Level.INFO));
    public static final Plugin PERMISSION = new Plugin("Perm", "PSR", LogHelper.getLogger("Perm", Level.INFO));
    public static final Plugin ALIAS = new Plugin("Alias", "PSA", LogHelper.getLogger("Alias", Level.INFO));

    protected Plugin(String name, String code, Logger logger) {
        super(name, code, logger);
    }
}
