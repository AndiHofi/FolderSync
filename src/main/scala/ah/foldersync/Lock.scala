package ah.foldersync

class Lock {
	private[this] val lock = new java.util.concurrent.locks.ReentrantLock(false)
	
	def synchronized[A](f: => A): A = {
	  lock.lock()
	  try f
	  finally lock.unlock()
	}
}