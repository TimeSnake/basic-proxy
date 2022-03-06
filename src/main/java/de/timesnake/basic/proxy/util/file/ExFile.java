package de.timesnake.basic.proxy.util.file;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ExFile {

    public static void deleteFile(File file) {
        if (file.isDirectory()) {
            if (Objects.requireNonNull(file.list()).length == 0) file.delete();
            else {
                String[] files = file.list();

                for (String temp : Objects.requireNonNull(files)) {
                    ExFile.deleteFile(new File(file, temp));
                }
                if (Objects.requireNonNull(file.list()).length == 0) file.delete();
            }
        } else file.delete();
    }

    public static String toPath(String... sections) {
        return String.join(".", sections);
    }

    protected final File configFile;
    protected Configuration config;

    public ExFile(String folder, String name) {
        this.configFile = new File("plugins/" + folder + "/" + name + ".yml");

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

        this.load();
    }

    public void load() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void save() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void set(String path, Object value) {
        if (value != null) {
            if (value instanceof UUID) {
                this.config.set(path, value.toString());
                return;
            }
        }
        this.load();
        this.config.set(path, value);
        this.save();
    }

    public boolean contains(String path) {
        return this.config.contains(path);
    }

    public String getString(String path) {
        this.load();
        return this.config.getString(path);
    }

    public Integer getInt(String path) {
        this.load();
        return this.config.getInt(path);
    }

    public boolean getBoolean(String path) {
        this.load();
        return this.config.getBoolean(path);
    }

    public double getDouble(String path) {
        this.load();
        return this.config.getDouble(path);
    }

    public long getLong(String path) {
        this.load();
        return this.config.getLong(path);
    }


    public boolean remove(String path) {
        this.load();
        if (this.contains(path)) {
            this.config.set(path, "");
            return true;
        }
        return false;
    }

    public List<Integer> getIntegerList(String path) {
        this.load();
        return this.config.getIntList(path);
    }

    public List<String> getStringList(String path) {
        this.load();
        return this.config.getStringList(path);
    }

    public List<UUID> getUUIDList(String path) {
        this.load();
        ArrayList<UUID> uuids = new ArrayList<>();
        for (String s : this.getStringList(path)) {
            try {
                uuids.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return uuids;
    }

    public UUID getUUID(String path) {
        this.load();
        try {
            return UUID.fromString(this.getString(path));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Configuration getConfigSection(String path) {
        return this.config.getSection(path);
    }

    public Collection<String> getPathStringList(String path) {
        this.load();
        if (this.config.contains(path)) {
            return this.getConfigSection(path).getKeys();
        }
        return null;
    }

    public Collection<Integer> getPathIntegerList(String path) {
        this.load();
        Collection<Integer> ids = new HashSet<>();
        if (this.config.contains(path)) {
            Collection<String> idStrings = this.getPathStringList(path);
            for (String idString : idStrings) {
                if (idString != null) {
                    try {
                        ids.add(Integer.valueOf(idString));
                    } catch (NumberFormatException ignored) {

                    }
                }
            }
        }
        return ids;
    }
}
