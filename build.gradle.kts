plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // NO HAY NADA DE COMPOSE AQUÍ
    alias(libs.plugins.google.gms.google.services) apply false
}