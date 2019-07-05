package com.maddenabbott.game.framework

interface Renderer<T, D> {
  fun render(render: (T, D) -> Unit, renders: Long, updates: Long, runtime: Double)
}