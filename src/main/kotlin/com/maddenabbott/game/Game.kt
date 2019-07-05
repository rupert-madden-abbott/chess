package com.maddenabbott.game

import com.almasb.fxgl.app.GameApplication
import com.almasb.fxgl.app.GameSettings
import com.almasb.fxgl.core.math.Vec2
import com.almasb.fxgl.dsl.entityBuilder
import com.almasb.fxgl.entity.EntityFactory
import com.maddenabbott.game.framework.ceil
import com.maddenabbott.game.framework.isEven
import com.maddenabbott.game.framework.isOdd
import com.maddenabbott.game.framework.ratio
import javafx.geometry.Dimension2D
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

class Square(
  val point: Vec2,
  val color: Color,
  val parent: Board
)

class Board(
  columns: Int,
  rows: Int
) {
  val size = Dimension2D(columns.toDouble(), rows.toDouble())

  val squares = (1..(rows * columns)).map {
    val row = (it / rows.toDouble()).ceil()
    val column = it - (rows * (row - 1))
    val color = if ((column.isEven() && row.isOdd()) || (column.isOdd() && row.isEven())) {
      Color.BLACK
    } else {
      Color.WHITE
    }

    Square(Vec2(column.toDouble(), row.toDouble()), color, this)
  }
}

class GameEntityFactory(private val size: Dimension2D) : EntityFactory {
  fun addSquare(square: Square) {
    val squareLength = if (size.ratio() > square.parent.size.ratio()) {
      size.width / square.parent.size.width
    } else {
      size.height / square.parent.size.height
    }

    val point = Vec2((square.point.x - 1) * squareLength, (square.point.y - 1) * squareLength)

    entityBuilder()
      .at(point)
      .view(Rectangle(squareLength, squareLength, square.color))
      .buildAndAttach()
  }
}

class Game : GameApplication() {
  private val size = Dimension2D(800.0, 800.0)

  private val gameEntityFactory = GameEntityFactory(size)

  override fun initSettings(settings: GameSettings) {
    settings.width = size.width.toInt()
    settings.height = size.height.toInt()
    settings.isProfilingEnabled = true
  }

  override fun initGame() {
    Board(8, 8).squares.forEach(gameEntityFactory::addSquare)
  }
}

fun main(args: Array<String>) {
  GameApplication.launch(Game::class.java, args)
}