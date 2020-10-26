// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl.welcomeScreen.learnIde

import com.intellij.icons.AllIcons.Ide.External_link_arrow
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.HelpTopicsAction
import com.intellij.ide.actions.JetBrainsTvAction
import com.intellij.ide.actions.OnlineDocAction
import com.intellij.ide.actions.WhatsNewAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.wm.InteractiveCourseFactory
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeScreenUIManager
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.LearnIdeContentColorsAndFonts.HEADER
import com.intellij.ui.AncestorListenerAdapter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.AncestorEvent

class LearnIdeContentPanel : JPanel() {

  private val interactiveCoursesPanel: JPanel = JPanel()
  private val helpAndResourcesPanel: JPanel = JPanel()

  private val viewComponent: JPanel = JPanel().apply { layout = BorderLayout(); background = WelcomeScreenUIManager.getProjectsBackground() }
  private val myScrollPane: JBScrollPane = JBScrollPane(viewComponent, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER).apply { border = JBUI.Borders.empty()}

  private val interactiveCoursesHeader: JTextPane = HeightLimitedPane(IdeBundle.message("welcome.screen.learnIde.interactive.courses.text"),
                                                                      5, HEADER)

  private val helpAndResourcesHeader: JTextPane = HeightLimitedPane(IdeBundle.message("welcome.screen.learnIde.help.and.resources.text"), 5, HEADER)

  init {
    layout = BorderLayout()
    isFocusable = false
    isOpaque = true
    background = WelcomeScreenUIManager.getProjectsBackground()

    val contentPanel = JPanel()
    contentPanel.layout = BoxLayout(contentPanel, BoxLayout.PAGE_AXIS)

    //get all Interactive Courses
    contentPanel.addInteractiveCoursesPanel()
    contentPanel.addHelpAndResourcesPanel()
    contentPanel.add(Box.createVerticalGlue())

    //set LearnPanel UI
    contentPanel.border = EmptyBorder(24, 24, 24, 24)
    contentPanel.isOpaque = false
    viewComponent.add(contentPanel, BorderLayout.CENTER)
    add(myScrollPane, BorderLayout.CENTER)

    contentPanel.bounds = Rectangle(contentPanel.location, contentPanel.preferredSize)
    revalidate()
    repaint()
  }

  private fun Container.addHelpAndResourcesPanel() {
    helpAndResourcesPanel.apply {
      layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
      isOpaque = false
      add(helpAndResourcesHeader)
      add(rigid(0, 1))
      addHelpActions()
    }
    this.add(helpAndResourcesPanel)
  }

  private fun Container.addInteractiveCoursesPanel() {
    val interactiveCourses = InteractiveCourseFactory.INTERACTIVE_COURSE_FACTORY_EP.extensions.map {it.getInteractiveCourseData()}
    interactiveCoursesPanel.layout = BoxLayout(interactiveCoursesPanel, BoxLayout.PAGE_AXIS)
    interactiveCoursesPanel.isOpaque = false

    if (interactiveCourses.isNotEmpty()) {
      interactiveCoursesPanel.add(interactiveCoursesHeader)
      var actionButton: JButton? = null
      for (interactiveCourse in interactiveCourses) {
        interactiveCoursesPanel.add(rigid(0, 12))
        val interactiveCoursePanel = InteractiveCoursePanel(interactiveCourse)
        interactiveCoursesPanel.add(interactiveCoursePanel)
        if (actionButton == null) actionButton = interactiveCoursePanel.startLearningButton
      }
      addAncestorListener(object : AncestorListenerAdapter() {
        override fun ancestorAdded(event: AncestorEvent?) {
          rootPane?.defaultButton = actionButton
        }
      })
      this.add(interactiveCoursesPanel)
      this.add(rigid(1, 32))
    }
  }


  private fun addHelpActions() {
    val whatsNewAction = WhatsNewAction()
    if (emptyWelcomeScreenEventFromAction(whatsNewAction).presentation.isEnabled) {
      helpAndResourcesPanel.add(linkLabelByAction(WhatsNewAction()))
      helpAndResourcesPanel.add(rigid(1, 16))
    }
    val helpActions = ActionManager.getInstance().getAction(IdeActions.GROUP_WELCOME_SCREEN_LEARN_IDE) as ActionGroup
    val anActionEvent = emptyWelcomeScreenEventFromAction(helpActions)
    helpActions.getChildren(anActionEvent).forEach {
      if (setOf<String>(HelpTopicsAction::class.java.simpleName, OnlineDocAction::class.java.simpleName,
                        JetBrainsTvAction::class.java.simpleName).any { simpleName ->  simpleName == it.javaClass.simpleName }) {
        helpAndResourcesPanel.add(linkLabelByAction(it).wrapWithUrlPanel())
      }
      else {
        helpAndResourcesPanel.add(linkLabelByAction(it))

      }
      helpAndResourcesPanel.add(rigid(1, 6))
    }
  }

  private fun LinkLabel<Any>.wrapWithUrlPanel(): JPanel {
    val jPanel =  JPanel()
    jPanel.isOpaque = false
    jPanel.layout = BoxLayout(jPanel, BoxLayout.LINE_AXIS)
    jPanel.add(this, BorderLayout.CENTER)
    jPanel.add(JLabel(External_link_arrow), BorderLayout.EAST)
    jPanel.maximumSize = jPanel.preferredSize
    jPanel.alignmentX = LEFT_ALIGNMENT
    return jPanel
  }

  private fun emptyWelcomeScreenEventFromAction(action: AnAction) =
    AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, DataContext.EMPTY_CONTEXT)

  private fun linkLabelByAction(it: AnAction): LinkLabel<Any> {
    return LinkLabel<Any>(it.templateText, null).apply {
      alignmentX = LEFT_ALIGNMENT
      setListener({ _, _ -> performActionOnWelcomeScreen(it) }, null)
    }
  }

  private fun rigid(_width: Int, _height: Int): Component {
    return Box.createRigidArea(
      Dimension(JBUI.scale(_width), JBUI.scale(_height))).apply { (this as JComponent).alignmentX = LEFT_ALIGNMENT }
  }

  private fun performActionOnWelcomeScreen(action: AnAction) {
    val anActionEvent = AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, DataContext.EMPTY_CONTEXT)
    ActionUtil.performActionDumbAware(action, anActionEvent)
  }

}