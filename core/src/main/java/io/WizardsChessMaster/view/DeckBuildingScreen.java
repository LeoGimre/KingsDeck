package io.WizardsChessMaster.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import io.WizardsChessMaster.Main;
import io.WizardsChessMaster.config.ConfigLoader;
import io.WizardsChessMaster.config.GameSettings;
import io.WizardsChessMaster.presenter.DeckBuildingPresenter;
import io.WizardsChessMaster.service.FirebaseService;
import io.WizardsChessMaster.model.DeckModel;
import io.WizardsChessMaster.model.pieces.Piece;
import io.WizardsChessMaster.model.pieces.PieceConfig;
import io.WizardsChessMaster.model.pieces.PieceFactory;
import io.WizardsChessMaster.model.pieces.PieceType;
import io.WizardsChessMaster.model.spells.Spell;
import io.WizardsChessMaster.model.spells.SpellFactory;
import io.WizardsChessMaster.view.interfaces.IDeckBuildingView;

public class DeckBuildingScreen extends ScreenAdapter implements Disposable, IDeckBuildingView {

    private static final String TAG = "DeckBuildingScreen";

    // Inner Classes for Drag & Drop Payloads
    public static class PieceDragPayload { String pieceTypeName; int sourceIndex; public PieceDragPayload(String n, int i) {pieceTypeName=n;sourceIndex=i;} }
    public static class SpellDragPayload { String spellTypeName; boolean fromDeck; int sourceIndex; public SpellDragPayload(String n, boolean f, int i) {spellTypeName=n;fromDeck=f;sourceIndex=i;} }

    // Core Components
    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final DeckBuildingPresenter controller;
    private final DragAndDrop dragAndDrop;
    private final GameSettings settings;

    // UI Elements
    private TextField deckNameField;
    private SelectBox<Integer> pointLimitSelectBox;
    private Label currentPointsLabel, statusMessageLabel, unsavedChangesIndicator;
    private TextButton saveButton, backButton, newDeckButton, deleteDeckButton;
    private com.badlogic.gdx.scenes.scene2d.ui.List<String> deckListWidget;
    private ScrollPane deckListScrollPane, pieceListScrollPane, spellListScrollPane;
    private Table boardTable, pieceListTable, spellListTable, spellGridTable;
    private final Array<Container<Image>> boardSquareContainers = new Array<>(DeckModel.PIECE_GRID_SIZE);
    private final Array<Container<Image>> spellSquareContainers = new Array<>(DeckModel.SPELL_GRID_SIZE);
    private Label currentSpellsLabel;
    private Container<Label> removeArea;

    // Internal State
    private final ObjectMap<String, Texture> loadedTextures = new ObjectMap<>();
    private Timer.Task clearStatusTask = null;
    private ChangeListener pointLimitListener;

    public DeckBuildingScreen(Main game, FirebaseService firebaseService, DeckModel deckToEdit, Skin sharedSkin) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.settings = ConfigLoader.getSettings();

        if (sharedSkin == null) {
            Gdx.app.error(TAG, "Error: Shared skin is null!");
            throw new RuntimeException("Shared skin cannot be null for DeckBuildingScreen");
        }
        this.skin = sharedSkin;

