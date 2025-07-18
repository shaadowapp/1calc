

package com.shaadow.onecalculator

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preference")
data class PreferenceEntity(
    @PrimaryKey val key: String, // e.g. "default_screen"
    val value: String // e.g. "calculator"
) 