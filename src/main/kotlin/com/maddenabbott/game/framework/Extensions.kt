package com.maddenabbott.game.framework

fun Int.isDivisibleBy(divisor: Int) = this % divisor == 0

fun Int.isEven() = isDivisibleBy(2)

fun Int.isOdd() = !isEven()

fun Double.ceil() = Math.ceil(this).toInt()