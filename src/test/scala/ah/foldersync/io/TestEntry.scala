package ah.foldersync.io

sealed trait TestEntry

case class TestDir(
  path: String,
  createTime: Option[String] = None,
  entries: Seq[TestEntry] = Seq()) extends TestEntry {
  def /(e: TestEntry*) = copy(entries = entries ++ e)
}

case class TestFile(
  path: String,
  createTime: Option[String] = None,
  content: String = "") extends TestEntry