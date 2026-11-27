package com.elementalskills.elements;

import com.elementalskills.elements.impl.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ElementRegistry {

    private final Map<String, ElementalMastery> elements = new LinkedHashMap<>();

    public void register(ElementalMastery element) {
        elements.put(element.getId().toLowerCase(), element);
    }

    public void registerAllDefaults() {
        register(new HoaElement());
        register(new ThuyElement());
        register(new BangElement());
        register(new PhongElement());
        register(new SetElement());
        register(new DatElement());
        register(new QuangElement());
        register(new AmElement());
    }

    public ElementalMastery get(String id) {
        if (id == null) return null;
        return elements.get(id.toLowerCase());
    }

    public Collection<ElementalMastery> getAll() {
        return elements.values();
    }
}
