package com.beiying.matrix.plugin.compat
import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.SigningConfig

class AgpCompat {

    companion object {
        @JvmField
        val getIntermediatesSymbolDirName = {
            when {
                VersionsCompat.lessThan(AGPVersion.AGP_3_6_0) -> "symbols"
                VersionsCompat.greatThanOrEqual(AGPVersion.AGP_3_6_0) -> "runtime_symbol_list"
                else -> "symbols"
            }
        }

        fun getSigningConfig(variant: BaseVariant): SigningConfig? {
            return variant.buildType.signingConfig
        }
    }

}