package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.ContainerTreeNode
import org.scalatest.{FlatSpec, Matchers}

class RepTest extends FlatSpec with Matchers {

  it should "handle rep of literal with 0 matches" in {
    val l1 = Literal("thing")
    val l = Rep(l1)
    val input = "woieurowieurowieur"
    l.matchPrefix(InputState(input)) match {
      case Right(PatternMatch(Some(tn), "", InputState(input, _, _), _)) =>
        tn.nodeName should be ("rep")
    }
  }

  it should "handle rep of literal with single instance" in {
    val l1 = Literal("thing", Some("thing"))
    val l = Rep(l1)
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(Some(tn: ContainerTreeNode), "thing", InputState(thing2, _, _), _)) =>
        tn.nodeName should be ("rep")
        tn.childNodes.size should be (1)
    }
  }

  it should "handle rep of literal with two instances" in {
    val l1 = Literal("thing", Some("thing"))
    val l = Rep(l1)
    val input = "thingthing2"
    l.matchPrefix(InputState(input)) match {
      case Right(pe: PatternMatch) =>
        pe.matched should be ("thingthing")
        pe.resultingInputState.input should be (input)
        pe.resultingInputState.offset should be ("thingthing".length)
        val tn = pe.node.get.asInstanceOf[ContainerTreeNode]
        tn.nodeName should be ("rep")
        tn.childNodes.size should be (2)

    }
  }

  it should "handle rep of regex with two instances" in {
    val l1 = Regex("thing", "t...g")
    val namedRep = Rep(l1, "myReppyName")
    val input = "thingthing2"
    namedRep.matchPrefix(InputState(input)) match {
      case Right(pe: PatternMatch) =>
        pe.matched should be ("thingthing")
        pe.resultingInputState.input should be (input)
        pe.resultingInputState.offset should be ("thingthing".length)
        val tn = pe.node.get.asInstanceOf[ContainerTreeNode]
        tn.nodeName should be (namedRep.name)
        tn.childNodes.size should be (2)

    }
  }

}



