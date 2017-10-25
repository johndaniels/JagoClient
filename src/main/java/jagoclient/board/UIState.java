package jagoclient.board;

public interface UIState {
    interface StateChangedHandler {
        void stateChanged();
    }

    void addStateChangedHandler(StateChangedHandler stateChangedHandler);

    /**
     * Indicate that some part of the state has changed. In general
     * handlers should update their local state to match the current
     * state of the UI. The expectation is that all listeners will
     * recheck all of their state against the canonical state, similar
     * to how React works.
     */
    void stateChanged();
}
