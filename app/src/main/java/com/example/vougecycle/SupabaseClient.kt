package com.example.vougecycle

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime

object Supabase {
    private const val SUPABASE_URL = "https://bcvaoqsigxmymwnptthv.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_BNAmJqd179c6Cj8wBUpr4w_2teLDtjT"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }
}
