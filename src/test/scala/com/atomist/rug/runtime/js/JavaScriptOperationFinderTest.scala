package com.atomist.rug.runtime.js

import com.atomist.project.{ProjectOperation, SimpleProjectOperationArguments}
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}


class JavaScriptOperationFinderTest  extends FlatSpec with Matchers {

  val SimpleProjectEditorWithParametersArray: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |    parameters: Parameter[] = [{name: "content", description: "Content", pattern: "^.*$$", maxLength: 100}]
       |    edit(project: Project, {content} : {content: string}) {
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val SimpleProjectEditorWithAnnotatedParameters: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |
       |    @parameter({pattern: "^.*$$", description: "foo bar"})
       |    content: string = "Test String";
       |
       |    @parameter({pattern: "^\\d+$$", description: "A nice round number"})
       |    amount: number = 10;
       |
       |    @parameter({pattern: "^\\d+$$", description: "A nice round number"})
       |    nope: boolean;
       |
       |    edit(project: Project, {content} : {content: string}) {
       |       if(this.amount != 10) {
       |          throw new Error("Number should be 10!");
       |       }
       |       if(this.content != "woot") {
       |          throw new Error("Name should be woot");
       |       }
       |    }
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  it should "find an editor with a parameters list" in {
    val eds = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleProjectEditorWithParametersArray))
    eds.parameters.size should be(1)
  }

  it should "find an editor using annotated parameters" in {
    val eds = invokeAndVerifySimple(StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleProjectEditorWithAnnotatedParameters))
    eds.parameters.size should be(3)
    eds.parameters(0).getDefaultValue should be("Test String")
    eds.parameters(1).getDefaultValue should be("10")
    eds.parameters(2).getDefaultValue should be("")

  }

  private def invokeAndVerifySimple(tsf: FileArtifact): JavaScriptInvokingProjectEditor = {
    val as = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val jsed = JavaScriptOperationFinder.fromJavaScriptArchive(as).head.asInstanceOf[JavaScriptInvokingProjectEditor]
    jsed.name should be("Simple")
    val target = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "nasty stuff"))
    jsed.modify(target,SimpleProjectOperationArguments("", Map("content" -> "woot")))
    jsed
  }
}
