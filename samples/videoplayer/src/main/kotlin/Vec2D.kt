
data class Vec2D(val x: Int, val y: Int) {
    operator fun minus(other: Vec2D) = Vec2D(x - other.x, y - other.y)
    operator fun div(b: Int) = Vec2D(x / b, y / b)
}