        this.dragAndDrop = new DragAndDrop(); this.dragAndDrop.setDragTime(150);
        this.controller = new DeckBuildingPresenter(game, firebaseService, deckToEdit, this);
        setupUi();
        setupDragAndDropTargets();
    }


    @Override public Skin getSkin() { return skin; }
    @Override public Stage getStage() { return stage; }

    // Texture Loading
    private Texture createErrorPlaceholderTexture(String path) { Gdx.app.error(TAG, "Creating placeholder texture for path: " + path); Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888); pixmap.setColor(Color.PURPLE); pixmap.fill(); pixmap.setColor(Color.YELLOW); pixmap.drawRectangle(0, 0, 64, 64); Texture texture = new Texture(pixmap); pixmap.dispose(); return texture; }
    private Texture getOrLoadTexture(String assetPath) { if (assetPath == null || assetPath.isEmpty()) { return createErrorPlaceholderTexture("Null/Empty Path"); } if (loadedTextures.containsKey(assetPath)) { return loadedTextures.get(assetPath); } Texture texture = null; try { FileHandle fh = Gdx.files.internal(assetPath); if (fh.exists()) { texture = new Texture(fh); } else { Gdx.app.error(TAG, "Texture file not found: " + assetPath); texture = createErrorPlaceholderTexture(assetPath); } } catch (Exception e) { Gdx.app.error(TAG, "Error loading texture: " + assetPath, e); texture = createErrorPlaceholderTexture(assetPath); } loadedTextures.put(assetPath, texture); return texture; }

    // UI Setup
    private void setupUi() {
        Table mainTable = new Table(skin); mainTable.setFillParent(true); stage.addActor(mainTable);
        // Left Panel
        Table leftPanel = new Table(skin); leftPanel.top().pad(10); Label dListLbl = new Label("Your Decks", skin); deckListWidget = new com.badlogic.gdx.scenes.scene2d.ui.List<>(skin); deckListScrollPane = new ScrollPane(deckListWidget, skin); deckListScrollPane.setFadeScrollBars(false); deckListScrollPane.setForceScroll(false, true); Table dBtns = new Table(skin); newDeckButton = new TextButton("New", skin); deleteDeckButton = new TextButton("Delete", skin); dBtns.add(newDeckButton).pad(5).growX().row(); dBtns.add(deleteDeckButton).pad(5).growX(); leftPanel.add(dListLbl).top().row(); leftPanel.add(deckListScrollPane).expandY().fillX().minHeight(200).padBottom(10).row(); leftPanel.add(dBtns).bottom().fillX();
        // Right Panel
        Table editorPanel = new Table(skin);
        // Top Controls
        Table topControls = new Table(skin); deckNameField = new TextField("", skin); unsavedChangesIndicator = new Label("", skin); unsavedChangesIndicator.setColor(Color.YELLOW); pointLimitSelectBox = new SelectBox<>(skin);
        pointLimitSelectBox.setItems(new Array<>(settings.deckBuilding.pointLimits.toArray(new Integer[0])));
        currentPointsLabel = new Label("Points: 0 / 40", skin); saveButton = new TextButton("Save Deck", skin); backButton = new TextButton("Back", skin); statusMessageLabel = new Label("", skin); statusMessageLabel.setWrap(true); statusMessageLabel.setAlignment(Align.center); Table nameTbl = new Table(skin); nameTbl.add("Deck Name:").padRight(5); nameTbl.add(deckNameField).width(200); nameTbl.add(unsavedChangesIndicator).padLeft(5); topControls.add(nameTbl).padRight(10); topControls.add("Limit:").padRight(5); topControls.add(pointLimitSelectBox).padRight(10); topControls.add(currentPointsLabel).width(120).padRight(10); topControls.add(saveButton).padRight(5); topControls.add(backButton); topControls.row(); topControls.add(statusMessageLabel).colspan(6).center().growX().padTop(5).minHeight(20);
        // Board and Current Spells Area
        boardTable = new Table(skin); float scrH = Gdx.graphics.getHeight(); float pSqrSz = scrH * 0.09f; pSqrSz = Math.max(50f, Math.min(100f, pSqrSz)); boardSquareContainers.clear(); for (int i=0; i<DeckModel.PIECE_GRID_SIZE; i++) { Container<Image> sqrC = new Container<>(); sqrC.setTouchable(Touchable.enabled); Drawable sqrBg = skin.getDrawable("textfield"); if (sqrBg == null) sqrBg = skin.newDrawable("white", Color.DARK_GRAY); sqrC.setBackground(sqrBg); sqrC.minSize(pSqrSz, pSqrSz); boardSquareContainers.add(sqrC); boardTable.add(sqrC).size(pSqrSz).pad(1); if ((i+1)%8==0) boardTable.row(); }
        spellGridTable = new Table(skin); spellSquareContainers.clear(); float spSqrSz = scrH * 0.07f; spSqrSz = Math.max(40f, Math.min(80f, spSqrSz));
        for (int i = 0; i < DeckModel.SPELL_GRID_SIZE; i++) {
            Container<Image> spellContainer = new Container<>(); spellContainer.setTouchable(Touchable.enabled);
            Drawable spellBg = skin.getDrawable("textfield");
            if (spellBg == null) spellBg = skin.newDrawable("white", Color.SLATE);
            spellContainer.setBackground(spellBg);
            spellContainer.minSize(spSqrSz, spSqrSz); spellSquareContainers.add(spellContainer); spellGridTable.add(spellContainer).size(spSqrSz).pad(2);
        }
        currentSpellsLabel = new Label("Equipped Spells", skin);
        Table boardAndSpellsArea = new Table(skin); boardAndSpellsArea.add(boardTable).expandY().center().pad(5).row(); boardAndSpellsArea.add(currentSpellsLabel).padTop(10).row(); boardAndSpellsArea.add(spellGridTable).pad(5);
        // Available Items Area
        pieceListTable = new Table(skin); pieceListTable.top().left(); pieceListScrollPane = new ScrollPane(pieceListTable, skin); pieceListScrollPane.setFadeScrollBars(false); pieceListScrollPane.setScrollingDisabled(true, false); spellListTable = new Table(skin); spellListTable.top().left(); spellListScrollPane = new ScrollPane(spellListTable, skin); spellListScrollPane.setFadeScrollBars(false); spellListScrollPane.setScrollingDisabled(true, false); Table availableItemsTbl = new Table(skin); availableItemsTbl.add(new Label("Available Pieces", skin)).left().padBottom(5).row(); availableItemsTbl.add(pieceListScrollPane).growX().height(Value.percentHeight(0.5f, availableItemsTbl)).fillX().row(); availableItemsTbl.add(new Label("Available Spells", skin)).left().padTop(10).padBottom(5).row(); availableItemsTbl.add(spellListScrollPane).growX().height(Value.percentHeight(0.5f, availableItemsTbl)).fillX();
        // Remove Area
        Label removeLbl = new Label("Drag piece/spell here to remove", skin); removeLbl.setAlignment(Align.center); removeArea = new Container<>(removeLbl); Drawable rmvBg = skin.getDrawable("buttonDown"); if (rmvBg == null) rmvBg = skin.newDrawable("white", Color.FIREBRICK); removeArea.setBackground(rmvBg); removeArea.fill();
        // Add Sections to Editor Panel
        editorPanel.add(topControls).expandX().fillX().pad(5).colspan(2).row(); editorPanel.add(boardAndSpellsArea).grow().row(); editorPanel.add(availableItemsTbl).expandX().fillX().height(Value.percentHeight(0.20f, editorPanel)).pad(5).colspan(2).row(); editorPanel.add(removeArea).expandX().fillX().height(Value.percentHeight(0.08f, editorPanel)).pad(5).colspan(2);
        // Add Panels to Main Table
        mainTable.add(leftPanel).expandY().fillY().width(Value.percentWidth(0.2f, mainTable)).pad(5); mainTable.add(editorPanel).expand().fill().pad(5);
        addListeners();
    }

    // --- Listeners ---
    private void addListeners() { newDeckButton.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){if(controller!=null)controller.handleNewDeck();}}); deleteDeckButton.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){String s=deckListWidget.getSelected();if(s!=null&&controller!=null)controller.handleDeleteDeck(s);else if(controller!=null)showStatusMessage("Select deck.", true);}}); deckListWidget.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){String s=deckListWidget.getSelected();if(s!=null&&controller!=null&&controller.getCurrentDeck()!=null&&!s.equals(controller.getCurrentDeck().getName()))controller.handleLoadDeck(s);else if(s!=null&&controller!=null&&controller.getCurrentDeck()==null)controller.handleLoadDeck(s);}}); pointLimitListener=new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){if(controller!=null&&pointLimitSelectBox.getSelected()!=null)controller.handlePointLimitChanged(pointLimitSelectBox.getSelected());}}; pointLimitSelectBox.addListener(pointLimitListener); deckNameField.addListener(new FocusListener(){@Override public void keyboardFocusChanged(FocusEvent e,Actor a,boolean f){if(!f&&controller!=null){controller.handleDeckNameChanged(deckNameField.getText());controller.checkForUnsavedChanges();}}}); deckNameField.setTextFieldListener((tf,k)->{if((k=='\n'||k=='\r')&&controller!=null){controller.handleDeckNameChanged(tf.getText());controller.checkForUnsavedChanges();stage.setKeyboardFocus(null);}}); saveButton.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){if(controller!=null)controller.handleDeckNameChanged(deckNameField.getText());if(controller!=null)controller.handleSaveDeck();}}); backButton.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){if(controller!=null)controller.handleGoBack();}}); }
    // --- List Population ---
    private void populatePieceList(){if(pieceListTable==null||controller==null)return;pieceListTable.clearChildren();float iS=Gdx.graphics.getHeight()*0.06f;iS=Math.max(35f,Math.min(70f,iS));int iPR=14;int cI=0;Collection<Piece> p=controller.getAvailablePiecePrototypes();for(Piece proto:p){if(proto==null)continue;String aP=proto.getAssetPath();Texture t=getOrLoadTexture(aP);if(t==null){Gdx.app.error(TAG,"Missing texture for prototype: "+proto.getTypeName());continue;}Image pI=new Image(t);pI.setUserObject(proto.getTypeName());pI.addListener(new TextTooltip(String.format(Locale.US,"%s (%d pts)\n%s",proto.getDisplayName(),proto.getPointValue(),proto.getDescription()),skin));pieceListTable.add(pI).size(iS).pad(3);cI++;if(cI%iPR==0)pieceListTable.row();}setupPieceListDragSources();}
    private void populateSpellList(){if(spellListTable==null||controller==null)return;spellListTable.clearChildren();float iS=Gdx.graphics.getHeight()*0.06f;iS=Math.max(35f,Math.min(70f,iS));int iPR=14;int cI=0;Collection<Spell> p=controller.getAvailableSpellPrototypes();for(Spell proto:p){if(proto==null)continue;String aP=proto.getIconPath();Texture t=getOrLoadTexture(aP);if(t==null){Gdx.app.error(TAG,"Missing texture for spell prototype: "+proto.getTypeName());continue;}Image sI=new Image(t);sI.setUserObject(proto.getTypeName());sI.addListener(new TextTooltip(String.format(Locale.US,"%s (%d pts)\n%s",proto.getDisplayName(),proto.getPointCost(),proto.getDescription()),skin));spellListTable.add(sI).size(iS).pad(3);cI++;if(cI%iPR==0)spellListTable.row();}setupSpellListDragSources();}

    // --- Grid Update ---
    @Override
    public void updateSpellGridSquare(int index) {
        if (index < 0 || index >= spellSquareContainers.size) return;
        Container<Image> container = spellSquareContainers.get(index);
        if (container == null || controller == null) return;
        DeckModel currentDeck = controller.getCurrentDeck();
        String spellTypeName = (currentDeck != null) ? currentDeck.getSpellTypeNameAt(index) : null;
        Actor existingActor = container.getActor();
        if (existingActor != null) { container.setActor(null); }
        Drawable defaultBg = skin.getDrawable("textfield");
        if (defaultBg == null) defaultBg = skin.newDrawable("white", Color.SLATE);
        container.setBackground(defaultBg);

        if (spellTypeName != null) {
            Spell spellProto = SpellFactory.getPrototype(spellTypeName);
            if (spellProto == null) { Gdx.app.error(TAG, "Cannot find prototype for spell: " + spellTypeName + " at index " + index); return; }
            String assetPath = spellProto.getIconPath();
            Texture texture = getOrLoadTexture(assetPath);
            if (texture != null) {
                Image spellImage = new Image(texture);
                spellImage.setUserObject(spellTypeName);
                spellImage.setTouchable(Touchable.enabled);
                spellImage.addListener(new TextTooltip(String.format(Locale.US, "%s (%d pts)\n%s", spellProto.getDisplayName(), spellProto.getPointCost(), spellProto.getDescription()), skin));
                container.setActor(spellImage);
                final int sourceIndex = index;
                final String currentSpellName = spellTypeName;
                dragAndDrop.addSource(new DragAndDrop.Source(spellImage) {
                    @Override
                    public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                        DragAndDrop.Payload payload = new DragAndDrop.Payload();
                        payload.setObject(new SpellDragPayload(currentSpellName, true, sourceIndex));
                        Spell proto = SpellFactory.getPrototype(currentSpellName);
                        Texture dragTexture = (proto != null) ? getOrLoadTexture(proto.getIconPath()) : null;
                        if (dragTexture == null) return null;
                        Image dragActor = new Image(dragTexture); dragActor.setSize(40, 40);
                        payload.setDragActor(dragActor); getActor().setVisible(false); return payload;
                    }
                    @Override
                    public void dragStop(InputEvent event, float x, float y, int pointer, DragAndDrop.Payload payload, DragAndDrop.Target target) {
                        if (getActor() != null) getActor().setVisible(true);
                    }
                });
            } else { Gdx.app.error(TAG, "Missing texture for spell type in grid: " + spellTypeName); }
        }
    }

    // --- Drag and Drop Source Setup ---
    private void setupPieceListDragSources() { if(pieceListTable==null||dragAndDrop==null||controller==null)return; for(Actor a:pieceListTable.getChildren()){if(a instanceof Image&&a.getUserObject() instanceof String){final String t=(String)a.getUserObject();final Piece p=PieceFactory.getPrototype(t);if(p==null){Gdx.app.error(TAG,"Cannot setup drag source: Prototype null for "+t);continue;}dragAndDrop.addSource(new DragAndDrop.Source(a){@Override public DragAndDrop.Payload dragStart(InputEvent e,float x,float y,int pt){DeckModel c=controller.getCurrentDeck();if(c==null)return null;PieceConfig cfg=PieceFactory.getConfig(t);if(cfg==null){Gdx.app.error(TAG,"DragStart failed: No config for "+t);return null;}if(!c.canAddPiece(cfg)){showStatusMessage("Cannot add "+cfg.displayName+" - exceeds point limit", true);return null;}DragAndDrop.Payload pl=new DragAndDrop.Payload();pl.setObject(new PieceDragPayload(t,-1));Texture dtx=getOrLoadTexture(p.getAssetPath());if(dtx==null)return null;Image di=new Image(dtx);di.setSize(50,50);pl.setDragActor(di);return pl;}@Override public void dragStop(InputEvent e,float x,float y,int pt,DragAndDrop.Payload pl,DragAndDrop.Target trg){}});}}}
    private void setupSpellListDragSources() { if(spellListTable==null||dragAndDrop==null||controller==null)return; for(Actor a:spellListTable.getChildren()){if(a instanceof Image&&a.getUserObject() instanceof String){final String t=(String)a.getUserObject();final Spell p=SpellFactory.getPrototype(t);if(p==null){Gdx.app.error(TAG,"Cannot setup drag source: Prototype null for "+t);continue;}dragAndDrop.addSource(new DragAndDrop.Source(a){@Override public DragAndDrop.Payload dragStart(InputEvent e,float x,float y,int pt){/*Validation check here tricky, rely on target validation.*/DragAndDrop.Payload pl=new DragAndDrop.Payload();pl.setObject(new SpellDragPayload(t,false,-1));Texture dtx=getOrLoadTexture(p.getIconPath());if(dtx==null)return null;Image di=new Image(dtx);di.setSize(40,40);pl.setDragActor(di);return pl;}@Override public void dragStop(InputEvent e,float x,float y,int pt,DragAndDrop.Payload pl,DragAndDrop.Target trg){}});}}}

    // --- Drag and Drop Target Setup ---
    private void setupDragAndDropTargets() { if(dragAndDrop==null){Gdx.app.error(TAG,"DragAndDrop is null during target setup!");return;}Gdx.app.debug(TAG,"Setting up D&D targets...");for(int i=0; i<boardSquareContainers.size; i++){final int index=i; final Container<Image> targetContainer=boardSquareContainers.get(i); if(targetContainer==null){Gdx.app.error(TAG,"Board square container at index "+index+" is null!");continue;} dragAndDrop.addTarget(new DragAndDrop.Target(targetContainer){final Drawable vtBg=skin.getDrawable("selection");final Drawable ivBg=skin.newDrawable("white",Color.FIREBRICK);Drawable dfBg=targetContainer.getBackground();@Override public boolean drag(DragAndDrop.Source s,DragAndDrop.Payload p,float x,float y,int ptr){if(dfBg==null)dfBg=skin.getDrawable("textfield");targetContainer.setBackground(dfBg);if(!(p.getObject() instanceof PieceDragPayload))return false;PieceDragPayload d=(PieceDragPayload)p.getObject();boolean v=(controller!=null)&&controller.validatePieceDrop(d.pieceTypeName,index);targetContainer.setBackground(v?vtBg:ivBg);return v;}@Override public void reset(DragAndDrop.Source s,DragAndDrop.Payload p){if(dfBg==null)dfBg=skin.getDrawable("textfield");targetContainer.setBackground(dfBg);}@Override public void drop(DragAndDrop.Source s,DragAndDrop.Payload p,float x,float y,int ptr){if(p.getObject() instanceof PieceDragPayload&&controller!=null){PieceDragPayload d=(PieceDragPayload)p.getObject();controller.pieceDroppedOrMoved(d.pieceTypeName,index,d.sourceIndex);}}});} for (int i=0; i<spellSquareContainers.size; i++){final int index=i; final Container<Image> targetContainer=spellSquareContainers.get(i); if(targetContainer==null){Gdx.app.error(TAG,"Spell square container at index "+index+" is null!");continue;} dragAndDrop.addTarget(new DragAndDrop.Target(targetContainer){final Drawable vtBg=skin.getDrawable("selection");final Drawable ivBg=skin.newDrawable("white",Color.FIREBRICK);Drawable dfBg=targetContainer.getBackground();@Override public boolean drag(DragAndDrop.Source s,DragAndDrop.Payload p,float x,float y,int ptr){if(dfBg==null)dfBg=skin.getDrawable("textfield");targetContainer.setBackground(dfBg);if(!(p.getObject() instanceof SpellDragPayload))return false;SpellDragPayload sd=(SpellDragPayload)p.getObject();if(sd.fromDeck)return false;boolean v=controller!=null&&controller.validateSpellDropOnGrid(sd.spellTypeName,index);targetContainer.setBackground(v?vtBg:ivBg);return v;}@Override public void reset(DragAndDrop.Source s,DragAndDrop.Payload p){if(dfBg==null)dfBg=skin.getDrawable("textfield");targetContainer.setBackground(dfBg);}@Override public void drop(DragAndDrop.Source s,DragAndDrop.Payload p,float x,float y,int ptr){if(p.getObject() instanceof SpellDragPayload&&controller!=null){SpellDragPayload sd=(SpellDragPayload)p.getObject();if(!sd.fromDeck){controller.handleSpellDroppedOnGrid(sd.spellTypeName,index);}}}}); }Gdx.app.debug(TAG,"Spell grid targets setup complete.");if(removeArea!=null){final Container<Label> fRA=removeArea; final Drawable oBg=fRA.getBackground(); final Drawable hBg=skin.newDrawable("white",Color.ORANGE); dragAndDrop.addTarget(new DragAndDrop.Target(removeArea){@Override public boolean drag(DragAndDrop.Source s,DragAndDrop.Payload p,float x,float y,int ptr){boolean cR=false;if(p.getObject() instanceof PieceDragPayload){PieceDragPayload pd=(PieceDragPayload)p.getObject();cR=pd.sourceIndex>=0&&!PieceType.KING.name().equals(pd.pieceTypeName);}else if(p.getObject() instanceof SpellDragPayload){SpellDragPayload sd=(SpellDragPayload)p.getObject();cR=sd.fromDeck;}fRA.setBackground(cR?hBg:oBg);return cR;}@Override public void reset(DragAndDrop.Source s,DragAndDrop.Payload p){fRA.setBackground(oBg);}@Override public void drop(DragAndDrop.Source s,DragAndDrop.Payload p,float x,float y,int ptr){if(controller!=null){if(p.getObject() instanceof PieceDragPayload){PieceDragPayload pd=(PieceDragPayload)p.getObject();if(pd.sourceIndex>=0&&!PieceType.KING.name().equals(pd.pieceTypeName)){controller.pieceRemoved(pd.sourceIndex);}}else if(p.getObject() instanceof SpellDragPayload){SpellDragPayload sd=(SpellDragPayload)p.getObject();if(sd.fromDeck){controller.handleSpellRemovedFromGrid(sd.sourceIndex);}}}fRA.setBackground(oBg);}}); } else { Gdx.app.error(TAG, "RemoveArea container is null during D&D target setup!"); }}

    // --- Board/UI Update Methods ---
    @Override public void updateBoardSquare(int index) { if(index<0||index>=boardSquareContainers.size)return; Container<Image> c=boardSquareContainers.get(index);if(c==null||controller==null)return; DeckModel d=controller.getCurrentDeck();String pName=(d!=null)?d.getPieceTypeNameAt(index):null;Actor ex=c.getActor();if(ex!=null){c.setActor(null);}c.setBackground(skin.getDrawable("textfield"));if(pName!=null){Piece pProto=PieceFactory.getPrototype(pName);if(pProto==null){Gdx.app.error(TAG,"Cannot find prototype for type: "+pName+" at index "+index);return;}String aPath=pProto.getAssetPath();Texture tex=getOrLoadTexture(aPath);if(tex!=null){Image pImg=new Image(tex);pImg.setUserObject(pName);pImg.setTouchable(Touchable.enabled);pImg.addListener(new TextTooltip(String.format(Locale.US,"%s (%d pts)\n%s",pProto.getDisplayName(),pProto.getPointValue(),pProto.getDescription()),skin));c.setActor(pImg);final int sIdx=index;final String cPName=pName;dragAndDrop.addSource(new DragAndDrop.Source(pImg){@Override public DragAndDrop.Payload dragStart(InputEvent ev,float x,float y,int pt){DragAndDrop.Payload pl=new DragAndDrop.Payload();pl.setObject(new PieceDragPayload(cPName,sIdx));Piece dProto=PieceFactory.getPrototype(cPName);String dAPath=(dProto!=null)?dProto.getAssetPath():null;Texture dTex=getOrLoadTexture(dAPath);if(dTex==null)return null;Image dAct=new Image(dTex);dAct.setSize(50,50);pl.setDragActor(dAct);getActor().setVisible(false);return pl;}@Override public void dragStop(InputEvent ev,float x,float y,int pt,DragAndDrop.Payload pl,DragAndDrop.Target trg){if(getActor()!=null)getActor().setVisible(true);}}); }else{Gdx.app.error(TAG,"Missing texture for piece type on board: "+pName);}}}
    private void initializeBoardState() { Gdx.app.log(TAG, "Initializing board state and D&D..."); if (dragAndDrop != null) { dragAndDrop.clear(); } else { Gdx.app.error(TAG, "DragAndDrop was null during initializeBoardState!"); } populatePieceList(); populateSpellList(); for (int i = 0; i < DeckModel.PIECE_GRID_SIZE; i++) { updateBoardSquare(i); } for (int i = 0; i < DeckModel.SPELL_GRID_SIZE; i++) { updateSpellGridSquare(i); } setupDragAndDropTargets(); Gdx.app.log(TAG, "Board and Spell states updated, D&D reinitialized."); }
    @Override public void updateEditorState(DeckModel deck) { if(deck==null){clearEditorState();return;}Gdx.app.log(TAG,"Updating editor fields for deck: "+deck.getName());resetDeckNameField(deck.getName());resetPointLimitSelector(deck.getPointLimit());if(deckNameField!=null)deckNameField.setDisabled(false);if(pointLimitSelectBox!=null)pointLimitSelectBox.setDisabled(false);if(saveButton!=null)saveButton.setDisabled(false);updatePointsLabel();initializeBoardState();}
    @Override public void clearEditorState() { Gdx.app.log(TAG,"Clearing editor state.");resetDeckNameField("New Deck");resetPointLimitSelector(settings.deckBuilding.defaultPointLimit);if(deckNameField!=null)deckNameField.setDisabled(false);if(pointLimitSelectBox!=null)pointLimitSelectBox.setDisabled(false);if(saveButton!=null)saveButton.setDisabled(false);updatePointsLabel();initializeBoardState();showUnsavedChangesIndicator(false);}
    @Override public void updatePointsLabel() { DeckModel c=(controller!=null)?controller.getCurrentDeck():null;if(currentPointsLabel!=null&&c!=null){int cp=c.getCurrentPoints();int l=c.getPointLimit();currentPointsLabel.setText(String.format(Locale.US,"Points: %d / %d",cp,l));currentPointsLabel.setColor(cp>l?Color.RED:Color.WHITE);}else if(currentPointsLabel!=null){int dl=settings.deckBuilding.defaultPointLimit;if(pointLimitSelectBox!=null&&pointLimitSelectBox.getSelected()!=null)dl=pointLimitSelectBox.getSelected();currentPointsLabel.setText(String.format(Locale.US,"Points: 0 / %d",dl));currentPointsLabel.setColor(Color.WHITE);}}
    @Override public void resetPointLimitSelector(int limit) { if(pointLimitSelectBox!=null&&pointLimitListener!=null){pointLimitSelectBox.removeListener(pointLimitListener);Integer lObj=limit;Array<Integer> itms=pointLimitSelectBox.getItems();if(!itms.contains(lObj,false)){Gdx.app.log(TAG,"Warning: Invalid point limit ("+limit+") in model, defaulting selector to "+itms.first());lObj=itms.first();}pointLimitSelectBox.setSelected(lObj);pointLimitSelectBox.addListener(pointLimitListener);}else{Gdx.app.error(TAG,"Cannot reset point limit selector - SelectBox or Listener is null.");} }
    @Override public void resetDeckNameField(String name) { if(deckNameField!=null){deckNameField.setText(name!=null?name:"");} }
    @Override public void showStatusMessage(final String message,final boolean isError) { if(statusMessageLabel!=null){Gdx.app.postRunnable(()->{statusMessageLabel.setText(message);statusMessageLabel.setColor(isError?Color.RED:Color.LIME);statusMessageLabel.setVisible(true);if(clearStatusTask!=null){clearStatusTask.cancel();}clearStatusTask=Timer.schedule(new Timer.Task(){@Override public void run(){if(statusMessageLabel!=null&&message.equals(statusMessageLabel.getText().toString())){statusMessageLabel.setText("");statusMessageLabel.setVisible(false);}}},isError?4.0f:2.5f);});}}
    @Override public void populateDeckList(List<DeckModel> decks) { if(deckListWidget!=null){Array<String> dNames=new Array<>();if(decks!=null){for(DeckModel d:decks){if(d!=null&&d.getName()!=null&&!d.getName().isEmpty()){dNames.add(d.getName());}else{Gdx.app.log(TAG,"Warning: Deck found with null/empty name or deck itself is null!");}}}else{Gdx.app.error(TAG,"Deck list provided to populateDeckList was null!");}deckListWidget.setItems(dNames);}else{Gdx.app.error(TAG,"deckListWidget is null in populateDeckList!");}}
    @Override public void selectDeckInList(String deckName) { if(deckListWidget!=null&&deckListScrollPane!=null){if(deckName==null){deckListWidget.getSelection().clear();}else{deckListWidget.setSelected(deckName);}deckListScrollPane.layout();float iH=deckListWidget.getItemHeight();int sIdx=deckListWidget.getSelectedIndex();if(sIdx!=-1&&iH>0){float lH=deckListWidget.getHeight();float iTY=lH-(iH*sIdx);float iBY=lH-(iH*(sIdx+1));float cS=deckListScrollPane.getScrollY();float mS=deckListScrollPane.getMaxY();float vH=deckListScrollPane.getHeight();if(iTY>cS+vH){deckListScrollPane.setScrollY(iTY-vH);}else if(iBY<cS){deckListScrollPane.setScrollY(iBY);}deckListScrollPane.setScrollY(Math.max(0,Math.min(deckListScrollPane.getScrollY(),mS)));}else if(deckName==null){deckListScrollPane.setScrollY(0);}}}
    @Override public void showUnsavedChangesIndicator(boolean show) { if(unsavedChangesIndicator!=null){unsavedChangesIndicator.setText(show?"*":"");} }

    // --- Screen Lifecycle Methods ---
    @Override public void render(float delta) { Gdx.gl.glClearColor(0.2f, 0.2f, 0.25f, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f)); stage.draw(); }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); if(pieceListTable!=null)pieceListTable.invalidateHierarchy();if(spellListTable!=null)spellListTable.invalidateHierarchy();if(spellGridTable!=null)spellGridTable.invalidateHierarchy();}
    @Override public void show() { Gdx.app.log(TAG, "Showing DeckBuildingScreen."); Gdx.input.setInputProcessor(stage); if(controller!=null)controller.loadUserDecks(); }
    @Override public void hide() { Gdx.app.log(TAG, "Hiding DeckBuildingScreen."); Gdx.input.setInputProcessor(null); if(clearStatusTask!=null){clearStatusTask.cancel();clearStatusTask=null;} }
    @Override public void dispose() { Gdx.app.log(TAG, "Disposing DeckBuildingScreen"); if(stage!=null)stage.dispose(); for(Texture t:loadedTextures.values()){if(t!=null){t.dispose();}}loadedTextures.clear();Gdx.app.log(TAG,"Deck building textures disposed.");if(clearStatusTask!=null){clearStatusTask.cancel();clearStatusTask=null;}
    }
}