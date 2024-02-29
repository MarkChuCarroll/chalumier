package org.goodmath.demakeink.geom

interface MathBridge<Left, Right, Product> {

    val leftMath: Math<Left>
    val rightMath: Math<Right>
    val prodMath: Math<Product>
    fun plus(one: Left, two: Right): Product

    fun plus(one: Right, two: Left): Product {
        return plus(two, one)
    }
    fun minus(one: Left, two: Right): Product = this.plus(one, this.neg(two))

    fun minus(one: Right, two: Left): Product = this.plus(one, this.neg(two))

    fun neg(t: Left): Left

    fun neg(u: Right): Right

    fun times(one: Left, two: Right): Product

    fun times(one: Right, two: Left): Product = this.times(two, one)


    fun div(one: Left, two: Right): Product = this.times(one, this.recip(two))

    fun div(one: Right, two: Left): Product = this.times(one, this.recip(two))

    fun recip(t: Left): Left

    fun recip(u: Right): Right

    // Invert is just a syntactic convenience for when you have a MathBridge<X, Y, Z>,
    // but you need a mathBridge<Y, X, Z>.
    fun invert(): MathBridge<Right, Left, Product> {
        val me = this
        return object: MathBridge<Right, Left, Product> {
            override val leftMath: Math<Right> = me.rightMath
            override val rightMath: Math<Left> = me.leftMath

            override val prodMath: Math<Product> = me.prodMath

            override fun plus(one: Right, two: Left): Product = me.plus(one, two)

            override fun neg(t: Right): Right  = me.neg(t)
            override fun times(one: Right, two: Left): Product = me.times(one, two)

            override fun recip(u: Left): Left  = me.recip(u)

            override fun recip(t: Right): Right  = me.recip(t)

            override fun neg(u: Left): Left = me.neg(u)

            override fun invert(): MathBridge<Left, Right, Product> {
                return me
            }
        }
    }
}

fun<T> doubleBridge(math: Math<T>): MathBridge<T, Double, T> = object: MathBridge<T, Double, T> {
    override val leftMath: Math<T> = math

    override val rightMath: Math<Double> = DoubleMath

    override val prodMath: Math<T> = math

    override fun recip(u: Double): Double {
        return 1.0 / u
    }

    override fun recip(t: T): T {
        return math.reciprocal(t)
    }

    override fun times(one: T, two: Double): T {
        return math.times(one, two)
    }

    override fun neg(u: Double): Double {
        return -u
    }

    override fun neg(t: T): T {
        return math.neg(t)
    }

    override fun plus(one: T, two: Double): T {
        throw Exception("Fuck this nonsense")
    }


}
