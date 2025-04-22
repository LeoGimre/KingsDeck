package io.WizardsChessMaster.presenter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;

import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.model.DeckModel;
import io.WizardsChessMaster.model.pieces.Piece;
import io.WizardsChessMaster.model.pieces.PieceConfig;
import io.WizardsChessMaster.model.pieces.PieceFactory;
import io.WizardsChessMaster.model.pieces.PieceType;
import io.WizardsChessMaster.model.spells.Spell;
import io.WizardsChessMaster.model.spells.SpellConfig;
import io.WizardsChessMaster.model.spells.SpellFactory;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.view.interfaces.IDeckBuildingView;

import java.util.*;

public class DeckBuildingPresenter {

    private static final String TAG = "DeckBuildingCtrl";

    private final Main game;
    private final FirebaseService firebaseService;
    private DeckModel currentDeck;
    private DeckModel savedDeckState;
    // Use interface type
    private final IDeckBuildingView view;

    private List<DeckModel> userDecks;
    private boolean hasUnsavedChanges = false;

    // Store prototypes from the factory
    private final List<Piece> availablePiecePrototypes;
    private final List<Spell> availableSpellPrototypes;


    public DeckBuildingPresenter(Main game, FirebaseService firebaseService, DeckModel deckToEdit, IDeckBuildingView view) {
        this.game = game;
        this.firebaseService = firebaseService;
        this.view = view;
        this.userDecks = new ArrayList<>();
        this.currentDeck = null;
        this.savedDeckState = null;

        this.availablePiecePrototypes = new ArrayList<>(PieceFactory.getPiecePrototypes());
        this.availablePiecePrototypes.removeIf(p -> PieceType.KING.name().equals(p.getTypeName()));
        this.availablePiecePrototypes.sort(Comparator.comparingInt(Piece::getPointValue).thenComparing(Piece::getTypeName));

        this.availableSpellPrototypes = new ArrayList<>(SpellFactory.getSpellPrototypes());
        this.availableSpellPrototypes.sort(Comparator.comparingInt(Spell::getPointCost).thenComparing(Spell::getTypeName));

        String initialDeckNameToLoad = (deckToEdit != null) ? deckToEdit.getName() : null;
        if (initialDeckNameToLoad != null) { Gdx.app.log(TAG, "Controller initialized to edit deck: " + initialDeckNameToLoad); }
        else { Gdx.app.log(TAG, "Controller initialized for new/first deck."); }
    }

    // --- Deck Loading/Saving/Management ---

    private void loadDeckIntoEditor(DeckModel deck) {
        this.currentDeck = deck;
        this.savedDeckState = deepCopyDeck(deck);
        this.hasUnsavedChanges = false;
        if (view != null) {
            if (deck != null) {
                view.updateEditorState(deck);
                view.showStatusMessage("Loaded deck: " + deck.getName(), false);
            } else {
                view.clearEditorState();
                view.showStatusMessage("No deck selected. Create a new one.", false);
            }
            view.showUnsavedChangesIndicator(false);
        }
        Gdx.app.log(TAG, "Loaded deck into editor: " + (deck != null ? deck.getName() : "<None>"));
    }

    private DeckModel deepCopyDeck(DeckModel original) {
        if (original == null) return null;
        DeckModel copy = new DeckModel(original.getName(), original.getPointLimit());
        // Copy piece map
        if (original.getPieceConfiguration() != null) {
            copy.setPieceConfiguration(new HashMap<>(original.getPieceConfiguration()));
        } else {
            copy.setPieceConfiguration(new HashMap<>());
        }
        // Copy spell map
        if (original.getSpellConfiguration() != null) {
            copy.setSpellConfiguration(new HashMap<>(original.getSpellConfiguration()));
        } else {
            copy.setSpellConfiguration(new HashMap<>());
        }
        return copy;
    }


