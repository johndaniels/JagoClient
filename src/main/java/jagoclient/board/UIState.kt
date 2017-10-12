package jagoclient.board

import jagoclient.Global
import java.util.*

class UIState (val size: Int) {
    interface StateChangedHandler {
        fun stateChanged()
    }
    private val stateChangedHandlers = ArrayList<StateChangedHandler>();
    fun addStateChangedHandler(stateChangedHandler: StateChangedHandler) {
        stateChangedHandlers.add(stateChangedHandler);
    }

    fun stateChanged() {
        for (stateChangedHandler in stateChangedHandlers) {
            stateChangedHandler.stateChanged();
        }
    }
    var textMarker = "A";
    var labelM = "";
    var number = 0;
    var boardState: BoardState = BoardState(size);
    val boardPosition: Position
        get() = boardState.boardPosition ;
    var uiMode : UIMode = UIMode.PLAY_BLACK
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