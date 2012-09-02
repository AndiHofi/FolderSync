package ah.foldersync

import java.io.IOException
package object io {
  def using[A <: java.io.Closeable, B](in: A)(f: A => B): B = {
    val resource = in
    var error: Throwable = null

    try {
      f(resource)
    } catch {
      case e: Throwable =>
        error = e
        throw e
    } finally {
      try {
        resource.close()
      } catch {
        case e: IOException =>
          if (error != null) error.addSuppressed(e)
          else throw e
      }
    }
  }
}