package de.timesnake.basic.proxy.core.rule;

import de.timesnake.basic.proxy.util.file.ExFile;

import java.util.Collection;

public class RuleFile extends ExFile {

    public static final String VERSION = "version";
    public static final String SECTIONS = "sections";
    public static final String TITLE = "title";
    public static final String PARAGRAPHS = "paragraphs";


    public RuleFile() {
        super("basic-proxy", "rules.toml");
    }

    public String getVersion() {
        return super.getString(VERSION);
    }

    public Collection<String> getSections() {
        return super.getList(SECTIONS);
    }

    public String getSectionTitle(String sectionName) {
        return super.getString(SECTIONS + "." + sectionName + "." + TITLE);
    }

    public Collection<String> getParagraphs(String sectionName) {
        return super.getList(SECTIONS + "." + sectionName + "." + PARAGRAPHS);
    }

    public Collection<String> getParagraphLines(String sectionName, String paragraphName) {
        return super.getList(SECTIONS + "." + sectionName + "." + PARAGRAPHS + "." + paragraphName);
    }


}
