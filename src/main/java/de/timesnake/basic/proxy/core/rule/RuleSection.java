package de.timesnake.basic.proxy.core.rule;

import java.util.Collection;
import java.util.Map;

public class RuleSection {

    private final Map<String, String> nameByLang;
    private final Map<String, String> titleByLang;
    private final Collection<RuleParagraph> paragraphs;

    public RuleSection(Map<String, String> nameByLang, Map<String, String> titleByLang,
                       Collection<RuleParagraph> paragraphs) {
        this.nameByLang = nameByLang;
        this.titleByLang = titleByLang;
        this.paragraphs = paragraphs;
    }

    public String getName(String lang) {
        return this.nameByLang.get(lang);
    }

    public String getTitle(String lang) {
        return this.titleByLang.get(lang);
    }

    public Collection<RuleParagraph> getParagraphs() {
        return paragraphs;
    }
}
