package org.goodmath.chalumier.optimize

import org.goodmath.chalumier.errors.ChalumierException

class NoMoreTasksException() : ChalumierException("Tried to retrieve a task from an empty queue.") {

}
