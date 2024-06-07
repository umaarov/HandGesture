package uz.umarov.handgesture.model

import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat
import uz.umarov.handgesture.model.enums.GestureCategory
import uz.umarov.handgesture.model.enums.GestureRecyclerViewItemColor

class GestureDetectOption(
    private val inactiveIcon: Drawable,
    val gestureCategory: GestureCategory,
) {

    private val activeIcon: Drawable = inactiveIcon.constantState?.newDrawable()?.mutate()!!

    init {
        DrawableCompat.setTint(this.activeIcon, GestureRecyclerViewItemColor.ACTIVE_COLOR.value)
        DrawableCompat.setTint(this.inactiveIcon, GestureRecyclerViewItemColor.INACTIVE_COLOR.value)
    }
}