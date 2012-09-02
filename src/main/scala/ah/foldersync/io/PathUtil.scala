package ah.foldersync.io

import java.nio.file._

object PathUtil {
  def path(first: String, rest: String*) = Paths.get(first, rest:_*)
  final val toPath = (first: String) => Paths.get(first)
  
  
}