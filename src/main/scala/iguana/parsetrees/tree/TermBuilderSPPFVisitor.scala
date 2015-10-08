package iguana.parsetrees.tree

import iguana.parsetrees.slot.NonterminalNodeType
import iguana.parsetrees.sppf._
import iguana.parsetrees.visitor.{Memoization, Visitor}

import scala.collection.mutable.Buffer

object TermBuilder {

  def build[T >: Any](node: SPPFNode, builder: TreeBuilder[T]): T
    = build(node, builder, new TermBuilderSPPFVisitor(builder) with Memoization[SPPFNode]);

  def build_no_memo[T >: Any](node: SPPFNode, builder: TreeBuilder[T]): T
    = build(node, builder, new TermBuilderSPPFVisitor(builder));

  private def build[T >: Any](node: SPPFNode, builder: TreeBuilder[T], visitor: TermBuilderSPPFVisitor): T =  {
    visitor.visit(node) match {
      case Some(v) => v.asInstanceOf[T]
      case None    => throw new RuntimeException()
    }
  }

}


class TermBuilderSPPFVisitor(builder: TreeBuilder[Any]) extends Visitor[SPPFNode] {

  override type T = Any

  case class StarList(l: Buffer[T])
  case class PlusList(l: Buffer[T])

  override def visit(node: SPPFNode): Option[T] = node match {

    case TerminalNode(slot, leftExtent, rightExtent) =>
      if (leftExtent == rightExtent) Some(builder.epsilon(leftExtent))
      else
        if (slot.terminalName == null) Some(builder.terminalNode(leftExtent, rightExtent))
        else Some(builder.terminalNode(slot.terminalName, leftExtent, rightExtent))

    case n@NonterminalNode(slot, child) =>
      if (n.isAmbiguous) {
        Some(builder.ambiguityNode(n.children.map(p => builder.branch(makeList(visit(p.leftChild)))), n.leftExtent, n.rightExtent))
      } else {
        n.slot.nodeType match {
          case NonterminalNodeType.Basic => Some(builder.nonterminalNode(child.rule, makeList(visit(child.leftChild)), n.leftExtent, n.rightExtent))
          case NonterminalNodeType.Star  => Some(flattenStar(visit(child.leftChild)))
          case NonterminalNodeType.Plus  => Some(flattenPlus(visit(child.leftChild)))
          case NonterminalNodeType.Opt   => Some(builder.opt(makeList(visit(child.leftChild)).head))
          case NonterminalNodeType.Seq   => Some(builder.group(makeList(visit(child.leftChild))))
          case NonterminalNodeType.Alt   => Some(builder.alt(makeList(visit(child.leftChild))))
        }
      }

    case IntermediateNode(slot, leftExtent, rightExtent, children) =>
      if (children.size > 1) // Ambiguous node
        Some(builder.ambiguityNode(children.map(n => builder.branch(merge(n).get)), leftExtent, rightExtent))
      else
        merge(children.head)

    case PackedNode(slot, pivot, leftChild, rightChild) => throw new RuntimeException("Should not come here!")
  }

  def makeList(v: Any): Buffer[T] = v match {
      case Some(null) => Buffer(builder.cycle())
      case None => Buffer()
      case Some(StarList(Buffer(PlusList(l), r@_*))) => Buffer(builder.star(l ++ r))
      case Some(PlusList(Buffer(PlusList(l), r@_*))) => Buffer(builder.plus(l ++ r))
      case Some(StarList(l))  => Buffer(builder.star(l))
      case Some(PlusList(l))  => Buffer(builder.plus(l))
      case Some(l: Buffer[T]) => l
      case Some(x) => Buffer(x)
  }

  def flattenStar(child: Any): Any = child match {
    // A* ::= epsilon
    case Some(e:Epsilon)     => builder.star(Buffer(e))
      // A* ::= A+
    case Some(PlusList(l)) => builder.star(l)
  }

  def flattenPlus(child: Any): Any = child match {

      // A+ ::= A+ A
      case Some(Buffer(PlusList(l), r@_*)) => PlusList(l ++ r)

      // A+ ::= A
      case Some(l:Buffer[Any]) => PlusList(l)
      case Some(x) => PlusList(Buffer(x))
  }

  /**
   * Gets the children of a packed node under intermediate node
   */
  def merge(p: PackedNode): Option[Buffer[Any]] =
      for { x <- visit(p.leftChild)
          y <- visit(p.rightChild) }
      yield merge(x, y)

  def merge(x: Any, y: Any): Buffer[Any] = (x, y) match {
      case (StarList(l), y)    => Buffer(builder.star(l)) :+ y
      case (PlusList(l), y)    => Buffer(PlusList(l :+ y))
      case (x, PlusList(l))    => merge(x, builder.plus(l))
      case (x, StarList(Buffer(PlusList(l), r@_*)))  => merge(x, builder.star(l ++ r))
      case (x, StarList(l))    => merge(x, builder.star(l))
      case (l: Buffer[Any], y) => l :+ y
      case (null, null)        => Buffer(builder.cycle(), builder.cycle())
      case (null, y)           => Buffer(builder.cycle(), y)
      case (x, null)           => Buffer(x, builder.cycle())
      case _                   => Buffer(x, y)
  }

}
