package com.maddenabbott.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.maddenabbott.game.framework.ceil
import com.maddenabbott.game.framework.isEven
import com.maddenabbott.game.framework.isOdd
import ktx.app.KtxGame
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.graphics.rect
import ktx.graphics.use

class Piece(
  val letter: Char,
  val color: Color,
  val square: Square
)

class Square(
  val row: Int,
  val column: Int,
  val length: Float,
  val color: Color
) {
  // Game coordinates begin in lower left but application coordinates begin in upper left. Therefore rows must be
  // "flipped" whereas columns can stay the same.
  val position = Vector2((column - 1) * length, ((row - 1) * length))
}

class Board(
  columns: Int,
  rows: Int,
  private val height: Float,
  private val width: Float
) {

  val squares = (1..(rows * columns)).map {
    val column = (it / rows.toDouble()).ceil()
    val row = it - (rows * (column - 1))
    val color = if ((column.isEven() && row.isOdd()) || (column.isOdd() && row.isEven())) {
      Color.WHITE
    } else {
      Color.DARK_GRAY
    }

    Square(row, column, width / columns, color)
  }

  fun get(column: Int, row: Int) = squares.find { it.column == column && it.row == row }!!
}

class GameEntityFactory(
  private val font: BitmapFont,
  private val batch: Batch,
  private val shapeRenderer: ShapeRenderer
) {
  private val glyphLayout = GlyphLayout()

  fun addSquare(square: Square) {
    shapeRenderer.use(ShapeRenderer.ShapeType.Filled) {
      it.color = square.color
      it.rect(square.position, square.length, square.length)
    }

    if (square.row == 1) {
      batch.use {
        font.data.setScale(1f)
        glyphLayout.setText(font, square.column.toString())
        font.color = Color.BLACK
        font.draw(
          it,
          square.column.toString(),
          square.position.x + (square.length - glyphLayout.width) / 2,
          square.position.y + glyphLayout.height + (square.length * .05F)
        )
      }
    }

    if (square.column == 1) {
      batch.use {
        font.data.setScale(1f)
        glyphLayout.setText(font, square.row.toString())
        font.color = Color.BLACK
        font.draw(
          batch,
          square.row.toString(),
          square.position.x + (square.length * .05F),
          square.position.y + glyphLayout.height + (square.length - glyphLayout.height) / 2
        )
      }
    }
  }

  fun addPiece(piece: Piece) {
    val square = piece.square

    val length = square.length / 2
    val padding = length * .35F
    val borderRadius = length - padding

    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.color = Color.BLACK
    shapeRenderer.circle(
      piece.square.position.x + (square.length / 2),
      piece.square.position.y + (square.length / 2),
      borderRadius,
      borderRadius.toInt()
    )
    shapeRenderer.end()

    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.color = piece.color
    val innerRadius = borderRadius - (borderRadius * .1F)
    shapeRenderer.circle(
      piece.square.position.x + (square.length / 2),
      piece.square.position.y + (square.length / 2),
      innerRadius,
      innerRadius.toInt()
    )
    shapeRenderer.end()

    batch.begin()
    font.data.setScale(2f)
    glyphLayout.setText(font, piece.letter.toString())
    font.color = if (piece.color == Color.WHITE) Color.BLACK else Color.WHITE
    font.draw(
      batch,
      piece.letter.toString(),
      piece.square.position.x + (square.length - glyphLayout.width) / 2,
      piece.square.position.y + glyphLayout.height + (square.length - glyphLayout.height) / 2
    )
    batch.end()
  }
}

class GameScreen(height: Float, width: Float) : KtxScreen, KtxInputAdapter {
  private val board = Board(8, 8, height, width)
  private val factory = GameEntityFactory(BitmapFont(), SpriteBatch(), ShapeRenderer())

  init {
    Gdx.input.inputProcessor = this
  }

  override fun render(delta: Float) {
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

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    println("$screenX $screenY $pointer $button")
    return true
  }
}

class Game(
  private val height: Float,
  private val width: Float
) : KtxGame<Screen>() {
  override fun create() {
    addScreen(GameScreen(height, width))
    setScreen<GameScreen>()
  }
}

fun main() {
  val height = 800
  val width = 800

  val config = Lwjgl3ApplicationConfiguration()
  config.setWindowedMode(width, height)
  Lwjgl3Application(Game(height.toFloat(), width.toFloat()), config)
}