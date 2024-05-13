//> using lib "org.jsoup:jsoup:1.17.2"
import scala.util.matching.Regex
import org.jsoup.Jsoup

/*
object SiteGenerator {

  case class HtmlPage(name: String, content: String)
  case class StyleSheet(localPath: String, remotePath: String)
  case class JsFile(localPath: String, remotePath: String)
  case class Pix(localPath: String, remotePath: String)

  case class Website(
      name: String,
      pages: List[HtmlPage],
      css: Option[StyleSheet],
      js: Option[JsFile],
      pixPath: Option[Pix]
  )

  val matthieuCourtXyz =
    Website("matthieucourt.xyz", Some("/css"), None, Some("/pix"))
  val nargothrondXyz =
    Website("nargothrond.xyz", Some("/css"), None, Some("/pix"))
  val foodNargothrondXyz =
    Website("food.nargothrond.xyz", Some("/css"), Some("/js"), Some("/pix"))

}
 */

sealed trait MarkdownElement
case class Header(level: Int, content: String)          extends MarkdownElement
case class Paragraph(content: String)                   extends MarkdownElement
case class ListItem(content: String)                    extends MarkdownElement
case class UnorderedList(items: List[ListItem])         extends MarkdownElement
case class CodeBlock(language: String, content: String) extends MarkdownElement
case class Link(text: String, url: String)              extends MarkdownElement
case class Image(alt: String, src: String)              extends MarkdownElement
case class Bold(text: String)                           extends MarkdownElement

object MarkdownParser {
  private val headerPattern    = "(^#+\\s*.*)".r
  private val listItemPattern  = "^-\\s+(.*)".r
  private val boldPattern      = "\\*\\*(.*?)\\*\\*".r
  private val linkPattern      = "\\[(.*?)\\]\\((.*?)\\)".r
  private val imagePattern     = "!\\[(.*?)\\]\\((.*?)\\)".r
  private val codeBlockPattern = "^```(\\w*)".r

  def tokenize(lines: List[String]): List[MarkdownElement] = {
    var elements: List[MarkdownElement] = List()
    var listItems: List[ListItem]       = List()
    var paragraphLines: List[String]    = List()
    var inCodeBlock: Boolean            = false
    var codeLanguage: String            = ""
    var codeContent: List[String]       = List()

    def flushParagraph(): Unit = {
      if (paragraphLines.nonEmpty) {
        elements :+= Paragraph(
          parseInlineElements(paragraphLines.mkString("\n"))
        )
        paragraphLines = List()
      }
    }

    def flushListItems(): Unit = {
      if (listItems.nonEmpty) {
        elements :+= UnorderedList(listItems)
        listItems = List()
      }
    }

    lines.foreach {
      case headerPattern(content) =>
        flushParagraph()
        flushListItems()
        val level = content.takeWhile(_ == '#').size
        elements :+= Header(
          level,
          parseInlineElements(content.drop(level).trim)
        )
      case listItemPattern(item) =>
        flushParagraph()
        listItems :+= ListItem(parseInlineElements(item))
      case codeBlockPattern(lang) if !lang.isEmpty =>
        flushParagraph()
        flushListItems()
        inCodeBlock = true
        codeLanguage = lang
      case codeBlockPattern(lang) if lang.isEmpty =>
        inCodeBlock = false
        elements :+= CodeBlock(codeLanguage, codeContent.mkString("\n"))
        codeContent = List()
      case line if inCodeBlock =>
        codeContent :+= line
      case imagePattern(alt, src) =>
        flushParagraph()
        flushListItems()
        elements :+= Image(alt, src)
      case text =>
        paragraphLines :+= text
    }
    flushParagraph()
    flushListItems()
    elements
  }

  def parseInlineElements(text: String): String = {
    linkPattern.replaceAllIn(
      boldPattern.replaceAllIn(text, m => s"<b>${m.group(1)}</b>"),
      m => s"<a href='${m.group(2)}'>${m.group(1)}</a>"
    )
  }

  def toHtml(elements: List[MarkdownElement]): String = elements
    .filter {
      case Paragraph(content) if content.isEmpty =>
        false
      case _ =>
        true
    }
    .map {
      case Header(level, content) => s"<h$level>$content</h$level>"
      case Paragraph(content) => s"""|<p>
                                     |  $content
                                     |</p>""".stripMargin
      case ListItem(content) => s"<li>$content</li>"
      case UnorderedList(items) =>
        s"<ul>${items.map(item => toHtml(List(item))).mkString}</ul>\n"
      case Link(text, url) => s"<a href='$url'>$text</a>"
      case Image(alt, src) => s"<img src='$src' alt='$alt' />"
      case Bold(text)      => s"<b>$text</b>"
      case CodeBlock(language, code) =>
        s"""|<pre class='code'><code class='$language'>
            |$code
            |</code></pre>""".stripMargin
    }
    .mkString("\n")

  def ppHtml(html: String): String = {
    val document = Jsoup.parse(html)
    document.outputSettings().prettyPrint(true)
    document.html()
  }

  def main(args: Array[String]): Unit = {
    val markdownExample = """
      |# Header 1
      |## Header 2
      |### Header 3
      |#### Header 4
      |**Bold text here**
      |[test link](https://www.example.com)
      |![Logo](https://www.logo.png)
      |- Item 1
      |- Item 2
      |```scala
      |val text = "Hello, Scala world!"
      |println(text)
      |```
      |```python
      |print("Hello, Python world!")
      |print("foobar")
      |```
      |More text follows here.
      |And this text is technically **still** in the same paragraph.
      |""".stripMargin

    val lines       = markdownExample.split("\n").toList
    val elements    = tokenize(lines)
    val htmlContent = toHtml(elements)
    println(ppHtml(htmlContent))
  }
}
