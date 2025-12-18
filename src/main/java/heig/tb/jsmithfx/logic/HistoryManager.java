package heig.tb.jsmithfx.logic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * A generic manager for Undo/Redo operations.
 * @param <T> The type of the object stored in history.
 */
public class HistoryManager<T> {
    private final Deque<T> undoStack = new ArrayDeque<>();
    private final Deque<T> redoStack = new ArrayDeque<>();

    public void push(T entry) {
        undoStack.push(entry);
        redoStack.clear(); // New action invalidates redo history
    }

    /**
     * Pushes an entry to the undo stack without clearing the redo stack.
     * Used specifically when performing a Redo operation.
     */
    public void pushUndo(T entry) {
        undoStack.push(entry);
    }

    public Optional<T> popUndo() {
        return undoStack.isEmpty() ? Optional.empty() : Optional.of(undoStack.pop());
    }

    public void pushRedo(T entry) {
        redoStack.push(entry);
    }

    public Optional<T> popRedo() {
        return redoStack.isEmpty() ? Optional.empty() : Optional.of(redoStack.pop());
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
