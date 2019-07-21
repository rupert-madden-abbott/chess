package com.maddenabbott.game.framework

import com.badlogic.gdx.math.Vector3
import kotlin.math.ceil

fun Int.isDivisibleBy(divisor: Int) = this % divisor == 0

fun Int.isEven() = isDivisibleBy(2)

fun Int.isOdd() = !isEven()

fun Double.ceil() = Math.ceil(this).toInt()

fun Vector3.set(x: Int, y: Int): Vector3 = set(x.toFloat(), y.toFloat(), 0F)

fun ceil(num: Float) = ceil(num.toDouble()).toInt()