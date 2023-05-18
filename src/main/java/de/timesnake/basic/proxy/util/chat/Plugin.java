/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.chat;

import de.timesnake.library.basic.util.LogHelper;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Plugin extends de.timesnake.library.extension.util.chat.Plugin {

  public static final Plugin GAME = new Plugin("Game", "PSG",
      LogHelper.getLogger("Game", Level.INFO));
  public static final Plugin SUPPORT = new Plugin("Support", "PSS",
      LogHelper.getLogger("Support", Level.INFO));
  public static final Plugin PUNISH = new Plugin("Punish", "PSP",
      LogHelper.getLogger("Punish", Level.INFO));
  public static final Plugin PERMISSION = new Plugin("Perm", "PSR",
      LogHelper.getLogger("Perm", Level.INFO));
  public static final Plugin ALIAS = new Plugin("Alias", "PSA",
      LogHelper.getLogger("Alias", Level.INFO));

  protected Plugin(String name, String code, Logger logger) {
    super(name, code);
  }
}
