//> using dep "org.jsoup:jsoup:1.17.2"
//> using dep "com.lihaoyi::scalatags:0.13.1"

import scala.util.matching.Regex
import scala.annotation.tailrec
import scala.io.Source
import scala.util.{Try, Success, Failure}
import scalatags.Text.all._
import scalatags.Text.tags2
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.{File, PrintWriter}
import scala.io.Source
import java.nio.file.{Files, Paths, StandardCopyOption}

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
  private val emptyLinePattern = "^\\s*$".r

  def tokenize(lines: List[String]): List[MarkdownElement] = {
  @tailrec
  def recHelper(
      remainingLines: List[String],
      elements: List[MarkdownElement] = List(),
      listItems: List[ListItem] = List(),
      paragraphLines: List[String] = List(),
      codeLanguage: String = "",
      codeContent: List[String] = List(),
      inCodeBlock: Boolean = false
  ): List[MarkdownElement] = remainingLines match {
    case Nil =>
      val finalElements = (paragraphLines.nonEmpty, listItems.nonEmpty, inCodeBlock) match {
        case (true, _, _) => elements :+ Paragraph(parseInlineElements(paragraphLines.mkString(" ")))
        case (_, true, _) => elements :+ UnorderedList(listItems)
        case (_, _, true) => elements :+ CodeBlock(codeLanguage, codeContent.mkString("\n"))
        case _ => elements
      }
      finalElements

    case line :: rest => line match {
      case headerPattern(content) =>
        val newElements = (paragraphLines.nonEmpty, listItems.nonEmpty) match {
          case (true, _) => elements :+ Paragraph(parseInlineElements(paragraphLines.mkString(" ")))
          case (_, true) => elements :+ UnorderedList(listItems)
          case _ => elements
        }
        val level = content.takeWhile(_ == '#').size
        recHelper(rest, newElements :+ Header(level, parseInlineElements(content.drop(level).trim)))

      case listItemPattern(item) =>
        recHelper(rest, elements, listItems :+ ListItem(parseInlineElements(item)))

      case codeBlockPattern(lang) if inCodeBlock =>
        recHelper(rest, elements :+ CodeBlock(codeLanguage, codeContent.mkString("\n")))

      case codeBlockPattern(lang) if lang.nonEmpty =>
        recHelper(rest, elements, List(), List(), lang, List(), true)

      case codeBlockPattern(_) =>
        recHelper(rest, elements, List(), List(), "", List(), false)

      case line if inCodeBlock =>
        recHelper(rest, elements, listItems, paragraphLines, codeLanguage, codeContent :+ line, true)

      case imagePattern(alt, src) =>
        val newElements = (paragraphLines.nonEmpty, listItems.nonEmpty) match {
          case (true, _) => elements :+ Paragraph(parseInlineElements(paragraphLines.mkString(" ")))
          case (_, true) => elements :+ UnorderedList(listItems)
          case _ => elements
        }
        recHelper(rest, newElements :+ Image(alt, src))

      case emptyLinePattern() =>
        val newElements = (paragraphLines.nonEmpty, listItems.nonEmpty) match {
          case (true, _) => elements :+ Paragraph(parseInlineElements(paragraphLines.mkString(" ")))
          case (_, true) => elements :+ UnorderedList(listItems)
          case _ => elements
        }
        recHelper(rest, newElements)

      case text =>
        recHelper(rest, elements, listItems, paragraphLines :+ text)
    }
  }
  recHelper(lines)
}

  def parseInlineElements(text: String): String = {
    linkPattern.replaceAllIn(
      boldPattern.replaceAllIn(text, m => s"<b>${m.group(1)}</b>"),
      m => s"<a href='${m.group(2)}'>${m.group(1)}</a>"
    )
  }

  def toHtml(elements: List[MarkdownElement]): String = elements
    .filter {
      case Paragraph(content) if content.isEmpty => false
      case _                                     => true
    }
    .map {
      case Header(level, content) => s"<h$level>$content</h$level>"
      case Paragraph(content)     => s"""<p>$content</p>"""
      case ListItem(content)      => s"<li>$content</li>"
      case UnorderedList(items) =>
        s"<ul>${items.map(item => toHtml(List(item))).mkString}</ul>"
      case Link(text, url) => s"<a href='$url'>$text</a>"
      case Image(alt, src) => s"<img src='$src' alt='$alt' />"
      case Bold(text)      => s"<b>$text</b>"
      case CodeBlock(language, code) =>
        s"""<pre class='code'><code class='$language'>$code</code></pre>"""
    }
    .mkString("\n")

  def ppHtml(html: String, iconPath: String): String = {
    val document: Document = Jsoup.parse(html)
    val head               = document.head()
    head.empty()

    head.appendElement("meta").attr("charset", "UTF-8")
    head
      .appendElement("meta")
      .attr("name", "viewport")
      .attr("content", "width=device-width, initial-scale=1.0")
    head
      .appendElement("script")
      .attr(
        "src",
        "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.6.0/highlight.min.js"
      )
    head.appendElement("script").text("hljs.highlightAll();")
    head.appendElement("script").attr("src", "script.js")
    head
      .appendElement("link")
      .attr("rel", "stylesheet")
      .attr(
        "href",
        "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.6.0/styles/default.min.css"
      )
    head
      .appendElement("link")
      .attr("rel", "icon")
      .attr("type", "image/png")
      .attr("href", iconPath)
    head
      .appendElement("link")
      .attr("rel", "stylesheet")
      .attr("href", "styles.css")

    document.outputSettings().prettyPrint(true)
    document.html()
  }
}


