package memory.concurrency;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * A package private tupling class that combines a blocking queue of arbitrary type
 * and a set of arbitrary type.
 *
 * @implSpec This type does not adhere to the standard 6.031 conventions of
 * encapsulation and representation independence. This is truly and literaly
 * meant to simply be a wrapper object over these two data structures and
 * is used only and exclusively in AcceptorBlockingQueue (thus the package private nature).
 *
 * @param <E> The element type of the set
 * @param <T> The element type of the blocking queue
 */
class RequestResponseBlockingQueue<E,T> {

    final Map<E,Boolean> request; // implNote this uses a map so that IdentityHashMap can be used
    final BlockingQueue<T> response;

    public RequestResponseBlockingQueue() {
        this.request = Collections.synchronizedMap(new IdentityHashMap<>());
        this.response = new LinkedBlockingQueue<>();
    }

}
