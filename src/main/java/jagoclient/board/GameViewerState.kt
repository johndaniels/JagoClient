package jagoclient.board

import jagoclient.Global
import java.util.*

class GameViewerState (val size: Int) : UIState {
    private val stateChangedHandlers = ArrayList<UIState.StateChangedHandler>();
    override fun addStateChangedHandler(stateChangedHandler: UIState.StateChangedHandler) {
        stateChangedHandlers.add(stateChangedHandler);
    }

    /**
     * Indicate that some part of the state has changed. In general
     * handlers should update their local state to match the current
     * state of the UI. The expectation is that all listeners will
     * recheck all of their state against the canonical state, similar
     * to how React works.
     */
    override fun stateChanged() {
        for (stateChangedHandler in stateChangedHandlers) {
            stateChangedHandler.stateChanged();
        }
    }

    var specialMarker = Field.Marker.SQUARE
    var textMarker = "A";
    var labelM = "";
    var number = 0;
    val gameTree: GameTree = GameTree(size);
    init {
        gameTree.addStateChangedHandler(this::stateChanged)
    }
    val boardPosition: Position
        get() = gameTree.boardPosition ;
    var uiMode : UIMode = UIMode.PLAY_BLACK
    set(value) {
        field = value;
        stateChanged();
    }
    enum class UIMode(val description: String) {
        PLAY_BLACK(Global.resourceString("Next_move__Black_")),
        PLAY_WHITE(Global.resourceString("Next_move__White_")),
        ADD_BLACK(Global.resourceString("Set_black_stones")),
        ADD_WHITE(Global.resourceString("Set_white_stones")),
        MARK(Global.resourceString("Mark_fields")),
        LETTER(Global.resourceString("Place_letters")),
        DELETE_STONE(Global.resourceString("Delete_stones")),
        REMOVE_GROUP( Global.resourceString("Remove_prisoners")),
        HIDE(""),
        SPECIAL_MARK(Global.resourceString("Set_special_marker")),
        TEXT_MARK(Global.resourceString("Text__"));
    }
}