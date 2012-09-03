package ah.foldersync

import scala.collection.mutable.ArrayBuffer
import scala.annotation.tailrec

/**
 * Utility to find differences in two lists of FileInfos.
 */
object DiffFinder {
  /**
   * Compares two lists of files and calculates the difference.
   * 
   * Both seqs must be sorted by name, otherwise the result is undefined.
   * 
   * When a file is present in leftFiles but missing in rightFiles then the file is considered removed.
   * When a file is not present in leftFiles but in rightFiles then the file is considered added.
   */
  def compare(leftFiles: Seq[FileInfo], rightFiles: Seq[FileInfo]): Difference = {
    val added = ArrayBuffer.empty[FileInfo]
    val removed = ArrayBuffer.empty[FileInfo]
    val modified = ArrayBuffer.empty[(FileInfo, FileInfo)]
    
    
    @tailrec
    def diff(leftIndex: Int, rightIndex: Int): (Int, Int) = {
      if (leftIndex >= leftFiles.length && rightIndex >= rightFiles.length) {
        (leftIndex, rightIndex)
      } else if (leftIndex >= leftFiles.length) {
        added ++= rightFiles drop rightIndex
        (leftIndex, rightIndex)
      } else if (rightIndex >= rightFiles.length) {
        removed ++= leftFiles drop leftIndex
        (leftIndex, rightIndex)
      } else {
        val left = leftFiles(leftIndex)
        val right = rightFiles(rightIndex)
        
        val compare = left.name.compareTo(right.name)
        if (compare == 0) {
          if (left.size == right.size && left.lastModified == right.lastModified) {
            diff(leftIndex + 1, rightIndex + 1)
          } else {
            modified += ((left, right))
            diff(leftIndex + 1, rightIndex + 1)
          }
        } else if (compare < 0) {
          removed += left
          diff(leftIndex + 1, rightIndex)
        } else { // compare > 0
          added += right
          diff(leftIndex, rightIndex + 1)
        }
      }
    }
    
    diff(0, 0)
    
    Difference(added, removed, modified)
  }

  /**
   * The difference between two lists.
   */
  case class Difference(added: Seq[FileInfo], removed: Seq[FileInfo], modified: Seq[(FileInfo, FileInfo)])
}

