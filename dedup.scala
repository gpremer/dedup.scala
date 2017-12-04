import sys.process._
import java.nio.file.Paths

type Size = Long
type Name = String
type Digest = String

case class NameAndSize(name: Name, size: Size)

case class NameAndDigest(name: Name, digest: Digest)

val sizeAndNamePat = """(\d+),(.*)""".r

val exifPat = """20\d\d\d\d\d\d_\d\d\d\d\d\d_(.*)\.(.*)""".r

val digestPat = """(\w+)\s+(.+)""".r

def findImages(path: String): Seq[NameAndSize] = {
  def parseNameAndSizeLine(line: String): Option[NameAndSize] =
    line match {
      case sizeAndNamePat(size, name) => Some(NameAndSize(name, size.toLong))
      case _ => None
    }

  val out = Seq("/usr/bin/find", path, "-type", "f", "-iregex", """.*\.\(jpg\|mp4\)""", "-printf", "%s,%p\\n").!!
  val lines = out.split("\n").toSeq
  lines.flatMap(parseNameAndSizeLine)
}

def calcSha256Grouped(files: Iterable[Name]): Map[Digest, List[Name]] = {
  def parseDigestAndName(line: String): Option[NameAndDigest] = 
    line match {
      case digestPat(digest, name) => Some(NameAndDigest(name, digest))
      case _ => None
    }

  def calcSha256(files: Iterable[Name]): Map[Digest, List[Name]] = {
    val out = ("sha256sum" :: files.toList).!!
    val lines = out.split("\n").toSeq
    lines.flatMap(parseDigestAndName).groupBy(_.digest).mapValues(_.map(_.name).toList)
  }

  def mergeMaps[K, V](m1: Map[K, V], m2: Map[K, V], valueMerge: (V, V) => V): Map[K, V] =
    m1.foldLeft(m2)((m, kv) => m + (kv._1 -> m.get(kv._1).map(valueMerge(_, kv._2)).getOrElse(kv._2)))

  def listConcat(l1: List[Name], l2: List[Name]) = 
    l1 ++ l2

  files
  .grouped(50)
  .map(calcSha256)
  .foldLeft(Map.empty[Digest, List[Name]])(mergeMaps(_, _, listConcat))
}

def fileKey(file: Name): Name = {
  val fileName = Paths.get(file).getFileName.toString

  fileName match {
    case exifPat(orig, _) if orig.length >= 1 => orig
    case _ => fileName
  }
}

def filesByKey(files: Seq[NameAndSize]): Map[Name, Seq[NameAndSize]] = 
  files.groupBy(file => fileKey(file.name))

def filesBySize(files: Seq[NameAndSize]): Map[Size, Seq[Name]] = 
  files.groupBy (_.size).mapValues(_.map(_.name))


def duplicatesByDigest(filesBySize: Map[Size, Seq[Name]]) : List[Name] = {
  val sameSizes = filesBySize.filter(_._2.size > 1)

  val sizeDupNames = sameSizes.values.flatten

  val duplicatesByDigest = calcSha256Grouped(sizeDupNames)

  val digestDups = duplicatesByDigest.filter(_._2.size > 1)

  println(digestDups.values.map(_.sorted.mkString("(",", ", ")")).toList.sorted.mkString("\n"))

  val digestDupNames = digestDups.values.flatten

  digestDupNames.toList
}

// Het eerste argument is de directory waar de volledige set moet in gezocht worden
// Het tweede argument is de directory waarin dubbels gezocht worden

if ( args.size < 2) {
  System.err.println("dedup originals_dir duplicates_dir")
}

val originalsDir :: duplicatesDir :: _ = args.toList


val originals = findImages(originalsDir)

val suspectedDuplicates = findImages(duplicatesDir).map(_.name)

val actualDuplicates = duplicatesByDigest(filesBySize(originals))

val retainedDuplicaties = suspectedDuplicates.toSet.intersect(actualDuplicates.toSet)

println(retainedDuplicaties.toList.sorted.mkString("\n"))
