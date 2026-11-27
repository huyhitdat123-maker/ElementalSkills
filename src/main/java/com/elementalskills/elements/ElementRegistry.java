package com.elementalskills.elements;

import com.elementalskills.elements.impl.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ElementRegistry {

    private final Map<String, ElementMastery> elements = new LinkedHashMap<>();

    public void register(ElementMastery element) {
        elements.put(element.getId().toLowerCase(), element);
    }

    public void registerAllDefaults() {
        // 8 Hệ Nguyên Tố Nâng Cấp
        register(new FireElement());
        register(new WaterElement());
        register(new IceElement());
        register(new WindElement());
        register(new LightningElement());
        register(new EarthElement());
        register(new LightElement());
        register(new DarkElement());

        // 12 Hệ Đặc Biệt Mới
        register(new DiamondElement());
        register(new LavaElement());
        register(new GojoElement());
        register(new SwordImmortalElement());
        register(new ExplosionElement());
        register(new SummonElement());
        register(new WoodElement());
        register(new BloodElement());
        register(new GravityElement());
        register(new TimeElement());
        register(new SoundElement());
        register(new PoisonElement());
    }

    public ElementMastery get(String id) {
        if (id == null) return null;
        return elements.get(id.toLowerCase());
    }

    public Collection<ElementMastery> getAll() {
        return elements.values();
    }
}
