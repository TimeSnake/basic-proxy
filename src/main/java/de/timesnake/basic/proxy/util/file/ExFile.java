/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.file;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExFile {

  public static void deleteFile(File file) {
    if (file.isDirectory()) {
      if (Objects.requireNonNull(file.list()).length == 0) {
        file.delete();
      } else {
        String[] files = file.list();

        for (String temp : Objects.requireNonNull(files)) {
          ExFile.deleteFile(new File(file, temp));
        }
        if (Objects.requireNonNull(file.list()).length == 0) {
          file.delete();
        }
      }
    } else {
      file.delete();
    }
  }

  public static String toPath(String... sections) {
    return String.join(".", sections);
  }

  protected final File configFile;
  protected Toml config;
  protected TomlWriter writer;

  public ExFile(String folder, String name) {
    this.configFile = new File("plugins/" + folder + "/" + name);

    //directory creation
    File dir = new File("plugins/" + folder);
    if (!dir.exists()) {
      dir.mkdir();
    }

    if (!configFile.exists()) {
      try {
        configFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    this.config = new Toml();

    this.load();
  }

  public void load() {
    config.read(configFile);

  }

  public void save() {
    writer.write(configFile);
  }

  public String getString(String key) {
    return config.getString(key);
  }

  public String getString(String key, String defaultValue) {
    return config.getString(key, defaultValue);
  }

  public Long getLong(String key) {
    return config.getLong(key);
  }

  public Long getLong(String key, Long defaultValue) {
    return config.getLong(key, defaultValue);
  }

  public <T> List<T> getList(String key) {
    return config.getList(key);
  }

  public <T> List<T> getList(String key, List<T> defaultValue) {
    return config.getList(key, defaultValue);
  }

  public Boolean getBoolean(String key) {
    return config.getBoolean(key);
  }

  public Boolean getBoolean(String key, Boolean defaultValue) {
    return config.getBoolean(key, defaultValue);
  }

  public Date getDate(String key) {
    return config.getDate(key);
  }

  public Date getDate(String key, Date defaultValue) {
    return config.getDate(key, defaultValue);
  }

  public Double getDouble(String key) {
    return config.getDouble(key);
  }

  public Double getDouble(String key, Double defaultValue) {
    return config.getDouble(key, defaultValue);
  }

  public Toml getTable(String key) {
    return config.getTable(key);
  }

  public List<Toml> getTables(String key) {
    return config.getTables(key);
  }

  public boolean contains(String key) {
    return config.contains(key);
  }

  public boolean containsPrimitive(String key) {
    return config.containsPrimitive(key);
  }

  public boolean containsTable(String key) {
    return config.containsTable(key);
  }

  public boolean containsTableArray(String key) {
    return config.containsTableArray(key);
  }

  public boolean isEmpty() {
    return config.isEmpty();
  }

  public Map<String, Object> toMap() {
    return config.toMap();
  }

  public Set<Map.Entry<String, Object>> entrySet() {
    return config.entrySet();
  }
}
