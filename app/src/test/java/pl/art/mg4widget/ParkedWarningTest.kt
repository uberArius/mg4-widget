package pl.art.mg4widget

import org.junit.Assert.*
import org.junit.Test

class ParkedWarningTest {
    private fun state(vararg values: Pair<Field, Double>) = Snapshot(values.associate { it.first to Reading(it.second, 1234L) })
    @Test fun `off is required and unknown is not off`() {
        assertFalse(state(Field.LOCKED to 0.0).parkedWarning())
        assertFalse(state(Field.ENGINE to 1.0, Field.LOCKED to 0.0).parkedWarning())
        assertTrue(state(Field.ENGINE to 0.0, Field.LOCKED to 0.0).parkedWarning())
    }
    @Test fun `every tyre below threshold warns but boundary does not`() {
        listOf(Field.TYRE_FL, Field.TYRE_FR, Field.TYRE_RL, Field.TYRE_RR).forEach {
            assertTrue(state(Field.ENGINE to 0.0, it to 2.39).parkedWarning())
            assertFalse(state(Field.ENGINE to 0.0, it to 2.4).parkedWarning())
            assertFalse(state(Field.ENGINE to 0.0, it to 2.8).parkedWarning())
        }
    }
    @Test fun `every opening can trigger independently of other fields`() {
        listOf(Field.DOOR_DRIVER,Field.DOOR_PASSENGER,Field.DOOR_RL,Field.DOOR_RR,
            Field.WINDOW_DRIVER,Field.WINDOW_PASSENGER,Field.WINDOW_RL,Field.WINDOW_RR,
            Field.BONNET,Field.BOOT,Field.SUNROOF).forEach {
            assertTrue(it.name, state(Field.ENGINE to 0.0, it to 1.0).parkedWarning())
            assertFalse(it.name, state(Field.ENGINE to 0.0, it to 0.0).parkedWarning())
        }
    }
    @Test fun `missing fields and climate do not trigger warning`() {
        assertFalse(state(Field.ENGINE to 0.0).parkedWarning())
        assertFalse(state(Field.ENGINE to 0.0,Field.LOCKED to 1.0,Field.CLIMATE to 2.0).parkedWarning())
    }
    @Test fun `axle thresholds apply to both wheels and shared warning tile`() {
        listOf(Field.TYRE_FL, Field.TYRE_FR, Field.TYRE_RL, Field.TYRE_RR).forEach { tyre ->
            val front = tyre == Field.TYRE_FL || tyre == Field.TYRE_FR
            val boundary = if (front) 2.2 else 2.7
            val below = state(Field.ENGINE to 0.0, tyre to boundary - 0.01)
            assertTrue(below.parkedWarning(2.2, 2.7))
            assertTrue(below.tileCausesWarning(Field.TYRES, 2.2, 2.7))
            assertEquals(setOf(tyre), below.parkedWarningFields(2.2, 2.7))
            assertFalse(state(Field.ENGINE to 0.0, tyre to boundary).parkedWarning(2.2, 2.7))
        }
        val data = state(Field.ENGINE to 0.0, Field.TYRE_FL to 2.3)
        assertTrue(data.parkedWarning())
        assertFalse(data.parkedWarning(2.2, 2.7))
        assertFalse(state(Field.ENGINE to 1.0, Field.TYRE_RR to 2.0).parkedWarning(2.2, 2.7))
    }
}
