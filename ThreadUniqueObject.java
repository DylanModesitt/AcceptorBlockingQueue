package memory.concurrency;


/**
 * An immutable, threadsafe object that is uniquely identified by the
 * thread that created it. All extending objects must call super in the
 * constructor and assert during equality that the super classes are also
 * considered equal.
 *
 * If subclasses choose to extend this object and make their representation
 * mutable, please not that the object will still be considered to originate
 * from the thread it was constructed on.
 */
public class ThreadUniqueObject {

    private final long threadId;

    public long getThreadId() {
        return threadId;
    }

    public ThreadUniqueObject() {
        this.threadId = Thread.currentThread().getId();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ThreadUniqueObject)) return false;
        return ((ThreadUniqueObject)obj).getThreadId() == this.getThreadId();
    }

    @Override
    public int hashCode() {
        return (int)threadId;
    }

}
