package de.timesnake.basic.proxy.core.rule;

import de.timesnake.basic.proxy.util.file.ExFile;

import java.util.Collection;

public class RuleFile extends ExFile {

    public static final String VERSION = "version";
    public static final String SECTIONS = "sections";
    public static final String TITLE = "title";
    public static final String PARAGRAPHS = "paragraphs";


    public RuleFile() {
        super("basic-proxy", "rules");
    }

    public String getVersion() {
        return super.getString(VERSION);
    }

    public Collection<Integer> getSections() {
        return super.getPathIntegerList(SECTIONS);
    }

    public String getSectionTitle(Integer sectionNumber) {
        return super.getString(SECTIONS + "." + sectionNumber + "." + TITLE);
    }

    public Collection<String> getParagraphs(Integer sectionNumber) {
        return super.getPathStringList(SECTIONS + "." + sectionNumber + "." + PARAGRAPHS);
    }

    public Collection<String> getParagraphLines(Integer sectionNumber, String paragraphName) {
        return super.getStringList(SECTIONS + "." + sectionNumber + "." + PARAGRAPHS + "." + paragraphName);
    }


}
