package fixture.b

import fixture.a.FixtureA

class FixtureB {
    fun value(): String = FixtureA().value() + "B1"
}