object SiteGenerator {

  /** SiteGenerator creates directories and files for nargosite structures. Each
    * nargosite has the following structure:
    *   - index.html
    *   - site_1.html ... site_n.html
    *   - style.css
    *   - script.js
    *   - pix/pic_1.png
    */

  case class HtmlPage(fpath: String)
  case class StyleSheet(fpath: String)
  case class JsFile(fpath: String)
  case class Pix(dir: String)
  case class Icon(fpath: String)
  case class DataFile(fpath: String)

  case class Website(
      name: String,
      pages: List[HtmlPage],
      css: StyleSheet,
      js: JsFile,
      pixPath: Pix,
      iconPath: Icon,
      dataFiles: Option[List[DataFile]]
  )

  private val RECIPES_DIR            = "recipes"
  private val SETUP_NOTES_DIR        = "setup-notes"
  private val MATTHIEU_DIR           = "matthieu-mds"
  private val STYLES_DIR             = "css"
  private val SCRIPTS_DIR            = "js"
  private val PIX_DIR                = "pix"
  private val INDEX_NARGOTHROND_HTML = "html/index-nargothrond.xyz.html"
  private val INDEX_FOOD_HTML        = "html/index-food.nargothrond.xyz.html"
  private val INDEX_MATTHIEU_HTML    = "html/index-matthieucourt.xyz.html"

  private def deleteDirectory(directory: File): Try[Unit] = Try {
    if (directory.exists()) {
      directory.listFiles().foreach { file =>
        if (file.isDirectory) deleteDirectory(file)
        else file.delete()
      }
      directory.delete()
    }
  }

  private def processMarkdownFile(mdFile: File, site: Website): Try[Unit] =
    Try {
      val source = Source.fromFile(mdFile)
      val lines  = source.getLines().toList
      source.close()

      val elements          = MarkdownParser.tokenize(lines)
      val htmlContent       = MarkdownParser.toHtml(elements)
      val prettyHtmlContent = MarkdownParser.ppHtml(htmlContent, site.iconPath.fpath)

      val htmlFile = new File(site.name, mdFile.getName.replace(".md", ".html"))
      val writer   = new PrintWriter(htmlFile)
      writer.write(prettyHtmlContent)
      writer.close()
    }

  private def copyFiles(src: File, dest: File): Try[Unit] = Try {
  if (src.exists()) {
    if (src.isDirectory) {
      dest.mkdir()
      src.listFiles().foreach { file =>
        copyFiles(file, new File(dest, file.getName)).get
      }
    } else {
      Files.copy(src.toPath, dest.toPath, StandardCopyOption.REPLACE_EXISTING)
    }
  }
}

  private def getMarkdownFiles(directory: String): List[HtmlPage] = {
    Option(new File(directory).listFiles())
      .toList.flatten
      .filter(_.getName.endsWith(".md"))
      .map(file => HtmlPage(file.getPath))
  }

  private def createSiteDirectory(site: Website): Try[Unit] = {

    val siteDir = new File(site.name)
    val dataDir = new File(siteDir, "data")
    for {
      _ <- deleteDirectory(siteDir).orElse(Success(()))
      _ <- Try(siteDir.mkdir())
      _ <- Try(Files.copy(Paths.get(site.css.fpath), Paths.get(site.name, "styles.css"), StandardCopyOption.REPLACE_EXISTING))
      _ <- Try(Files.copy(Paths.get(site.js.fpath), Paths.get(site.name, "script.js"), StandardCopyOption.REPLACE_EXISTING))
      _ <- copyFiles(new File(site.pixPath.dir), new File(site.name, "pix"))
      _ <- Try {
        site.pages.foreach { page =>
          val file = new File(page.fpath)
          file match {
            case f if f.getName.endsWith(".md") => processMarkdownFile(f, site).get
            case f if f.getName.endsWith(".html") && f.getName.contains("index") =>
              Files.copy(f.toPath, Paths.get(site.name, "index.html"), StandardCopyOption.REPLACE_EXISTING)
            case f =>
              Files.copy(f.toPath, Paths.get(site.name, f.getName), StandardCopyOption.REPLACE_EXISTING)
          }
        }
      }
      _ <- site.dataFiles match {
      case Some(dataFiles) =>
        for {
          _ <- Try(dataDir.mkdir())
          _ <- Try {
            dataFiles.foreach { dataFile =>
              copyFiles(new File(dataFile.fpath), new File(dataDir, new File(dataFile.fpath).getName)).get
            }
          }
        } yield ()
      case None => Success(())
    }
      _ <- if (site.name == "food.nargothrond.xyz") createFoodHtml(siteDir) else Success(())
    } yield ()
  }

