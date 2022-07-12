package de.timesnake.basic.proxy.core.rule;

import java.util.Collection;
import java.util.Map;

public class RuleParagraph {

    private final Map<String, String> nameByLang;
    private final Map<String, Collection<String>> partsByLang;

    public RuleParagraph(Map<String, String> nameByLang, Map<String, Collection<String>> partsByLang) {
        this.nameByLang = nameByLang;
        this.partsByLang = partsByLang;
    }

    public String getName(String lang) {
        return this.nameByLang.get(lang);
    }

    public Collection<String> getParts(String lang) {
        return this.partsByLang.get(lang);
    }
}
