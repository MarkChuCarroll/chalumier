# Parallel Computing in Demakein with Neso


In Demakein, ph used nesoni, which is a parallel gene-sequence analysis
tool. Nesoni was designed specifically for the needs of the genetic analysis
project, not for what we're doing here, but ph knew it well because he wrote it.
Like the rest of ph's code, it's poorly documented, wierd, sloppy, and
incomprehensible. So i'm not going to try to directly translate it. Intead,
I'm trying to understand the underlying mechanics that ph used in demakein,
and building those in a way that's natural for kotlin. And I'll include
documentation.

The basics, as I've been able to figure out:
- Nesoni is based on a coordinator/worker-pool model.
- Within that, the programming model is based on a future mechanism.
- A set of tasks to be computed are given to coordinator, which returns
  futures for the result of the tasks.
- Workers ask the coordinator for a future, perform the computation, and return
  the result.
- A worker can create more futures at any time.
- To get the value of a future, you invoke it as a function.

Unknowns:
- there's a bunch of prose about make-like dependency functionality, like
  the following, from nesoni/legion.py. I have no idea what that means,
  nor any clue of how just invoking with no args can tell a remote process
  that it needs to remake anything.

```python
    # The return value can be later called with
    # no arguments to get the result of the function.
    # This has the side effect of synchronizing with
    # the process that was created, and this process
    # being "infected" with the need to remake if
    # necessary.
  ```
- ph goes out of his way to tell us that his implementation of future
  requires "non-obvious" implementation tricks for correctness, but
  he doesn't bother to tell us what those are.
  
  ```python
  LIFO allocation of cores
  - LIFO is generally more memory efficient
  - Behaves as expected when only one core used
    
  Processes are assumed to start owning one core.

  Keep track of the number of cores a process has is it's own business.

  Maintaining LIFOness makes this is a delicate and subtle dance.  
  
  The implementation of Future(...) below is non-obvious.
  ```

For kotlin, it seems to make sense to do this using coroutines. We
can set up a pool of threads for the coroutines, and then the workers
and future infrastructure can be built up within that. The futures
can be built (maybe? not sure because of that "remake" stuff?) as
deferred values.

## Basic Nesoni Types and Operations

### Coordinator

The coordinator is a central shared service that manages
the parallel computation and synchronization. Every worker has
access to the coordinator at all times.

-`legion.coordinator().new_future(): Int`
   - Creates a new future. A future, here, isn't what we usually think of as
     a future - it's more of a semaphore or coordination point for exchanging
     a value. You can think of it as something very similar to a single-use
     channel in Go.
- legion.future(worker, task, fut)
  - start a new task/job to perform a computation. 
  - task is a function that actually does the computation.
  - worker is a function that runs a loop, retrieving inputs, performing
    the task on them, and returning the results.
  - fut is the identifier for the future that the worker will use to retrieve
    its first input.
  -  For example, in optimizer.py:
     - scorer is a function `(ArrayList<Double>)->Double` from InstrumentDesigner.scorer. The
       goal of the parallel computation is to do lots of scorers at once.
     - worker is a function  `(scorer, fut: Future) -> Unit`. It uses the future
       to retrieve the next input vector, runs scorer on it, and then pushes
       the result back to the coordinator, along with a future it will wait on
       to retrieve its next input.

- `value = legion.coordinator().get_future(fut)`
   - This retrieve the value of the future. If it hasn't been  computed yet,
     then the caller will block until it's ready.
- `legion.coordinator().deliver_future(futureNumber: Int, value: Any?)`
  - Sets the value of a future. This unblocks any tasks waiting on the
    result of the future.
  - In then nesoni code, new_future() seems to just return an integer. That
    integer is used as a name for the future, and it's really an index
    into a table.
  - That table contains entries which are triples: (lock: threading.Event, value: ValueTriple, refCount: Int)
    - The Event objects are basically semaphores wrapping a boolean. Each one has three
      methods: set() (which sets the value to true), clear() (which sets the  flag
      to  false), and wait() (which blocks until the flag is set to true.
     - the value field is, itself, a triple.
  - When someone tries to get a future value, if that event is false, their thread
    blocks until someone sets it to true. It returns a triple of
     (executionTime: Long, exception: Throwable?, value: Any?).
  - When someone sets a future value (using Coordinator.deliverFuture(number, value),
    it sets the value field of the tuple to the value, and then sets the
    the Event to release any waiters. That value field is the same triple as above.

### Future_reference:

- NOTE: this is not actually used by demakein, but it's what nesoni
   was designed for, so it's worth understanding. This is the intended
   flow; ph just hijacked it by using the underlying primitives.a
- This is a class that wraps the integer returned by coordinator to
   make it easier to use.
- The future can be invoked as a function. If it is, then the
  value is retrieved from the coordinator. If exception is non-null,
  the exception is re-thrown in the retrieving thread; otherwise
  it returns the value. This synchronizes with the process
  that reads that value - so the process owning the future will
  block once its computation is completed until someone retrieves it.
- `Future.future(func, args: VarArgs<Any?>): Future<Any?>`
  - this creates a new process to evaluate func on its args, wrapped in a future.
  - This is how processes get created.

  - Time and re-execution! (Finally figured this one out.)
  - things get labelled with a timestamp of when they were started.
  - When someone synchronizes with someone else, they exchange times with
    their partner. If their time is before their partners, that means that
    something changed, and they need to re-compute.
  - This is part of Future_ref, *not* used by demakein.

## Workers in demakein.
- There are two "kinds" of futures in demakein; I'll call them assignments
  and results.
  - An assignment future is used to send a task to a worker to be computed.
  - A result future is used by a worker to send a computation result back
    to the coordinator.
- what a worker really does is:
   ```kotlin
   while (true) {
      task = getNextTask()
      reportResult(task, compute(task))
   }
  ```
- The nesoni version of this is:
   ```kotlin
  fun worker(initialTaskId: Int) {
    var nextTask = initialTaskId
    while (true) {
      val currentTask = nextTask
      val (reply_future, task) = coordinator.get_future(nextTask)
      val result = compute(task)
      nextTask = coordinator.new_future()
      coordinator.deliver_future(currentTask, Pair(result, nextTask))
    }
  }
  ```

