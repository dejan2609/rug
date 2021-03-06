package com.atomist.tree

/**
  * Convenient terminal node implementation.
  * Prefer PaddingNode for padding.
  * @param nodeName name of the field (usually unimportant)
  * @param value field content.
  */
case class SimpleTerminalTreeNode(nodeName: String,
                                  value: String,
                                  types: Set[String] = Set())
  extends TerminalTreeNode {

  override val nodeType: Set[String] = super.nodeType ++ types
}

/**
  * Convenient class for padding nodes
  * @param description description of what's being padded
  * @param value padding content
  */
case class PaddingTreeNode(description: String, value: String)
  extends TerminalTreeNode {

  override def nodeName = s"padding:$description"

}