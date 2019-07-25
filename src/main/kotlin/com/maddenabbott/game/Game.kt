package com.maddenabbott.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.maddenabbott.game.framework.ceil
import com.maddenabbott.game.framework.isEven
import com.maddenabbott.game.framework.isOdd
import com.maddenabbott.game.framework.set
import ktx.app.KtxGame
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.graphics.rect
import ktx.graphics.use

enum class Player(
  val color: Color,
  private val direction: Int
) {
  WHITE(Color.WHITE, 1),
  BLACK(Color.BLACK, -1);

  fun next() = if (this == WHITE) BLACK else WHITE

  fun isAheadOf(origin: Square, destination: Square): Boolean {
    val vector = destination.row - origin.row
    return vector > 0 && direction > 0 || vector < 1 && direction < 1
  }
}

fun isPawnForwards(
  o: Square,
  d: Square,
  p: Player
) = d.isAheadOf(o, p) && (
    o.rowsApart(d) == 1
        || o.rowsApart(d) == 2 && ((o.row == 2 && p == Player.WHITE) || (o.row == 7 && p == Player.BLACK))
    )

fun isPawnDiagonal(o: Square, d: Square, p: Player) = p.isAheadOf(o, d) && o.rowsApart(d) == 1 && o.isDiagonalTo(d)

enum class PieceType(val letter: Char, val isValidPath: (Square, Square, Player) -> Boolean) {
  PAWN('P', { o, d, p -> isPawnForwards(o, d, p) || isPawnDiagonal(o, d, p) }),
  KNIGHT('N', { o, d, _ -> o.distanceTo(d) > 2 && o.distanceTo(d) < 2.5 }),
  BISHOP('B', { o, d, _ -> o.isDiagonalTo(d) }),
  ROOK('R', { o, d, _ -> o.isInlineWith(d) }),
  QUEEN('Q', { o, d, _ -> o.isDiagonalTo(d) || o.isInlineWith(d) }),
  KING('K', { o, d, _ -> (o.isDiagonalTo(d) || o.isInlineWith(d)) && o.distanceTo(d) < 2 });
}

data class Piece(
  val type: PieceType,
  val owner: Player
) {
  val color = owner.color
  val letter = type.letter
}

data class Square(
  val row: Int,
  val column: Int,
  val length: Float,
  val color: Color
) {
  // Game coordinates begin in lower left but application coordinates begin in upper left. Therefore rows must be
  // "flipped" whereas columns can stay the same.
  val position = Vector2((column - 1) * length, ((row - 1) * length))

  private fun columnsApart(square: Square) = distance(this.column, square.column)

  fun rowsApart(square: Square) = distance(this.row, square.row)

  fun distanceTo(square: Square) =
    Math.sqrt((columnsApart(square) * columnsApart(square) + rowsApart(square) * rowsApart(square)).toDouble())

  private fun isInColumnWith(square: Square) = column == square.column

  fun isInlineWith(square: Square) = column == square.column || row == square.row

  fun isDiagonalTo(square: Square) = rowsApart(square) == columnsApart(square)

  fun isAheadOf(square: Square, player: Player) = isInColumnWith(square) && player.isAheadOf(square, this)

  private fun distance(firstCoordinate: Int, secondCoordinate: Int) = Math.abs(firstCoordinate - secondCoordinate)

  fun isBetween(first: Square, second: Square) = this != first && this != second
      && (
      isInlineWith(first) && isInlineWith(second) && first.isInlineWith(second)
          || isDiagonalTo(first) && isDiagonalTo(second) && first.isDiagonalTo(second)
      )
      && (first.row < row && row < second.row || first.row > row && row > second.row
      || first.column < column && column < second.column || first.column > column && column > second.column)

}

