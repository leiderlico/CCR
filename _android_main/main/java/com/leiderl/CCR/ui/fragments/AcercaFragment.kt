package com.leiderl.CCR.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import com.leiderl.CCR.R
import com.leiderl.CCR.ui.MainActivity

class AcercaFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_acerca, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = "Acerca de CCR"
        ajustarPaddingBottomNav(view)
    }

    private fun ajustarPaddingBottomNav(view: View) {
        val bottomNav = (activity as? MainActivity)
            ?.findViewById<View>(R.id.bottomNav) ?: return
        bottomNav.post {
            val alto = bottomNav.height
            if (alto == 0) return@post
            val scroll = view.findViewById<ScrollView>(R.id.scrollAcerca) ?: return@post
            scroll.setPadding(scroll.paddingLeft, scroll.paddingTop, scroll.paddingRight, alto)
        }
    }
}