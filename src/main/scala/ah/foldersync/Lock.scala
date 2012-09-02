package ah.foldersync

/**
 * A simple wrapper over java.util.concurrent.ReentrantLock to allow the synchronized syntax.
 */
class Lock {
	private[this] val lock = new java.util.concurrent.locks.ReentrantLock(false)
	
	def synchronized[A](f: => A): A = {
	  lock.lock()
	  try f
	  finally lock.unlock()
	}
}