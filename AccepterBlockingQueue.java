import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A BlockinqQueue in which consumers may block producers (temporarily)
 * to prevent them from producing more items to consume. To allow a producer
 * to continue operation, an element taken from this queue must be accepted,
 * by calling {@link AccepterBlockingQueue#accept(ThreadUniqueObject)}.
 *
 * If an element is not accepted, any other elements created on the same thread
 * of the not yet accepted element will not be able to be put on this queue.
 *
 * 1 or more consumers can take and accept threads, though any one consumer
 * choosing to accept will allow that producer to continue operation globally,
 * for all producers.
 *
 * Note that accept must be called on the object returned from take.
 *
 * To improve this data structure, one should consider extending it to 
 * follow BlockingQueue and perhaps have some more clever way at detecting
 * what thread an object was last used on instead of having those objects
 * exteend ThreadUniqueObject.
 *
 * @param <E>
 */
public class AccepterBlockingQueue<E extends ThreadUniqueObject> {

    // Thread Safety
    /**
     * The threrad safety of this object is somewhat involved. Firstly,
     * on a global class level there are two general objects, the pq, or
     * permission "queue", and the bq, or the blocking queue.
     *
     * These variables are private and final and never exposed to the client,
     * only internally used. The pq is a concurrent hashMap and the bq
     * is a BlockingQueue, synchronzied by design.
     *
     * Their are three locking mechanisms. Two simple object locks for take
     * and accept and a mapping of locks from threadIds (long) to arbitrary
     * object locks.
     *
     * @see this#take()
     *     This method is thread safe becasue it maintains its own lock.
     *     This lock will prevent several threads from concurrently taking.
     *     In the case of multiple consumers being blocked to take, one of them
     *     waiting for BlockinQueue to return an entry, the first one
     *     to aquire the lock after one the waiting thread continues runningw will
     *     go into the thread state of waiting by being blocked by the take method.
     *     of the blocking queue.
     *     @see BlockingQueue#take()
     *
     * @See this#accept
     *     This method mutates the internal state of the pq. Thus, it aquires its own method
     *     lock to enter the block. This operation is atomic with repsect to the data structure,
     *     and no interleaving between threads can occur.
     *
     *     Note, however, take and accept operations can happen concurrently by design.
     *     This is threadsafe.
     *
     *     This method does not block at any stage, so deadlock concerns are not necessary here.
     *
     *
     * @see this#put(ThreadUniqueObject)
     *     This method also mutates the internal state of both the pq and the bq. However,
     *     note that mutations the pq will remain to be atomic, as any thread b pushing an item
     *     created on thread a, (that may and most comonnly will be b as well) will be blocked
     *     untill any other item created on thread a has finished its blocking.
     *
     *     For example, imagine many objects being created on thread a. These objects are now
     *     asynchronosly, by different threads, requested to be put on this object. This
     *     will behave "synchonosly" since these objects were all created on the same thread.
     *
     * The relationship between put, accept and take
     *     Note none of these operations require obtaining locks of the other. This implies
     *     that it is threasafe to perform any of these operations at any stage in between
     *     one another.
     *
     *     This statement remains true given that the precondition of calling accept must occur
     *     after calling take on an object from a take call.
     *
     *     The thread safety of the blocking queue and the concurrent hash map ensure
     *     these operations remain atomic and independent. Accepts allow for waiting on
     *     threads that are blocked in put to continue as it puts the accept signal for example.
     *
      */


    private enum Signal {
        ACCEPT
    }

    // Queue Representation
    private final Map<Long, RequestResponseBlockingQueue<E, Signal>> pq = new ConcurrentHashMap<>();
    private final BlockingQueue<E> bq = new LinkedBlockingQueue<>();

    // Locking Mechanism
    final Map<Long, Object> producerThreadLocks = Collections.synchronizedMap(new HashMap<>());
    private final Object takeLock = new Object();
    private final Object acceptLock = new Object();


    /**
     * Aquire a ReentrantLock for a given threadId. If this producer thread has been seen before,
     * then the same lock is returned. Note that
     *
     * @param threadId The threadId to the coresponding lock
     * @return a ReentrantLock unique to the threadId
     */
    private synchronized Object getThreadIdLock(Long threadId) {
        if (!producerThreadLocks.containsKey(threadId))
            producerThreadLocks.put(threadId, new Object());
        return producerThreadLocks.get(threadId);
    }

    /**
     * Put an item in the acceptor blocking queue. Must be called from a producer
     * from a unique thread.
     *
     * @param e the item to put in the queue
     * @throws InterruptedException if interrupted
     */
    public void put(E e) throws InterruptedException {

        long threadId = e.getThreadId();
        Object lock = getThreadIdLock(threadId);

        synchronized (lock) {
            if(pq.containsKey(threadId)) {
                RequestResponseBlockingQueue<E, Signal> unit = pq.get(threadId);
                // block until put is valid from the response
                unit.request.put(e, true);
                bq.put(e);
                Signal sgnl = unit.response.take();
            } else {
                RequestResponseBlockingQueue<E, Signal> unit = new RequestResponseBlockingQueue<>();
                pq.put(threadId, unit);
                unit.request.put(e, true);
                bq.put(e);
                Signal sgnl = unit.response.take();
            }
        }

    }

    /**
     * Accept the item, along the producer who manufactured the item to continue
     * production. A precondiiton on this method is that the object was pulled
     * from this.take().
     *
     * @param e The item to accept.
     * @throws InterruptedException if interrupted
     * @throws IllegalArgumentException if the item was not produced on the thread it reports
     */
    public void accept(E e) throws InterruptedException, IllegalArgumentException {
        synchronized (acceptLock) {
            try {
                RequestResponseBlockingQueue<E, Signal> unit = pq.get(e.getThreadId());
                if(unit.request.containsKey(e)) {
                    unit.response.put(Signal.ACCEPT);
                    unit.request.remove(e);
                } else {
                    System.out.println(unit.request);
                    System.out.println(e);
                    throw new IllegalArgumentException("No record of the enqueuing of this item");
                }
            } catch (NullPointerException np) {
                throw new IllegalArgumentException("Thread recorded to have produced is not consistant");
            }
        }
    }


    /**
     * Take an item from the queue that has produced. Blocks until a producer has created
     * an item.
     *
     * @return The next item on the queue
     * @throws InterruptedException if interrupted
     */
    public E take() throws InterruptedException {
        synchronized (takeLock) {
            E value = bq.take();
            return value;
        }
    }

}
