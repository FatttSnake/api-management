package fixture.plugin

import fixture.b.FixtureB

class FixturePlugin {
    fun value(): String = FixtureB().value() + "P"
}