    public boolean checkForUnsavedChanges() {
        // Rely on DeckModel's equals method which now compares both maps
        boolean areEqual = Objects.equals(currentDeck, savedDeckState);
        hasUnsavedChanges = !areEqual;
        if (view != null) {
            view.showUnsavedChangesIndicator(hasUnsavedChanges);
        }
        return hasUnsavedChanges;
    }

    public boolean hasUnsavedChanges() {
        return checkForUnsavedChanges();
    }

    private void markUnsavedChanges() {
        if (!hasUnsavedChanges) {
            Gdx.app.log(TAG, "Deck modified. Unsaved changes present.");
            hasUnsavedChanges = true;
            if (view != null) view.showUnsavedChangesIndicator(true);
        }
        if (view != null) view.updatePointsLabel();
    }

    private void resetUnsavedChangesFlag() {
        hasUnsavedChanges = false;
        savedDeckState = deepCopyDeck(currentDeck);
        if (view != null) view.showUnsavedChangesIndicator(false);
    }


    private void showDiscardChangesDialog(Runnable onConfirm) {
        if (view == null || view.getSkin() == null || view.getStage() == null) {
            Gdx.app.error(TAG, "View/Skin/Stage is null, cannot show discard dialog.");
            onConfirm.run(); return;
        }
        Dialog dialog = new Dialog("Unsaved Changes", view.getSkin(), "dialog") {
            @Override protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) { Gdx.app.log(TAG, "User chose to discard changes."); onConfirm.run(); }
                else { Gdx.app.log(TAG, "User cancelled discarding changes."); }
            }
        };
        dialog.text("You have unsaved changes. Discard them?");
        dialog.button("Yes, Discard", true); dialog.button("No, Cancel", false);
        dialog.key(com.badlogic.gdx.Input.Keys.ENTER, true); dialog.key(com.badlogic.gdx.Input.Keys.ESCAPE, false);
        dialog.show(view.getStage());
    }

    private void executePotentiallyUnsafeAction(Runnable action) {
        if (checkForUnsavedChanges()) { showDiscardChangesDialog(action); }
        else { action.run(); }
    }

    private void initializeNewDeckModel() {
        this.currentDeck = new DeckModel("New Deck", 40);
        int kingDefaultIndex = 4; // e1
        currentDeck.setPieceAt(kingDefaultIndex, PieceType.KING.name());
        Gdx.app.log(TAG, "Initialized new deck model with King at index " + kingDefaultIndex);
    }

    // --- Getters ---
    public DeckModel getCurrentDeck() { return currentDeck; }
    public List<Piece> getAvailablePiecePrototypes() { return availablePiecePrototypes; }
    public List<Spell> getAvailableSpellPrototypes() { return availableSpellPrototypes; }
    public List<DeckModel> getUserDecks() { return userDecks; }

    // --- Event Handlers ---
    public void loadUserDecks() {
        Gdx.app.log(TAG, "Loading user decks...");
        if (view != null) view.showStatusMessage("Loading decks...", false);
        firebaseService.loadDecks(new FirebaseService.DeckLoadListener() {
            @Override
            public void onSuccess(List<DeckModel> decks) {
                Gdx.app.log(TAG, "Successfully loaded " + (decks != null ? decks.size() : 0) + " decks.");
                userDecks = (decks != null) ? decks : new ArrayList<>();
                userDecks.sort(Comparator.comparing(DeckModel::getName, String.CASE_INSENSITIVE_ORDER));

                if (view != null) {
                    view.populateDeckList(userDecks);
                    String deckToSelect = null; DeckModel deckToLoad = null;
                    String targetDeckName = (savedDeckState != null && !savedDeckState.getName().equals("New Deck")) ? savedDeckState.getName() : ((currentDeck != null && !currentDeck.getName().equals("New Deck")) ? currentDeck.getName() : null);
                    if(targetDeckName != null) { for(DeckModel d : userDecks) { if(targetDeckName.equals(d.getName())) { deckToLoad = d; deckToSelect = d.getName(); break; } } }
                    if (deckToLoad == null && !userDecks.isEmpty()) { deckToLoad = userDecks.get(0); deckToSelect = userDecks.get(0).getName(); }
                    if (deckToLoad != null) { loadDeckIntoEditor(deckToLoad); view.selectDeckInList(deckToSelect); }
                    else { initializeNewDeckModel(); loadDeckIntoEditor(currentDeck); view.selectDeckInList(null); }
                    view.showStatusMessage("", false);
                }
            }
            @Override
            public void onFailure(String errorMessage) {
                Gdx.app.error(TAG, "Failed to load decks: " + errorMessage);
                if (view != null) view.showStatusMessage("Error loading decks: " + errorMessage, true);
                initializeNewDeckModel(); loadDeckIntoEditor(currentDeck); userDecks.clear();
                if (view != null) { view.populateDeckList(userDecks); view.selectDeckInList(null); }
            }
        });
    }

    public void handleLoadDeck(String deckName) {
        executePotentiallyUnsafeAction(() -> {
            DeckModel deckToLoad = null;
            if (deckName != null && userDecks != null) { for (DeckModel deck : userDecks) { if (deck != null && deckName.equals(deck.getName())) { deckToLoad = deck; break; } } }
            if (deckToLoad != null) { loadDeckIntoEditor(deckToLoad); }
            else { Gdx.app.error(TAG, "Selected deck not found in userDecks: " + deckName); if (view != null) view.showStatusMessage("Error: Could not find deck '" + deckName + "'.", true); }
        });
    }

    public void handleNewDeck() {
        executePotentiallyUnsafeAction(() -> { initializeNewDeckModel(); loadDeckIntoEditor(currentDeck); if (view != null) view.selectDeckInList(null); });
    }

    public void handleDeleteDeck(String deckName) {
        if (deckName == null || deckName.isEmpty()) { if (view != null) view.showStatusMessage("No deck selected to delete.", true); return; }
        if (view == null || view.getSkin() == null || view.getStage() == null) return;
        Dialog dialog = new Dialog("Delete Deck", view.getSkin(), "dialog") { @Override protected void result(Object object) { if (Boolean.TRUE.equals(object)) { Gdx.app.log(TAG, "Confirmed deletion: " + deckName); performDeleteDeck(deckName); } else { Gdx.app.log(TAG, "Cancelled deletion: " + deckName); } } };
        dialog.text("Delete deck '" + deckName + "'?"); dialog.button("Yes, Delete", true); dialog.button("No, Cancel", false);
        dialog.key(com.badlogic.gdx.Input.Keys.ENTER, true); dialog.key(com.badlogic.gdx.Input.Keys.ESCAPE, false);
        dialog.show(view.getStage());
    }

    private void performDeleteDeck(String deckName) {
        if (view != null) view.showStatusMessage("Deleting deck...", false);
        firebaseService.deleteDeck(deckName, new FirebaseService.DeckDeleteListener() {
            @Override public void onSuccess() { Gdx.app.log(TAG, "Deleted deck: " + deckName); boolean deletedCurrent = (currentDeck != null && deckName.equals(currentDeck.getName())); loadUserDecks(); if (deletedCurrent && view != null) { view.showStatusMessage("Deck '" + deckName + "' deleted.", false); } }
            @Override public void onFailure(String m) { Gdx.app.error(TAG, "Delete failed: " + m); if (view != null) view.showStatusMessage("Delete error: " + m, true); }
        });
    }

    public void handlePointLimitChanged(int selectedLimit) {
        if (currentDeck == null) return;
        if (currentDeck.getPointLimit() != selectedLimit) { currentDeck.setPointLimit(selectedLimit); markUnsavedChanges(); Gdx.app.log(TAG, "Point limit changed to: " + selectedLimit); }
    }

    public void handleDeckNameChanged(String newName) {
        if (currentDeck == null) return;
        if (newName == null) newName = "";
        if (!Objects.equals(currentDeck.getName(), newName.trim())) { currentDeck.setName(newName.trim()); markUnsavedChanges(); Gdx.app.log(TAG, "Deck name changed to: " + currentDeck.getName()); }
    }

    public void handleSaveDeck() {
        if (currentDeck == null) { if (view != null) view.showStatusMessage("No deck loaded.", true); return; }
        String deckName = currentDeck.getName();
        if (deckName == null || deckName.trim().isEmpty()) { if (view != null) view.showStatusMessage("Deck name cannot be empty.", true); return; }
        if (currentDeck.findKingIndex() == -1) { if (view != null) view.showStatusMessage("Deck must contain exactly one King.", true); return; }
        if (currentDeck.getCurrentPoints() > currentDeck.getPointLimit()) { if (view != null) view.showStatusMessage("Deck exceeds point limit.", true); return; }
        String originalName = (savedDeckState != null && !savedDeckState.getName().equals("New Deck")) ? savedDeckState.getName() : null;
        String newName = deckName.trim();
        currentDeck.setName(newName);
        boolean isRenaming = originalName != null && !Objects.equals(originalName, newName);
        boolean nameExistsAsOtherDeck = false;
        if (userDecks != null) { for (DeckModel deck : userDecks) { if (deck != null && deck.getName() != null && newName.equalsIgnoreCase(deck.getName())) { if (!Objects.equals(originalName, deck.getName())) { nameExistsAsOtherDeck = true; break; } } } }
        if (nameExistsAsOtherDeck) { if (view != null) view.showStatusMessage("Deck name '" + newName + "' already exists.", true); return; }
        if (isRenaming && originalName != null) {
            Gdx.app.log(TAG, "Renaming deck: '" + originalName + "' -> '" + newName + "'. Deleting old.");
            if (view != null) view.showStatusMessage("Renaming...", false);
            firebaseService.deleteDeck(originalName, new FirebaseService.DeckDeleteListener() {
                @Override public void onSuccess() { Gdx.app.log(TAG, "Old deck '" + originalName + "' deleted during rename, saving new."); performSave(currentDeck); }
                @Override public void onFailure(String m) { Gdx.app.error(TAG, "Failed delete old deck '" + originalName + "' during rename: " + m); if (view != null) view.showStatusMessage("Rename error (delete step failed): " + m, true); }
            });
        } else { performSave(currentDeck); }
    }

    private void performSave(DeckModel deckToSave) {
        Gdx.app.log(TAG, "Saving deck: " + deckToSave.getName());
        if (view != null) view.showStatusMessage("Saving...", false);
        DeckModel finalDeckToSave = deepCopyDeck(deckToSave);
        if (finalDeckToSave == null || finalDeckToSave.getName() == null || finalDeckToSave.getName().trim().isEmpty()) { Gdx.app.error(TAG, "Attempted to save invalid deck state."); if(view != null) view.showStatusMessage("Save error (invalid deck).", true); return; }
        firebaseService.saveDeck(finalDeckToSave, new FirebaseService.DeckSaveListener() {
            @Override public void onSuccess() { Gdx.app.log(TAG, "Deck '" + finalDeckToSave.getName() + "' saved successfully!"); savedDeckState = finalDeckToSave; hasUnsavedChanges = false; if (view != null) { view.showUnsavedChangesIndicator(false); view.showStatusMessage("Deck saved!", false); } loadUserDecks(); }
            @Override public void onFailure(String m) { Gdx.app.error(TAG, "Save failed: " + m); if (view != null) view.showStatusMessage("Save error: " + m, true); }
        });
    }

    public void handleGoBack() { executePotentiallyUnsafeAction(() -> { Gdx.app.log(TAG, "Navigating back to Main Menu"); game.showMainMenuScreen(); }); }


    // --- Drag and Drop Logic Helpers ---

    /** Validates if a piece *type* can be dropped onto a target piece grid square. */
    public boolean validatePieceDrop(String pieceTypeName, int targetIndex) {
        if (currentDeck == null || pieceTypeName == null || targetIndex < 0 || targetIndex >= DeckModel.PIECE_GRID_SIZE) return false;
        PieceConfig pieceConfig = PieceFactory.getConfig(pieceTypeName);
        if (pieceConfig == null) { Gdx.app.error(TAG, "Validation failed: Unknown piece type name: " + pieceTypeName); return false; }
        String currentPieceOnTargetName = currentDeck.getPieceTypeNameAt(targetIndex);
        if (PieceType.KING.name().equals(currentPieceOnTargetName) && !PieceType.KING.name().equals(pieceTypeName)) { return false; }
        if (PieceType.KING.name().equals(pieceTypeName)) { int existingKingIndex = currentDeck.findKingIndex(); if (existingKingIndex != -1 && existingKingIndex != targetIndex) { return false; } }
        // Use DeckModel's validation method
        if (!currentDeck.canSwapPiece(targetIndex, pieceConfig)) {
            if (view != null) view.showStatusMessage("Cannot place " + pieceConfig.displayName + " - exceeds point limit.", true);
            return false;
        }
        return true;
    }

    /** Validates if a spell *type* can be dropped onto a target spell grid slot. */
    public boolean validateSpellDropOnGrid(String spellTypeName, int targetIndex) {
        if (currentDeck == null || spellTypeName == null || targetIndex < 0 || targetIndex >= DeckModel.SPELL_GRID_SIZE) return false;
        SpellConfig spellConfig = SpellFactory.getConfig(spellTypeName);
        if (spellConfig == null) { Gdx.app.error(TAG, "Validation failed: Unknown spell type name: " + spellTypeName); return false; }
        // Use DeckModel's swap validation for the spell grid
        if (!currentDeck.canSwapSpell(targetIndex, spellConfig)) {
            if (view != null) view.showStatusMessage("Cannot place " + spellConfig.displayName + " - exceeds point limit.", true);
            return false;
        }
        return true;
    }

    // --- Drag and Drop Event Handlers ---

    public void pieceDroppedOrMoved(String pieceTypeName, int targetIndex, int sourceIndex) {
        if (currentDeck == null || pieceTypeName == null || targetIndex < 0 || targetIndex >= DeckModel.PIECE_GRID_SIZE) { Gdx.app.error(TAG, "Invalid state for pieceDroppedOrMoved."); return; }
        Gdx.app.log(TAG, "Piece drop/move: " + pieceTypeName + " to " + targetIndex + " from " + sourceIndex);
        PieceConfig droppedConfig = PieceFactory.getConfig(pieceTypeName);
        if (droppedConfig == null) { Gdx.app.error(TAG, "Invalid drop: Unknown piece type " + pieceTypeName); return; }
        boolean modelChanged = false;
        if (sourceIndex >= 0) {
            String pieceBeingMovedName = currentDeck.getPieceTypeNameAt(sourceIndex);
            String currentPieceOnTargetName = currentDeck.getPieceTypeNameAt(targetIndex);
            if (PieceType.KING.name().equals(currentPieceOnTargetName) && !PieceType.KING.name().equals(pieceTypeName)) { Gdx.app.error(TAG, "Invalid move: Tried to replace King via drag."); return; }
            if(PieceType.KING.name().equals(pieceTypeName) && currentPieceOnTargetName != null) { Gdx.app.error(TAG, "Invalid move: Tried to move King onto occupied square " + targetIndex); return; }
            currentDeck.setPieceAt(sourceIndex, currentPieceOnTargetName);
            modelChanged = currentDeck.setPieceAt(targetIndex, pieceTypeName);
            markUnsavedChanges();
            if (view != null) { view.updateBoardSquare(sourceIndex); view.updateBoardSquare(targetIndex); }
        } else {
            String targetOccupantName = currentDeck.getPieceTypeNameAt(targetIndex);
            if (PieceType.KING.name().equals(targetOccupantName) && !PieceType.KING.name().equals(pieceTypeName)) { Gdx.app.error(TAG,"Invalid drop: Cannot replace King."); return; }
            if (targetOccupantName != null && PieceType.KING.name().equals(pieceTypeName)) { Gdx.app.error(TAG,"Invalid drop: Cannot drop King onto occupied square " + targetIndex); return; }
            if (PieceType.KING.name().equals(pieceTypeName)) {
                int existingKingIndex = currentDeck.findKingIndex();
                if(existingKingIndex != -1 && existingKingIndex != targetIndex) {
                    if (currentDeck.setPieceAt(existingKingIndex, null)) { modelChanged = true; if(view != null) view.updateBoardSquare(existingKingIndex); }
                }
            }
            boolean placedNew = currentDeck.setPieceAt(targetIndex, pieceTypeName);
            if (placedNew || modelChanged) { markUnsavedChanges(); if (view != null) { view.updateBoardSquare(targetIndex); } }
            else { Gdx.app.log(TAG, "Model state did not change after piece drop."); if (view != null) { view.updateBoardSquare(targetIndex); } }
        }
    }

    /** Handles a spell being dropped onto a specific spell grid slot. */
    public void handleSpellDroppedOnGrid(String spellTypeName, int targetIndex) {
        if (currentDeck == null || spellTypeName == null || targetIndex < 0 || targetIndex >= DeckModel.SPELL_GRID_SIZE) { Gdx.app.error(TAG, "Invalid state for handleSpellDroppedOnGrid."); return; }
        Gdx.app.log(TAG, "Spell dropped on grid: " + spellTypeName + " to index " + targetIndex);
        SpellConfig config = SpellFactory.getConfig(spellTypeName);
        if (config == null) { Gdx.app.error(TAG, "Invalid drop: Unknown spell type " + spellTypeName); return; }

        // Use the canSwapSpell validation before attempting set
        if (currentDeck.canSwapSpell(targetIndex, config)) {
            // Use the new setSpellAt method in DeckModel
            boolean changed = currentDeck.setSpellAt(targetIndex, spellTypeName);
            if (changed) {
                markUnsavedChanges();
                if (view != null) {
                    view.updateSpellGridSquare(targetIndex);
                }
            } else {
                Gdx.app.log(TAG, "Model state did not change after spell drop on grid.");
                if (view != null) view.updateSpellGridSquare(targetIndex);
            }
        } else {
            Gdx.app.log(TAG, "Failed to add spell " + spellTypeName + " to grid index " + targetIndex + " (validation failed).");
            if (view != null) view.showStatusMessage("Cannot place " + config.displayName + " - exceeds point limit.", true);
            if (view != null) view.updateSpellGridSquare(targetIndex);
        }
    }

    public void pieceRemoved(int sourceIndex) {
        if (currentDeck == null || sourceIndex < 0 || sourceIndex >= DeckModel.PIECE_GRID_SIZE) return;
        String removedPieceName = currentDeck.getPieceTypeNameAt(sourceIndex);
        Gdx.app.log(TAG, "Attempting piece removal from board: " + removedPieceName + " from index " + sourceIndex);
        if (removedPieceName == null) return;
        if (PieceType.KING.name().equals(removedPieceName)) { if (view != null) view.showStatusMessage("Cannot remove the King.", true); return; }
        boolean changed = currentDeck.setPieceAt(sourceIndex, null);
        if (changed) { markUnsavedChanges(); if (view != null) { view.updateBoardSquare(sourceIndex); } }
    }

    /** Handles a spell being removed from a specific grid slot (e.g., dragged to remove area). */
    public void handleSpellRemovedFromGrid(int sourceIndex) {
        if (currentDeck == null || sourceIndex < 0 || sourceIndex >= DeckModel.SPELL_GRID_SIZE) return;
        String removedSpellName = currentDeck.getSpellTypeNameAt(sourceIndex);
        Gdx.app.log(TAG, "Attempting spell removal from grid index: " + sourceIndex + " (" + removedSpellName + ")");
        if (removedSpellName == null) return;

        // Use setSpellAt with null to clear the slot
        boolean changed = currentDeck.setSpellAt(sourceIndex, null);
        if (changed) {
            markUnsavedChanges();
            if (view != null) {
                view.updateSpellGridSquare(sourceIndex);
            }
        }
    }

}