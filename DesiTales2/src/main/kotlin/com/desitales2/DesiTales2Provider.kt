package com.desitales2

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class DesiTales2Plugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DesiTales2Provider())
    }
}
