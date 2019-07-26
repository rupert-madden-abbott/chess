package com.maddenabbott.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.*
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
import kotlin.math.abs
import kotlin.math.sqrt

enum class Player(
  private val direction: Int,
  val textureY: Int
) {
  WHITE(1, 0),
  BLACK(-1, 1);

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

enum class PieceType(
  val textureX: Int,
  val isValidPath: (Square, Square, Player) -> Boolean
) {
  PAWN(5, { o, d, p -> isPawnForwards(o, d, p) || isPawnDiagonal(o, d, p) }),
  KNIGHT(3, { o, d, _ -> o.distanceTo(d) > 2 && o.distanceTo(d) < 2.5 }),
  BISHOP(2, { o, d, _ -> o.isDiagonalTo(d) }),
  ROOK(4, { o, d, _ -> o.isInlineWith(d) }),
  QUEEN(1, { o, d, _ -> o.isDiagonalTo(d) || o.isInlineWith(d) }),
  KING(0, { o, d, _ -> (o.isDiagonalTo(d) || o.isInlineWith(d)) && o.distanceTo(d) < 2 });
}

data class Piece(
  val type: PieceType,
  val owner: Player,
  val texture: Texture
) {
  val textureRegion = TextureRegion(texture, type.textureX * 107, owner.textureY * 107, 107, 107)
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
    sqrt((columnsApart(square) * columnsApart(square) + rowsApart(square) * rowsApart(square)).toDouble())

  private fun isInColumnWith(square: Square) = column == square.column

  fun isInlineWith(square: Square) = column == square.column || row == square.row

  fun isDiagonalTo(square: Square) = rowsApart(square) == columnsApart(square)

  fun isAheadOf(square: Square, player: Player) = isInColumnWith(square) && player.isAheadOf(square, this)

  private fun distance(firstCoordinate: Int, secondCoordinate: Int) = abs(firstCoordinate - secondCoordinate)

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
  width: Float,
  private val texture: Texture
) {
  private val squareWidth = width / columns

  var selectedSquare: Square? = null

  private var enPassantSquare: Square? = null

  private var enPassantPieceSquare: Square? = null

  val validMoveSquares = mutableListOf<Square>()

  private var currentPlayer = Player.WHITE

  var winningPlayer: Player? = null

  var pieces = mutableMapOf<Square, Piece>()

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

  init {
    reset()
  }

  private fun reset() {
    selectedSquare = null
    enPassantSquare = null
    enPassantPieceSquare = null
    currentPlayer = Player.WHITE
    winningPlayer = null
    pieces = mutableMapOf(
      get(1, 1) to Piece(PieceType.ROOK, Player.WHITE, texture),
      get(2, 1) to Piece(PieceType.KNIGHT, Player.WHITE, texture),
      get(3, 1) to Piece(PieceType.BISHOP, Player.WHITE, texture),
      get(4, 1) to Piece(PieceType.QUEEN, Player.WHITE, texture),
      get(5, 1) to Piece(PieceType.KING, Player.WHITE, texture),
      get(6, 1) to Piece(PieceType.BISHOP, Player.WHITE, texture),
      get(7, 1) to Piece(PieceType.KNIGHT, Player.WHITE, texture),
      get(8, 1) to Piece(PieceType.ROOK, Player.WHITE, texture),
      get(1, 2) to Piece(PieceType.PAWN, Player.WHITE, texture),
      get(2, 2) to Piece(PieceType.PAWN, Player.WHITE, texture),
      get(3, 2) to Piece(PieceType.PAWN, Player.WHITE, texture),
      get(4, 2) to Piece(PieceType.PAWN, Player.WHITE, texture),
      get(5, 2) to Piece(PieceType.PAWN, Player.WHITE, texture),
      get(6, 2) to Piece(PieceType.PAWN, Player.WHITE, texture),
      get(7, 2) to Piece(PieceType.PAWN, Player.WHITE, texture),
      get(8, 2) to Piece(PieceType.PAWN, Player.WHITE, texture),
      get(1, 8) to Piece(PieceType.ROOK, Player.BLACK, texture),
      get(2, 8) to Piece(PieceType.KNIGHT, Player.BLACK, texture),
      get(3, 8) to Piece(PieceType.BISHOP, Player.BLACK, texture),
      get(4, 8) to Piece(PieceType.QUEEN, Player.BLACK, texture),
      get(5, 8) to Piece(PieceType.KING, Player.BLACK, texture),
      get(6, 8) to Piece(PieceType.BISHOP, Player.BLACK, texture),
      get(7, 8) to Piece(PieceType.KNIGHT, Player.BLACK, texture),
      get(8, 8) to Piece(PieceType.ROOK, Player.BLACK, texture),
      get(1, 7) to Piece(PieceType.PAWN, Player.BLACK, texture),
      get(2, 7) to Piece(PieceType.PAWN, Player.BLACK, texture),
      get(3, 7) to Piece(PieceType.PAWN, Player.BLACK, texture),
      get(4, 7) to Piece(PieceType.PAWN, Player.BLACK, texture),
      get(5, 7) to Piece(PieceType.PAWN, Player.BLACK, texture),
      get(6, 7) to Piece(PieceType.PAWN, Player.BLACK, texture),
      get(7, 7) to Piece(PieceType.PAWN, Player.BLACK, texture),
      get(8, 7) to Piece(PieceType.PAWN, Player.BLACK, texture)
    )
  }

  fun get(column: Int, row: Int) = squares.find { it.column == column && it.row == row }!!

  fun touch(x: Float, y: Float) {
    if (winningPlayer != null) {
      reset()
      return
    }

    val touchedSquare = get(ceil(x / squareWidth), ceil(y / squareWidth))
    val selectedPiece = pieces[selectedSquare]

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
      if (selectedPiece.type == PieceType.PAWN && touchedSquare == enPassantSquare) {
        pieces.remove(enPassantPieceSquare)
      }

      if (selectedPiece.type == PieceType.PAWN && previousSelectedSquare != null
        && touchedSquare.rowsApart(previousSelectedSquare) == 2
      ) {
        enPassantSquare = squares.first { it.isBetween(touchedSquare, previousSelectedSquare) }
        enPassantPieceSquare = touchedSquare
      } else {
        enPassantSquare = null
        enPassantPieceSquare = null
      }

      pieces.remove(previousSelectedSquare)
      pieces[touchedSquare] = selectedPiece
      selectedSquare = null
      currentPlayer = currentPlayer.next()

      if(pieces.filter { it.value.owner == currentPlayer }
        .flatMap { (square, piece) -> calculateValidMoveSquares(square, piece, pieces) }
        .isEmpty()) {
        winningPlayer = currentPlayer.next()
      }
    }

    validMoveSquares.clear()
    val touchedPiece = pieces[touchedSquare]
    if (touchedPiece?.owner == currentPlayer && touchedSquare != previousSelectedSquare) {
      validMoveSquares.addAll(calculateValidMoveSquares(touchedSquare, touchedPiece, pieces))
    }
  }

  private fun calculateValidAttackSquares(
    location: Square,
    piece: Piece,
    pieceLocations: Map<Square, Piece>
  ): List<Square> {
    val pieceSquares = pieceLocations.keys

    return squares.asSequence()
      .filter { piece.type.isValidPath(location, it, piece.owner) }
      .filter { possibleSquare -> !pieceSquares.any { it.isBetween(possibleSquare, location) } }
      .filter { piece.type != PieceType.PAWN || it.isDiagonalTo(location) || enPassantSquare == it }
      .toList()
  }

  private fun calculateValidMoveSquares(
    location: Square,
    piece: Piece,
    pieces: Map<Square, Piece>
  ): List<Square> {
    val pieceSquares = pieces.keys
    val ownPieceSquares = pieces.filter { it.value.owner == piece.owner }.keys
    val opponentPieces = pieces.filter { it.value.owner != piece.owner }
    val opponentPieceSquares = opponentPieces.keys

    val kingLocation = pieces
      .filter { it.value.owner == piece.owner && it.value.type == PieceType.KING }
      .keys.first()

    return squares.asSequence()
      .filter { piece.type.isValidPath(location, it, piece.owner) }
      .filter { !ownPieceSquares.contains(it) }
      .filter { possibleSquare -> !pieceSquares.any { it.isBetween(possibleSquare, location) } }
      .filter {
        piece.type != PieceType.PAWN
            || it.isAheadOf(location, piece.owner) && !opponentPieceSquares.contains(it)
            || it.isDiagonalTo(location) && opponentPieceSquares.contains(it)
            || enPassantSquare == it
      }.filter {
        piece.type != PieceType.KING || !opponentPieces
          .flatMap { (square, piece) -> calculateValidAttackSquares(square, piece, pieces) }
          .distinct()
          .contains(it)
      }
      .filter {
        val nextPieces = pieces.toMutableMap()
        nextPieces.remove(location)
        nextPieces[it] = piece
        val nextOpponentPieces = nextPieces.filter { entry -> entry.value.owner != piece.owner }
        !nextOpponentPieces
          .flatMap { (square, piece) -> calculateValidAttackSquares(square, piece, nextPieces) }
          .distinct()
          .contains(if (piece.type == PieceType.KING) it else kingLocation)
      }
      .toList()
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
    batch.use {
      it.draw(
        piece.textureRegion,
        square.position.x + (square.length - piece.textureRegion.regionWidth) / 2,
        square.position.y + (square.length - piece.textureRegion.regionHeight) / 2
      )
    }
  }

  fun add(winningPlayer: Player, width: Float, height: Float) {
    val text = "Checkmate! $winningPlayer has won!"
    font.data.setScale(3f)
    glyphLayout.setText(font, text)

    val fontX = (width - glyphLayout.width) / 2
    val fontY = (height + glyphLayout.height) / 2
    val padding = 30

    batch.use {
      shapeRenderer.use(ShapeRenderer.ShapeType.Filled) {
        it.color = Color.WHITE
        it.rect(fontX - padding / 2, fontY - (padding / 2) - glyphLayout.height, glyphLayout.width + padding, glyphLayout.height + padding)
      }
    }

    batch.use {
      font.color = Color.BLACK
      font.draw(batch, text, fontX, fontY)
    }
  }
}

class GameScreen(
  private val width: Float,
  private val height: Float
) : KtxScreen, KtxInputAdapter {
  private val board = Board(8, 8, width, Texture(Gdx.files.internal("pieces.png")))
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
    board.pieces.forEach { (square, piece) -> factory.add(square, piece) }
    board.winningPlayer?.also { factory.add(it, width, height) }
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