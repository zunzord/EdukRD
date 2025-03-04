package com.edukrd.app.models

data class StoreItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val price: Int = 0,
    val available: Boolean = true,
    val stock: Int = 0
)
