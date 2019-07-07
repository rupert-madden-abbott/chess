package com.maddenabbott.game

import com.almasb.fxgl.app.GameApplication
import com.almasb.fxgl.app.GameSettings
import com.almasb.fxgl.core.math.Vec2
import com.almasb.fxgl.dsl.entityBuilder
import com.almasb.fxgl.entity.Entity
import com.almasb.fxgl.entity.EntityFactory
import com.maddenabbott.game.framework.ceil
import com.maddenabbott.game.framework.isEven
import com.maddenabbott.game.framework.isOdd
import javafx.geometry.Dimension2D
import javafx.geometry.Insets
import javafx.scene.Group
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.Text

class Piece(
  val letter: Char,
  val color: Color,
  val square: Square
)

class Square(
  val row: Int,
  val column: Int,
  val length: Double,
  val color: Color
) {
// Game coordinates begin in lower left but application coordinates begin in upper left. Therefore rows must be
// "flipped" whereas columns can stay the same.
  val position = Vec2((column - 1) * length, ((-row + 9) - 1) * length)
}

class Board(
  columns: Int,
  rows: Int,
  size: Dimension2D
) {

  val squares = (1..(rows * columns)).map {
    val column = (it / rows.toDouble()).ceil()
    val row = it - (rows * (column - 1))
    val color = if ((column.isEven() && row.isOdd()) || (column.isOdd() && row.isEven())) {
      Color.WHITE
    } else {
      Color.DARKGRAY.darker()
    }

    Square(row, column, size.width / columns, color)
  }

  fun get(column: Int, row: Int) = squares.find { it.column == column && it.row == row }!!
}

class GameEntityFactory : EntityFactory {
  fun addSquare(square: Square) {
    val group = Group()

    val children = group.children

    val rectangle = Rectangle(square.length, square.length, square.color)
    children.add(rectangle)

    if (square.row == 1) {
      val text = Text(square.column.toString())
      text.font = Font(square.length)
      text.fill = Color.BLACK
      text.viewOrder = -1.0
      text.x = square.position.x + (square.length / 2)
      text.y = square.position.y + (square.length / 2)

      children.add(text)
    }

    entityBuilder()
      .at(square.position)
      .view(group)
      .buildAndAttach()
  }

  fun addPiece(piece: Piece): Entity {
    val pane = StackPane()

    val square = piece.square

    val text = Text(piece.letter.toString())
    text.font = Font(square.length / 2)
    text.fill = piece.color.invert()
    text.viewOrder = -1.0

    val length = square.length / 2
    val padding = length / 100 * 10
    val border = Circle(length - padding, Color.BLACK)

    val base = Circle(border.radius - (border.radius / 100 * 2), piece.color)

    pane.children.add(text)
    pane.children.add(border)
    pane.children.add(base)

    pane.padding = Insets(padding)

    return entityBuilder()
      .at(square.position)
      .view(pane)
      .buildAndAttach()
  }
}

class Game : GameApplication() {
  private val size = Dimension2D(800.0, 800.0)

  private val factory = GameEntityFactory()

  override fun initSettings(settings: GameSettings) {
    settings.width = size.width.toInt()
    settings.height = size.height.toInt()
  }

  override fun initGame() {
    val board = Board(8, 8, size)
    board.squares.forEach(factory::addSquare)

    factory.addPiece(Piece('R', Color.WHITE, board.get(1, 1)))
    factory.addPiece(Piece('N', Color.WHITE, board.get(2, 1)))
    factory.addPiece(Piece('B', Color.WHITE, board.get(3, 1)))
    factory.addPiece(Piece('Q', Color.WHITE, board.get(4, 1)))
    factory.addPiece(Piece('K', Color.WHITE, board.get(5, 1)))
    factory.addPiece(Piece('B', Color.WHITE, board.get(6, 1)))
    factory.addPiece(Piece('N', Color.WHITE, board.get(7, 1)))
    factory.addPiece(Piece('R', Color.WHITE, board.get(8, 1)))
    factory.addPiece(Piece('P', Color.WHITE, board.get(1, 2)))
    factory.addPiece(Piece('P', Color.WHITE, board.get(2, 2)))
    factory.addPiece(Piece('P', Color.WHITE, board.get(3, 2)))
    factory.addPiece(Piece('P', Color.WHITE, board.get(4, 2)))
    factory.addPiece(Piece('P', Color.WHITE, board.get(5, 2)))
    factory.addPiece(Piece('P', Color.WHITE, board.get(6, 2)))
    factory.addPiece(Piece('P', Color.WHITE, board.get(7, 2)))
    factory.addPiece(Piece('P', Color.WHITE, board.get(8, 2)))

    factory.addPiece(Piece('R', Color.BLACK, board.get(1, 8)))
    factory.addPiece(Piece('N', Color.BLACK, board.get(2, 8)))
    factory.addPiece(Piece('B', Color.BLACK, board.get(3, 8)))
    factory.addPiece(Piece('Q', Color.BLACK, board.get(4, 8)))
    factory.addPiece(Piece('K', Color.BLACK, board.get(5, 8)))
    factory.addPiece(Piece('B', Color.BLACK, board.get(6, 8)))
    factory.addPiece(Piece('N', Color.BLACK, board.get(7, 8)))
    factory.addPiece(Piece('R', Color.BLACK, board.get(8, 8)))
    factory.addPiece(Piece('P', Color.BLACK, board.get(1, 7)))
    factory.addPiece(Piece('P', Color.BLACK, board.get(2, 7)))
    factory.addPiece(Piece('P', Color.BLACK, board.get(3, 7)))
    factory.addPiece(Piece('P', Color.BLACK, board.get(4, 7)))
    factory.addPiece(Piece('P', Color.BLACK, board.get(5, 7)))
    factory.addPiece(Piece('P', Color.BLACK, board.get(6, 7)))
    factory.addPiece(Piece('P', Color.BLACK, board.get(7, 7)))
    factory.addPiece(Piece('P', Color.BLACK, board.get(8, 7)))
  }
}

fun main(args: Array<String>) {
  GameApplication.launch(Game::class.java, args)
}