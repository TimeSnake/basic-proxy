package de.timesnake.basic.proxy.core.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class RuleManager {

    public static final String PARAGRAPH_REPLACEMENT = "$";

    private final RuleFile file;

    private final String version;
    private final LinkedHashMap<Integer, RuleSection> ruleSections = new LinkedHashMap<>();

    public RuleManager() {
        this.file = new RuleFile();
        this.version = this.file.getVersion();
        this.loadRules();
    }

    public void loadRules() {
        for (Integer sectionNumber : this.file.getSections()) {
            Collection<RuleParagraph> paragraphs = new ArrayList<>();
            for (String paragraphName : this.file.getParagraphs(sectionNumber)) {
                String name = paragraphName.replace("§", PARAGRAPH_REPLACEMENT);
                List<String> parts = new ArrayList<>();
                for (String part : this.file.getParagraphLines(sectionNumber, paragraphName)) {
                    parts.add(part.replace("§", PARAGRAPH_REPLACEMENT));
                }
                paragraphs.add(new RuleParagraph(name, parts));
            }
            String title = this.file.getSectionTitle(sectionNumber).replace("§", PARAGRAPH_REPLACEMENT);
            this.ruleSections.put(sectionNumber, new RuleSection(sectionNumber, title, paragraphs));
        }
    }

    public String getVersion() {
        return version;
    }

    public Collection<RuleSection> getSections() {
        return this.ruleSections.values();
    }

    public RuleSection getSection(Integer number) {
        return this.ruleSections.get(number);
    }
}
