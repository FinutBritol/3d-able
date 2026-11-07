package com.example.dbtest

import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class Slider(
    rootView: View,
    private val animDuration: Long = 300L
) {

    private val panelView: View = rootView.findViewById(R.id.buildingPanel)
    private val scrimView: View = rootView.findViewById(R.id.scrim)
    private val panelTitle: TextView = rootView.findViewById(R.id.panelBuildingName)
    private val closeButton: Button = rootView.findViewById(R.id.btn_close_panel)
    private val btnReserve: Button = rootView.findViewById(R.id.btn_reserve)


    private var panelWidthPx = 0

    var isOpen = false
        private set

    private var currentReservable = true

    var onOpen: (() -> Unit)? = null
    var onClose: (() -> Unit)? = null
    var onReserveClick: (() -> Unit)? = null

    init {
        panelView.post {
            panelWidthPx = panelView.width
            panelView.translationX = panelWidthPx.toFloat()
        }

        closeButton.setOnClickListener { close() }
        scrimView.setOnClickListener { close() }

        btnReserve.setOnClickListener {
            onReserveClick?.invoke()
        }
    }

    fun open(title: String, isReserved: Boolean = false, reservable: Boolean = true) {
        panelTitle.text = title
        currentReservable = reservable
        setReservationState(isReserved)
        isOpen = true

        scrimView.visibility = View.VISIBLE
        scrimView.alpha = 0f
        scrimView.animate().alpha(1f).setDuration(animDuration).start()
        panelView.animate().translationX(0f).setDuration(animDuration).start()

        onOpen?.invoke()
    }

    fun close() {
        isOpen = false

        panelView.animate()
            .translationX(panelWidthPx.toFloat())
            .setDuration(animDuration)
            .start()
        scrimView.animate()
            .alpha(0f)
            .setDuration(animDuration)
            .withEndAction { scrimView.visibility = View.GONE }
            .start()

        onClose?.invoke()
    }

    fun setReservationState(isReserved: Boolean) {
        btnReserve.visibility = if (currentReservable) View.VISIBLE else View.GONE
    }
}