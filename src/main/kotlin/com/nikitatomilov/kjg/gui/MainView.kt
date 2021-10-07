package com.nikitatomilov.kjg.gui

import javafx.event.ActionEvent
import javafx.scene.control.Button
import javafx.scene.layout.AnchorPane
import mu.KLogging
import tornadofx.View
import tornadofx.hbox
import tornadofx.label

class MainView : View() {

  override val root: AnchorPane by fxml("/fxml/main.fxml")

  private val cmdHelloWorld: Button by fxid()

  fun helloWorldPressed(event: ActionEvent?) {
    logger.info { cmdHelloWorld.text }

    //MessageBoxes.showAlert("It Works", "Hello World")
    find<HelloWorld>().openWindow()
  }

  companion object : KLogging()
}

class HelloWorld : View() {

  override val root = hbox {
    label("Hello world")
  }

  companion object : KLogging()
}