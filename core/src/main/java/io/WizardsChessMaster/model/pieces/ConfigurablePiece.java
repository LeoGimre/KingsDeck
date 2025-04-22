package io.WizardsChessMaster.model.pieces;

import com.badlogic.gdx.Gdx;
import io.WizardsChessMaster.model.BoardPosition;
import io.WizardsChessMaster.model.GameModel;
import io.WizardsChessMaster.model.Team;
import io.WizardsChessMaster.model.pieces.move.MoveComponent;
import io.WizardsChessMaster.model.pieces.move.MoveComponentFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * A generic Piece implementation driven by external configuration (PieceConfig).
 * Holds state variables (Booleans) and delegates move logic to configured MoveComponents.
 */
public class ConfigurablePiece implements Piece {

    private static final String TAG = "ConfigurablePiece";

    protected Team team;
    protected BoardPosition position;
    protected PieceConfig config;
    protected Map<String, Boolean> stateVariables;
    protected List<MoveComponent> moveComponents;

    // TODO: Replace with injected dependency later
    private static MoveComponentFactory moveComponentFactory = new MoveComponentFactory();

    public ConfigurablePiece() {
        this.stateVariables = new HashMap<>();
        this.moveComponents = new ArrayList<>();
    }

    /**
     * Configures the piece based on loaded data. Called by PieceFactory.
     */
    public void configure(PieceConfig config, Team team, BoardPosition position, MoveComponentFactory compFactory) {
        if (config == null || team == null || position == null || compFactory == null) {
            throw new IllegalArgumentException("Cannot configure piece with null arguments.");
        }
        this.config = config;
        this.team = team;
        this.position = position;

        this.stateVariables.clear();
        if (config.initialState != null) {
            config.initialState.forEach((key, value) -> {
                if (value != null) {
                    this.stateVariables.put(key, (Boolean) value);
                } else {
                    Gdx.app.error(TAG, "Invalid non-boolean value found in initialState for key '" + key + "' in config '" + config.typeName + "'. Ignoring.");
                }
            });
        }

        adjustInitialStateBasedOnPosition(position, team);

        this.moveComponents.clear();
        if (config.moveComponents != null) {
            for (PieceConfig.MoveComponentConfig compConfig : config.moveComponents) {
                try {
                    MoveComponent component = compFactory.createComponent(compConfig.type, this, compConfig.params);
                    this.moveComponents.add(component);
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Failed to create or initialize component type '" + compConfig.type + "' for piece '" + config.typeName + "': " + e.getMessage());
                }
            }
        }
        Gdx.app.debug(TAG, "Configured piece: " + getTypeName() + " at " + position + " with components: " + moveComponents.stream().map(MoveComponent::getIdentifier).collect(Collectors.toList()));
    }

    /** Adjusts the initial 'hasMoved' state based on standard chess starting positions. */
    private void adjustInitialStateBasedOnPosition(BoardPosition startPos, Team startTeam) {
        if (stateVariables.containsKey("hasMoved")) {
            boolean standardStart = false;
            int startRank = (startTeam == Team.WHITE) ? 0 : 7;

            switch (config.typeName) {
                case "KING":
                    standardStart = (startPos.getY() == startRank && startPos.getX() == 4);
                    break;
                case "ROOK":
                    standardStart = (startPos.getY() == startRank && (startPos.getX() == 0 || startPos.getX() == 7));
                    break;
                case "PAWN":
                    standardStart = !Boolean.TRUE.equals(stateVariables.get("hasMoved"));
                    break;
                default:
                    standardStart = !Boolean.TRUE.equals(stateVariables.get("hasMoved"));
            }
            if (!standardStart || Boolean.TRUE.equals(config.initialState.get("hasMoved"))) {
                if (!"PAWN".equals(config.typeName)) {
                    stateVariables.put("hasMoved", true);
                }
            }
        }

        if ("PAWN".equals(config.typeName)) {
            stateVariables.putIfAbsent("justMovedTwoSquares", false);
        } else if (stateVariables.containsKey("justMovedTwoSquares")) {
            stateVariables.put("justMovedTwoSquares", false);
        }
    }


    // --- Piece Interface Implementation ---

    @Override public String getTypeName() { return config != null ? config.typeName : "UNKNOWN"; }
    @Override public Team getTeam() { return team; }
    @Override public BoardPosition getPosition() { return position; }
    @Override public void setPosition(BoardPosition position) { this.position = position; }

