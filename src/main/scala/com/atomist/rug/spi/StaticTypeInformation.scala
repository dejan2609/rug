package com.atomist.rug.spi

import java.util.{List => JList}

import org.springframework.util.ReflectionUtils

import scala.collection.JavaConverters._

/**
  * Type information about a language element such as a Type.
  * Useful for tooling and document generation as well as
  * compile time validation.
  */
sealed trait TypeInformation

/**
  * TypeInformation subtrait indicating when operations
  * on the type are not known.
  */
trait DynamicTypeInformation extends TypeInformation

/**
  * Trait that types should extend when all operations on the type are known.
  * ReflectiveStaticTypeInformation subinterface computes this automatically
  * using reflection.
  * @see ReflectiveStaticTypeInformation
  */
trait StaticTypeInformation extends TypeInformation {

  def operations: Seq[TypeOperation]

  /**
    * Exposes for callers who may need Java
    * @return
    */
  def operationsAsJava: JList[TypeOperation] = operations.asJava

}

case class TypeParameter(
                   name: String,
                   parameterType: String,
                   description: Option[String]
                   ) {

  def getDescription: String = description.getOrElse("")

  override def toString: String = {
    name + " : " + parameterType + " : " + description.getOrElse("No Description")
  }
}

// TODO flesh out parameters to include type information
case class TypeOperation(
                          name: String,
                          description: String,
                          readOnly: Boolean,
                          parameters: Seq[TypeParameter],
                          returnType: String,
                          example: Option[String]) {

  def parametersAsJava: JList[TypeParameter] = parameters.asJava

  def hasExample = example.isDefined

  def exampleAsJava = example.getOrElse("")

  def invoke(target: Object, args: Seq[AnyRef]): Object = {
    val methods = target.getClass.getMethods.toSeq.filter(m =>
      this.name.equals(m.getName) && this.parameters.size == m.getParameterCount
    )
    if (methods.size != 1)
      throw new IllegalArgumentException(s"Operation [$name] cannot be invoked on [${target.getClass.getName}]: Found ${methods.size} definitions with ${parameters.size}, required exactly 1")
    methods.head.invoke(target, args:_*)
  }
}
