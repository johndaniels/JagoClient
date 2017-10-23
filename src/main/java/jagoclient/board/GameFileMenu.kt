package jagoclient.board

import jagoclient.BMPFile
import jagoclient.Global
import jagoclient.dialogs.Message
import jagoclient.gui.MyMenu
import rene.util.FileName
import rene.util.xml.XmlReader
import rene.util.xml.XmlReaderException
import java.awt.CheckboxMenuItem
import java.awt.FileDialog
import java.awt.MenuItem
import java.awt.event.ActionEvent
import java.io.*

class GameFileMenu(val board: Board, val boardState : BoardState, val frame : GoFrame, val mutable: Boolean) : MyMenu(Global.resourceString("File")) {
    var useXML: Boolean = false;
    val useXMLCheckbox: CheckboxMenuItem
    val useSGFCheckbox: CheckboxMenuItem
    private fun makeMenuItem(label: String, listener : (ActionEvent) -> Unit) : MenuItem {
        val returnValue = MenuItem(label)
        returnValue.addActionListener(listener)
        return returnValue
    }

    private fun makeCheckboxMenuItem(label: String, listener: (ActionEvent) -> Unit) : CheckboxMenuItem {
        val returnValue = CheckboxMenuItem(label)
        returnValue.addActionListener(listener)
        return returnValue
    }
    init {
        if (mutable) {
            this.add(makeMenuItem(Global.resourceString("New"), this::newGame))
            this.add(makeMenuItem(Global.resourceString("Load"), this::loadGame));
        }
        this.add(makeMenuItem(Global.resourceString("Save"), this::saveGame))
        this.addSeparator()
        useXMLCheckbox = makeCheckboxMenuItem(Global.resourceString("Use_XML"), this::setUseXML)
        useXMLCheckbox.addActionListener(this::newGame)
        this.add(useXMLCheckbox)
        useSGFCheckbox = makeCheckboxMenuItem(Global.resourceString("Use_SGF"), this::setUseSGF)
        this.add(useSGFCheckbox)
        updateState()
        this.addSeparator()
        this.add(makeMenuItem(Global.resourceString("Save_Bitmap"), this::saveBitmap))
        this.addSeparator()
    }

    private fun updateState() {
        this.useXMLCheckbox.state = useXML
        this.useSGFCheckbox.state = !useXML
    }

    private fun newGame(event : ActionEvent) {
    }

    private fun loadGame(event: ActionEvent) {
        val fd = FileDialog(frame, Global
                .resourceString("Load_Game"), FileDialog.LOAD)
        fd.setFilenameFilter({ dir, name -> name.endsWith("." + extension()) })
        fd.setFile("*." + extension())
        frame.center(fd)
        fd.setVisible(true)
        val fn = fd.getFile()
        try
        // print out using the board class
        {
            if (useXML) {
                val `in` = FileInputStream(fd.getDirectory() + fn)
                try {
                    boardState.loadXml(XmlReader(`in`))
                } catch (e: XmlReaderException) {
                    Message(frame, "Error in file!\n" + e.text).isVisible = true
                }

                `in`.close()
            } else {
                val fi: BufferedReader
                if (Global.isApplet())
                    fi = BufferedReader(InputStreamReader(
                            FileInputStream(fd.getDirectory() + fn), "utf-8"))
                else
                    fi = BufferedReader(InputStreamReader(
                            FileInputStream(fd.getDirectory() + fn), "utf-8"))
                try {
                    boardState.load(fi)
                } catch (e: IOException) {
                    Message(frame, "Error in file!").isVisible = true
                }

                fi.close()
            }
        } catch (ex: IOException) {
            Message(frame, Global.resourceString("Read_error_") + "\n"
                    + ex.toString()).isVisible = true
            return
        }

        val s = boardState.firstnode().getaction(Action.Type.GAME_NAME)
        if (s != null && s != "")
            frame.setTitle(s)
        else {
            boardState.firstnode().setaction(Action.Type.GAME_NAME, FileName.purefilename(fn))
            frame.setTitle(FileName.purefilename(fn))
        }
        if (fn.toLowerCase().indexOf("kogo") >= 0)
            board.setVariationStyle(false, false)
    }
    private fun saveGame(event: ActionEvent) {
        val fd = FileDialog(frame, Global.resourceString("Save"),
                FileDialog.SAVE)
        val s = boardState.firstnode().getaction(Action.Type.GAME_NAME)
        if (s != null && s != "")
            fd.setFile(s + "." + extension())
        else
            fd.setFile("*." + Global.getParameter("extension", extension()))
        fd.setFilenameFilter({ dir, name -> name.endsWith("." + extension()) })
        frame.center(fd)
        fd.setVisible(true)
        val fn = fd.getFile()
        frame.setGameTitle(FileName.purefilename(fn))
        if (fn == null) return
        val fo: PrintWriter
        if (useXML) {
            fo = PrintWriter(OutputStreamWriter(
                    FileOutputStream(fd.getDirectory() + fn),
                    "UTF8"))
            boardState.saveXML(fo, "utf-8")
        } else {
            fo = PrintWriter(OutputStreamWriter(
                    FileOutputStream(fd.getDirectory() + fn), Global
                    .getParameter("encoding", System
                            .getProperty("file.encoding"))))
            boardState.save(fo)
        }
        fo.close()
    }

    private fun extension() : String {
        if (useXML) {
            return "xml";
        } else {
            return "sgf"
        }
    }

    private fun setUseXML(event: ActionEvent) {
        useXML = true
        updateState()
    }

    private fun setUseSGF(event: ActionEvent) {
        useXML = false
        updateState()
    }

    private fun saveBitmap(event: ActionEvent) {
        val fd = FileDialog(frame, Global
                .resourceString("Save_Bitmap"), FileDialog.SAVE)
        val s = boardState.firstnode().getaction(Action.Type.GAME_NAME)
        if (s != null && s != "")
            fd.setFile(s + "." + Global.getParameter("extension", "bmp"))
        else
            fd.setFile("*." + Global.getParameter("extension", "bmp"))
        fd.setFilenameFilter({dir, name ->  name.endsWith(".bmp")})
        frame.center(fd)
        fd.setVisible(true)
        val fn = fd.getFile() ?: return
        try
        // print out using the board class
        {
            val F = BMPFile()
            val d = board.getBoardImageSize()
            F.saveBitmap(fd.getDirectory() + fn, board.getBoardImage(),
                    d.width, d.height)
        } catch (ex: Exception) {
            Message(frame, Global.resourceString("Write_error_") + "\n"
                    + ex.toString()).isVisible = true
            return
        }


    }
}