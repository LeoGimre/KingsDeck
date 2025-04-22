package io.WizardsChessMaster.model.spells;

import com.badlogic.gdx.Gdx;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Factory for creating SpellEffectComponent instances based on type names.
 * Uses ServiceLoader to dynamically discover available implementations.
 */
public class SpellEffectComponentFactory {

    private static final String TAG = "SpellEffectCompFactory";
    private static final Map<String, Class<? extends SpellEffectComponent>> componentRegistry = new HashMap<>();

    static {
        Gdx.app.log(TAG, "Initializing SpellEffectComponentFactory using ServiceLoader...");
        loadComponents();
        Gdx.app.log(TAG, "SpellEffectComponentFactory initialized. Found " + componentRegistry.size() + " component types.");
    }

    private static void loadComponents() {
        ServiceLoader<SpellEffectComponent> loader = ServiceLoader.load(SpellEffectComponent.class);
        componentRegistry.clear();

        for (SpellEffectComponent component : loader) {
            if (component == null) {
                Gdx.app.error(TAG, "ServiceLoader found a null SpellEffectComponent implementation.");
                continue;
            }
            String identifier = component.getIdentifier();
            if (identifier == null || identifier.trim().isEmpty()) {
                Gdx.app.error(TAG, "Found SpellEffectComponent implementation " + component.getClass().getName() + " with null or empty identifier. Skipping.");
                continue;
            }
            identifier = identifier.toUpperCase();

            if (componentRegistry.containsKey(identifier)) {
                Gdx.app.error(TAG, "Warning - Duplicate SpellEffectComponent identifier '" + identifier + "' found. Implementation " + component.getClass().getName() + " will be ignored. Existing: " + componentRegistry.get(identifier).getName());
            } else {
                componentRegistry.put(identifier, component.getClass());
                Gdx.app.log(TAG, "Registered SpellEffectComponent type '" + identifier + "' -> " + component.getClass().getName());
            }
        }
        if (componentRegistry.isEmpty()) {
            Gdx.app.log(TAG, "No SpellEffectComponent implementations found via ServiceLoader. Check META-INF/services configuration if components exist.");
        } else {
            Gdx.app.log(TAG, "Registered SpellEffectComponent Identifiers: " + componentRegistry.keySet());
        }
    }

    /**
     * Creates and initializes a SpellEffectComponent instance.
     * @param typeIdentifier The unique identifier for the component type (case-insensitive).
     * @param spell The spell instance this component will belong to.
     * @param params Configuration parameters for the component. Assumed to be Map<String, String>. Can be null.
     * @return A configured SpellEffectComponent instance.
     * @throws IllegalArgumentException if the typeIdentifier is unknown, instantiation fails, or initialization fails.
     */
    public SpellEffectComponent createComponent(String typeIdentifier, Spell spell, Map<String, String> params) {
        if (typeIdentifier == null) {
            throw new IllegalArgumentException("SpellEffectComponent type identifier cannot be null.");
        }
        String upperTypeIdentifier = typeIdentifier.toUpperCase();
        Class<? extends SpellEffectComponent> componentClass = componentRegistry.get(upperTypeIdentifier);

        if (componentClass == null) {
            Gdx.app.error(TAG, "Unknown SpellEffectComponent type requested: '" + typeIdentifier + "'. Available: " + componentRegistry.keySet());
            throw new IllegalArgumentException("Unknown SpellEffectComponent type: '" + typeIdentifier + "'");
        }

        try {
            SpellEffectComponent newComponent = componentClass.getDeclaredConstructor().newInstance();
            Map<String, String> effectiveParams = (params != null) ? params : new HashMap<>();
            newComponent.initialize(spell, effectiveParams);
            Gdx.app.debug(TAG, "Created and initialized component: " + upperTypeIdentifier);
            return newComponent;
        } catch (NoSuchMethodException nsme) {
            Gdx.app.error(TAG, "Error creating component '" + upperTypeIdentifier + "'. Implementation " + componentClass.getName() + " is missing a public no-argument constructor.", nsme);
            throw new IllegalArgumentException("Failed to create component: No-arg constructor missing for " + componentClass.getName(), nsme);
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error creating or initializing component of type '" + upperTypeIdentifier + "'.", e);
            if (e instanceof IllegalArgumentException) { throw (IllegalArgumentException) e; }
            throw new RuntimeException("Failed to create or initialize component of type '" + upperTypeIdentifier + "'. Cause: " + e.getMessage(), e);
        }
    }

    public boolean knowsComponentType(String typeIdentifier) {
        if (typeIdentifier == null) return false;
        return componentRegistry.containsKey(typeIdentifier.toUpperCase());
    }

    public Set<String> getRegisteredComponentTypes() {
        return java.util.Collections.unmodifiableSet(componentRegistry.keySet());
    }
}