package com.atomist.tree.pathexpression

import java.util.Objects

import com.atomist.source.StringFileArtifact
import com.atomist.tree.content.text.TreeNodeOperations._
import com.atomist.tree.pathexpression.PathExpressionParsingConstants._
import com.atomist.util.scalaparsing.CommonTypesParser

/**
  * Scala parser combinator for path expressions
  */
trait PathExpressionParser extends CommonTypesParser {

  private def nodeName: Parser[String] = identifierRefString(Set(), ident)

  private def objectType: Parser[String] = identifierRefString(Set(), ident)

  private def child: Parser[AxisSpecifier] = opt(s"$ChildAxis::") ^^ {
    case Some(_) => Child
    case _ => Child
  }

  private def navigationAxis: Parser[AxisSpecifier] = identifierRefString(StandardAxes, ident) <~ "::" ^^
    (s => NavigationAxis(s))

  private def nodeTypeTest: Parser[ObjectType] = objectType <~ "()" ^^ (p => ObjectType(p))

  private def propertyTest: Parser[Predicate] =
    "@" ~> nodeName ~ EqualsToken ~ (singleQuotedString | doubleQuotedString) ^^ {
      case prop ~ op ~ literal => prop match {
        case "name" =>
          NodeNamePredicate(literal)
        case "type" =>
          NodeTypePredicate(literal)
        case propName: String =>
          PropertyValuePredicate(propName, literal)
      }
    }

  private def nullLiteral: Parser[Object] = "null" ^^ (_ => null)

  private def integer: Parser[Integer] = decimalNumber ^^ (s => s.toInt)

  private def literal: Parser[Any] = nullLiteral | singleQuotedString | integer

  private def methodInvocationTest: Parser[Predicate] = "." ~> nodeName ~ args ~ EqualsToken ~ literal ^^ {
    case methodName ~ args ~ _ ~ literal =>
      FunctionPredicate(s".$methodName", (n, among) => {
        val invoked = invokeMethod[Any](n, methodName, args)
        Objects.equals(literal, invoked)
      })
  }

  private def arg: Parser[String] = singleQuotedString

  private def args: Parser[Seq[String]] = "(" ~> repsep(arg, ",") <~ ")"

  private def booleanMethodInvocation: Parser[Predicate] = "." ~> nodeName ~ args ^^ {
    case methodName ~ args =>
      FunctionPredicate(s".$methodName", (n, among) => invokeMethod[Boolean](n, methodName, args))
  }

  private def index: Parser[Predicate] = integer ^^ {
    n => IndexPredicate(n)
  }

  private def truePredicate: Parser[Predicate] = "true" ^^ (_ => TruePredicate)

  private def falsePredicate: Parser[Predicate] = "false" ^^ (_ => FalsePredicate)

  private def nestedPathExpressionPredicate: Parser[Predicate] =
    pathExpression ^^ (pe => NestedPathExpressionPredicate(pe))

  private def predicateTerm: Parser[Predicate] =
    methodInvocationTest |
      propertyTest |
      booleanMethodInvocation |
      truePredicate |
      falsePredicate |
      index |
      nestedPathExpressionPredicate

  private def negatedPredicate: Parser[Predicate] = "not" ~> "(" ~> predicateExpression <~ ")" ^^ {
    pred => NegationOfPredicate(pred)
  }

  private def logicalOp: Parser[String] = "and" | "or"

  private def predicateAnd: Parser[Predicate] = predicateTerm ~ logicalOp ~ predicateExpression ^^ {
    case a ~ "and" ~ b => a and b
    case a ~ "or" ~ b => a or b
  }

  private def predicateExpression: Parser[Predicate] = predicateAnd | negatedPredicate | predicateTerm

  private def predicate: Parser[Predicate] =
    PredicateOpen ~> predicateExpression ~ PredicateClose ~ opt(PredicateOptional) ^^ {
      case p ~ _ ~ Some(_) => OptionalPredicate(p)
      case p ~ _ ~ None => p
    }

  private def nodeNameTest: Parser[NodeTest] = nodeName ^^
    (s => NamedNodeTest(s))

  private def allNodes: Parser[NodeTest] = "*" ^^ (_ => All)

  private def nodeTest: Parser[NodeTest] = nodeTypeTest | nodeNameTest | allNodes

  private def property: Parser[AxisSpecifier] = (s"$PropertyAxis::" | "@") ^^ (s => Attribute)

  private def descendant: Parser[AxisSpecifier] = (s"$DescendantAxis::" | "/") ^^
    (s => Descendant)

  private def axis: Parser[AxisSpecifier] =
    property |
      navigationAxis |
      descendant |
      child

  private def locationStep: Parser[LocationStep] = axis ~ nodeTest ~ rep(predicate) ^^ {
    case a ~ t ~ preds => LocationStep(a, t, preds)
  }

  private val slashSeparator = "/"

  def relativePathExpression: Parser[PathExpression] = repsep(locationStep, slashSeparator) ^^
    (steps => PathExpression(steps))

  def absolutePathExpression: Parser[PathExpression] = slashSeparator ~> relativePathExpression

  def pathExpression: Parser[PathExpression] = absolutePathExpression | relativePathExpression

  def parsePathExpression(expr: String): PathExpression = {
    try {
      parseTo(StringFileArtifact("<input>", expr), phrase(absolutePathExpression))
    } catch {
      case e: IllegalArgumentException =>
        throw new IllegalArgumentException(s"Path expression '$expr' is invalid: [${e.getMessage}]", e)
    }
  }
}

/**
  * Default implementation of PathExpressionParser. Import this
  * class for default conversion from Strings to path expressions. 
  */
object PathExpressionParser extends PathExpressionParser {

  implicit def parseString(expr: String): PathExpression = PathExpressionParser.parsePathExpression(expr)

}

private object PathExpressionParsingConstants {

  val DotSeparator = "."

  val SlashSeparator = "/"

  val AmongSeparator = "$"

  val SlashSlash = "//"

  val PredicateOpen = "["

  val PredicateClose = "]"

  val PredicateOptional = "?"

  val PropertyAxis = "property"

  val ChildAxis = "child"

  val DescendantAxis = "descendant"

  val SelfAxis = "self"

  /**
    * Axes that can't be a property
    */
  val StandardAxes = Set(PropertyAxis, ChildAxis, DescendantAxis, SelfAxis)

}
