package pl.art.mg4widget

import org.junit.Assert.*
import org.junit.Test

class DashboardSelectionTest {
    @Test fun `selection prepends removes and does not duplicate`() {
        val original = listOf(Field.SOC, Field.RANGE)
        assertEquals(listOf(Field.TYRES,Field.SOC,Field.RANGE),Tiles.toggled(original,Field.TYRES))
        assertEquals(listOf(Field.RANGE),Tiles.toggled(original,Field.SOC))
        assertEquals(emptyList<Field>(),Tiles.toggled(listOf(Field.SOC),Field.SOC))
    }
    @Test fun `drag moves in both directions and unselected destination means end`() {
        val original = listOf(Field.SOC,Field.RANGE,Field.TYRES)
        assertEquals(listOf(Field.RANGE,Field.TYRES,Field.SOC),Tiles.moved(original,Field.SOC,Field.TYRES))
        assertEquals(listOf(Field.TYRES,Field.SOC,Field.RANGE),Tiles.moved(original,Field.TYRES,Field.SOC))
        assertEquals(listOf(Field.RANGE,Field.TYRES,Field.SOC),Tiles.moved(original,Field.SOC,Field.DOORS))
        assertEquals(original,Tiles.moved(original,Field.DOORS,Field.SOC))
        assertEquals(original,Tiles.moved(original,Field.SOC,Field.SOC))
    }
    @Test fun `alarm identifies the responsible grouped tile only`() {
        val snapshot=Snapshot(mapOf(Field.ENGINE to Reading(0.0,1),Field.TYRE_FR to Reading(2.38,1),Field.DOOR_RL to Reading(1.0,1)))
        assertEquals(setOf(Field.TYRE_FR,Field.DOOR_RL),snapshot.parkedWarningFields())
        assertTrue(snapshot.tileCausesWarning(Field.TYRES))
        assertTrue(snapshot.tileCausesWarning(Field.DOORS))
        assertFalse(snapshot.tileCausesWarning(Field.WINDOWS))
        assertFalse(snapshot.tileCausesWarning(Field.SOC))
    }
    @Test fun `pressure display preserves hundredths near alarm boundary`() {
        val snapshot=Snapshot(mapOf(Field.TYRE_FL to Reading(2.38,1),Field.TYRE_FR to Reading(2.4,1)))
        assertEquals("2,38",Tiles.cell(snapshot,Field.TYRE_FL))
        assertEquals("2,40",Tiles.cell(snapshot,Field.TYRE_FR))
        assertEquals("2,38 bar",snapshot.value(Field.TYRE_FL))
    }
}
