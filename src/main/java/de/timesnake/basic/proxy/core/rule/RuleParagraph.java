package de.timesnake.basic.proxy.core.rule;

import java.util.Collection;

public class RuleParagraph {

    private final String name;
    private final Collection<String> parts;

    public RuleParagraph(String name, Collection<String> parts) {
        this.name = name;
        this.parts = parts;
    }

    public String getName() {
        return name;
    }

    public Collection<String> getParts() {
        return parts;
    }
}
