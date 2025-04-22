package io.WizardsChessMaster.model.pieces.move;

import com.badlogic.gdx.Gdx;
import io.WizardsChessMaster.model.pieces.Piece;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Factory for creating MoveComponent instances based on type names.
 * Uses ServiceLoader to dynamically discover available MoveComponent implementations.
 */
public class MoveComponentFactory {

    private static final String TAG = "MoveComponentFactory";
    private static final Map<String, Class<? extends MoveComponent>> componentRegistry = new HashMap<>();

    // Static initializer to load component implementations using ServiceLoader
    static {
        Gdx.app.log(TAG, "Initializing MoveComponentFactory using ServiceLoader...");
        loadComponents();
        Gdx.app.log(TAG, "MoveComponentFactory initialized. Found " + componentRegistry.size() + " component types.");
    }

    private static void loadComponents() {
        ServiceLoader<MoveComponent> loader = ServiceLoader.load(MoveComponent.class);
        componentRegistry.clear();

        for (MoveComponent component : loader) {
            if (component == null) {
                Gdx.app.error(TAG, "ServiceLoader found a null MoveComponent implementation.");
                continue;
            }
            String identifier = component.getIdentifier();
            if (identifier == null || identifier.trim().isEmpty()) {
                Gdx.app.error(TAG, "Found MoveComponent implementation " + component.getClass().getName() + " with null or empty identifier. Skipping.");
                continue;
            }
            identifier = identifier.toUpperCase();

            if (componentRegistry.containsKey(identifier)) {
                Gdx.app.error(TAG, "Warning - Duplicate MoveComponent identifier '" + identifier + "' found. Implementation " + component.getClass().getName() + " will be ignored. Existing: " + componentRegistry.get(identifier).getName());
            } else {
                componentRegistry.put(identifier, component.getClass());
                Gdx.app.log(TAG, "Registered MoveComponent type '" + identifier + "' -> " + component.getClass().getName());
            }
        }
        // Log registered components for debugging
        if (componentRegistry.isEmpty()) {
            Gdx.app.error(TAG, "No MoveComponent implementations found via ServiceLoader! Check META-INF/services configuration.");
        } else {
            Gdx.app.log(TAG, "Registered Component Identifiers: " + componentRegistry.keySet());
        }
    }

    /**
     * Creates and initializes a MoveComponent instance.
     *
     * @param typeIdentifier The unique identifier for the component type (case-insensitive, matches config 'type').
     * @param piece The piece this component will belong to.
     * @param params Configuration parameters (Integers) for the component (can be null).
     * @return A configured MoveComponent instance.
     * @throws IllegalArgumentException if the typeIdentifier is unknown, instantiation fails, or initialization fails.
     */
    public MoveComponent createComponent(String typeIdentifier, Piece piece, Map<String, Integer> params) {
        if (typeIdentifier == null) {
            throw new IllegalArgumentException("MoveComponent type identifier cannot be null.");
        }
        String upperTypeIdentifier = typeIdentifier.toUpperCase();
        Class<? extends MoveComponent> componentClass = componentRegistry.get(upperTypeIdentifier);

        if (componentClass == null) {
            Gdx.app.error(TAG, "Unknown MoveComponent type requested: '" + typeIdentifier + "'. Available: " + componentRegistry.keySet());
            throw new IllegalArgumentException("Unknown MoveComponent type: '" + typeIdentifier + "'");
        }

        try {
            // Instantiate using the public no-arg constructor (required for ServiceLoader)
            MoveComponent newComponent = componentClass.getDeclaredConstructor().newInstance();

            // Initialize the component with parameters
            Map<String, Integer> effectiveParams = (params != null) ? params : new HashMap<>();
            newComponent.initialize(piece, effectiveParams);

            Gdx.app.debug(TAG, "Created and initialized component: " + upperTypeIdentifier);
            return newComponent;
        } catch (NoSuchMethodException nsme) {
            Gdx.app.error(TAG, "Error creating component '" + upperTypeIdentifier + "'. Implementation " + componentClass.getName() + " is missing a public no-argument constructor.", nsme);
            throw new IllegalArgumentException("Failed to create component: No-arg constructor missing for " + componentClass.getName(), nsme);
        } catch (Exception e) {
            Gdx.app.error(TAG, "Error creating or initializing component of type '" + upperTypeIdentifier + "'.", e);
            // Catch initialization errors as IllegalArgumentException
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            // Wrap other exceptions
            throw new RuntimeException("Failed to create or initialize component of type '" + upperTypeIdentifier + "'. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a component type identifier is registered.
     * @param typeIdentifier The identifier to check (case-insensitive).
     * @return true if the identifier is known, false otherwise.
     */
    public boolean knowsComponentType(String typeIdentifier) {
        if (typeIdentifier == null) return false;
        return componentRegistry.containsKey(typeIdentifier.toUpperCase());
    }

    /**
     * Gets the registered component types.
     * @return A set of known component type identifiers
     */
    public Set<String> getRegisteredComponentTypes() {
        return java.util.Collections.unmodifiableSet(componentRegistry.keySet());
    }
}
