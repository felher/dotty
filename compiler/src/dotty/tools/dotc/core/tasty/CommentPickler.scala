package dotty.tools.dotc.core.tasty

import dotty.tools.dotc.ast.{tpd, untpd}
import dotty.tools.dotc.core.Comments.{Comment, CommentsContext, ContextDocstrings}
import dotty.tools.dotc.core.Contexts._

import dotty.tools.tasty.TastyBuffer
import TastyBuffer.{Addr, NoAddr}

import java.nio.charset.Charset

class CommentPickler(pickler: TastyPickler, addrOfTree: tpd.Tree => Addr, docString: untpd.MemberDef => Option[Comment]):
  private val buf = new TastyBuffer(5000)
  pickler.newSection("Comments", buf)

  def pickleComment(root: tpd.Tree): Unit = traverse(root)

  private def pickleComment(addr: Addr, comment: Comment): Unit =
    if addr != NoAddr then
      val bytes = comment.raw.getBytes(Charset.forName("UTF-8"))
      val length = bytes.length
      buf.writeAddr(addr)
      buf.writeNat(length)
      buf.writeBytes(bytes, length)
      buf.writeLongInt(comment.span.coords)

  private def traverse(x: Any): Unit = x match
    case x: untpd.Tree @unchecked =>
      x match
        case x: tpd.MemberDef @unchecked => // at this point all MembderDefs are t(y)p(e)d.
          for comment <- docString(x) do pickleComment(addrOfTree(x), comment)
        case _ =>
      val limit = x.productArity
      var n = 0
      while n < limit do
        traverse(x.productElement(n))
        n += 1
    case y :: ys =>
      traverse(y)
      traverse(ys)
    case _ =>

end CommentPickler

