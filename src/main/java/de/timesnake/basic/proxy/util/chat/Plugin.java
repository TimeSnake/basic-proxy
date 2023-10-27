/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.chat;

public class Plugin extends de.timesnake.library.extension.util.chat.Plugin {

  public static final Plugin GAME = new Plugin("Game", "PSG");
  public static final Plugin SUPPORT = new Plugin("Support", "PSS");
  public static final Plugin PUNISH = new Plugin("Punish", "PSP");
  public static final Plugin PERMISSION = new Plugin("Perm", "PSR");
  public static final Plugin ALIAS = new Plugin("Alias", "PSA");

  protected Plugin(String name, String code) {
    super(name, code);
  }
}
