package de.timesnake.basic.proxy.core.rule;

import java.util.Collection;

public class RuleSection {

    private final Integer number;
    private final String name;
    private final Collection<RuleParagraph> paragraphs;

    public RuleSection(Integer number, String name, Collection<RuleParagraph> paragraphs) {
        this.number = number;
        this.name = name;
        this.paragraphs = paragraphs;
    }

    public Integer getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public Collection<RuleParagraph> getParagraphs() {
        return paragraphs;
    }
}
