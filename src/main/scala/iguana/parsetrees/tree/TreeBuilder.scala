package iguana.parsetrees.tree

import iguana.utils.input.Input
//import scala.collection.mutable.Set

/**
 * Represents an ambiguity branch
 */
trait Branch[T] {
  def children: Seq[T]
  def leftExtent: Int
  def rightExtent: Int
}

trait TreeBuilder[T] {
  def terminalNode(l: Int, r: Int): T
  def nonterminalNode(ruleType: Any, children: Seq[T], l: Int, r: Int): T
  def ambiguityNode(children: Iterable[Branch[T]], l:Int, r:Int): T
  def branch(children: Seq[T]): Branch[T]
  def star(children: Seq[T]): T
  def plus(children: Seq[T]): T
  def opt(child: T): T
  def group(children: Seq[T]): T
  def cycle(): T
  def epsilon(i: Int): T
}

object TreeBuilderFactory {
  def getDefault(input: Input) = new DefaultTreeBuilder(input)
}

class DefaultTreeBuilder(input: Input) extends TreeBuilder[Tree] {

  type T = Tree

  override def terminalNode(l: Int, r: Int): Tree = Terminal(l, r)

  //  def layoutNode(s: Any, l: Int, r: Int): T
  override def nonterminalNode(ruleType: Any, children: Seq[Tree], l: Int, r: Int): Tree = RuleNode(ruleType, children)

  override def ambiguityNode(children: Iterable[Branch[Tree]], l: Int, r: Int): Tree = Amb(children.toSet[Branch[Tree]])

  override def branch(children: Seq[Tree]): Branch[Tree] = TreeBranch(children)

  override def cycle() = Cycle()

  override def epsilon(i: Int): Tree = Epsilon(i)

  override def star(children: Seq[Tree]): Tree = Star(children)

  override def plus(children: Seq[Tree]): Tree = Plus(children)

  override def group(children: Seq[Tree]): Tree = Group(children)

  override def opt(child: Tree): Tree = Opt(child)
}
