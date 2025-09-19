package com.unciv.logic.battle

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.map.unit.MapUnit
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.worldscreen.WorldScreen
import com.unciv.ui.components.extensions.addSeparator
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.widgets.ExtensibleWindow

object BattleDefenderSelector {
    fun show(attacker: MapUnit, defenders: List<MapUnit>): MapUnit {
        var chosen: MapUnit? = null

        val screen = BaseScreen.current as? WorldScreen
            ?: throw IllegalStateException("WorldScreen not active")

        val window = ExtensibleWindow("Choose Defender", screen)
        val table = Table()

        table.add("Attacker: ${attacker.baseUnit.name}".toLabel()).row()
        table.addSeparator()

        for (unit in defenders) {
            val btn = unit.baseUnit.name.toLabel()
            btn.setFontScale(1.2f)
            btn.setOnClickListener {
                chosen = unit
                window.remove()
            }
            table.add(btn).row()
        }

        window.add(table).row()
        screen.addActor(window)

        // 간단 블로킹 (임시) – 실제로는 코루틴/콜백으로 바꾸는게 더 적절함
        while (chosen == null) {
            Thread.sleep(50)
        }

        return chosen!!
    }
}
