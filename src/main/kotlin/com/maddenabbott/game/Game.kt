package com.maddenabbott.game

import com.almasb.fxgl.app.GameApplication
import com.almasb.fxgl.app.GameSettings
import com.almasb.fxgl.core.math.Vec2
import com.almasb.fxgl.dsl.entityBuilder
import com.almasb.fxgl.dsl.getGameWorld
import com.almasb.fxgl.entity.Entity
import com.almasb.fxgl.entity.EntityFactory
import com.almasb.fxgl.entity.SpawnData
import com.almasb.fxgl.entity.Spawns
import com.maddenabbott.game.framework.ceil
import com.maddenabbott.game.framework.isEven
import com.maddenabbott.game.framework.isOdd
import com.maddenabbott.game.framework.ratio
import javafx.geometry.Dimension2D
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

class Square(
  val point: Vec2,
  val color: Color
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

    Square(Vec2(column.toDouble(), row.toDouble()), color)
  }
}

enum class EntityType {
  SQUARE
}

class GameEntityFactory(private val size: Dimension2D) : EntityFactory {
  @Spawns("SQUARE")
  fun newSqaure(data: SpawnData): Entity {
    return entityBuilder()
      .from(data)
      .view(Rectangle(data.get("squareLength"), data.get("squareLength"), data.get("color")))
      .build()
  }
}

class Game : GameApplication() {
  private val size = Dimension2D(800.0, 800.0)

  override fun initSettings(settings: GameSettings) {
    settings.width = size.width.toInt()
    settings.height = size.height.toInt()
    settings.isProfilingEnabled = true
  }

  override fun initGame() {
    getGameWorld().addEntityFactory(GameEntityFactory(size))

    val board = Board(8, 8)

    board.squares.forEach { square ->
      val squareLength = if (size.ratio() > board.size.ratio()) {
        size.width / board.size.width
      } else {
        size.height / board.size.height
      }

      val point = Vec2((square.point.x - 1) * squareLength, (square.point.y - 1) * squareLength)

      getGameWorld().spawn(EntityType.SQUARE.name, SpawnData(point.toPoint2D()).put("squareLength", squareLength)
        .put("color", square.color))
    }
  }
}

fun main(args: Array<String>) {
  GameApplication.launch(Game::class.java, args)
}