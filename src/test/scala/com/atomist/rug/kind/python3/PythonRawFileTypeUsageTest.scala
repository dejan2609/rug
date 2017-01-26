package com.atomist.rug.kind.python3

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.TestUtils
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class PythonRawFileTypeUsageTest extends FlatSpec with Matchers {

  import Python3ParserTest._

  val Simple: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("setup.py", setupDotPy)
    ))

  val Flask1: ArtifactSource = Simple + new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("hello.py", flask1)
    ))

  def executePython(tsFilename: String, as: ArtifactSource, params: Map[String,String] = Map()): ModificationAttempt = {
    val pe = TestUtils.editorInSideFile(this, tsFilename)
    pe.modify(as, SimpleProjectOperationArguments("", params))
  }

  import PythonFileType._

  def modifyPythonAndReparseSuccessfully(tsFilename: String, as: ArtifactSource, params: Map[String,String] = Map()): ArtifactSource = {
    val parser = new Python3Parser
    executePython(tsFilename, as, params) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles
          .filter(_.name.endsWith(PythonExtension))
          .map(py => parser.parse(py.content))
          .map(tree => tree.childNodes.nonEmpty)
        sm.result
    }
  }

  it should "enumerate imports in simple project" in {
    val r = executePython("Imports.ts", Flask1)
    r match {
      case nmn: NoModificationNeeded =>
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("setup.py").get
        fail
    }
  }

  /*
  it should "modify imports in simple file" in {
    val prog =
      """
        |editor ImportUpdater
        |
        |with PythonFile
        | with import
        |   do setName "newImport"
      """.stripMargin
    val r = modifyPythonAndReparseSuccessfully(prog, Simple)
    val f = r.findFile("setup.py").get
    f.content.contains("newImport") should be(true)
  }

  private val newRoute =
    """
      |@app.route("/")
      |def hello2():
      |    return "Hello World!"
    """.stripMargin

  it should "add Flask route to Python file" in {
    val prog =
      """
        |editor AddFlaskRoute
        |
        |param new_route: ^[\s\S]*$
        |
        |with PythonFile when filename = "hello.py"
        | do append new_route
      """.stripMargin
    val r = modifyPythonAndReparseSuccessfully(prog, Flask1, Map(
      "new_route" -> newRoute
    ))
    val f = r.findFile("hello.py").get
    f.content.contains(newRoute) should be(true)

    val reparsed = pythonParser.parse(f.content)
    // TODO assert 2 methods
    // reparsed.f
  }

*/
}
