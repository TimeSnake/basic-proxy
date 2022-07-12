package de.timesnake.basic.proxy.core.rule;

import java.util.Collection;
import java.util.LinkedHashMap;

public class RuleManager {

    public static final String PARAGRAPH_REPLACEMENT = "$";

    private final RuleFile file;

    private final String version;
    private final LinkedHashMap<String, RuleSection> ruleSections = new LinkedHashMap<>();

    public RuleManager() {
        this.file = new RuleFile();
        this.version = this.file.getVersion();
        this.loadRules();
    }

    public void loadRules() {
        /*
        for (String sectionName : this.file.getSections()) {
            Collection<RuleParagraph> paragraphs = new ArrayList<>();
            for (String paragraphName : this.file.getParagraphs(sectionName)) {
                String name = paragraphName.replace("§", PARAGRAPH_REPLACEMENT);
                List<String> parts = new ArrayList<>();
                for (String part : this.file.getParagraphLines(sectionName, paragraphName)) {
                    parts.add(part.replace("§", PARAGRAPH_REPLACEMENT));
                }
                paragraphs.add(new RuleParagraph(name, parts));
            }
            String title = this.file.getSectionTitle(sectionName).replace("§", PARAGRAPH_REPLACEMENT);
            this.ruleSections.put(sectionName, new RuleSection(sectionName, title, paragraphs));
        }

         */
    }

    public String getVersion() {
        return version;
    }

    public Collection<RuleSection> getSections() {
        return this.ruleSections.values();
    }

    public RuleSection getSection(String name) {
        return this.ruleSections.get(name);
    }
}
