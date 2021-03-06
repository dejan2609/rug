import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'


class BananaType implements TypeProvider {

 typeName = "banana"

 find(context: TreeNode): TreeNode[] {
   return [ new Banana()]
 }

}

class Banana implements TreeNode {

  nodeName(): string { return "banana" }

  nodeType(): string[] { return [ this.nodeName()] }

  value(): string { return "yellow" }

  update(newValue: string) {}

  children() { return [] }

}

class SimpleBanana implements ProjectEditor {
    name: string = "Constructed"
    description: string = "Uses single microgrammar"

    edit(project: Project) {
      console.log("Editing")
      let mg = new BananaType()
      let eng: PathExpressionEngine = project.context().pathExpressionEngine().addType(mg)

      let i = 0
      eng.with<Banana>(project, "//File()/banana()", n => {
        //console.log("Checking color of banana")
        if (n.value() != "yellow")
         throw new Error(`Banana is not yellow but [${n.value()}]. Sad.`)
        i++
      })
      if (i == 0)
       throw new Error("No bananas tested. Sad.")
    }
  }
export let editor = new SimpleBanana();