  private def createFoodHtml(siteDir: File): Try[Unit] = {
  val recipesDir = new File(RECIPES_DIR)
  val recipes = Option(recipesDir.listFiles())
    .toList.flatten
    .filter(_.getName.endsWith(".md"))
    .map(_.getName.stripSuffix(".md"))

    val foodHtmlContent =
  html(
    head(
      meta(charset := "UTF-8"),
      meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
      tag("title")("food.nargothrond.xyz"),
      link(href := "https://fonts.googleapis.com/css2?family=Roboto:wght@400;500&display=swap", rel := "stylesheet"),
      link(href := "pix/food.png", rel := "icon"),
      tags2.style(
        """
        body {
          font-family: 'Arial', sans-serif;
        }
        .search-container {
          display: flex;
          justify-content: center;
          width: 100%;
        }
        """
      )
    ),
    body(
      header(
        div(
          `style` := """display: flex; align-items: center; justify-content: center; 
                        flex-wrap: wrap; padding: 20px 0; background-color: #fff; 
                        width: 100%; text-align: center;"""
        )(
          img(
            src := "pix/food.png",
            alt := "food",
            widthA := 40,
            heightA := 40,
            `style` := "margin-right: 10px;"
          ),
          h1(
            `style` := "color: #333; margin: 0; display: inline-block;"
          )("food.nargothrond.xyz")
        ),
        p(
          `style` := "width: 100%; margin: 10px 0 0; text-align: center;"
        )("No ads, no spyware, just recipes."),
        div(
          `class` := "search-container"
        )(
          input(
            `type` := "text",
            id := "searchBar",
            placeholder := "Search recipes...",
            onkeyup := "filterRecipes()",
            `style` := """padding: 10px; width: 50%; max-width: 300px; margin: 10px 0; 
                          border: 2px solid #ccc; border-radius: 5px; color: #333; 
                          background-color: #fff;"""
          )
        )
      ),
      div(
        `class` := "recipe-container",
        `style` := """display: flex; flex-wrap: wrap; justify-content: space-between; 
                      max-width: 640px; margin: 20px auto; row-gap: 5px;"""
      )(
        recipes.map { recipe =>
          a(
            href := s"${recipe.replaceAll(" ", "-").toLowerCase}.html",
            `class` := "recipe",
            attr("data-name") := recipe,
            `style` := """width: 30%; padding: 2px; margin: 5px 0; text-align: center; 
                          color: #007bff; text-decoration: none; transition: color 0.3s ease;"""
          )(recipe)
        }
      ),
      script(src := "script.js")
    )
  ).render

  Try {
    val foodHtmlFile = new File(siteDir, "index.html")
    val writer = new PrintWriter(foodHtmlFile)
    writer.write(foodHtmlContent)
    writer.close()
  }
}

  def main(args: Array[String]): Unit = {
    val sites = List(
      Website(
        name = "nargothrond.xyz",
        pages = getMarkdownFiles(SETUP_NOTES_DIR) ++ List(HtmlPage(INDEX_NARGOTHROND_HTML)),
        css = StyleSheet(s"$STYLES_DIR/styles-nargothrond.xyz.css"),
        js = JsFile(s"$SCRIPTS_DIR/script-food.nargothrond.xyz.js"),
        pixPath = Pix(s"$PIX_DIR/nargothrond.xyz"),
        iconPath = Icon(s"$PIX_DIR/nargothrond.png"),
        dataFiles = None
      ),
      Website(
        name = "food.nargothrond.xyz",
        pages = getMarkdownFiles(RECIPES_DIR) ++ List(HtmlPage(INDEX_FOOD_HTML)),
        css = StyleSheet(s"$STYLES_DIR/styles-food.nargothrond.xyz.css"),
        js = JsFile(s"$SCRIPTS_DIR/script-food.nargothrond.xyz.js"),
        pixPath = Pix(s"$PIX_DIR/food.nargothrond.xyz"),
        iconPath = Icon(s"$PIX_DIR/food.png"),
        dataFiles = None
      ),
      Website(
        name = "matthieucourt.xyz",
        pages = getMarkdownFiles(MATTHIEU_DIR) ++ List(HtmlPage(INDEX_MATTHIEU_HTML)),
        css = StyleSheet(s"$STYLES_DIR/styles-matthieucourt.xyz.css"),
        js = JsFile(s"$SCRIPTS_DIR/script-food.nargothrond.xyz.js"),
        pixPath = Pix(s"$PIX_DIR/matthieucourt.xyz"),
        iconPath = Icon(s"$PIX_DIR/matthieu-tengwar.png"),
        dataFiles = Some(List(DataFile("data/matthieu-pub.asc")))
      )
    )

    sites.foreach { site =>
      createSiteDirectory(site) match {
        case Success(_) => println(s"✅ Successfully created site: ${site.name}")
        case Failure(exception) => println(s"❌ Failed to create site: ${site.name}, due to: ${exception.getMessage}")
      }
    }
  }
}
