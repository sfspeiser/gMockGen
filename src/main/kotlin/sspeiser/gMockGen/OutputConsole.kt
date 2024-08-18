package sspeiser.gMockGen

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

enum class ConsoleLogLevel {
    INFO, ERROR
}

@Service(Service.Level.PROJECT)
class OutputConsole(project: Project) {
    private val myProject: Project = project

    @get:Synchronized
    private var consoleView: ConsoleView? = null
        get() {
            if (field == null) {
                field = createConsoleView(myProject)
            }
            return field
        }

    private fun createConsoleView(project: Project): ConsoleView {
        val newConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        val toolWindow: ToolWindow? = ToolWindowManager.getInstance(project)
            .getToolWindow("gMockGen")
        ApplicationManager.getApplication().invokeLater {
            if (toolWindow != null) {
                toolWindow.show {
                    val content: Content = toolWindow.getContentManager()
                        .getFactory()
                        .createContent(newConsoleView.component, "gMock Generator", true)
                    toolWindow.getContentManager().addContent(content)
                }
            }
        }
        return newConsoleView
    }

    fun log(msg: String?, logLevel: ConsoleLogLevel) {
        val consoleViewContentType = if (logLevel == ConsoleLogLevel.INFO) {
            ConsoleViewContentType.NORMAL_OUTPUT
        } else if (logLevel == ConsoleLogLevel.ERROR) {
            ConsoleViewContentType.ERROR_OUTPUT
        } else {
            throw IllegalArgumentException(String.format("Unknown log level %s", logLevel))
        }
        consoleView?.print(
            java.lang.String.format("%s %s > %s\n", logLevel, nowFormatted, msg),
            consoleViewContentType
        )
    }

    fun info(msg: String?) {
        log(msg, ConsoleLogLevel.INFO)
    }

    fun error(msg: String?) {
        log(msg, ConsoleLogLevel.ERROR)
    }

    private val nowFormatted: String
        get() = DateTimeFormat.forPattern("HH:mm:ss.SSS").print(DateTime.now())

    fun clear() {
        consoleView!!.clear()
    }

    companion object {
        fun get(project: Project): OutputConsole {
            return project.getService(OutputConsole::class.java)
        }
    }
}