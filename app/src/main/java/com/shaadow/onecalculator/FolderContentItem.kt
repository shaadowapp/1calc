package com.shaadow.onecalculator

sealed class FolderContentItem {
    abstract val name: String
    abstract val path: String
    
    data class SubFolder(
        override val name: String,
        override val path: String,
        val itemCount: Int
    ) : FolderContentItem()
    
    data class MediaFile(
        override val name: String,
        override val path: String,
        val size: Long,
        val type: FileType
    ) : FolderContentItem()
}

enum class FileType {
    IMAGE, VIDEO, PDF, DOCUMENT, OTHER
}