    @Override
    public Set<BoardPosition> getValidMoves(GameModel gameModel) {
        Set<BoardPosition> allMoves = new HashSet<>();
        if (moveComponents != null) {
            for (MoveComponent component : moveComponents) {
                try {
                    allMoves.addAll(component.getValidMoves(this, gameModel));
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Error getting moves from component " + component.getIdentifier() + " for piece " + getTypeName(), e);
                }
            }
        }
        return allMoves;
    }

    @Override
    public Set<BoardPosition> getAttackedSquares(GameModel gameModel) {
        Set<BoardPosition> allAttacked = new HashSet<>();
        if (moveComponents != null) {
            for (MoveComponent component : moveComponents) {
                try {
                    allAttacked.addAll(component.getAttackedSquares(this, gameModel));
                } catch (Exception e) {
                    Gdx.app.error(TAG, "Error getting attacked squares from component " + component.getIdentifier() + " for piece " + getTypeName(), e);
                }
            }
        }
        return allAttacked;
    }

    @Override public String getAssetPath() {
        if (config == null || config.assetBaseName == null) return "pieces/white_pawn.png";
        String colorPrefix = (team == Team.BLACK) ? "black_" : "white_";
        return "pieces/" + colorPrefix + config.assetBaseName + ".png";
    }
    @Override public int getPointValue() { return config != null ? config.pointCost : 0; }
    @Override public String getDisplayName() { return config != null ? config.displayName : "Unknown"; }
    @Override public String getDescription() { return config != null ? config.description : ""; }
    @Override public void onCapture(Piece capturedPiece) {
        Gdx.app.debug(TAG, getTypeName() + " at " + getPosition() + " captured " + capturedPiece.getTypeName() + " at " + capturedPiece.getPosition());
    }

    /** Updates the piece's position and modifies relevant state variables (Booleans). */
    @Override
    public void onMove(BoardPosition newPosition) {
        BoardPosition oldPosition = this.position;
        setPosition(newPosition);

        if (stateVariables.containsKey("hasMoved")) {
            stateVariables.put("hasMoved", true);
        }

        if ("PAWN".equals(getTypeName()) && stateVariables.containsKey("justMovedTwoSquares")) {
            if (oldPosition != null) {
                int dy = Math.abs(newPosition.getY() - oldPosition.getY());
                boolean wasFirstMove = dy == 2;
                stateVariables.put("justMovedTwoSquares", wasFirstMove);
            } else {
                stateVariables.put("justMovedTwoSquares", false);
            }
        } else if (stateVariables.containsKey("justMovedTwoSquares")) {
            stateVariables.put("justMovedTwoSquares", false);
        }
    }

    /** Creates a deep copy of this piece, including its configuration, state, and components. */
    @Override
    public Piece copy() {
        if (this.config == null) {
            Gdx.app.error(TAG, "Cannot copy piece - configuration is null!");
            return new ConfigurablePiece();
        }
        ConfigurablePiece newPiece = new ConfigurablePiece();
        newPiece.configure(this.config, this.team, this.position, ConfigurablePiece.moveComponentFactory);
        newPiece.stateVariables = new HashMap<>(this.stateVariables);
        return newPiece;
    }

    // --- State Variable Access ---

    @Override
    public Object getStateVariable(String key) {
        return stateVariables.get(key);
    }

    /** Gets a state variable known to be a Boolean. */
    public Boolean getBooleanStateVariable(String key) {
        return stateVariables.get(key);
    }


    @Override
    public void setStateVariable(String key, Object value) {
        if (key != null) {
            if (value instanceof Boolean) {
                stateVariables.put(key, (Boolean) value);
            } else if (value == null) {
                stateVariables.remove(key);
            } else {
                Gdx.app.error(TAG,"Attempted to set non-Boolean state variable '" + key + "' with type " + value.getClass().getName() + ". Ignoring.");
            }
        }
    }

    /** Sets a Boolean state variable. */
    public void setBooleanStateVariable(String key, Boolean value) {
        if (key != null) {
            if (value == null) {
                stateVariables.remove(key);
            } else {
                stateVariables.put(key, value);
            }
        }
    }


    @Override
    public String toString() {
        return (team != null ? team.name() : "NO_TEAM") + "_" + getTypeName() + " at " + position;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurablePiece that = (ConfigurablePiece) o;
        return team == that.team &&
                Objects.equals(position, that.position) &&
                Objects.equals(config, that.config) &&
                Objects.equals(stateVariables, that.stateVariables);
    }
    @Override
    public int hashCode() {
        return Objects.hash(team, position, config, stateVariables);
    }
}