class Board(
  columns: Int,
  rows: Int,
  width: Float
) {
  private val squareWidth = width / columns

  var selectedSquare: Square? = null

  val validMoveSquares = mutableListOf<Square>()

  private var currentPlayer = Player.WHITE

  val squares = (1..(rows * columns)).map {
    val column = (it / rows.toDouble()).ceil()
    val row = it - (rows * (column - 1))
    val color = if ((column.isEven() && row.isOdd()) || (column.isOdd() && row.isEven())) {
      Color.WHITE
    } else {
      Color.DARK_GRAY
    }

    Square(row, column, squareWidth, color)
  }

  val pieceLocations = mutableMapOf(
    get(1, 1) to Piece(PieceType.ROOK, Player.WHITE),
    get(2, 1) to Piece(PieceType.KNIGHT, Player.WHITE),
    get(3, 1) to Piece(PieceType.BISHOP, Player.WHITE),
    get(4, 1) to Piece(PieceType.QUEEN, Player.WHITE),
    get(5, 1) to Piece(PieceType.KING, Player.WHITE),
    get(6, 1) to Piece(PieceType.BISHOP, Player.WHITE),
    get(7, 1) to Piece(PieceType.KNIGHT, Player.WHITE),
    get(8, 1) to Piece(PieceType.ROOK, Player.WHITE),
    get(1, 2) to Piece(PieceType.PAWN, Player.WHITE),
    get(2, 2) to Piece(PieceType.PAWN, Player.WHITE),
    get(3, 2) to Piece(PieceType.PAWN, Player.WHITE),
    get(4, 2) to Piece(PieceType.PAWN, Player.WHITE),
    get(5, 2) to Piece(PieceType.PAWN, Player.WHITE),
    get(6, 2) to Piece(PieceType.PAWN, Player.WHITE),
    get(7, 2) to Piece(PieceType.PAWN, Player.WHITE),
    get(8, 2) to Piece(PieceType.PAWN, Player.WHITE),
    get(1, 8) to Piece(PieceType.ROOK, Player.BLACK),
    get(2, 8) to Piece(PieceType.KNIGHT, Player.BLACK),
    get(3, 8) to Piece(PieceType.BISHOP, Player.BLACK),
    get(4, 8) to Piece(PieceType.QUEEN, Player.BLACK),
    get(5, 8) to Piece(PieceType.KING, Player.BLACK),
    get(6, 8) to Piece(PieceType.BISHOP, Player.BLACK),
    get(7, 8) to Piece(PieceType.KNIGHT, Player.BLACK),
    get(8, 8) to Piece(PieceType.ROOK, Player.BLACK),
    get(1, 7) to Piece(PieceType.PAWN, Player.BLACK),
    get(2, 7) to Piece(PieceType.PAWN, Player.BLACK),
    get(3, 7) to Piece(PieceType.PAWN, Player.BLACK),
    get(4, 7) to Piece(PieceType.PAWN, Player.BLACK),
    get(5, 7) to Piece(PieceType.PAWN, Player.BLACK),
    get(6, 7) to Piece(PieceType.PAWN, Player.BLACK),
    get(7, 7) to Piece(PieceType.PAWN, Player.BLACK),
    get(8, 7) to Piece(PieceType.PAWN, Player.BLACK)
  )

  fun get(column: Int, row: Int) = squares.find { it.column == column && it.row == row }!!

  fun touch(x: Float, y: Float) {
    val touchedSquare = get(ceil(x / squareWidth), ceil(y / squareWidth))
    val selectedPiece = pieceLocations[selectedSquare]

    val previousSelectedSquare = selectedSquare

    if (touchedSquare == previousSelectedSquare) {
      //Deselecting a square
      selectedSquare = null
    } else if (
      selectedPiece == null
      || selectedPiece.owner != currentPlayer
      || !validMoveSquares.contains(touchedSquare)
    ) {
      //Selecting a new square
      selectedSquare = touchedSquare
    } else {
      //Moving a piece
      pieceLocations.remove(previousSelectedSquare)
      pieceLocations[touchedSquare] = selectedPiece
      selectedSquare = null
      currentPlayer = currentPlayer.next()
    }

    validMoveSquares.clear()
    val touchedPiece = pieceLocations[touchedSquare]
    if (touchedPiece?.owner == currentPlayer && touchedSquare != previousSelectedSquare) {
      validMoveSquares.addAll(calculateValidMoveSquares(touchedPiece, touchedSquare))
    }
  }

  private fun calculateValidMoveSquares(piece: Piece, location: Square): List<Square> {
    val ownPieceSquares = pieceLocations.keys.filter { pieceLocations[it]?.owner == piece.owner }
    val otherPieceSquares = pieceLocations.keys.filter { pieceLocations[it]?.owner != piece.owner }

    return squares.asSequence()
      .filter { piece.type.isValidPath(location, it, piece.owner) }
      .filter { !ownPieceSquares.contains(it) }
      .filter { possibleSquare -> !ownPieceSquares.any { it.isBetween(possibleSquare, location) } }
      .filter { possibleSquare -> !otherPieceSquares.any { it.isBetween(possibleSquare, location) } }
      .filter {
        piece.type != PieceType.PAWN
            || it.isAheadOf(location, piece.owner) && !otherPieceSquares.contains(it)
            || it.isDiagonalTo(location) && otherPieceSquares.contains(it)
      }.toList()
  }
}

class GameEntityFactory(
  private val font: BitmapFont,
  private val batch: Batch,
  private val shapeRenderer: ShapeRenderer
) {
  private val glyphLayout = GlyphLayout()

  fun add(board: Board, square: Square) {
    shapeRenderer.use(ShapeRenderer.ShapeType.Filled) {
      it.color = when {
        board.selectedSquare == square -> Color.RED
        board.validMoveSquares.contains(square) -> Color.BLUE
        else -> square.color
      }
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

  fun add(square: Square, piece: Piece) {
    val length = square.length / 2
    val padding = length * .35F
    val borderRadius = length - padding

    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.color = Color.BLACK
    shapeRenderer.circle(
      square.position.x + (square.length / 2),
      square.position.y + (square.length / 2),
      borderRadius,
      borderRadius.toInt()
    )
    shapeRenderer.end()

    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.color = piece.color
    val innerRadius = borderRadius - (borderRadius * .1F)
    shapeRenderer.circle(
      square.position.x + (square.length / 2),
      square.position.y + (square.length / 2),
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
      square.position.x + (square.length - glyphLayout.width) / 2,
      square.position.y + glyphLayout.height + (square.length - glyphLayout.height) / 2
    )
    batch.end()
  }
}

class GameScreen(width: Float, height: Float) : KtxScreen, KtxInputAdapter {
  private val board = Board(8, 8, width)
  private val factory = GameEntityFactory(BitmapFont(), SpriteBatch(), ShapeRenderer())
  private val camera = OrthographicCamera(width, height)
  private val touchPosition = Vector3()

  init {
    Gdx.input.inputProcessor = this
    camera.setToOrtho(false)
  }

  override fun render(delta: Float) {
    camera.update()
    board.squares.forEach { square -> factory.add(board, square) }
    board.pieceLocations.forEach { (square, piece) -> factory.add(square, piece) }
  }

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    camera.unproject(touchPosition.set(screenX, screenY))
    board.touch(touchPosition.x, touchPosition.y)
    return true
  }
}

class Game(
  private val width: Float,
  private val height: Float
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